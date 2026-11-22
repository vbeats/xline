package com.csf.xline.db.sqlserver.jtds;

import com.alibaba.druid.DbType;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.core.mssql.MssqlProperties;
import com.csf.xline.db.support.DruidDataSourceSpec;
import com.csf.xline.db.support.DruidDataSourceSupport;
import org.anyline.metadata.type.DatabaseType;
import org.springframework.util.StringUtils;

/**
 * SQL Server 数据源初始化（jTDS 驱动）。
 *
 * <p>jTDS 的 URL 与 mssql-jdbc 不同：
 * 库名写在路径中、属性名也与官方驱动不一致（{@code instance} 而非 {@code instanceName}），
 * 且不支持 {@code encrypt} / {@code trustServerCertificate}
 * （jTDS 的加密走 {@code ssl} 属性，需加密请直接配置 jdbcUrl）。
 */
public class DataSourceInitializer {

    /** jTDS 驱动 */
    private static final String DEFAULT_DRIVER = "net.sourceforge.jtds.jdbc.Driver";

    public static void initDataSource(JtdsNode node) throws Exception {
        MssqlProperties properties = node.getMssql() == null ? new MssqlProperties() : node.getMssql();
        String jdbcUrl = properties.explicitJdbcUrl() != null ? properties.explicitJdbcUrl() : buildUrl(properties);
        String driver = properties.resolvedDriverClassName();
        DruidDataSourceSpec spec = new DruidDataSourceSpec(
                node.getName(), NodeType.MSSQL, DbType.sqlserver, DatabaseType.MSSQL)
                .setJdbcUrl(jdbcUrl)
                .setDriverClassName(driver == null ? DEFAULT_DRIVER : driver)
                .setUsername(properties.getUsername())
                .setPassword(properties.getPassword())
                .setLogName("Sql Server(jTDS)");
        DruidDataSourceSupport.initialize(properties, spec);
    }

    /**
     * 销毁节点数据源。
     *
     * @param node SQL Server 节点
     * @throws Exception anyline 注销数据源失败
     */
    public static void destroy(JtdsNode node) throws Exception {
        DruidDataSourceSupport.destroy(node.getName());
    }

    /**
     * 按 jTDS 格式拼接 URL：库名写在路径中，属性以分号分隔。
     *
     * @param properties 节点配置
     * @return JDBC URL
     */
    private static String buildUrl(MssqlProperties properties) {
        StringBuilder url = new StringBuilder(
                String.format("jdbc:jtds:sqlserver://%s:%d", properties.resolvedHost(), properties.resolvedPort()));
        if (StringUtils.hasText(properties.getDatabaseName())) {
            url.append('/').append(properties.getDatabaseName());
        }
        if (StringUtils.hasText(properties.getInstance())) {
            url.append(";instance=").append(properties.getInstance());
        }
        if (StringUtils.hasText(properties.getDomain())) {
            url.append(";domain=").append(properties.getDomain());
        }
        return url.toString();
    }
}
