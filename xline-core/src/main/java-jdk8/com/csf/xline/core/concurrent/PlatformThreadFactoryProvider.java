package com.csf.xline.core.concurrent;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JDK 1.8 分支的线程工厂实现：平台线程。
 *
 * <p>所在目录 {@code src/main/java-jdk8} 仅在 jdk8 profile 激活时参与编译。
 * 注意：本文件必须使用 Java 8 语法。
 */
@Component
public class PlatformThreadFactoryProvider implements ThreadFactoryProvider {

    /** 线程类型标识 */
    public static final String TYPE = "platform-thread";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public ThreadFactory create(String name) {
        return new NamedThreadFactory(name);
    }

    /** 具名线程工厂，便于在线程栈中定位来源 */
    private static final class NamedThreadFactory implements ThreadFactory {

        private final String prefix;

        private final AtomicLong counter = new AtomicLong();

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName(prefix + "-" + counter.incrementAndGet());
            return thread;
        }
    }
}
