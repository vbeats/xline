package com.csf.xline.db.mysql;

import com.alibaba.druid.DbType;
import com.alibaba.druid.pool.DruidDataSource;
import com.csf.xline.core.XLineContext;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.core.mysql.MysqlProperties;
import lombok.extern.slf4j.Slf4j;
import org.anyline.data.datasource.DataSourceHolder;
import org.anyline.data.runtime.DataRuntime;
import org.anyline.metadata.type.DatabaseType;
import org.flywaydb.core.Flyway;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
        MysqlProperties properties = node.getMysql();

        if (!StringUtils.hasText(properties.getJdbcUrl())) {
            properties.setJdbcUrl(String.format(
                    "jdbc:mysql://%s:%s/%s?useSSL=false&useUnicode=true&characterEncoding=utf-8"
                            + "&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true"
                            + "&tinyInt1isBit=false&allowMultiQueries=true&serverTimezone=GMT%%2B8"
                            + "&allowPublicKeyRetrieval=true",
                    properties.getHost(), properties.resolvedPort(), properties.getDatabaseName()));
        }

        DruidDataSource ds = new DruidDataSource();
        ds.setName(node.getName());
        ds.setUrl(properties.getJdbcUrl());
        ds.setDbType(DbType.mysql);
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

        // 连接失败自动重试，避免因数据库短暂不可用导致节点不可用
        ds.setBreakAfterAcquireFailure(false);
        ds.setTimeBetweenConnectErrorMillis(5000L);
        ds.setConnectionErrorRetryAttempts(Integer.MAX_VALUE);

        DataRuntime dataRuntime = DataSourceHolder.reg(node.getName(), ds, DatabaseType.MySQL);
        Assert.notNull(dataRuntime, String.format("【%s】 DataSource Init Failed", node.getName()));

        // MyBatis-Plus 方式：按节点名留存数据源，供路由数据源取用
        XLineContext.DATA_SOURCES.put(node.getName(), ds);

        log.info("【DataSource - MySQL】reg success: {}", node.getName());

        if (Boolean.TRUE.equals(properties.getFlyway())) {
            Flyway.configure()
                    .dataSource(ds)
                    .baselineOnMigrate(true)
                    .locations(String.format("classpath:%s/%s", NodeType.MYSQL.getSegment(), node.getName()))
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
