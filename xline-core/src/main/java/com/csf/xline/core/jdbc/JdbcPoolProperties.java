package com.csf.xline.core.jdbc;

import lombok.Data;
import org.springframework.util.Assert;

/**
 * JDBC 连接池配置（{@code xline.nodes[].<数据库>.pool}），全部可选。
 *
 * <p>默认值即各数据库节点此前的内置值；core 只承载配置，由各数据库模块应用到 Druid。
 */
@Data
public class JdbcPoolProperties {
    /** 默认连接校验 SQL；个别数据库（如 Oracle）不接受该写法，由各自配置段替换为合法语句 */
    public static final String DEFAULT_VALIDATION_QUERY = "select 1";

    /** 初始连接数 */
    private int initialSize = 5;
    /** 最小空闲连接数 */
    private int minIdle = 5;
    /** 最大活动连接数 */
    private int maxActive = 10;
    /** 获取连接的最长等待（毫秒） */
    private long maxWaitMillis = 60000L;
    /** 连接校验 SQL */
    private String validationQuery = DEFAULT_VALIDATION_QUERY;
    /** 连接校验超时（秒） */
    private int validationQueryTimeoutSeconds = 3;
    /** 借出时校验 */
    private boolean testOnBorrow;
    /** 归还时校验 */
    private boolean testOnReturn;
    /** 空闲时校验 */
    private boolean testWhileIdle = true;
    /** 空闲检测间隔（毫秒） */
    private long timeBetweenEvictionRunsMillis = 60000L;
    /** 连接最短空闲回收时间（毫秒） */
    private long minEvictableIdleTimeMillis = 300000L;
    /** 建连失败后的重试间隔（毫秒）；数据库短暂不可用时节点持续重试 */
    private long timeBetweenConnectErrorMillis = 5000L;

    /**
     * 校验取值范围。
     *
     * @param prefix 配置前缀，用于报错提示，如 {@code mysql}
     */
    public void validate(String prefix) {
        Assert.isTrue(initialSize >= 0, prefix + ".pool.initial-size 不能为负数");
        Assert.isTrue(minIdle >= 0, prefix + ".pool.min-idle 不能为负数");
        Assert.isTrue(maxActive > 0, prefix + ".pool.max-active 必须大于 0");
        Assert.isTrue(initialSize <= maxActive && minIdle <= maxActive,
                prefix + ".pool.initial-size / min-idle 不能大于 max-active");
        Assert.isTrue(maxWaitMillis > 0, prefix + ".pool.max-wait-millis 必须大于 0");
        Assert.hasText(validationQuery, prefix + ".pool.validation-query 不能为空");
        Assert.isTrue(validationQueryTimeoutSeconds > 0, prefix + ".pool.validation-query-timeout-seconds 必须大于 0");
        Assert.isTrue(timeBetweenEvictionRunsMillis > 0, prefix + ".pool.time-between-eviction-runs-millis 必须大于 0");
        Assert.isTrue(minEvictableIdleTimeMillis >= 30000L, prefix + ".pool.min-evictable-idle-time-millis 不能小于 30000");
        Assert.isTrue(timeBetweenConnectErrorMillis > 0, prefix + ".pool.time-between-connect-error-millis 必须大于 0");
    }
}
