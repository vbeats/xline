package com.csf.xline.core.eventbus;

import com.csf.xline.core.INode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 下游节点分派器：总线消费线程把数据交给它，由工作线程池异步执行各节点的处理逻辑。
 *
 * <p>每个节点一个 {@link NodeMailbox}：节点内串行、节点间并行。
 * 总线消费线程只做入队，不执行任何业务代码，因此慢节点只拖慢自己，不会卡住整条总线。
 */
@Slf4j
public class XLineDispatcher {

    /** 停机时等待工作线程收尾的时限（毫秒） */
    private static final long SHUTDOWN_TIMEOUT_MILLIS = 5_000L;
    /** 外部生产者等待下游腾出空间的最长时间（毫秒）：下游卡死时不无限期拖住生产者 */
    private static final long BACKPRESSURE_MAX_WAIT_MILLIS = 30_000L;
    /** 反压等待的轮询间隔（纳秒） */
    private static final long BACKPRESSURE_PARK_NANOS = 200_000L;

    private final ExecutorService workers;
    private final int capacity;
    private final Map<String, NodeMailbox> mailboxes = new ConcurrentHashMap<>();
    /** 已关闭的节点：总线可能刚取到节点引用，关闭后不得再为其重建队列 */
    private final Set<String> closed = ConcurrentHashMap.newKeySet();

    /**
     * @param workers  工作线程池，由本类负责关闭
     * @param capacity 单节点待处理数据上限：外部生产者在此等待；硬上限（丢弃）为其 2 倍
     */
    public XLineDispatcher(ExecutorService workers, int capacity) {
        this.workers = workers;
        this.capacity = capacity;
    }

    /**
     * 把数据交给下游节点异步处理。
     *
     * @param node    下游节点
     * @param payload 数据
     */
    public void dispatch(INode node, Object payload) {
        NodeMailbox mailbox = mailbox(node);
        if (mailbox != null) {
            mailbox.offer(payload);
        }
    }

    /**
     * 反压：外部生产者推送前，若任一下游节点待处理数据已达上限则等待其腾出空间。
     *
     * <p>工作线程内（处理器里再推送）不等待，避免与自身所在节点互相等待；
     * 最长等待 {@value #BACKPRESSURE_MAX_WAIT_MILLIS} ms，下游卡死时放行，超出硬上限的数据由队列丢弃并告警。
     *
     * @param downstream 下游节点名
     */
    public void awaitCapacity(List<String> downstream) {
        if (downstream.isEmpty() || NodeMailbox.inWorker()) {
            return;
        }
        long deadline = 0L;
        while (saturated(downstream)) {
            long now = System.nanoTime();
            if (deadline == 0L) {
                deadline = now + TimeUnit.MILLISECONDS.toNanos(BACKPRESSURE_MAX_WAIT_MILLIS);
            } else if (now - deadline >= 0) {
                log.warn("下游节点 {} 持续 {} ms 未腾出空间，停止等待", downstream, BACKPRESSURE_MAX_WAIT_MILLIS);
                return;
            }
            LockSupport.parkNanos(BACKPRESSURE_PARK_NANOS);
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
        }
    }

    private boolean saturated(List<String> downstream) {
        for (String name : downstream) {
            NodeMailbox mailbox = mailboxes.get(name);
            if (mailbox != null && mailbox.saturated()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 关闭节点的待处理队列：节点销毁前调用，之后到达的数据不再处理。
     *
     * @param nodeName 节点名
     */
    public void close(String nodeName) {
        closed.add(nodeName);
        NodeMailbox mailbox = mailboxes.remove(nodeName);
        if (mailbox == null) {
            return;
        }
        int discarded = mailbox.close();
        if (discarded > 0) {
            log.warn("节点 [{}] 销毁，丢弃未处理数据 {} 条", nodeName, discarded);
        }
    }

    /**
     * 停止工作线程池：等待在途处理收尾，超时后中断。
     */
    public void shutdown() {
        for (NodeMailbox mailbox : mailboxes.values()) {
            mailbox.close();
        }
        mailboxes.clear();
        workers.shutdown();
        try {
            if (!workers.awaitTermination(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                workers.shutdownNow();
                log.warn("工作线程池关闭超时（{} ms），已中断仍在执行的节点处理", SHUTDOWN_TIMEOUT_MILLIS);
            }
        } catch (InterruptedException e) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 取节点的待处理队列；节点以同名重新登记时换新队列。
     *
     * @param node 节点
     * @return 待处理队列，节点已关闭时为 {@code null}
     */
    private NodeMailbox mailbox(INode node) {
        String name = node.getName();
        if (closed.contains(name)) {
            return null;
        }
        NodeMailbox mailbox = mailboxes.get(name);
        if (mailbox != null && mailbox.node() == node) {
            return mailbox;
        }
        NodeMailbox created = new NodeMailbox(node, workers, capacity, (int) Math.min(capacity * 2L, Integer.MAX_VALUE));
        if (mailbox == null) {
            NodeMailbox existing = mailboxes.putIfAbsent(name, created);
            if (existing != null) {
                return existing;
            }
            // 与 close() 并发：建队列期间节点已被关闭，撤回新建的队列
            if (closed.contains(name)) {
                mailboxes.remove(name, created);
                created.close();
                return null;
            }
            return created;
        }
        // 只有总线消费线程会走到这里（单线程），直接替换即可
        mailboxes.put(name, created);
        mailbox.close();
        return created;
    }
}
