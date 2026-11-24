package com.csf.xline.db.oracle;

import com.alibaba.druid.DbType;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.core.oracle.OracleProperties;
import com.csf.xline.db.support.DruidDataSourceSpec;
import com.csf.xline.db.support.DruidDataSourceSupport;
import org.anyline.metadata.type.DatabaseType;

/**
 * Oracle 数据源初始化（11g+）。
 *
 * <p>仅在节点配置了 {@code oracle} 配置段时被调用；未配置 Oracle 的节点不会创建任何连接。
 *
 * <p>数据源同时对外暴露两种使用方式：
 * <ol>
 *     <li>anyline —— 注册进 {@code DataSourceHolder}；</li>
 *     <li>MyBatis-Plus —— 存入 {@link XLineContext#DATA_SOURCES}，
 *         经容器中的路由数据源按节点名取用。</li>
 * </ol>
 */
public class DataSourceInitializer {

    /**
     * 创建并注册节点数据源。
     *
     * @param node Oracle 节点
     * @throws Exception 数据源注册失败
     */
    public static void initDataSource(OracleNode node) throws Exception {
        OracleProperties properties = node.getOracle() == null ? new OracleProperties() : node.getOracle();
        DruidDataSourceSpec spec = new DruidDataSourceSpec(
                node.getName(), NodeType.ORACLE, DbType.oracle, DatabaseType.Oracle)
                .setJdbcUrl(properties.resolvedJdbcUrl())
                .setDriverClassName(properties.resolvedDriverClassName())
                .setUsername(properties.getUsername())
                .setPassword(properties.getPassword())
                .setLogName("Oracle");
        DruidDataSourceSupport.initialize(properties, spec);
    }

    /**
     * 销毁节点数据源。
     *
     * @param node Oracle 节点
     * @throws Exception anyline 注销数据源失败
     */
    public static void destroy(OracleNode node) throws Exception {
        DruidDataSourceSupport.destroy(node.getName());
    }
}
