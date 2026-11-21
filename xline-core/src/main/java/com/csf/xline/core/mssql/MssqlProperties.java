package com.csf.xline.core.mssql;

import com.csf.xline.core.jdbc.JdbcServerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.util.StringUtils;

/**
 * SQL Server 节点配置段（{@code xline.nodes[].mssql}）。
 *
 * <p>本类只承载「连什么库」，不承载「怎么连」：
 * mssql-jdbc 与 jTDS 的 URL 格式、可用属性、默认驱动均不同，
 * 由引入的驱动模块按各自情况拼接（见各模块的 {@code DataSourceInitializer}）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MssqlProperties extends JdbcServerProperties {
    /** SQL Server 默认端口 */
    public static final int DEFAULT_PORT = 1433;

    /** 实例名：mssql-jdbc 拼为 {@code instanceName}，jTDS 拼为 {@code instance} */
    private String instance;
    /** 域：Windows 域认证时使用，两个驱动均支持 domain 属性 */
    private String domain;
    /** 传输加密（仅 mssql-jdbc），默认 false */
    private Boolean encrypt = Boolean.FALSE;
    /** 信任服务端证书（仅 mssql-jdbc），默认 true */
    private Boolean trustServerCertificate = Boolean.TRUE;

    @Override
    protected int defaultPort() {
        return DEFAULT_PORT;
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
        return StringUtils.hasText(getDriverClassName()) ? getDriverClassName().trim() : null;
    }
}
