package com.csf.xline.db.postgresql;

import com.alibaba.druid.DbType;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.core.postgres.PostgresProperties;
import com.csf.xline.db.support.DruidDataSourceSpec;
import com.csf.xline.db.support.DruidDataSourceSupport;
import org.anyline.metadata.type.DatabaseType;

/**
 * PostgreSQL 数据源初始化，仅在节点配置了 {@code postgresql} 配置段时被调用。
 */
public class DataSourceInitializer {

    public static void initDataSource(PostgresNode node) throws Exception {
        PostgresProperties properties = node.getPostgresql() == null ? new PostgresProperties() : node.getPostgresql();
        DruidDataSourceSpec spec = new DruidDataSourceSpec(
                node.getName(), NodeType.POSTGRESQL, DbType.postgresql, DatabaseType.PostgreSQL)
                .setJdbcUrl(properties.resolvedJdbcUrl())
                .setDriverClassName(properties.resolvedDriverClassName())
                .setUsername(properties.getUsername())
                .setPassword(properties.getPassword())
                .setLogName("PostgreSQL");
        DruidDataSourceSupport.initialize(properties, spec);
    }

    /**
     * 销毁节点数据源。
     *
     * @param node PostgreSQL 节点
     * @throws Exception anyline 注销数据源失败
     */
    public static void destroy(PostgresNode node) throws Exception {
        DruidDataSourceSupport.destroy(node.getName());
    }
}
