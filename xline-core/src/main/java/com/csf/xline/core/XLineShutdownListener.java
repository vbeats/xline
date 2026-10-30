package com.csf.xline.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 节点统一销毁钩子。
 *
 * <p>节点不是 Spring Bean（登记在 {@link XLineContext#NODES} 而非容器中），
 * Spring 的销毁回调够不到它们，因此由本监听器在容器关闭时统一销毁：
 * {@code server.shutdown: graceful} 先等在途 Web 请求结束，
 * 之后 {@link ContextClosedEvent} 触发本钩子。
 *
 * <p>{@link ContextClosedEvent} 发布于 Bean 销毁之前：
 * <ol>
 *     <li>先回调各节点的业务处理器（{@code DataHandler.destroy}，此时数据源仍在，可做收尾写入）；</li>
 *     <li>再释放节点占用的资源 —— 数据库连接池、TCP 连接与线程、串口占用等；</li>
 *     <li>最后 Spring 销毁剩余 Bean（含事件总线 Disruptor 的关闭）。</li>
 * </ol>
 * 即：节点销毁早于事件总线关闭，销毁期间仍在途的事件依旧可被消费。
 *
 * <p>大规模资源（几十万 TCP 连接、大量串口）下的吞吐与鲁棒性：
 * <ul>
 *     <li>分组并行销毁 —— 共享同一业务处理器的节点归为一组串行销毁
 *     （处理器实现可能非线程安全），无处理器的节点各自成组，组间并行，
 *     并行度取节点组数与 CPU 数的较小值；</li>
 *     <li>总超时兜底 —— 单个节点销毁卡死（如无法关闭的连接）不会无限期拖住停机，
 *     超时后中断剩余销毁线程并告警，未释放的资源退化为由操作系统回收；</li>
 *     <li>销毁线程一律 daemon —— 即使超时中断后仍卡死，也不阻止 JVM 退出。</li>
 * </ul>
 *
 * <p>节点初始化失败的（未登记进 {@code NODES}）不参与销毁；
 * 单个节点销毁异常不阻断其余节点。
 */
@Slf4j
@Component
public class XLineShutdownListener implements ApplicationListener<ContextClosedEvent> {

    /** 节点销毁总时限（毫秒）：超时即中断剩余销毁，避免停机被单个卡死的资源拖住 */
    private static final long DESTROY_TIMEOUT_MILLIS = 30_000L;

    /** 并行度下限：节点组很少时也保留基本并行能力 */
    private static final int MIN_PARALLELISM = 4;

    /** 销毁线程编号 */
    private static final AtomicInteger THREAD_INDEX = new AtomicInteger();

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        // key 为分组依据：业务处理器（共享者串行），无处理器时以节点自身为 key
        Map<Object, List<Node>> groups = new LinkedHashMap<>();
        for (INode value : XLineContext.NODES.values()) {
            if (!(value instanceof Node)) {
                continue;
            }
            Node node = (Node) value;
            Object key = node.dataHandler() != null ? node.dataHandler() : node;
            List<Node> group = groups.get(key);
            if (group == null) {
                group = new ArrayList<>();
                groups.put(key, group);
            }
            group.add(node);
        }

        if (groups.isEmpty()) {
            log.info("xline 节点销毁完成：0 个");
            return;
        }

        int parallelism = Math.min(groups.size(),
                Math.max(MIN_PARALLELISM, Runtime.getRuntime().availableProcessors()));
        // 尚未销毁完的节点名：超时时用于告警定位
        Set<String> pending = ConcurrentHashMap.newKeySet();
        for (INode node : XLineContext.NODES.values()) {
            pending.add(node.getName());
        }

        ExecutorService executor = Executors.newFixedThreadPool(parallelism, r -> {
            Thread thread = new Thread(r, "xline-node-destroy-" + THREAD_INDEX.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        try {
            for (List<Node> group : groups.values()) {
                executor.execute(() -> {
                    for (Node node : group) {
                        try {
                            node.destroy();
                        } catch (Exception e) {
                            log.error("节点 [{}] 销毁失败", node.getName(), e);
                        } finally {
                            pending.remove(node.getName());
                        }
                    }
                });
            }
        } finally {
            executor.shutdown();
        }

        boolean finished;
        try {
            finished = executor.awaitTermination(DESTROY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            finished = false;
        }
        if (!finished) {
            executor.shutdownNow();
            log.warn("xline 节点销毁超时（{} ms），剩余节点 [{}] 改由操作系统回收",
                    DESTROY_TIMEOUT_MILLIS, String.join(", ", pending));
        }

        int count = XLineContext.NODES.size();
        XLineContext.NODES.clear();
        log.info("xline 节点销毁完成：{} 个", count);
    }
}
