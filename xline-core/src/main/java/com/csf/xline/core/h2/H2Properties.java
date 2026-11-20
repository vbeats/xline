package com.csf.xline.core.h2;

import lombok.Data;
import lombok.ToString;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.List;

@Data
public class H2Properties {

    private String jdbcUrl;

    private Mode mode = Mode.MEMORY;

    private String databaseName;

    private String dbPath = "./xline";

    private String driverClassName = "org.h2.Driver";

    private String username = "sa";

    @ToString.Exclude
    private String password = "";

    private Pool pool = new Pool();

    private boolean flyway;

    private boolean baselineOnMigrate = true;

    private List<String> flywayLocations;

    public String resolvedJdbcUrl(String nodeName) {
        if (StringUtils.hasText(jdbcUrl)) {
            String url = jdbcUrl.trim();
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
        return StringUtils.hasText(driverClassName) ? driverClassName.trim() : "org.h2.Driver";
    }

    public Pool resolvedPool() {
        return pool == null ? new Pool() : pool;
    }

    public String[] resolvedFlywayLocations(String nodeName) {
        if (flywayLocations == null || flywayLocations.isEmpty()) {
            Assert.hasText(nodeName, "h2 default Flyway location requires a node name");
            return new String[]{"classpath:h2/" + nodeName};
        }
        for (String location : flywayLocations) {
            Assert.hasText(location, "h2.flyway-locations must not contain blank locations");
        }
        return flywayLocations.toArray(new String[0]);
    }

    public enum Mode {
        MEMORY,
        FILE
    }

    @Data
    public static class Pool {

        private int initialSize = 1;

        private int minIdle = 1;

        private int maxActive = 10;

        private long maxWaitMillis = 30000L;

        private String validationQuery = "SELECT 1";

        private int validationQueryTimeoutSeconds = 3;

        private boolean testOnBorrow;

        private boolean testWhileIdle = true;

        private long timeBetweenEvictionRunsMillis = 60000L;

        private long minEvictableIdleTimeMillis = 300000L;

        public void validate() {
            Assert.isTrue(initialSize >= 0, "h2.pool.initial-size must be >= 0");
            Assert.isTrue(minIdle >= 0, "h2.pool.min-idle must be >= 0");
            Assert.isTrue(maxActive > 0, "h2.pool.max-active must be > 0");
            Assert.isTrue(initialSize <= maxActive, "h2.pool.initial-size must be <= max-active");
            Assert.isTrue(minIdle <= maxActive, "h2.pool.min-idle must be <= max-active");
            Assert.isTrue(maxWaitMillis > 0, "h2.pool.max-wait-millis must be > 0");
            Assert.hasText(validationQuery, "h2.pool.validation-query must not be blank");
            Assert.isTrue(validationQueryTimeoutSeconds > 0,
                    "h2.pool.validation-query-timeout-seconds must be > 0");
            Assert.isTrue(timeBetweenEvictionRunsMillis > 0,
                    "h2.pool.time-between-eviction-runs-millis must be > 0");
            Assert.isTrue(minEvictableIdleTimeMillis >= 30000L,
                    "h2.pool.min-evictable-idle-time-millis must be >= 30000");
        }
    }
}
