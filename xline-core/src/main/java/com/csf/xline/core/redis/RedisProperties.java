package com.csf.xline.core.redis;

import lombok.Data;
import lombok.ToString;

@Data
public class RedisProperties {

    private String configLocation;
    private String address = "redis://127.0.0.1:6379";
    private String username;
    @ToString.Exclude
    private String password;
    private int database = 0;
    private int connectTimeoutMillis = 10000;
    private int timeoutMillis = 3000;
    private int retryAttempts = 3;
    private int idleConnectionTimeoutMillis = 10000;
    private int connectionMinimumIdleSize = 1;
    private int connectionPoolSize = 16;
    private int subscriptionConnectionMinimumIdleSize = 1;
    private int subscriptionConnectionPoolSize = 8;
    private String clientName;
    private int threads = 0;
    private int nettyThreads = 0;
    private long shutdownQuietPeriodMillis = 0;
    private long shutdownTimeoutMillis = 5000;
}
