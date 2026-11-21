package com.csf.xline.db.sqlite;

import com.alibaba.druid.DbType;
import com.alibaba.druid.pool.DruidDataSource;
import com.csf.xline.core.XLineContext;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.core.jdbc.JdbcPoolProperties;
import com.csf.xline.core.sqlite.SqliteProperties;
import lombok.extern.slf4j.Slf4j;
import org.anyline.data.datasource.DataSourceHolder;
import org.anyline.data.runtime.DataRuntime;
import org.anyline.metadata.type.DatabaseType;
import org.flywaydb.core.Flyway;
import org.springframework.util.Assert;

import javax.sql.DataSource;

/**
 * SQLite 数据源初始化，仅在节点配置了 {@code sqlite} 配置段时被调用。
 */
@Slf4j
public class DataSourceInitializer {

    public static void initDataSource(SqliteNode node) throws Exception {
        SqliteProperties properties = node.getSqlite() == null ? new SqliteProperties() : node.getSqlite();
        String jdbcUrl = properties.resolvedJdbcUrl();
        JdbcPoolProperties pool = properties.resolvedPool();
        pool.validate(NodeType.SQLITE.getSegment());

        DruidDataSource ds = new DruidDataSource();
        ds.setName(node.getName());
        ds.setUrl(jdbcUrl);
        ds.setDbType(DbType.sqlite);
        ds.setDriverClassName(properties.resolvedDriverClassName());
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
        ds.setConnectProperties(properties.resolvedConnectionProperties(NodeType.SQLITE.getSegment()));

        // 连接失败自动重试，避免因数据库短暂不可用导致节点不可用
        ds.setBreakAfterAcquireFailure(false);
        ds.setTimeBetweenConnectErrorMillis(pool.getTimeBetweenConnectErrorMillis());
        ds.setConnectionErrorRetryAttempts(Integer.MAX_VALUE);

        DataRuntime dataRuntime = DataSourceHolder.reg(node.getName(), ds, DatabaseType.SQLite);
        Assert.notNull(dataRuntime, String.format("【%s】 DataSource Init Failed", node.getName()));

        XLineContext.DATA_SOURCES.put(node.getName(), ds);

        log.info("【DataSource - SQLite】reg success: {}", node.getName());

        if (properties.flywayEnabled()) {
            Flyway.configure()
                    .dataSource(ds)
                    .baselineOnMigrate(properties.resolvedBaselineOnMigrate())
                    .locations(properties.resolvedFlywayLocations(NodeType.SQLITE.getSegment(), node.getName()))
                    .load()
                    .migrate();
        }
    }

    /**
     * 销毁节点数据源。
     *
     * @param node SQLite 节点
     * @throws Exception anyline 注销数据源失败
     */
    public static void destroy(SqliteNode node) throws Exception {
        DataSourceHolder.destroy(node.getName());

        DataSource dataSource = XLineContext.DATA_SOURCES.remove(node.getName());
        if (dataSource instanceof DruidDataSource) {
            ((DruidDataSource) dataSource).close();
        }
    }
}
