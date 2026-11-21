package com.csf.xline.core.jdbc;

import lombok.Data;
import lombok.ToString;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 关系型数据库节点的公共配置：驱动、连接串、连接池、驱动属性与 Flyway。
 *
 * <p>除连接目标外均为可选项；core 只承载配置，Druid / Flyway 由各数据库模块应用。
 */
@Data
public abstract class JdbcProperties {
    /** JDBC 驱动类名，未配置时使用该数据库的默认驱动 */
    private String driverClassName;
    /** 完整 JDBC 连接串，配置后忽略按字段拼装的地址 */
    private String jdbcUrl;
    /** 连接池配置，未配置时使用默认值 */
    private JdbcPoolProperties pool = new JdbcPoolProperties();
    /** 额外驱动属性，原样传给 JDBC 驱动 */
    @ToString.Exclude
    private Map<String, String> connectionProperties = new LinkedHashMap<>();
    /** 是否执行 Flyway 迁移，默认关闭 */
    private Boolean flyway = Boolean.FALSE;
    /** Flyway 遇到非空库时是否先建基线，默认开启 */
    private Boolean baselineOnMigrate = Boolean.TRUE;
    /** Flyway 脚本位置，默认 {@code classpath:<数据库类型>/<节点名>} */
    private List<String> flywayLocations;

    /**
     * 取显式配置的连接串。
     *
     * @return 去除首尾空白的 {@code jdbc-url}，未配置时为 {@code null}，由调用方按字段拼装
     */
    public String explicitJdbcUrl() {
        return StringUtils.hasText(jdbcUrl) ? jdbcUrl.trim() : null;
    }

    /** @return 生效的连接池配置，未配置时为默认值 */
    public JdbcPoolProperties resolvedPool() {
        return pool == null ? new JdbcPoolProperties() : pool;
    }

    /** @return 是否执行 Flyway 迁移 */
    public boolean flywayEnabled() {
        return Boolean.TRUE.equals(flyway);
    }

    /** @return Flyway 是否先建基线，未配置时为 {@code true} */
    public boolean resolvedBaselineOnMigrate() {
        return !Boolean.FALSE.equals(baselineOnMigrate);
    }

    /**
     * 取生效的 Flyway 脚本位置。
     *
     * @param segment  数据库类型配置段名，如 {@code mysql}
     * @param nodeName 节点名
     * @return 脚本位置
     */
    public String[] resolvedFlywayLocations(String segment, String nodeName) {
        if (flywayLocations == null || flywayLocations.isEmpty()) {
            return new String[]{String.format("classpath:%s/%s", segment, nodeName)};
        }
        for (String location : flywayLocations) {
            Assert.hasText(location, segment + ".flyway-locations 不能包含空项");
        }
        return flywayLocations.toArray(new String[0]);
    }

    /**
     * 取额外驱动属性。
     *
     * @param segment 数据库类型配置段名，用于报错提示
     * @return 驱动属性
     */
    public Properties resolvedConnectionProperties(String segment) {
        Properties properties = new Properties();
        if (connectionProperties != null) {
            for (Map.Entry<String, String> entry : connectionProperties.entrySet()) {
                Assert.hasText(entry.getKey(), segment + ".connection-properties 的键不能为空");
                Assert.notNull(entry.getValue(), segment + ".connection-properties 的值不能为 null");
                properties.setProperty(entry.getKey(), entry.getValue());
            }
        }
        return properties;
    }
}
