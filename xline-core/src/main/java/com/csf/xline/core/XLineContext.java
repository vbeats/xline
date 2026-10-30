package com.csf.xline.core;

import com.csf.xline.core.eventbus.XLineEvent;
import com.lmax.disruptor.RingBuffer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * xline 运行时上下文。
 *
 * <p>持有全部已初始化的节点，并负责把数据投递到事件总线；
 * 事件总线按 {@code xline.struct} 解析出下游节点后逐层推送，数据流单向传递。
 *
 * <p>数据源对外暴露两种使用方式：
 * <ol>
 *     <li>anyline —— 各数据库模块初始化时注册进 {@code DataSourceHolder}，
 *         可按节点名直接使用 anyline 的库操作能力；</li>
 *     <li>MyBatis-Plus —— 数据源同时存入 {@link #DATA_SOURCES}，
 *         并通过容器中的路由 {@code DataSource} Bean 供 MyBatis-Plus 使用。</li>
 * </ol>
 */
@Slf4j
@Component
public class XLineContext {

    /** 已初始化的节点：key 为节点名 */
    public static final Map<String, INode> NODES = new ConcurrentHashMap<>();

    /** 已初始化的数据源：key 为节点名，供 MyBatis-Plus 等方式使用 */
    public static final Map<String, DataSource> DATA_SOURCES = new ConcurrentHashMap<>();

    private final XLineProperties props;

    /** 事件总线，由事件总线配置在启动阶段回写，避免构造期循环依赖 */
    private volatile RingBuffer<XLineEvent> ringBuffer;

    public XLineContext(XLineProperties props) {
        this.props = props;
    }

    /**
     * 绑定事件总线。
     *
     * @param ringBuffer 事件总线环形缓冲
     */
    public void bindRingBuffer(RingBuffer<XLineEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    /**
     * 按出发节点名向事件总线注入数据（底层路由 API）。
     *
     * <p>{@code nodeName} 是 {@code xline.struct} 的路由键，
     * 事件总线靠它解析下游，因此本方法无法去掉该参数。
     * 业务代码请改用 {@link Node#pushMsg(Object)}：节点自带名字，无需传参。
     *
     * @param nodeName 出发节点名
     * @param payload  待传递的数据
     */
    public void pushMsg(String nodeName, Object payload) {
        RingBuffer<XLineEvent> buffer = this.ringBuffer;
        if (buffer == null) {
            log.warn("事件总线尚未就绪，消息被丢弃：节点 [{}]", nodeName);
            return;
        }
        long sequence = buffer.next();
        try {
            XLineEvent event = buffer.get(sequence);
            event.setKey(nodeName);
            event.setPayload(payload);
        } finally {
            buffer.publish(sequence);
        }
    }

    /**
     * 查询节点的下游节点。
     *
     * <p>数据流单向：只返回 {@code struct} 中以该节点为 key 的 value 列表，
     * 即「上一层推送给下一层」。
     *
     * @param nodeName 当前节点名
     * @return 下游节点名列表，无下游时返回空列表
     */
    public List<String> nextNodes(String nodeName) {
        Map<String, List<String>> struct = props.getStruct();
        if (struct == null || nodeName == null) {
            return Collections.emptyList();
        }
        List<String> next = struct.get(nodeName);
        return next == null ? new ArrayList<>() : next;
    }
}
