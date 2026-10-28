package com.csf.xline.tcp;

import com.csf.xline.core.XLineContext;
import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.autoconfigure.AbstractNodeAutoConfiguration;
import com.csf.xline.core.concurrent.ThreadFactoryProvider;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.tcp.handler.TcpDataHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * TCP 节点装配。
 *
 * <p>仅处理配置为 {@link NodeType#TCP} 的节点，装配流程由父类统一完成。
 */
@Configuration
@ConditionalOnClass(TcpNode.class)
public class TcpNodeAutoConfiguration extends AbstractNodeAutoConfiguration<TcpNode, TcpDataHandler> {

    private final ThreadFactoryProvider threadFactoryProvider;

    public TcpNodeAutoConfiguration(XLineProperties xLineProperties,
                                    XLineContext xLineContext,
                                    ThreadFactoryProvider threadFactoryProvider) {
        super(xLineProperties, xLineContext);
        this.threadFactoryProvider = threadFactoryProvider;
    }

    @Override
    protected NodeType nodeType() {
        return NodeType.TCP;
    }

    @Override
    protected Class<TcpNode> nodeClass() {
        return TcpNode.class;
    }

    @Override
    protected Class<TcpDataHandler> handlerClass() {
        return TcpDataHandler.class;
    }

    /**
     * 初始化前注入线程工厂：节点的接收 / 连接线程按其创建。
     *
     * @param node 待初始化的节点
     */
    @Override
    protected void beforeInit(TcpNode node) {
        node.setThreadFactory(threadFactoryProvider);
    }
}
