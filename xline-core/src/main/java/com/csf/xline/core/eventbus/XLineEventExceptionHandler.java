package com.csf.xline.core.eventbus;

import com.lmax.disruptor.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 事件总线异常处理：记录异常但不中断事件投递。
 */
@Slf4j
public class XLineEventExceptionHandler implements ExceptionHandler<XLineEvent> {

    @Override
    public void handleEventException(Throwable ex, long sequence, XLineEvent event) {
        // 不打印 payload：大数据量时 toString 本身就会放大内存与日志压力
        log.error("事件处理异常：sequence={}, 源节点={}, 数据类型={}", sequence,
                event == null ? null : event.getKey(),
                event == null || event.getPayload() == null ? null : event.getPayload().getClass().getName(), ex);
    }

    @Override
    public void handleOnStartException(Throwable ex) {
        log.error("事件总线启动异常", ex);
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
        log.error("事件总线关闭异常", ex);
    }
}
