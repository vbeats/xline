package com.csf.xline.core.eventbus;

import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.TimeUnit;

/**
 * 事件总线的停机收尾：先停总线，再停工作线程池，两步都有时限。
 *
 * <p>停机前节点已由 {@code XLineShutdownListener} 销毁；此处只负责线程资源，
 * 任何一步卡住都不会无限期拖住进程退出。
 */
@Slf4j
public class XLineBusLifecycle implements DisposableBean {

    /** 等待总线消费完在途事件的时限（秒） */
    private static final long BUS_SHUTDOWN_TIMEOUT_SECONDS = 5L;

    private final Disruptor<XLineEvent> disruptor;
    private final XLineDispatcher dispatcher;

    public XLineBusLifecycle(Disruptor<XLineEvent> disruptor, XLineDispatcher dispatcher) {
        this.disruptor = disruptor;
        this.dispatcher = dispatcher;
    }

    @Override
    public void destroy() {
        try {
            disruptor.shutdown(BUS_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("事件总线关闭超时（{} s），强制停止", BUS_SHUTDOWN_TIMEOUT_SECONDS);
            disruptor.halt();
        }
        dispatcher.shutdown();
    }
}
