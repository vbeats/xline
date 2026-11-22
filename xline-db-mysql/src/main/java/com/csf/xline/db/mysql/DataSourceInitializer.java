package com.csf.xline.db.mysql;

import com.alibaba.druid.DbType;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.core.mysql.MysqlProperties;
import com.csf.xline.db.support.DruidDataSourceSpec;
import com.csf.xline.db.support.DruidDataSourceSupport;
import org.anyline.metadata.type.DatabaseType;

/**
 * MySQL 数据源初始化。
 *
 * <p>仅在节点配置了 {@code mysql} 配置段时被调用；未配置 MySQL 的节点不会创建任何连接。
 *
 * <p>数据源同时对外暴露两种使用方式：
 * <ol>
 *     <li>anyline —— 注册进 {@code DataSourceHolder}（vline 同款）；</li>
 *     <li>MyBatis-Plus —— 存入 {@link XLineContext#DATA_SOURCES}，
 *         经容器中的路由数据源按节点名取用。</li>
 * </ol>
 */
public class DataSourceInitializer {

    /**
     * 创建并注册节点数据源。
     *
     * @param node MySQL 节点
     * @throws Exception 数据源注册失败
     */
    public static void initDataSource(MysqlNode node) throws Exception {
        MysqlProperties properties = node.getMysql() == null ? new MysqlProperties() : node.getMysql();
        DruidDataSourceSpec spec = new DruidDataSourceSpec(
                node.getName(), NodeType.MYSQL, DbType.mysql, DatabaseType.MySQL)
                .setJdbcUrl(properties.resolvedJdbcUrl())
                .setDriverClassName(properties.resolvedDriverClassName())
                .setUsername(properties.getUsername())
                .setPassword(properties.getPassword())
                .setLogName("MySQL");
        DruidDataSourceSupport.initialize(properties, spec);
    }

    /**
     * 销毁节点数据源。
     *
     * @param node MySQL 节点
     * @throws Exception anyline 注销数据源失败
     */
    public static void destroy(MysqlNode node) throws Exception {
        DruidDataSourceSupport.destroy(node.getName());
    }
}
