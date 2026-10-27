package com.csf.xline.core.concurrent;

import java.util.concurrent.ThreadFactory;

/**
 * 线程工厂供给接口（JDK 特性分叉的统一抽象）。
 *
 * <p>JDK 21 可使用虚拟线程，JDK 1.8 只能使用平台线程。
 * 业务代码只面向接口编程，不感知当前运行在哪个 JDK 分支。
 *
 * <p>实现位于 JDK 专属源码目录，由构建期 profile 决定编译哪一个：
 * <ul>
 *     <li>{@code src/main/java-jdk8} —— 平台线程工厂（JDK 1.8 分支）</li>
 *     <li>{@code src/main/java-jdk21} —— 虚拟线程工厂（JDK 21 分支）</li>
 * </ul>
 * 同一时刻只有一个实现参与编译，容器中只会存在一个 Bean，无需条件注解。
 */
public interface ThreadFactoryProvider {

    /**
     * 线程类型标识，用于启动日志与排查。
     *
     * @return 线程类型标识
     */
    String type();

    /**
     * 按名称创建线程工厂。
     *
     * @param name 线程名前缀
     * @return 线程工厂
     */
    ThreadFactory create(String name);
}
