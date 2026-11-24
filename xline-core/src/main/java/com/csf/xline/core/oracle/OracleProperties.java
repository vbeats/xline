package com.csf.xline.core.oracle;

import com.csf.xline.core.jdbc.JdbcPoolProperties;
import com.csf.xline.core.jdbc.JdbcServerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.util.StringUtils;

/**
 * Oracle 节点配置段（{@code xline.nodes[].oracle}）。
 *
 * <p>支持 11g(11.2.0.4) 及以上全部服务端版本。连接目标按优先级取用：
 * {@code jdbc-url} 显式连接串 → {@code service-name}（服务名，12c+ 常用）→
 * {@code database-name}（SID，11g 常用）。
 *
 * <p>默认校验 SQL 为 {@code select 1 from dual}：Oracle 不接受缺省的 {@code select 1}，
 * 显式配置了 {@code pool.validation-query} 时以配置为准。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OracleProperties extends JdbcServerProperties {
    /** Oracle 默认端口 */
    public static final int DEFAULT_PORT = 1521;
    /** 默认驱动：ojdbc8 / ojdbc11 通用 */
    public static final String DEFAULT_DRIVER = "oracle.jdbc.OracleDriver";
    /** Oracle 默认连接校验 SQL */
    public static final String ORACLE_VALIDATION_QUERY = "select 1 from dual";

    /** 数据库服务名，配置后 {@code database-name} 不再参与连接串拼装 */
    private String serviceName;

    @Override
    protected int defaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    public JdbcPoolProperties resolvedPool() {
        JdbcPoolProperties pool = super.resolvedPool();
        if (JdbcPoolProperties.DEFAULT_VALIDATION_QUERY.equals(pool.getValidationQuery())) {
            pool.setValidationQuery(ORACLE_VALIDATION_QUERY);
        }
        return pool;
    }

    /** @return 生效驱动类名 */
    public String resolvedDriverClassName() {
        return StringUtils.hasText(getDriverClassName()) ? getDriverClassName().trim() : DEFAULT_DRIVER;
    }

    /** @return 生效连接串：显式 {@code jdbc-url} 优先，否则按服务名或 SID 拼装 */
    public String resolvedJdbcUrl() {
        String explicit = explicitJdbcUrl();
        if (explicit != null) {
            return explicit;
        }
        if (StringUtils.hasText(serviceName)) {
            return String.format("jdbc:oracle:thin:@//%s:%d/%s",
                    resolvedHost(), resolvedPort(), serviceName.trim());
        }
        // 11g 及部分旧部署只识别 SID 形态；两种形态不能混用，由配置方二选一
        return String.format("jdbc:oracle:thin:@%s:%d:%s",
                resolvedHost(), resolvedPort(), requiredDatabaseName("oracle"));
    }
}
