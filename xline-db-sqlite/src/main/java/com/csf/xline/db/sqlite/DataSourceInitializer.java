package com.csf.xline.db.sqlite;

import com.alibaba.druid.DbType;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.core.sqlite.SqliteProperties;
import com.csf.xline.db.support.DruidDataSourceSpec;
import com.csf.xline.db.support.DruidDataSourceSupport;
import org.anyline.metadata.type.DatabaseType;

/**
 * SQLite 数据源初始化，仅在节点配置了 {@code sqlite} 配置段时被调用。
 */
public class DataSourceInitializer {

    public static void initDataSource(SqliteNode node) throws Exception {
        SqliteProperties properties = node.getSqlite() == null ? new SqliteProperties() : node.getSqlite();
        DruidDataSourceSpec spec = new DruidDataSourceSpec(
                node.getName(), NodeType.SQLITE, DbType.sqlite, DatabaseType.SQLite)
                .setJdbcUrl(properties.resolvedJdbcUrl())
                .setDriverClassName(properties.resolvedDriverClassName())
                .setLogName("SQLite");
        DruidDataSourceSupport.initialize(properties, spec);
    }

    /**
     * 销毁节点数据源。
     *
     * @param node SQLite 节点
     * @throws Exception anyline 注销数据源失败
     */
    public static void destroy(SqliteNode node) throws Exception {
        DruidDataSourceSupport.destroy(node.getName());
    }
}
