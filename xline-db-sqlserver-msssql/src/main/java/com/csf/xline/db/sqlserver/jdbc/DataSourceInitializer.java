package com.csf.xline.db.sqlserver.jdbc;

import com.alibaba.druid.DbType;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.core.mssql.MssqlProperties;
import com.csf.xline.db.support.DruidDataSourceSpec;
import com.csf.xline.db.support.DruidDataSourceSupport;
import org.anyline.metadata.type.DatabaseType;
import org.springframework.util.StringUtils;

/**
 * SQL Server 数据源初始化（mssql-jdbc 驱动）。
 *
 * <p>mssql-jdbc 的 URL 形如
 * {@code jdbc:sqlserver://host:port;databaseName=db;encrypt=false;trustServerCertificate=true;}，
 * 全部属性以分号分隔、库名用 {@code databaseName} 属性指定。
 */
public class DataSourceInitializer {

    /** 微软官方驱动 */
    private static final String DEFAULT_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    public static void initDataSource(MssqlNode node) throws Exception {
        MssqlProperties properties = node.getMssql() == null ? new MssqlProperties() : node.getMssql();
        String jdbcUrl = properties.explicitJdbcUrl() != null ? properties.explicitJdbcUrl() : buildUrl(properties);
        String driver = properties.resolvedDriverClassName();
        DruidDataSourceSpec spec = new DruidDataSourceSpec(
                node.getName(), NodeType.MSSQL, DbType.sqlserver, DatabaseType.MSSQL)
                .setJdbcUrl(jdbcUrl)
                .setDriverClassName(driver == null ? DEFAULT_DRIVER : driver)
                .setUsername(properties.getUsername())
                .setPassword(properties.getPassword())
                .setLogName("Sql Server(mssql-jdbc)");
        DruidDataSourceSupport.initialize(properties, spec);
    }

    /**
     * 销毁节点数据源。
     *
     * @param node SQL Server 节点
     * @throws Exception anyline 注销数据源失败
     */
    public static void destroy(MssqlNode node) throws Exception {
        DruidDataSourceSupport.destroy(node.getName());
    }

    /**
     * 按 mssql-jdbc 格式拼接 URL：属性以分号分隔，库名用 {@code databaseName}。
     *
     * @param properties 节点配置
     * @return JDBC URL
     */
    private static String buildUrl(MssqlProperties properties) {
        StringBuilder url = new StringBuilder(String.format(
                "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=%s;trustServerCertificate=%s;",
                properties.resolvedHost(), properties.resolvedPort(), properties.requiredDatabaseName(NodeType.MSSQL.getSegment()),
                Boolean.TRUE.equals(properties.getEncrypt()), !Boolean.FALSE.equals(properties.getTrustServerCertificate())));
        if (StringUtils.hasText(properties.getInstance())) {
            url.append("instanceName=").append(properties.getInstance()).append(';');
        }
        if (StringUtils.hasText(properties.getDomain())) {
            url.append("domain=").append(properties.getDomain()).append(';');
        }
        return url.toString();
    }
}
