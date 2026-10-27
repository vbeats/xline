package com.csf.xline.core.eventbus;

import lombok.Data;

/**
 * 事件载体。
 */
@Data
public class XLineEvent {

    /** 产生数据的节点名 */
    private String key;

    /** 传递的数据 */
    private Object payload;
}
