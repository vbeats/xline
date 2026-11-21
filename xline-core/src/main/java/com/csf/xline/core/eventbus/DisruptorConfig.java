package com.csf.xline.core.eventbus;

import com.csf.xline.core.XLineContext;
import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.concurrent.ThreadFactoryProvider;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadFactory;

/**
 * 事件总线配置。
 *
 * <p>总线消费线程固定为一个平台线程：它只做路由，长期存活且在等待策略内阻塞，
 * 不适合虚拟线程（JDK 21 下会长期占住载体线程）。
 * 节点处理在 {@link XLineDispatcher} 的工作线程池中执行，
 * 线程类型由 {@link ThreadFactoryProvider} 按 JDK 分支决定（JDK 21 虚拟线程，JDK 1.8 平台线程）。
 */
@Configuration
public class DisruptorConfig {

    /**
     * 下游节点分派器。
     *
     * @param properties      xline 根配置
     * @param factoryProvider 按 JDK 分支提供的线程工厂
     * @return 分派器（由 {@link XLineBusLifecycle} 统一关闭）
     */
    @Bean(destroyMethod = "")
    public XLineDispatcher xLineDispatcher(XLineProperties properties, ThreadFactoryProvider factoryProvider) {
        XLineBusProperties bus = bus(properties);
        return new XLineDispatcher(
                factoryProvider.newWorkerPool("xline-worker", bus.resolvedWorkerThreads()),
                bus.resolvedNodeQueueCapacity());
    }

    /**
     * 创建事件总线并回写到上下文。
     *
     * @param context    运行时上下文
     * @param properties xline 根配置
     * @param dispatcher 下游节点分派器
     * @return 事件总线（由 {@link XLineBusLifecycle} 统一关闭）
     */
    @Bean(destroyMethod = "")
    public Disruptor<XLineEvent> xLineDisruptor(XLineContext context, XLineProperties properties,
                                                XLineDispatcher dispatcher) {
        Disruptor<XLineEvent> disruptor = new Disruptor<XLineEvent>(
                new XLineEventFactory(),
                bus(properties).resolvedBufferSize(),
                busThreadFactory(),
                ProducerType.MULTI,
                new BlockingWaitStrategy());
        disruptor.setDefaultExceptionHandler(new XLineEventExceptionHandler());
        disruptor.handleEventsWith(new XLineEventHandler(context, dispatcher));
        RingBuffer<XLineEvent> ringBuffer = disruptor.start();
        context.bindBus(ringBuffer, dispatcher);
        return disruptor;
    }

    /**
     * 总线停机收尾。
     *
     * @param disruptor  事件总线
     * @param dispatcher 下游节点分派器
     * @return 停机收尾 Bean
     */
    @Bean
    public XLineBusLifecycle xLineBusLifecycle(Disruptor<XLineEvent> disruptor, XLineDispatcher dispatcher) {
        return new XLineBusLifecycle(disruptor, dispatcher);
    }

    /**
     * 事件总线环形缓冲。
     *
     * @param disruptor 事件总线
     * @return 环形缓冲
     */
    @Bean
    public RingBuffer<XLineEvent> xLineRingBuffer(Disruptor<XLineEvent> disruptor) {
        return disruptor.getRingBuffer();
    }

    private static XLineBusProperties bus(XLineProperties properties) {
        return properties.getBus() == null ? new XLineBusProperties() : properties.getBus();
    }

    /** 总线消费线程：平台线程、daemon，不阻止 JVM 退出 */
    private static ThreadFactory busThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "xline-bus");
            thread.setDaemon(true);
            return thread;
        };
    }
}
