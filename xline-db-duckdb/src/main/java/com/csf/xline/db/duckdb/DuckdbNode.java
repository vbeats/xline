package com.csf.xline.db.duckdb;

import com.csf.xline.core.Node;
import com.csf.xline.core.duckdb.DuckdbProperties;
import lombok.extern.slf4j.Slf4j;
import org.duckdb.DuckDBConnection;
import org.duckdb.DuckDBDriver;
import org.springframework.util.Assert;

@Slf4j
public class DuckdbNode extends Node {

    private transient DuckDBConnection connection;

    public DuckDBConnection connection() {
        return connection;
    }

    @Override
    protected void initDataSource() throws Exception {
        Assert.state(connection == null, "duckdb node is already initialized");
        DuckdbProperties properties = getDuckdb() == null ? new DuckdbProperties() : getDuckdb();
        connection = (DuckDBConnection) new DuckDBDriver().connect(
                properties.resolvedJdbcUrl(), properties.resolvedConnectionProperties());
        Assert.notNull(connection, "duckdb connection initialization failed");
        log.info("【DataSource - DuckDB】reg success: {}", getName());
    }

    @Override
    protected void destroyDataSource() throws Exception {
        DuckDBConnection current = connection;
        connection = null;
        if (current != null) {
            current.close();
        }
    }
}
