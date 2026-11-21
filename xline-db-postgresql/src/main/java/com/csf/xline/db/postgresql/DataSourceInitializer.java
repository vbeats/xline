package com.csf.xline.db.postgresql;

import com.alibaba.druid.DbType;
import com.alibaba.druid.pool.DruidDataSource;
import com.csf.xline.core.XLineContext;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.core.jdbc.JdbcPoolProperties;
import com.csf.xline.core.postgres.PostgresProperties;
import lombok.extern.slf4j.Slf4j;
import org.anyline.data.datasource.DataSourceHolder;
import org.anyline.data.runtime.DataRuntime;
import org.anyline.metadata.type.DatabaseType;
import org.flywaydb.core.Flyway;
import org.springframework.util.Assert;

import javax.sql.DataSource;

/**
 * PostgreSQL 数据源初始化，仅在节点配置了 {@code postgresql} 配置段时被调用。
 */
@Slf4j
public class DataSourceInitializer {

    public static void initDataSource(PostgresNode node) throws Exception {
        PostgresProperties properties = node.getPostgresql() == null ? new PostgresProperties() : node.getPostgresql();
        String jdbcUrl = properties.resolvedJdbcUrl();
        JdbcPoolProperties pool = properties.resolvedPool();
        pool.validate(NodeType.POSTGRESQL.getSegment());

        DruidDataSource ds = new DruidDataSource();
        ds.setName(node.getName());
        ds.setUrl(jdbcUrl);
        ds.setDbType(DbType.postgresql);
        ds.setDriverClassName(properties.resolvedDriverClassName());
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
        ds.setConnectProperties(properties.resolvedConnectionProperties(NodeType.POSTGRESQL.getSegment()));

        // 连接失败自动重试，避免因数据库短暂不可用导致节点不可用
        ds.setBreakAfterAcquireFailure(false);
        ds.setTimeBetweenConnectErrorMillis(pool.getTimeBetweenConnectErrorMillis());
        ds.setConnectionErrorRetryAttempts(Integer.MAX_VALUE);

        DataRuntime dataRuntime = DataSourceHolder.reg(node.getName(), ds, DatabaseType.PostgreSQL);
        Assert.notNull(dataRuntime, String.format("【%s】 DataSource Init Failed", node.getName()));

        XLineContext.DATA_SOURCES.put(node.getName(), ds);

        log.info("【DataSource - PostgreSQL】reg success: {}", node.getName());

        if (properties.flywayEnabled()) {
            Flyway.configure()
                    .dataSource(ds)
                    .baselineOnMigrate(properties.resolvedBaselineOnMigrate())
                    .locations(properties.resolvedFlywayLocations(NodeType.POSTGRESQL.getSegment(), node.getName()))
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
