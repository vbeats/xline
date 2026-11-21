package com.csf.xline.core.eventbus;

import com.csf.xline.core.INode;
import com.csf.xline.core.XLineContext;
import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 事件处理器（总线唯一消费者）。
 *
 * <p>收到事件后按 {@code xline.struct} 解析下游节点，把数据交给 {@link XLineDispatcher}
 * 异步处理，自身不执行任何业务代码：处理器里再推送数据（多级链路）也不会与总线互相等待。
 *
 * <p>处理完立即清空事件槽位，避免环形缓冲长期持有已处理的数据。
 */
@Slf4j
public class XLineEventHandler implements EventHandler<XLineEvent> {

    private final XLineContext context;
    private final XLineDispatcher dispatcher;

    public XLineEventHandler(XLineContext context, XLineDispatcher dispatcher) {
        this.context = context;
        this.dispatcher = dispatcher;
    }

    @Override
    public void onEvent(XLineEvent event, long sequence, boolean endOfBatch) {
        try {
            route(event);
        } finally {
            event.setKey(null);
            event.setPayload(null);
        }
    }

    private void route(XLineEvent event) {
        if (event.getKey() == null) {
            return;
        }
        List<String> nextNodes = context.nextNodes(event.getKey());
        for (String nodeName : nextNodes) {
            INode node = XLineContext.NODES.get(nodeName);
            if (node == null) {
                log.debug("下游节点 [{}] 尚未初始化，数据未推送", nodeName);
                continue;
            }
            dispatcher.dispatch(node, event.getPayload());
        }
    }
}
