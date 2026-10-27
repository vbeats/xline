package com.csf.xline.core;

import com.csf.xline.core.routing.XLineRoutingDataSource;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * xline 核心装配。
 *
 * <p>采用「组件扫描 + {@code @Bean}」装配，不使用 {@code spring.factories} 与
 * {@code AutoConfiguration.imports} 注册自动配置：二者在 SpringBoot 2.7 与 4.x 之间
 * 互斥且不能共存，走组件扫描可让同一套源码在两个大版本下行为完全一致。
 */
@Configuration
@ConfigurationPropertiesScan(basePackages = "com.csf.xline")
public class XLineConfiguration {

    /**
     * 节点路由数据源，供 MyBatis-Plus 等依赖单一数据源的组件使用。
     *
     * @return 路由数据源
     */
    @Bean
    public DataSource dataSource() {
        return new XLineRoutingDataSource();
    }
}
