package com.csf.xline.tcp;

import com.csf.xline.core.Node;
import com.csf.xline.core.concurrent.ThreadFactoryProvider;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * TCP 节点。
 *
 * <p>连接的建立与释放委托给 {@link TcpEndpoint}；
 * 生命周期与数据处理由 {@link Node} 的模板统一完成：
 * <ul>
 *     <li>收到对端数据 —— 以本节点为源头 {@code pushMsg} 推向下游；</li>
 *     <li>收到上游数据 —— 由业务处理器决定是否 {@link #send(Object)} 写往对端。</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TcpNode extends Node {

    /**
     * 线程工厂供给：JDK 21 为虚拟线程、JDK 1.8 为平台线程，由装配阶段注入。
     *
     * <p>声明为 transient：仅存在于运行期。
     */
    @ToString.Exclude
    private transient ThreadFactoryProvider threadFactory;

    @Override
    protected void initDataSource() throws Exception {
        TcpEndpoint.start(this);
    }

    @Override
    protected void destroyDataSource() throws Exception {
        TcpEndpoint.stop(this);
    }

    /**
     * 向对端发送数据：server 模式广播给全部在线连接，client 模式写往对端。
     *
     * @param data 待发送数据，按 {@code charset} 编码写出
     * @throws Exception 节点未初始化或写出失败
     */
    public void send(Object data) throws Exception {
        TcpEndpoint.send(this, data);
    }
}
