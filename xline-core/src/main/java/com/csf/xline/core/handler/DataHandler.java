package com.csf.xline.core.handler;

import com.csf.xline.core.Node;

/**
 * 节点数据处理契约。
 *
 * <p>由业务侧实现，决定节点在初始化、接收到上游数据、销毁时分别做什么。
 * 各数据库模块提供对应的子接口（如 {@code MysqlDataHandler}），
 * 节点在对应生命周期回调业务实现。
 *
 * @param <N> 节点类型
 */
public interface DataHandler<N extends Node> {

    /**
     * 节点初始化时回调。
     *
     * @param node 当前节点
     */
    void init(N node) throws Exception;

    /**
     * 接收到上游推送的数据时回调。
     *
     * @param node 当前节点
     * @param data 上游数据
     */
    void rec(N node, Object data) throws Exception;

    /**
     * 节点销毁时回调。
     *
     * @param node 当前节点
     */
    void destroy(N node) throws Exception;
}
