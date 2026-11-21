package com.csf.xline.core.postgres;

import com.csf.xline.core.jdbc.JdbcServerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.util.StringUtils;

/**
 * PostgreSQL 节点配置段（{@code xline.nodes[].postgresql}）。
 *
 * <p>最少只需 {@code database-name}（或 {@code jdbc-url}）与账号，其余均有默认值。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PostgresProperties extends JdbcServerProperties {
    /** PostgreSQL 默认端口 */
    public static final int DEFAULT_PORT = 5432;
    /** 默认驱动 */
    public static final String DEFAULT_DRIVER = "org.postgresql.Driver";

    /** 默认 schema（拼为 {@code currentSchema}），未配置时由服务端 search_path 决定 */
    private String schema;

    @Override
    protected int defaultPort() {
        return DEFAULT_PORT;
    }

    /** @return 生效驱动类名 */
    public String resolvedDriverClassName() {
        return StringUtils.hasText(getDriverClassName()) ? getDriverClassName().trim() : DEFAULT_DRIVER;
    }

    /** @return 生效连接串：显式 {@code jdbc-url} 优先，否则按字段拼装 */
    public String resolvedJdbcUrl() {
        String explicit = explicitJdbcUrl();
        if (explicit != null) {
            return explicit;
        }
        String url = String.format("jdbc:postgresql://%s:%d/%s",
                resolvedHost(), resolvedPort(), requiredDatabaseName("postgresql"));
        return StringUtils.hasText(schema) ? url + "?currentSchema=" + schema.trim() : url;
    }
}
