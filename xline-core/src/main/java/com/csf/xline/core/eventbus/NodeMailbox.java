package com.csf.xline.core.eventbus;

import com.csf.xline.core.INode;
import lombok.extern.slf4j.Slf4j;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 单个下游节点的待处理队列。
 *
 * <p>同一节点同一时刻最多只有一个任务在工作线程池中执行，数据按到达顺序串行处理，
 * 因此 {@code DataHandler} 无需线程安全；不同节点的队列互相独立，并行处理。
 * 每轮最多处理 {@link #BATCH} 条后让出线程，避免繁忙节点长期独占工作线程。
 *
 * <p>两级上限：
 * <ul>
 *     <li>{@code capacity}（软上限）—— 达到后外部生产者（TCP / MQTT / 串口读线程等）在推送前等待，
 *     形成反压，数据不丢；</li>
 *     <li>{@code limit}（硬上限）—— 总线消费线程从不等待，超过时丢弃新数据并限频告警。
 *     只有工作线程内（处理器里再推送）的数据可能越过软上限，硬上限防止其无限积压。</li>
 * </ul>
 * 总线与工作线程都不阻塞，因此「处理器内再推送」不会与总线互相等待而卡死。
 */
@Slf4j
final class NodeMailbox implements Runnable {

    /** 每轮最多处理条数 */
    private static final int BATCH = 256;
    /** 丢弃告警最小间隔（毫秒） */
    private static final long WARN_INTERVAL_MILLIS = 10_000L;
    /** null 数据占位：并发队列不接受 null */
    private static final Object NULL = new Object();
    /** 当前线程是否为节点工作线程：工作线程推送数据时不等待反压 */
    private static final ThreadLocal<Boolean> IN_WORKER = new ThreadLocal<>();

    private final INode node;
    private final Executor executor;
    private final int capacity;
    private final int limit;
    private final Queue<Object> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger size = new AtomicInteger();
    private final AtomicBoolean scheduled = new AtomicBoolean();
    private final AtomicLong dropped = new AtomicLong();
    private volatile long lastWarnMillis;
    private volatile boolean closed;

    NodeMailbox(INode node, Executor executor, int capacity, int limit) {
        this.node = node;
        this.executor = executor;
        this.capacity = capacity;
        this.limit = Math.max(limit, capacity);
    }

    /** @return 当前线程是否为节点工作线程 */
    static boolean inWorker() {
        return Boolean.TRUE.equals(IN_WORKER.get());
    }

    /** @return 待处理数据已达软上限（外部生产者应等待） */
    boolean saturated() {
        return !closed && size.get() >= capacity;
    }

    INode node() {
        return node;
    }

    /**
     * 放入一条待处理数据。
     *
     * @param payload 数据
     */
    void offer(Object payload) {
        if (closed) {
            return;
        }
        if (size.incrementAndGet() > limit) {
            size.decrementAndGet();
            onDropped();
            return;
        }
        queue.offer(payload == null ? NULL : payload);
        schedule();
    }

    /**
     * 关闭队列：不再接收新数据，已排队的数据丢弃。
     *
     * @return 丢弃的排队数据条数
     */
    int close() {
        closed = true;
        int discarded = 0;
        while (queue.poll() != null) {
            size.decrementAndGet();
            discarded++;
        }
        return discarded;
    }

    @Override
    public void run() {
        IN_WORKER.set(Boolean.TRUE);
        try {
            for (int i = 0; i < BATCH && !closed; i++) {
                Object payload = queue.poll();
                if (payload == null) {
                    break;
                }
                size.decrementAndGet();
                // receiveData 内部已捕获业务异常；此处兜底 Error 之外的运行期异常，保证队列继续调度
                try {
                    node.receiveData(payload == NULL ? null : payload);
                } catch (RuntimeException e) {
                    log.error("节点 [{}] 处理数据失败", node.getName(), e);
                }
            }
        } finally {
            IN_WORKER.remove();
            scheduled.set(false);
            if (!closed && !queue.isEmpty()) {
                schedule();
            }
        }
    }

    /** 仅在未调度时提交一个处理任务：同一节点不会被多个线程同时处理 */
    private void schedule() {
        if (!scheduled.compareAndSet(false, true)) {
            return;
        }
        try {
            executor.execute(this);
        } catch (RejectedExecutionException e) {
            // 工作线程池已关闭（停机中）：数据不再处理
            scheduled.set(false);
            log.debug("工作线程池已关闭，节点 [{}] 的数据不再处理", node.getName());
        }
    }

    private void onDropped() {
        long count = dropped.incrementAndGet();
        long now = System.currentTimeMillis();
        if (now - lastWarnMillis >= WARN_INTERVAL_MILLIS) {
            lastWarnMillis = now;
            log.warn("节点 [{}] 待处理数据超过硬上限 {}，新数据被丢弃（累计丢弃 {} 条）：处理器内推送过快",
                    node.getName(), limit, count);
        }
    }
}
