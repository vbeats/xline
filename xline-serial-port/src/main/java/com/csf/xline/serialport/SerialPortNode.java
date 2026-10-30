package com.csf.xline.serialport;

import com.csf.xline.core.Node;
import com.csf.xline.core.concurrent.ThreadFactoryProvider;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * 串口节点。
 *
 * <p>串口的打开与关闭委托给 {@link SerialPortEndpoint}；
 * 生命周期与数据处理由 {@link Node} 的模板统一完成：
 * <ul>
 *     <li>读到串口数据 —— 以本节点为源头 {@link #pushMsg(Object)} 推向下游；</li>
 *     <li>收到上游数据 —— 有业务处理器时交给它处理，否则直接 {@link #send(Object)} 写往串口，
 *         使 struct 中成对的串口无需业务代码即可双向转发。</li>
 * </ul>
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class SerialPortNode extends Node {

    /**
     * 线程工厂供给：JDK 21 为虚拟线程、JDK 1.8 为平台线程，由装配阶段注入。
     *
     * <p>声明为 transient：仅存在于运行期。
     */
    @ToString.Exclude
    private transient ThreadFactoryProvider threadFactory;

    @Override
    protected void initDataSource() throws Exception {
        SerialPortEndpoint.start(this);
    }

    @Override
    protected void destroyDataSource() throws Exception {
        SerialPortEndpoint.stop(this);
    }

    /**
     * 收到上游数据：有业务处理器则交给业务，否则透明转发到串口。
     *
     * @param data 上游数据
     */
    @Override
    public void receiveData(Object data) {
        if (dataHandler() == null) {
            try {
                send(data);
            } catch (Exception e) {
                log.error("节点 [{}] 转发数据到串口失败", getName(), e);
            }
            return;
        }
        super.receiveData(data);
    }

    /**
     * 向串口写出数据。
     *
     * @param data 待发送数据：{@code byte[]} 原样写出，其余按 {@code charset} 转成文本后写出
     * @throws Exception 节点未初始化或写出失败
     */
    public void send(Object data) throws Exception {
        SerialPortEndpoint.send(this, data);
    }
}
