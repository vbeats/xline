package com.csf.xline.db.sqlite;

import com.csf.xline.core.Node;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SQLite 节点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SqliteNode extends Node {

    @Override
    protected void initDataSource() throws Exception {
        DataSourceInitializer.initDataSource(this);
    }

    @Override
    protected void destroyDataSource() throws Exception {
        DataSourceInitializer.destroy(this);
    }
}
