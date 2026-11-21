package com.csf.xline.core.jdbc;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 网络型数据库（MySQL / PostgreSQL / SQL Server）的公共配置。
 *
 * <p>{@code host} 默认 {@code 127.0.0.1}，{@code port} 默认该数据库的标准端口；
 * 未配置 {@code jdbc-url} 时 {@code database-name} 必填。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class JdbcServerProperties extends JdbcProperties {
    /** 默认主机 */
    public static final String DEFAULT_HOST = "127.0.0.1";

    /** 主机，默认 127.0.0.1 */
    private String host;
    /** 端口，默认该数据库的标准端口 */
    private Integer port;
    /** 库名，yml 中写 database-name */
    private String databaseName;
    private String username;
    @ToString.Exclude
    private String password;

    /** @return 该数据库的标准端口 */
    protected abstract int defaultPort();

    /** @return 生效主机 */
    public String resolvedHost() {
        return StringUtils.hasText(host) ? host.trim() : DEFAULT_HOST;
    }

    /** @return 生效端口 */
    public int resolvedPort() {
        return port == null || port <= 0 ? defaultPort() : port;
    }

    /**
     * 取库名：仅在按字段拼装连接串时必填。
     *
     * @param segment 数据库类型配置段名，用于报错提示
     * @return 库名
     */
    public String requiredDatabaseName(String segment) {
        Assert.hasText(databaseName, segment + " 节点未配置 jdbc-url 时必须配置 database-name");
        return databaseName.trim();
    }
}
