package com.csf.xline.db.sqlserver.jtds;

import com.alibaba.druid.DbType;
import com.alibaba.druid.pool.DruidDataSource;
import com.csf.xline.core.XLineContext;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.core.jdbc.JdbcPoolProperties;
import com.csf.xline.core.mssql.MssqlProperties;
import lombok.extern.slf4j.Slf4j;
import org.anyline.data.datasource.DataSourceHolder;
import org.anyline.data.runtime.DataRuntime;
import org.anyline.metadata.type.DatabaseType;
import org.flywaydb.core.Flyway;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * SQL Server 数据源初始化（jTDS 驱动）。
 *
 * <p>jTDS 的 URL 与 mssql-jdbc 不同：
 * 库名写在路径中、属性名也与官方驱动不一致（{@code instance} 而非 {@code instanceName}），
 * 且不支持 {@code encrypt} / {@code trustServerCertificate}
 * （jTDS 的加密走 {@code ssl} 属性，需加密请直接配置 jdbcUrl）。
 */
@Slf4j
public class DataSourceInitializer {

    /** jTDS 驱动 */
    private static final String DEFAULT_DRIVER = "net.sourceforge.jtds.jdbc.Driver";

    public static void initDataSource(JtdsNode node) throws Exception {
        MssqlProperties properties = node.getMssql() == null ? new MssqlProperties() : node.getMssql();
        String jdbcUrl = properties.explicitJdbcUrl() != null ? properties.explicitJdbcUrl() : buildUrl(properties);
        JdbcPoolProperties pool = properties.resolvedPool();
        pool.validate(NodeType.MSSQL.getSegment());

        DruidDataSource ds = new DruidDataSource();
        ds.setName(node.getName());
        ds.setUrl(jdbcUrl);
        ds.setDbType(DbType.sqlserver);

        String driver = properties.resolvedDriverClassName();
        ds.setDriverClassName(driver == null ? DEFAULT_DRIVER : driver);

        ds.setUsername(properties.getUsername());
        ds.setPassword(properties.getPassword());
        ds.setInitialSize(pool.getInitialSize());
        ds.setMinIdle(pool.getMinIdle());
        ds.setMaxActive(pool.getMaxActive());
        ds.setMaxWait(pool.getMaxWaitMillis());
        ds.setValidationQuery(pool.getValidationQuery());
        ds.setValidationQueryTimeout(pool.getValidationQueryTimeoutSeconds());
        ds.setTestOnBorrow(pool.isTestOnBorrow());
        ds.setTestOnReturn(pool.isTestOnReturn());
        ds.setTestWhileIdle(pool.isTestWhileIdle());
        ds.setTimeBetweenEvictionRunsMillis(pool.getTimeBetweenEvictionRunsMillis());
        // 先抬高最大回收时间，再设最小值：Druid 要求 min <= max
        ds.setMaxEvictableIdleTimeMillis(Math.max(ds.getMaxEvictableIdleTimeMillis(),
                pool.getMinEvictableIdleTimeMillis()));
        ds.setMinEvictableIdleTimeMillis(pool.getMinEvictableIdleTimeMillis());
        ds.setConnectProperties(properties.resolvedConnectionProperties(NodeType.MSSQL.getSegment()));

        // 连接失败自动重试，避免因数据库短暂不可用导致节点不可用
        ds.setBreakAfterAcquireFailure(false);
        ds.setTimeBetweenConnectErrorMillis(pool.getTimeBetweenConnectErrorMillis());
        ds.setConnectionErrorRetryAttempts(Integer.MAX_VALUE);

        DataRuntime dataRuntime = DataSourceHolder.reg(node.getName(), ds, DatabaseType.MSSQL);
        Assert.notNull(dataRuntime, String.format("【%s】 DataSource Init Failed", node.getName()));

        XLineContext.DATA_SOURCES.put(node.getName(), ds);

        log.info("【DataSource - Sql Server(jTDS)】reg success: {}", node.getName());

        if (properties.flywayEnabled()) {
            Flyway.configure()
                    .dataSource(ds)
                    .baselineOnMigrate(properties.resolvedBaselineOnMigrate())
                    .locations(properties.resolvedFlywayLocations(NodeType.MSSQL.getSegment(), node.getName()))
                    .load()
                    .migrate();
        }
    }

    /**
     * 销毁节点数据源。
     *
     * @param node SQL Server 节点
     * @throws Exception anyline 注销数据源失败
     */
    public static void destroy(JtdsNode node) throws Exception {
        DataSourceHolder.destroy(node.getName());

        DataSource dataSource = XLineContext.DATA_SOURCES.remove(node.getName());
        if (dataSource instanceof DruidDataSource) {
            ((DruidDataSource) dataSource).close();
        }
    }

    /**
     * 按 jTDS 格式拼接 URL：库名写在路径中，属性以分号分隔。
     *
     * @param properties 节点配置
     * @return JDBC URL
     */
    private static String buildUrl(MssqlProperties properties) {
        StringBuilder url = new StringBuilder(
                String.format("jdbc:jtds:sqlserver://%s:%d", properties.resolvedHost(), properties.resolvedPort()));
        if (StringUtils.hasText(properties.getDatabaseName())) {
            url.append('/').append(properties.getDatabaseName());
        }
        if (StringUtils.hasText(properties.getInstance())) {
            url.append(";instance=").append(properties.getInstance());
        }
        if (StringUtils.hasText(properties.getDomain())) {
            url.append(";domain=").append(properties.getDomain());
        }
        return url.toString();
    }
}
