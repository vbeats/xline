package com.csf.xline.redis;

import com.csf.xline.core.Node;
import com.csf.xline.core.redis.RedisProperties;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisNode extends Node {

    private transient volatile RedissonClient client;
    private transient RedisProperties settings;

    public RedissonClient client() {
        RedissonClient current = client;
        Assert.state(current != null && !current.isShutdown(), "redis 节点尚未初始化或已关闭");
        return current;
    }

    @Override
    protected void initDataSource() throws Exception {
        settings = getRedis() == null ? new RedisProperties() : getRedis();
        Assert.isTrue(settings.getShutdownQuietPeriodMillis() >= 0, "redis shutdown-quiet-period-millis 不能为负数");
        Assert.isTrue(settings.getShutdownTimeoutMillis() > 0
                && settings.getShutdownTimeoutMillis() >= settings.getShutdownQuietPeriodMillis(), "redis 关闭超时配置无效");
        Config config;
        if (StringUtils.hasText(settings.getConfigLocation())) {
            try (InputStream input = new DefaultResourceLoader().getResource(settings.getConfigLocation()).getInputStream()) {
                config = Config.fromYAML(input);
            }
        } else {
            validateSingleServer(settings);
            config = new Config();
            config.setThreads(settings.getThreads());
            config.setNettyThreads(settings.getNettyThreads());
            config.setUsername(StringUtils.hasText(settings.getUsername()) ? settings.getUsername() : null);
            config.setPassword(StringUtils.hasText(settings.getPassword()) ? settings.getPassword() : null);
            SingleServerConfig server = config.useSingleServer();
            server.setAddress(settings.getAddress());
            server.setDatabase(settings.getDatabase());
            server.setConnectTimeout(settings.getConnectTimeoutMillis());
            server.setTimeout(settings.getTimeoutMillis());
            server.setRetryAttempts(settings.getRetryAttempts());
            server.setIdleConnectionTimeout(settings.getIdleConnectionTimeoutMillis());
            server.setConnectionMinimumIdleSize(settings.getConnectionMinimumIdleSize());
            server.setConnectionPoolSize(settings.getConnectionPoolSize());
            server.setSubscriptionConnectionMinimumIdleSize(settings.getSubscriptionConnectionMinimumIdleSize());
            server.setSubscriptionConnectionPoolSize(settings.getSubscriptionConnectionPoolSize());
            server.setClientName(StringUtils.hasText(settings.getClientName()) ? settings.getClientName() : getName());
        }
        if (config.getCodec() == null) {
            config.setCodec(StringCodec.INSTANCE);
        }
        client = Redisson.create(config);
        log.info("【Redis】reg success: {}", getName());
    }

    @Override
    protected void destroyDataSource() {
        RedissonClient current = client;
        client = null;
        if (current != null) {
            current.shutdown(settings.getShutdownQuietPeriodMillis(), settings.getShutdownTimeoutMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private static void validateSingleServer(RedisProperties properties) {
        Assert.hasText(properties.getAddress(), "redis address 不能为空");
        URI address = URI.create(properties.getAddress());
        Assert.isTrue("redis".equals(address.getScheme()) || "rediss".equals(address.getScheme()),
                "redis address 必须使用 redis:// 或 rediss://");
        Assert.hasText(address.getHost(), "redis address 必须包含主机");
        Assert.isTrue(address.getPort() > 0 && address.getPort() <= 65535, "redis address 必须包含有效端口");
        Assert.isTrue(properties.getDatabase() >= 0, "redis database 不能为负数");
        Assert.isTrue(properties.getConnectTimeoutMillis() > 0 && properties.getTimeoutMillis() > 0, "redis 连接和命令超时必须大于 0");
        Assert.isTrue(properties.getRetryAttempts() >= 0, "redis retry-attempts 不能为负数");
        Assert.isTrue(properties.getIdleConnectionTimeoutMillis() >= 0, "redis idle-connection-timeout-millis 不能为负数");
        Assert.isTrue(properties.getConnectionMinimumIdleSize() >= 0
                && properties.getConnectionPoolSize() > 0
                && properties.getConnectionPoolSize() >= properties.getConnectionMinimumIdleSize(), "redis 连接池大小配置无效");
        Assert.isTrue(properties.getSubscriptionConnectionMinimumIdleSize() >= 0
                && properties.getSubscriptionConnectionPoolSize() > 0
                && properties.getSubscriptionConnectionPoolSize() >= properties.getSubscriptionConnectionMinimumIdleSize(), "redis 订阅连接池大小配置无效");
        Assert.isTrue(properties.getThreads() >= 0 && properties.getNettyThreads() >= 0, "redis 线程数不能为负数");
    }
}
