package com.csf.xline.db.support;

import com.alibaba.druid.pool.DruidDataSource;
import com.csf.xline.core.XLineContext;
import com.csf.xline.core.jdbc.JdbcPoolProperties;
import com.csf.xline.core.jdbc.JdbcProperties;
import lombok.extern.slf4j.Slf4j;
import org.anyline.data.datasource.DataSourceHolder;
import org.anyline.data.runtime.DataRuntime;
import org.anyline.data.runtime.RuntimeHolder;
import org.anyline.metadata.type.DatabaseType;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;

/**
 * 关系型数据库节点共用的 Druid、AnyLine 与 Flyway 生命周期。
 */
@Slf4j
public final class DruidDataSourceSupport {

    private DruidDataSourceSupport() {
    }

    /**
     * 创建、注册并按配置迁移节点数据源。
     *
     * @param properties JDBC 公共配置
     * @param spec       数据库差异参数
     * @throws Exception 数据源注册或迁移失败
     */
    public static void initialize(JdbcProperties properties, DruidDataSourceSpec spec) throws Exception {
        Assert.notNull(properties, "jdbc properties must not be null");
        Assert.notNull(spec, "druid datasource spec must not be null");
        Assert.hasText(spec.getNodeName(), "node name must not be blank");
        Assert.notNull(spec.getNodeType(), "node type must not be null");
        Assert.notNull(spec.getDruidType(), "druid database type must not be null");
        Assert.notNull(spec.getDatabaseType(), "anyline database type must not be null");
        Assert.hasText(spec.getJdbcUrl(), "jdbc url must not be blank");
        Assert.hasText(spec.getDriverClassName(), "jdbc driver class name must not be blank");

        String segment = spec.getNodeType().getSegment();
        JdbcPoolProperties pool = properties.resolvedPool();
        pool.validate(segment);

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setName(spec.getNodeName());
        dataSource.setUrl(spec.getJdbcUrl());
        dataSource.setDbType(spec.getDruidType());
        dataSource.setDriverClassName(spec.getDriverClassName());
        if (spec.getUsername() != null) {
            dataSource.setUsername(spec.getUsername());
        }
        if (spec.getPassword() != null) {
            dataSource.setPassword(spec.getPassword());
        }
        applyPool(dataSource, pool);
        dataSource.setConnectProperties(properties.resolvedConnectionProperties(segment));

        // 网络数据库短暂不可用时持续重试，节点无需由外部重建。
        dataSource.setBreakAfterAcquireFailure(false);
        dataSource.setTimeBetweenConnectErrorMillis(pool.getTimeBetweenConnectErrorMillis());
        dataSource.setConnectionErrorRetryAttempts(Integer.MAX_VALUE);

        DataRuntime runtime = DataSourceHolder.reg(spec.getNodeName(), dataSource, spec.getDatabaseType());
        Assert.notNull(runtime, String.format("【%s】 DataSource Init Failed", spec.getNodeName()));
        XLineContext.DATA_SOURCES.put(spec.getNodeName(), dataSource);

        log.info("【DataSource - {}】reg success: {}", spec.getLogName(), spec.getNodeName());
        migrate(dataSource, properties, segment, spec.getNodeName());
    }

    /**
     * 应用共用连接池参数。
     *
     * @param dataSource Druid 数据源
     * @param pool       连接池配置
     */
    public static void applyPool(DruidDataSource dataSource, JdbcPoolProperties pool) {
        dataSource.setInitialSize(pool.getInitialSize());
        dataSource.setMinIdle(pool.getMinIdle());
        dataSource.setMaxActive(pool.getMaxActive());
        dataSource.setMaxWait(pool.getMaxWaitMillis());
        dataSource.setValidationQuery(pool.getValidationQuery());
        dataSource.setValidationQueryTimeout(pool.getValidationQueryTimeoutSeconds());
        dataSource.setTestOnBorrow(pool.isTestOnBorrow());
        dataSource.setTestOnReturn(pool.isTestOnReturn());
        dataSource.setTestWhileIdle(pool.isTestWhileIdle());
        dataSource.setTimeBetweenEvictionRunsMillis(pool.getTimeBetweenEvictionRunsMillis());
        // Druid 要求最小空闲回收时间不大于最大值，先抬高最大值再设置最小值。
        dataSource.setMaxEvictableIdleTimeMillis(Math.max(dataSource.getMaxEvictableIdleTimeMillis(),
                pool.getMinEvictableIdleTimeMillis()));
        dataSource.setMinEvictableIdleTimeMillis(pool.getMinEvictableIdleTimeMillis());
    }

    /**
     * 按节点配置执行 Flyway。
     *
     * @param dataSource 数据源
     * @param properties JDBC 公共配置
     * @param segment    配置段名
     * @param nodeName   节点名
     */
    public static void migrate(DataSource dataSource, JdbcProperties properties,
                               String segment, String nodeName) {
        if (!properties.flywayEnabled()) {
            return;
        }
        Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(properties.resolvedBaselineOnMigrate())
                .locations(properties.resolvedFlywayLocations(segment, nodeName))
                .load()
                .migrate();
    }

    /**
     * 销毁普通关系型数据库节点的数据源。
     *
     * @param nodeName 节点名
     * @throws Exception AnyLine 注销失败
     */
    public static void destroy(String nodeName) throws Exception {
        DataSourceHolder.destroy(nodeName);
        DataSource dataSource = XLineContext.DATA_SOURCES.remove(nodeName);
        if (dataSource instanceof DruidDataSource) {
            ((DruidDataSource) dataSource).close();
        }
    }

    /**
     * 注册不允许覆盖同名项的数据源。
     *
     * @param nodeName     节点名
     * @param dataSource   数据源
     * @param databaseType AnyLine 数据库类型
     * @return AnyLine 运行时
     */
    public static DataRuntime registerIfAbsent(String nodeName, DruidDataSource dataSource,
                                               DatabaseType databaseType) throws Exception {
        return DataSourceHolder.reg(nodeName, dataSource, databaseType, false);
    }

    /**
     * 仅注销并关闭当前节点实际拥有的数据源。
     *
     * @param nodeName   节点名
     * @param dataSource 节点创建的数据源
     * @throws Exception AnyLine 注销失败
     */
    public static void destroyOwned(String nodeName, DruidDataSource dataSource) throws Exception {
        try {
            XLineContext.DATA_SOURCES.remove(nodeName, dataSource);
            if (DataSourceHolder.contains(nodeName) && ownsRegistration(nodeName, dataSource)) {
                Assert.state(DataSourceHolder.destroy(nodeName), "datasource unregistration failed: " + nodeName);
            }
        } finally {
            dataSource.close();
        }
    }

    private static boolean ownsRegistration(String nodeName, DruidDataSource dataSource) {
        Object processor = RuntimeHolder.runtime(nodeName).getProcessor();
        return processor == dataSource || (processor instanceof JdbcTemplate
                && ((JdbcTemplate) processor).getDataSource() == dataSource);
    }
}
