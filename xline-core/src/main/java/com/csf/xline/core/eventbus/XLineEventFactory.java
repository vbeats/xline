package com.csf.xline.core.eventbus;

import com.lmax.disruptor.EventFactory;

/**
 * 事件工厂：预分配事件对象，避免运行期反复创建。
 */
public class XLineEventFactory implements EventFactory<XLineEvent> {

    @Override
    public XLineEvent newInstance() {
        return new XLineEvent();
    }
}
