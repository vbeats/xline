package com.csf.xline.core.sqlite;

import com.csf.xline.core.jdbc.JdbcProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.util.StringUtils;

/**
 * SQLite 节点配置段（{@code xline.nodes[].sqlite}）。
 *
 * <p>SQLite 为文件型数据库，无 host / port / username / password 语义；整段可省略，默认库文件 {@code xline.db}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SqliteProperties extends JdbcProperties {
    /** 未配置文件路径时使用的默认库文件 */
    public static final String DEFAULT_DB_FILE = "xline.db";
    /** 默认驱动 */
    public static final String DEFAULT_DRIVER = "org.sqlite.JDBC";

    /** 数据库文件路径，支持 {@code :memory:} 内存库，默认 {@code xline.db} */
    private String dbPath;

    /** @return 生效库文件路径 */
    public String resolvedDbPath() {
        return StringUtils.hasText(dbPath) ? dbPath.trim() : DEFAULT_DB_FILE;
    }

    /** @return 生效驱动类名 */
    public String resolvedDriverClassName() {
        return StringUtils.hasText(getDriverClassName()) ? getDriverClassName().trim() : DEFAULT_DRIVER;
    }

    /** @return 生效连接串：显式 {@code jdbc-url} 优先，否则按库文件拼装 */
    public String resolvedJdbcUrl() {
        String explicit = explicitJdbcUrl();
        return explicit != null ? explicit : "jdbc:sqlite:" + resolvedDbPath();
    }
}
