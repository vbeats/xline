package com.csf.xline.core.sqlite;

import lombok.Data;

/**
 * SQLite 节点配置段（{@code xline.nodes[].sqlite}）。
 *
 * <p>SQLite 为文件型数据库，无 host / port / username / password 语义。
 */
@Data
public class SqliteProperties {

    /** 未配置文件路径时使用的默认库文件 */
    public static final String DEFAULT_DB_FILE = "xline.db";

    /** 数据库文件路径，支持 {@code :memory:} 内存库 */
    private String dbPath;

    private String driverClassName;

    private String jdbcUrl;

    private Boolean flyway = Boolean.FALSE;

    public String resolvedDbPath() {
        return dbPath == null || dbPath.trim().isEmpty() ? DEFAULT_DB_FILE : dbPath;
    }

    public String resolvedDriverClassName() {
        return driverClassName == null || driverClassName.trim().isEmpty()
                ? "org.sqlite.JDBC" : driverClassName;
    }
}
