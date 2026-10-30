package com.csf.xline.tcp;

import com.csf.xline.core.XLineContext;
import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.autoconfigure.AbstractNodeAutoConfiguration;
import com.csf.xline.core.concurrent.ThreadFactoryProvider;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.tcp.codec.LineFrameDecoder;
import com.csf.xline.tcp.codec.TcpFrameDecoder;
import com.csf.xline.tcp.handler.TcpDataHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * TCP 节点装配。
 *
 * <p>仅处理配置为 {@link NodeType#TCP} 的节点，装配流程由父类统一完成。
 */
@Configuration
@ConditionalOnClass(TcpNode.class)
public class TcpNodeAutoConfiguration extends AbstractNodeAutoConfiguration<TcpNode, TcpDataHandler> {

    private final ThreadFactoryProvider threadFactoryProvider;

    private final ApplicationContext applicationContext;

    public TcpNodeAutoConfiguration(XLineProperties xLineProperties,
                                    XLineContext xLineContext,
                                    ThreadFactoryProvider threadFactoryProvider,
                                    ApplicationContext applicationContext) {
        super(xLineProperties, xLineContext);
        this.threadFactoryProvider = threadFactoryProvider;
        this.applicationContext = applicationContext;
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
     * 初始化前注入运行期依赖：线程工厂与帧解码器。
     *
     * @param node 待初始化的节点
     */
    @Override
    protected void beforeInit(TcpNode node) {
        node.setThreadFactory(threadFactoryProvider);
        node.setDecoder(resolveDecoder(node.getName()));
    }

    /**
     * 解析节点使用的帧解码器：优先按 Bean 名与节点名匹配，
     * 单实现时所有节点共用，无实现时沿用 {@link LineFrameDecoder}。
     *
     * @param nodeName 节点名
     * @return 帧解码器
     */
    private TcpFrameDecoder resolveDecoder(String nodeName) {
        Map<String, TcpFrameDecoder> decoders = applicationContext.getBeansOfType(TcpFrameDecoder.class);
        TcpFrameDecoder matched = resolveBean(decoders, nodeName);
        return matched == null ? new LineFrameDecoder() : matched;
    }
}
