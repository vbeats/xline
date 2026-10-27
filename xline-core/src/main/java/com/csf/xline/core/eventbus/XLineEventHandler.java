package com.csf.xline.core.eventbus;

import com.csf.xline.core.INode;
import com.csf.xline.core.XLineContext;
import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 事件处理器。
 *
 * <p>收到事件后，按 {@code xline.struct} 解析当前节点的下游节点，
 * 并把数据逐个推送给下游节点，实现「上一层 → 下一层」的单向数据流。
 */
@Slf4j
public class XLineEventHandler implements EventHandler<XLineEvent> {

    private final XLineContext context;

    public XLineEventHandler(XLineContext context) {
        this.context = context;
    }

    @Override
    public void onEvent(XLineEvent event, long sequence, boolean endOfBatch) {
        if (event == null || event.getKey() == null) {
            return;
        }
        List<String> nextNodes = context.nextNodes(event.getKey());
        if (nextNodes == null || nextNodes.isEmpty()) {
            return;
        }
        for (String nodeName : nextNodes) {
            INode node = XLineContext.NODES.get(nodeName);
            if (node == null) {
                log.debug("下游节点 [{}] 尚未初始化，数据未推送", nodeName);
                continue;
            }
            try {
                node.receiveData(event.getPayload());
            } catch (Exception e) {
                log.error("节点 [{}] 向下游 [{}] 推送数据失败", event.getKey(), nodeName, e);
            }
        }
    }
}
