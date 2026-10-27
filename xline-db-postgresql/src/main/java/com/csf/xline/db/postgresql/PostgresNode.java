package com.csf.xline.db.postgresql;

import com.csf.xline.core.Node;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * PostgreSQL 节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostgresNode extends Node {

    @Override
    protected void initDataSource() throws Exception {
        DataSourceInitializer.initDataSource(this);
    }

    @Override
    protected void destroyDataSource() throws Exception {
        DataSourceInitializer.destroy(this);
    }
}
