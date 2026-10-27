package com.csf.xline.core.postgres;

import lombok.Data;

/**
 * PostgreSQL 节点配置段（{@code xline.nodes[].postgresql}）。
 */
@Data
public class PostgresProperties {

    /** PostgreSQL 默认端口 */
    public static final int DEFAULT_PORT = 5432;

    private String host;

    private Integer port;

    private String databaseName;

    private String username;

    private String password;

    private String driverClassName;

    private String jdbcUrl;

    private Boolean flyway = Boolean.FALSE;

    public int resolvedPort() {
        return port == null || port <= 0 ? DEFAULT_PORT : port;
    }

    public String resolvedDriverClassName() {
        return driverClassName == null || driverClassName.trim().isEmpty()
                ? "org.postgresql.Driver" : driverClassName;
    }
}
