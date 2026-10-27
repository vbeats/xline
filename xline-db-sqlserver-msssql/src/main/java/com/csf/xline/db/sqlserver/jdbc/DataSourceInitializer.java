package com.csf.xline.db.sqlserver.jdbc;

import com.alibaba.druid.DbType;
import com.alibaba.druid.pool.DruidDataSource;
import com.csf.xline.core.XLineContext;
import com.csf.xline.core.enums.NodeType;
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
        MssqlProperties properties = node.getMssql();

        if (!StringUtils.hasText(properties.getJdbcUrl())) {
            properties.setJdbcUrl(buildUrl(properties));
        }

        DruidDataSource ds = new DruidDataSource();
        ds.setName(node.getName());
        ds.setUrl(properties.getJdbcUrl());
        ds.setDbType(DbType.sqlserver);

        String driver = properties.resolvedDriverClassName();
        ds.setDriverClassName(driver == null ? DEFAULT_DRIVER : driver);

        ds.setUsername(properties.getUsername());
        ds.setPassword(properties.getPassword());
        ds.setInitialSize(5);
        ds.setMaxActive(10);
        ds.setMinIdle(5);
        ds.setMaxWait(60000L);
        ds.setValidationQuery("select 1");
        ds.setValidationQueryTimeout(2000);
        ds.setTestOnBorrow(false);
        ds.setTestOnReturn(false);
        ds.setTestWhileIdle(true);
        ds.setTimeBetweenEvictionRunsMillis(60000L);
        ds.setMinEvictableIdleTimeMillis(300000L);

        ds.setBreakAfterAcquireFailure(false);
        ds.setTimeBetweenConnectErrorMillis(5000L);
        ds.setConnectionErrorRetryAttempts(Integer.MAX_VALUE);

        DataRuntime dataRuntime = DataSourceHolder.reg(node.getName(), ds, DatabaseType.MSSQL);
        Assert.notNull(dataRuntime, String.format("【%s】 DataSource Init Failed", node.getName()));

        XLineContext.DATA_SOURCES.put(node.getName(), ds);

        log.info("【DataSource - Sql Server(mssql-jdbc)】reg success: {}", node.getName());

        if (Boolean.TRUE.equals(properties.getFlyway())) {
            Flyway.configure()
                    .dataSource(ds)
                    .baselineOnMigrate(true)
                    .locations(String.format("classpath:%s/%s", NodeType.MSSQL.getSegment(), node.getName()))
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
                properties.getHost(), properties.resolvedPort(), properties.getDatabaseName(),
                properties.getEncrypt(), properties.getTrustServerCertificate()));
        if (StringUtils.hasText(properties.getInstance())) {
            url.append("instanceName=").append(properties.getInstance()).append(';');
        }
        if (StringUtils.hasText(properties.getDomain())) {
            url.append("domain=").append(properties.getDomain()).append(';');
        }
        return url.toString();
    }
}
