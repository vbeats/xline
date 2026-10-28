package com.csf.xline.core;

import com.csf.xline.core.enums.NodeType;
import com.csf.xline.core.handler.DataHandler;
import com.csf.xline.core.mssql.MssqlProperties;
import com.csf.xline.core.mysql.MysqlProperties;
import com.csf.xline.core.postgres.PostgresProperties;
import com.csf.xline.core.serialport.SerialPortProperties;
import com.csf.xline.core.sqlite.SqliteProperties;
import com.csf.xline.core.http.HttpProperties;
import com.csf.xline.core.tcp.TcpProperties;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点模型与生命周期模板。
 *
 * <p>一个节点只能配置一种数据源：书写了哪个配置段，{@link #type} 即由对应 setter 推断，
 * 同时配置多个不同类型的段会直接报错（见 {@link #bindType(NodeType)}）。
 * 各数据库模块只处理属于自己的 {@link NodeType}，
 * 未书写任何配置段的节点 {@code type} 为 null，不会被任何模块初始化。
 *
 * <p>生命周期由本类统一实现：
 * {@code 建立数据源 → 回调处理器}、{@code 回调处理器 → 释放数据源}、
 * {@code 有处理器则消费数据，无处理器则跳过}。
 * 数据源如何建立与释放交给 {@link #initDataSource()} / {@link #destroyDataSource()} 钩子，
 * 各数据库节点只需实现这两个钩子，无需再编写重复的生命周期代码。
 *
 * <p>本类同时是 yml 配置的绑定载体，因此必须可实例化。
 */
@Data
@Slf4j
public class Node implements INode {

    private NodeType type;

    private String name;

    /** 业务自定义扩展配置 */
    private Map<String, Object> extra = new HashMap<>();

    private MysqlProperties mysql;

    private PostgresProperties postgresql;

    private SqliteProperties sqlite;

    private MssqlProperties mssql;

    private TcpProperties tcp;

    private HttpProperties http;

    private SerialPortProperties serialPort;

    /**
     * 业务数据处理实现，由装配阶段按节点名匹配注入。
     *
     * <p>声明为 transient：仅存在于运行期，不参与 equals / hashCode 与配置拷贝语义。
     */
    @ToString.Exclude
    private transient DataHandler<? extends Node> dataHandler;

    /**
     * 运行时上下文：由装配阶段统一注入，节点经它把数据投递到事件总线。
     *
     * <p>声明为 transient：仅存在于运行期，不参与 equals / hashCode 与配置拷贝语义。
     */
    @ToString.Exclude
    private transient XLineContext context;

    /**
     * 配置段 setter：仅在配置段存在时推断节点类型。
     *
     * <p>传 null 表示该段未配置，不改变 {@link #type}：
     * 否则属性拷贝（未配置的段也会以 null 调用 setter）会把已推断出的类型覆盖掉。
     */
    public void setMysql(MysqlProperties mysql) {
        this.mysql = mysql;
        if (mysql != null) {
            this.type = bindType(NodeType.MYSQL);
        }
    }

    public void setPostgresql(PostgresProperties postgresql) {
        this.postgresql = postgresql;
        if (postgresql != null) {
            this.type = bindType(NodeType.POSTGRESQL);
        }
    }

    public void setSqlite(SqliteProperties sqlite) {
        this.sqlite = sqlite;
        if (sqlite != null) {
            this.type = bindType(NodeType.SQLITE);
        }
    }

    public void setMssql(MssqlProperties mssql) {
        this.mssql = mssql;
        if (mssql != null) {
            this.type = bindType(NodeType.MSSQL);
        }
    }

    /**
     * 配置段 setter：仅在配置段存在时推断节点类型。
     *
     * <p>传 null 表示该段未配置，不改变 {@link #type}：
     * 否则属性拷贝（未配置的段也会以 null 调用 setter）会把已推断出的类型覆盖掉。
     */
    public void setTcp(TcpProperties tcp) {
        this.tcp = tcp;
        if (tcp != null) {
            this.type = bindType(NodeType.TCP);
        }
    }

    /**
     * 配置段 setter：仅在配置段存在时推断节点类型。
     *
     * <p>传 null 表示该段未配置，不改变 {@link #type}：
     * 否则属性拷贝（未配置的段也会以 null 调用 setter）会把已推断出的类型覆盖掉。
     */
    public void setHttp(HttpProperties http) {
        this.http = http;
        if (http != null) {
            this.type = bindType(NodeType.HTTP);
        }
    }

    /**
     * 配置段 setter：仅在配置段存在时推断节点类型。
     *
     * <p>传 null 表示该段未配置，不改变 {@link #type}：
     * 否则属性拷贝（未配置的段也会以 null 调用 setter）会把已推断出的类型覆盖掉。
     */
    public void setSerialPort(SerialPortProperties serialPort) {
        this.serialPort = serialPort;
        if (serialPort != null) {
            this.type = bindType(NodeType.SERIAL_PORT);
        }
    }

    @Override
    public void init() throws Exception {
        initDataSource();

        DataHandler<Node> handler = handler();
        if (handler != null) {
            handler.init(this);
        }
    }

    @Override
    public void destroy() throws Exception {
        try {
            DataHandler<Node> handler = handler();
            if (handler != null) {
                handler.destroy(this);
            }
        } finally {
            destroyDataSource();
        }
    }

    @Override
    public void receiveData(Object data) {
        DataHandler<Node> handler = handler();
        if (handler == null) {
            log.debug("节点 [{}] 未注册 DataHandler，数据未处理", getName());
            return;
        }
        try {
            handler.rec(this, data);
        } catch (Exception e) {
            log.error("节点 [{}] 处理数据失败", getName(), e);
        }
    }

    /**
     * 以本节点为出发节点向事件总线投递数据。
     *
     * <p>等价于 {@code context.pushMsg(getName(), payload)}：
     * 数据由事件总线按 {@code xline.struct} 推给本节点的下游。
     * 业务处理器拿到的就是节点自身，故直接调用本方法即可继续向下游传递，
     * 无需再关心「出发节点名」这个路由参数。
     *
     * @param payload 待传递的数据
     * @throws IllegalStateException 节点未注入上下文（未经装配即使用）
     */
    public void pushMsg(Object payload) {
        if (context == null) {
            throw new IllegalStateException(String.format("节点 [%s] 未注入上下文，无法推送数据", getName()));
        }
        context.pushMsg(getName(), payload);
    }

    /**
     * 建立数据源的钩子，由各数据库节点覆写；默认无操作（节点仅作为配置存在）。
     *
     * @throws Exception 建立数据源失败
     */
    protected void initDataSource() throws Exception {
        log.debug("节点 [{}] 未配置数据库，跳过数据源初始化", getName());
    }

    /**
     * 释放数据源的钩子，由各数据库节点覆写；默认无操作。
     *
     * @throws Exception 释放数据源失败
     */
    protected void destroyDataSource() throws Exception {
        // 默认无数据源可释放
    }

    /**
     * 校验并绑定节点类型：一个节点只能配置一种数据源。
     *
     * @param newType 本次配置段对应的节点类型
     * @return 生效的节点类型
     */
    private NodeType bindType(NodeType newType) {
        if (type != null && type != newType) {
            throw new IllegalStateException(String.format(
                    "节点 [%s] 只能配置一种数据源，已配置 %s，不能同时配置 %s", name, type, newType));
        }
        return newType;
    }

    /**
     * 取当前节点的数据处理器。
     *
     * <p>处理器声明类型按节点细分（如 {@code DataHandler<MysqlNode>}），
     * 此处统一按 {@code DataHandler<Node>} 使用：
     * 处理器与节点的对应关系由装配阶段保证，运行期类型必然匹配。
     *
     * @return 数据处理器，未注册时为 {@code null}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private DataHandler<Node> handler() {
        return (DataHandler) dataHandler;
    }
}
