package com.csf.xline.core.routing;

import com.csf.xline.core.XLineContext;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 节点路由数据源。
 *
 * <p>节点数据源在 {@code ApplicationReadyEvent} 阶段才创建完成，晚于容器刷新，
 * 因此向容器提供一个常驻的 {@link DataSource} Bean：
 * <ol>
 *     <li>满足 SpringBoot 契约，{@code DataSourceAutoConfiguration} 因
 *         {@code @ConditionalOnMissingBean} 自动退让，无需按版本排除自动配置类
 *         （其包名在 SpringBoot 2.7 与 4.x 之间并不相同）；</li>
 *     <li>让 MyBatis-Plus 等依赖单一 DataSource 的组件正常装配，
 *         运行期按 {@link NodeContext} 路由到对应节点。</li>
 * </ol>
 *
 * <p>anyline 方式无需经过本类：数据源初始化时已注册进 {@code DataSourceHolder}，
 * 直接按节点名使用即可。
 *
 * <p>直接实现 JDK 标准 {@link DataSource} 接口而非继承 Spring 的 {@code AbstractDataSource}：
 * core 是公共模块，不引入 spring-jdbc 等数据库相关依赖。
 */
public class XLineRoutingDataSource implements DataSource {

    @Override
    public Connection getConnection() throws SQLException {
        return resolve().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return resolve().getConnection(username, password);
    }

    /**
     * 解析当前应使用的数据源：优先取 {@link NodeContext} 指定的节点，否则取首个就绪节点。
     *
     * @return 目标数据源
     * @throws IllegalStateException 指定了不存在的节点，或尚未初始化任何节点
     */
    private DataSource resolve() {
        Map<String, DataSource> dataSources = XLineContext.DATA_SOURCES;
        String nodeName = NodeContext.current();
        DataSource dataSource = nodeName == null ? first(dataSources) : dataSources.get(nodeName);
        if (dataSource == null) {
            throw new IllegalStateException("没有可用的节点数据源"
                    + (nodeName == null ? "（尚未初始化任何节点）" : "：" + nodeName));
        }
        return dataSource;
    }

    /** 取首个就绪的节点数据源 */
    private DataSource first(Map<String, DataSource> dataSources) {
        for (DataSource dataSource : dataSources.values()) {
            if (dataSource != null) {
                return dataSource;
            }
        }
        return null;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        // 节点数据源自身的日志由各自连接池负责
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        // 登录超时由各节点连接池配置
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
