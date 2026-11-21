package com.csf.xline.db.mysql;

import com.alibaba.druid.DbType;
import com.alibaba.druid.pool.DruidDataSource;
import com.csf.xline.core.XLineContext;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.core.jdbc.JdbcPoolProperties;
import com.csf.xline.core.mysql.MysqlProperties;
import lombok.extern.slf4j.Slf4j;
import org.anyline.data.datasource.DataSourceHolder;
import org.anyline.data.runtime.DataRuntime;
import org.anyline.metadata.type.DatabaseType;
import org.flywaydb.core.Flyway;
import org.springframework.util.Assert;

import javax.sql.DataSource;

/**
 * MySQL 数据源初始化。
 *
 * <p>仅在节点配置了 {@code mysql} 配置段时被调用；未配置 MySQL 的节点不会创建任何连接。
 *
 * <p>数据源同时对外暴露两种使用方式：
 * <ol>
 *     <li>anyline —— 注册进 {@code DataSourceHolder}（vline 同款）；</li>
 *     <li>MyBatis-Plus —— 存入 {@link XLineContext#DATA_SOURCES}，
 *         经容器中的路由数据源按节点名取用。</li>
 * </ol>
 */
@Slf4j
public class DataSourceInitializer {

    /**
     * 创建并注册节点数据源。
     *
     * @param node MySQL 节点
     * @throws Exception 数据源注册失败
     */
    public static void initDataSource(MysqlNode node) throws Exception {
        MysqlProperties properties = node.getMysql() == null ? new MysqlProperties() : node.getMysql();
        String jdbcUrl = properties.resolvedJdbcUrl();
        JdbcPoolProperties pool = properties.resolvedPool();
        pool.validate(NodeType.MYSQL.getSegment());

        DruidDataSource ds = new DruidDataSource();
        ds.setName(node.getName());
        ds.setUrl(jdbcUrl);
        ds.setDbType(DbType.mysql);
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
        ds.setConnectProperties(properties.resolvedConnectionProperties(NodeType.MYSQL.getSegment()));

        // 连接失败自动重试，避免因数据库短暂不可用导致节点不可用
        ds.setBreakAfterAcquireFailure(false);
        ds.setTimeBetweenConnectErrorMillis(pool.getTimeBetweenConnectErrorMillis());
        ds.setConnectionErrorRetryAttempts(Integer.MAX_VALUE);

        DataRuntime dataRuntime = DataSourceHolder.reg(node.getName(), ds, DatabaseType.MySQL);
        Assert.notNull(dataRuntime, String.format("【%s】 DataSource Init Failed", node.getName()));

        // MyBatis-Plus 方式：按节点名留存数据源，供路由数据源取用
        XLineContext.DATA_SOURCES.put(node.getName(), ds);

        log.info("【DataSource - MySQL】reg success: {}", node.getName());

        if (properties.flywayEnabled()) {
            Flyway.configure()
                    .dataSource(ds)
                    .baselineOnMigrate(properties.resolvedBaselineOnMigrate())
                    .locations(properties.resolvedFlywayLocations(NodeType.MYSQL.getSegment(), node.getName()))
                    .load()
                    .migrate();
        }
    }

    /**
     * 销毁节点数据源。
     *
     * @param node MySQL 节点
     * @throws Exception anyline 注销数据源失败
     */
    public static void destroy(MysqlNode node) throws Exception {
        DataSourceHolder.destroy(node.getName());

        DataSource dataSource = XLineContext.DATA_SOURCES.remove(node.getName());
        if (dataSource instanceof DruidDataSource) {
            ((DruidDataSource) dataSource).close();
        }
    }
}
