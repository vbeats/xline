package com.csf.xline.serialport.handler;

import com.csf.xline.core.handler.DataHandler;
import com.csf.xline.serialport.SerialPortNode;

/**
 * 串口节点数据处理契约。
 *
 * <p>由业务侧实现并注册为 Spring Bean：
 * 容器中有多个实现时按 Bean 名与节点名匹配，只有一个实现时所有节点共用。
 *
 * <p>不注册任何实现时，节点按「透明转发」处理：
 * 读到的字节直接推给下游，上游来的数据直接写入串口。
 */
public interface SerialPortDataHandler extends DataHandler<SerialPortNode> {
}
