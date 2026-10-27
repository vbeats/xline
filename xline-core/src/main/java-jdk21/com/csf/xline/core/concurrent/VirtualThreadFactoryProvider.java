package com.csf.xline.core.concurrent;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadFactory;

/**
 * JDK 21 分支的线程工厂实现：虚拟线程。
 *
 * <p>所在目录 {@code src/main/java-jdk21} 仅在 jdk21 profile 激活时参与编译，
 * 因此可以安全使用虚拟线程；JDK 1.8 构建时本文件不参与编译。
 */
@Component
public class VirtualThreadFactoryProvider implements ThreadFactoryProvider {

    /** 线程类型标识 */
    public static final String TYPE = "virtual-thread";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public ThreadFactory create(String name) {
        // 虚拟线程命名：name-0、name-1 …… 便于日志与线程栈排查
        return Thread.ofVirtual().name(name + "-", 0).factory();
    }
}
