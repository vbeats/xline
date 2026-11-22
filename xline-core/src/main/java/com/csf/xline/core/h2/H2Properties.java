package com.csf.xline.core.h2;

import com.csf.xline.core.jdbc.JdbcPoolProperties;
import com.csf.xline.core.jdbc.JdbcProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.File;

@Data
@EqualsAndHashCode(callSuper = true)
public class H2Properties extends JdbcProperties {

    private Mode mode = Mode.MEMORY;

    private String databaseName;

    private String dbPath = "./xline";

    private String username = "sa";

    @ToString.Exclude
    private String password = "";

    public H2Properties() {
        setPool(defaultPool());
    }

    public String resolvedJdbcUrl(String nodeName) {
        String explicitUrl = explicitJdbcUrl();
        if (explicitUrl != null) {
            String url = explicitUrl;
            Assert.isTrue(url.startsWith("jdbc:h2:"), "h2.jdbc-url must start with jdbc:h2:");
            return url;
        }
        if (mode == Mode.FILE) {
            String path = StringUtils.hasText(dbPath) ? dbPath.trim().replace('\\', '/') : "./xline";
            if (!new File(path).isAbsolute() && !path.startsWith("~/")
                    && !path.startsWith("./") && !path.startsWith("../")) {
                path = "./" + path;
            }
            return "jdbc:h2:file:" + path;
        }
        String name = StringUtils.hasText(databaseName) ? databaseName.trim() : nodeName;
        Assert.hasText(name, "h2 memory mode requires database-name or a node name");
        return "jdbc:h2:mem:" + name;
    }

    public String resolvedDriverClassName() {
        return StringUtils.hasText(getDriverClassName()) ? getDriverClassName().trim() : "org.h2.Driver";
    }

    @Override
    public JdbcPoolProperties resolvedPool() {
        return getPool() == null ? defaultPool() : getPool();
    }

    public enum Mode {
        MEMORY,
        FILE
    }

    private static JdbcPoolProperties defaultPool() {
        JdbcPoolProperties pool = new JdbcPoolProperties();
        pool.setInitialSize(1);
        pool.setMinIdle(1);
        pool.setMaxActive(10);
        pool.setMaxWaitMillis(30000L);
        pool.setValidationQuery("SELECT 1");
        return pool;
    }
}
