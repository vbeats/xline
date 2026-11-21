package com.csf.xline.core.mysql;

import com.csf.xline.core.jdbc.JdbcServerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.util.StringUtils;

/**
 * MySQL 节点配置段（{@code xline.nodes[].mysql}）。
 *
 * <p>最少只需 {@code database-name}（或 {@code jdbc-url}）与账号，其余均有默认值。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MysqlProperties extends JdbcServerProperties {
    /** MySQL 默认端口 */
    public static final int DEFAULT_PORT = 3306;
    /** 默认驱动 */
    public static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";

    /** 是否启用 SSL，默认关闭 */
    private Boolean useSsl = Boolean.FALSE;
    /** 会话时区，默认 GMT+8 */
    private String serverTimezone = "GMT+8";

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
        String timezone = StringUtils.hasText(serverTimezone) ? serverTimezone.trim() : "GMT+8";
        return String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&useUnicode=true&characterEncoding=utf-8"
                        + "&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true"
                        + "&tinyInt1isBit=false&allowMultiQueries=true&serverTimezone=%s"
                        + "&allowPublicKeyRetrieval=true",
                resolvedHost(), resolvedPort(), requiredDatabaseName("mysql"),
                Boolean.TRUE.equals(useSsl), timezone.replace("+", "%2B"));
    }
}
