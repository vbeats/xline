package com.csf.xline.core;

import com.csf.xline.core.enums.NodeType;

/**
 * 节点契约。
 *
 * <p>每个节点既是数据流的接收方，也可作为发送方：
 * 上游节点通过 {@link XLineContext#pushMsg(String, Object)} 投递数据，
 * 事件总线按 {@code xline.struct} 找到下游节点，调用其 {@link #receiveData(Object)}，
 * 数据流严格单向传递。
 *
 * <p>本接口只暴露节点身份与生命周期，不涉及任何具体数据库实现，
 * 因此上下文与事件总线均面向本接口编程。
 */
public interface INode {

    /**
     * 节点名，对应 yml 中的 {@code xline.nodes[].name}。
     *
     * @return 节点名
     */
    String getName();

    /**
     * 节点类型，由 yml 中书写的配置段推断。
     *
     * @return 节点类型，未配置任何数据库时为 {@code null}
     */
    NodeType getType();

    /** 初始化节点：建立数据源、回调业务处理器等 */
    void init() throws Exception;

    /** 销毁节点：回调业务处理器、释放数据源等 */
    void destroy() throws Exception;

    /**
     * 接收上游推送的数据。
     *
     * @param data 上游传递的数据
     */
    void receiveData(Object data);
}
