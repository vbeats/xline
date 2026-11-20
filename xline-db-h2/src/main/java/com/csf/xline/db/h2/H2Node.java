package com.csf.xline.db.h2;

import com.alibaba.druid.DbType;
import com.alibaba.druid.pool.DruidDataSource;
import com.csf.xline.core.Node;
import com.csf.xline.core.XLineContext;
import com.csf.xline.core.h2.H2Properties;
import lombok.extern.slf4j.Slf4j;
import org.anyline.data.datasource.DataSourceHolder;
import org.anyline.data.runtime.DataRuntime;
import org.anyline.data.runtime.RuntimeHolder;
import org.anyline.metadata.type.DatabaseType;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
public class H2Node extends Node {

    private transient DruidDataSource dataSource;

    private transient String registeredName;

    public DataSource dataSource() {
        return dataSource;
    }

    @Override
    protected void initDataSource() throws Exception {
        Assert.hasText(getName(), "h2 node name must not be blank");
        Assert.state(dataSource == null, "h2 node is already initialized");
        H2Properties properties = getH2() == null ? new H2Properties() : getH2();
        H2Properties.Pool pool = properties.resolvedPool();
        pool.validate();
        String jdbcUrl = properties.resolvedJdbcUrl(getName());
        Assert.state(!DataSourceHolder.contains(getName()), "h2 datasource name is already registered");

        DruidDataSource ds = new DruidDataSource();
        dataSource = ds;
        registeredName = getName();
        ds.setName(registeredName);
        ds.setUrl(jdbcUrl);
        ds.setDbType(DbType.h2);
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
        ds.setTestOnReturn(false);
        ds.setTestWhileIdle(pool.isTestWhileIdle());
        ds.setTimeBetweenEvictionRunsMillis(pool.getTimeBetweenEvictionRunsMillis());
        ds.setMinEvictableIdleTimeMillis(pool.getMinEvictableIdleTimeMillis());
        ds.setMaxEvictableIdleTimeMillis(Math.max(ds.getMaxEvictableIdleTimeMillis(),
                pool.getMinEvictableIdleTimeMillis()));
        ds.setInitExceptionThrow(true);
        ds.setBreakAfterAcquireFailure(true);
        ds.setConnectionErrorRetryAttempts(0);
        ds.setFailFast(true);
        ds.init();
        try (Connection connection = ds.getConnection()) {
            Assert.state(connection.isValid(pool.getValidationQueryTimeoutSeconds()),
                    "h2 datasource connection validation failed");
        }

        if (properties.isFlyway()) {
            Flyway.configure()
                    .dataSource(ds)
                    .baselineOnMigrate(properties.isBaselineOnMigrate())
                    .locations(properties.resolvedFlywayLocations(registeredName))
                    .load()
                    .migrate();
        }

        DataRuntime runtime = DataSourceHolder.reg(registeredName, ds, DatabaseType.H2, false);
        Assert.notNull(runtime, "h2 datasource registration failed");
        DataSource existing = XLineContext.DATA_SOURCES.putIfAbsent(registeredName, ds);
        Assert.state(existing == null || existing == ds, "h2 routing datasource name is already registered");
        log.info("【DataSource - H2】reg success: {}", registeredName);
    }

    @Override
    protected void destroyDataSource() throws Exception {
        DruidDataSource ds = dataSource;
        String name = registeredName;
        dataSource = null;
        registeredName = null;
        if (ds == null) {
            return;
        }
        try {
            if (name != null) {
                XLineContext.DATA_SOURCES.remove(name, ds);
                if (DataSourceHolder.contains(name)) {
                    Object processor = RuntimeHolder.runtime(name).getProcessor();
                    if (processor == ds || (processor instanceof JdbcTemplate
                            && ((JdbcTemplate) processor).getDataSource() == ds)) {
                        Assert.state(DataSourceHolder.destroy(name), "h2 datasource unregistration failed");
                    }
                }
            }
        } finally {
            ds.close();
        }
    }
}
