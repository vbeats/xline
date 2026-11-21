package com.csf.xline.core.eventbus;

import lombok.Data;

/**
 * 事件总线配置（{@code xline.bus}），全部可选。
 *
 * <p>总线消费线程只做路由：把数据放进下游节点各自的待处理队列，
 * 节点的 {@code DataHandler.rec} 在工作线程池中异步执行。
 * 同一节点的数据串行、按到达顺序处理；不同节点之间并行，互不保证顺序。
 */
@Data
public class XLineBusProperties {
    /** 环形缓冲区大小，非 2 的幂时向上取整 */
    private int bufferSize = 1024;
    /**
     * 工作线程数：仅 JDK 8 分支（平台线程池）生效，0 表示自动（CPU 数 × 2，至少 4）。
     * JDK 21 分支每次处理使用一个虚拟线程，无需配置。
     */
    private int workerThreads = 0;
    /**
     * 单个节点待处理数据上限：达到后外部生产者（TCP / MQTT / 串口读线程等）推送前等待下游腾出空间，数据不丢；
     * 处理器内推送不等待，超过该值 2 倍时丢弃并告警，防止积压导致内存溢出。
     */
    private int nodeQueueCapacity = 10000;

    /** @return 生效的环形缓冲区大小（2 的幂） */
    public int resolvedBufferSize() {
        int size = Math.max(bufferSize, 2);
        int highest = Integer.highestOneBit(size);
        return highest == size ? size : highest << 1;
    }

    /** @return 生效的工作线程数 */
    public int resolvedWorkerThreads() {
        return workerThreads > 0 ? workerThreads
                : Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
    }

    /** @return 生效的节点队列上限 */
    public int resolvedNodeQueueCapacity() {
        return nodeQueueCapacity > 0 ? nodeQueueCapacity : 10000;
    }
}
