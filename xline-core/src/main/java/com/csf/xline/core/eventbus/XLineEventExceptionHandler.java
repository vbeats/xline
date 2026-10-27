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
        log.error("事件处理异常：sequence={}, event={}", sequence, event, ex);
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
