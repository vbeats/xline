package com.csf.xline.core.mysql;

import lombok.Data;

/**
 * MySQL 节点配置段（{@code xline.nodes[].mysql}）。
 */
@Data
public class MysqlProperties {

    /** MySQL 默认端口 */
    public static final int DEFAULT_PORT = 3306;

    private String host;

    private Integer port;

    /** yml 中写 database-name */
    private String databaseName;

    private String username;

    private String password;

    /** JDBC 驱动类名，未配置时使用驱动自带默认值 */
    private String driverClassName;

    /** 完整 JDBC 连接串，配置后忽略 host/port/database-name 拼装 */
    private String jdbcUrl;

    /** 是否执行 Flyway 迁移，默认关闭 */
    private Boolean flyway = Boolean.FALSE;

    /** 生效端口：未配置时使用默认端口 */
    public int resolvedPort() {
        return port == null || port <= 0 ? DEFAULT_PORT : port;
    }

    /** 默认驱动类名 */
    public String resolvedDriverClassName() {
        return driverClassName == null || driverClassName.trim().isEmpty()
                ? "com.mysql.cj.jdbc.Driver" : driverClassName;
    }
}
