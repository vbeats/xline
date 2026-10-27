package com.csf.xline.db.postgresql;

import com.alibaba.druid.DbType;
import com.alibaba.druid.pool.DruidDataSource;
import com.csf.xline.core.XLineContext;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.core.postgres.PostgresProperties;
import lombok.extern.slf4j.Slf4j;
import org.anyline.data.datasource.DataSourceHolder;
import org.anyline.data.runtime.DataRuntime;
import org.anyline.metadata.type.DatabaseType;
import org.flywaydb.core.Flyway;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * PostgreSQL 数据源初始化，仅在节点配置了 {@code postgresql} 配置段时被调用。
 */
@Slf4j
public class DataSourceInitializer {

    public static void initDataSource(PostgresNode node) throws Exception {
        PostgresProperties properties = node.getPostgresql();

        if (!StringUtils.hasText(properties.getJdbcUrl())) {
            properties.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s",
                    properties.getHost(), properties.resolvedPort(), properties.getDatabaseName()));
        }

        DruidDataSource ds = new DruidDataSource();
        ds.setName(node.getName());
        ds.setUrl(properties.getJdbcUrl());
        ds.setDbType(DbType.postgresql);
        ds.setDriverClassName(properties.resolvedDriverClassName());
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

        DataRuntime dataRuntime = DataSourceHolder.reg(node.getName(), ds, DatabaseType.PostgreSQL);
        Assert.notNull(dataRuntime, String.format("【%s】 DataSource Init Failed", node.getName()));

        XLineContext.DATA_SOURCES.put(node.getName(), ds);

        log.info("【DataSource - PostgreSQL】reg success: {}", node.getName());

        if (Boolean.TRUE.equals(properties.getFlyway())) {
            Flyway.configure()
                    .dataSource(ds)
                    .baselineOnMigrate(true)
                    .locations(String.format("classpath:%s/%s", NodeType.POSTGRESQL.getSegment(), node.getName()))
                    .load()
                    .migrate();
        }
    }

    /**
     * 销毁节点数据源。
     *
     * @param node PostgreSQL 节点
     * @throws Exception anyline 注销数据源失败
     */
    public static void destroy(PostgresNode node) throws Exception {
        DataSourceHolder.destroy(node.getName());

        DataSource dataSource = XLineContext.DATA_SOURCES.remove(node.getName());
        if (dataSource instanceof DruidDataSource) {
            ((DruidDataSource) dataSource).close();
        }
    }
}
