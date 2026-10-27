package com.csf.xline.core.mssql;

import lombok.Data;

/**
 * SQL Server 节点配置段（{@code xline.nodes[].sqlserver}）。
 *
 * <p>本类只承载「连什么库」，不承载「怎么连」：
 * mssql-jdbc 与 jTDS 的 URL 格式、可用属性、默认驱动均不同，
 * 由引入的驱动模块按各自情况拼接（见各模块的 {@code DataSourceInitializer}）。
 */
@Data
public class MssqlProperties {

    /** SQL Server 默认端口 */
    public static final int DEFAULT_PORT = 1433;

    private String host;

    private Integer port;

    private String databaseName;

    private String username;

    private String password;

    private String driverClassName;

    private String jdbcUrl;

    /** 实例名：mssql-jdbc 拼为 {@code instanceName}，jTDS 拼为 {@code instance} */
    private String instance;

    /** 域：Windows 域认证时使用，两个驱动均支持 domain 属性 */
    private String domain;

    /** 传输加密，与 vline 一致默认 false */
    private Boolean encrypt = Boolean.FALSE;

    /** 信任服务端证书，与 vline 一致默认 true */
    private Boolean trustServerCertificate = Boolean.TRUE;

    private Boolean flyway = Boolean.FALSE;

    public int resolvedPort() {
        return port == null || port <= 0 ? DEFAULT_PORT : port;
    }

    /**
     * 显式配置的驱动类名。
     *
     * <p>未配置时返回 {@code null}：默认驱动取决于引入的是哪个驱动模块
     * （mssql-jdbc 与 jTDS 的驱动类不同），由各模块自行兜底。
     *
     * @return 显式配置的驱动类名，未配置时为 {@code null}
     */
    public String resolvedDriverClassName() {
        return driverClassName == null || driverClassName.trim().isEmpty() ? null : driverClassName;
    }
}
