package com.csf.xline.core.enums;

/**
 * 节点类型。
 *
 * <p>与节点配置段一一对应：yml 中书写了哪个配置段，节点即被推断为对应类型，
 * 各数据库模块只处理属于自己的类型。
 */
public enum NodeType {

    /** MySQL */
    MYSQL("mysql"),

    /** PostgreSQL */
    POSTGRESQL("postgresql"),

    /** SQLite */
    SQLITE("sqlite"),

    /** SQL Server */
    MSSQL("mssql"),

    /** TCP：以 socket 收发数据的节点 */
    TCP("tcp"),

    /** HTTP：以 Forest 发送 HTTP 请求的节点 */
    HTTP("http"),

    /** SERIAL_PORT：以串口（232 / 485）收发数据的节点 */
    SERIAL_PORT("serial-port");

    /** 配置段名 */
    private final String segment;

    NodeType(String segment) {
        this.segment = segment;
    }

    public String getSegment() {
        return segment;
    }

    /**
     * 按配置段名查找节点类型。
     *
     * @param segment 配置段名，如 {@code mysql}
     * @return 节点类型，未匹配时返回 {@code null}
     */
    public static NodeType ofSegment(String segment) {
        if (segment == null) {
            return null;
        }
        for (NodeType type : values()) {
            if (type.segment.equalsIgnoreCase(segment)) {
                return type;
            }
        }
        return null;
    }
}
