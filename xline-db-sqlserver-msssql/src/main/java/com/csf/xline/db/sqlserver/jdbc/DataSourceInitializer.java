package com.csf.xline.db.sqlserver.jdbc;

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
 * SQL Server 数据源初始化（mssql-jdbc 驱动）。
 *
 * <p>mssql-jdbc 的 URL 形如
 * {@code jdbc:sqlserver://host:port;databaseName=db;encrypt=false;trustServerCertificate=true;}，
 * 全部属性以分号分隔、库名用 {@code databaseName} 属性指定。
 */
@Slf4j
public class DataSourceInitializer {

    /** 微软官方驱动 */
    private static final String DEFAULT_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    public static void initDataSource(MssqlNode node) throws Exception {
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

        log.info("【DataSource - Sql Server(mssql-jdbc)】reg success: {}", node.getName());

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
    public static void destroy(MssqlNode node) throws Exception {
        DataSourceHolder.destroy(node.getName());

        DataSource dataSource = XLineContext.DATA_SOURCES.remove(node.getName());
        if (dataSource instanceof DruidDataSource) {
            ((DruidDataSource) dataSource).close();
        }
    }

    /**
     * 按 mssql-jdbc 格式拼接 URL：属性以分号分隔，库名用 {@code databaseName}。
     *
     * @param properties 节点配置
     * @return JDBC URL
     */
    private static String buildUrl(MssqlProperties properties) {
        StringBuilder url = new StringBuilder(String.format(
                "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=%s;trustServerCertificate=%s;",
                properties.resolvedHost(), properties.resolvedPort(), properties.requiredDatabaseName(NodeType.MSSQL.getSegment()),
                Boolean.TRUE.equals(properties.getEncrypt()), !Boolean.FALSE.equals(properties.getTrustServerCertificate())));
        if (StringUtils.hasText(properties.getInstance())) {
            url.append("instanceName=").append(properties.getInstance()).append(';');
        }
        if (StringUtils.hasText(properties.getDomain())) {
            url.append("domain=").append(properties.getDomain()).append(';');
        }
        return url.toString();
    }
}
