package com.csf.xline.core.eventbus;

import com.csf.xline.core.XLineContext;
import com.csf.xline.core.concurrent.ThreadFactoryProvider;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 事件总线配置。
 *
 * <p>线程工厂由 {@link ThreadFactoryProvider} 提供：
 * JDK 21 分支为虚拟线程，JDK 1.8 分支自动降级为平台线程，本类无需关心差异。
 */
@Configuration
public class DisruptorConfig {

    /** 环形缓冲区大小，必须为 2 的幂 */
    private static final int BUFFER_SIZE = 1024;

    /**
     * 创建事件总线并回写到上下文。
     *
     * @param context          运行时上下文
     * @param factoryProvider  按 JDK 分支提供的线程工厂
     * @return 事件总线
     */
    @Bean(destroyMethod = "shutdown")
    public Disruptor<XLineEvent> xLineDisruptor(XLineContext context, ThreadFactoryProvider factoryProvider) {
        Disruptor<XLineEvent> disruptor = new Disruptor<XLineEvent>(
                new XLineEventFactory(),
                BUFFER_SIZE,
                factoryProvider.create("xline-disruptor"),
                ProducerType.MULTI,
                new BlockingWaitStrategy());
        disruptor.setDefaultExceptionHandler(new XLineEventExceptionHandler());
        disruptor.handleEventsWith(new XLineEventHandler(context));

        RingBuffer<XLineEvent> ringBuffer = disruptor.start();
        context.bindRingBuffer(ringBuffer);
        return disruptor;
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
}
