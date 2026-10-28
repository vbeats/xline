package com.csf.xline.serialport;

import com.csf.xline.core.XLineContext;
import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.autoconfigure.AbstractNodeAutoConfiguration;
import com.csf.xline.core.concurrent.ThreadFactoryProvider;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.serialport.handler.SerialPortDataHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * 串口节点装配。
 *
 * <p>仅处理配置为 {@link NodeType#SERIAL_PORT} 的节点，装配流程由父类统一完成。
 */
@Configuration
@ConditionalOnClass(SerialPortNode.class)
public class SerialPortNodeAutoConfiguration extends AbstractNodeAutoConfiguration<SerialPortNode, SerialPortDataHandler> {

    private final ThreadFactoryProvider threadFactoryProvider;

    private final XLineContext xLineContext;

    public SerialPortNodeAutoConfiguration(XLineProperties xLineProperties,
                                           ThreadFactoryProvider threadFactoryProvider,
                                           XLineContext xLineContext) {
        super(xLineProperties);
        this.threadFactoryProvider = threadFactoryProvider;
        this.xLineContext = xLineContext;
    }

    @Override
    protected NodeType nodeType() {
        return NodeType.SERIAL_PORT;
    }

    @Override
    protected Class<SerialPortNode> nodeClass() {
        return SerialPortNode.class;
    }

    @Override
    protected Class<SerialPortDataHandler> handlerClass() {
        return SerialPortDataHandler.class;
    }

    /**
     * 初始化前注入上下文与线程工厂：
     * 上下文用于把读到的数据推向下游，线程工厂用于创建串口重开线程。
     *
     * @param node 待初始化的节点
     */
    @Override
    protected void beforeInit(SerialPortNode node) {
        node.setContext(xLineContext);
        node.setThreadFactory(threadFactoryProvider);
    }
}
