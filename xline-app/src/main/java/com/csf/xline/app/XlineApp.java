package com.csf.xline.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * xline 启动类。
 *
 * <p>显式指定扫描根路径为 {@code com.csf.xline}：
 * 各数据库模块以 {@code @Configuration} 提供节点装配，靠组件扫描被发现，
 * 从而避免使用 spring.factories 与 AutoConfiguration.imports
 * （这两者在 SpringBoot 2.7 与 4.x 之间互斥，不能共存）。
 */
@SpringBootApplication(scanBasePackages = "com.csf.xline")
public class XlineApp {

    public static void main(String[] args) {
        SpringApplication.run(XlineApp.class, args);
    }
}
