package com.csf.xline.core.duckdb;

import lombok.Data;
import lombok.ToString;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Data
public class DuckdbProperties {

    private String jdbcUrl;

    private String dbPath = ":memory:";

    private boolean readOnly;

    @ToString.Exclude
    private Map<String, String> connectionProperties = new HashMap<>();

    public String resolvedJdbcUrl() {
        if (StringUtils.hasText(jdbcUrl)) {
            String url = jdbcUrl.trim();
            Assert.isTrue(url.startsWith("jdbc:duckdb:"), "duckdb.jdbc-url must start with jdbc:duckdb:");
            return url;
        }
        String path = StringUtils.hasText(dbPath) ? dbPath.trim() : ":memory:";
        return "jdbc:duckdb:" + (":memory:".equals(path) ? "" : path);
    }

    public Properties resolvedConnectionProperties() {
        Properties properties = new Properties();
        if (connectionProperties != null) {
            for (Map.Entry<String, String> entry : connectionProperties.entrySet()) {
                Assert.hasText(entry.getKey(), "duckdb.connection-properties keys must not be blank");
                Assert.notNull(entry.getValue(), "duckdb.connection-properties values must not be null");
                properties.setProperty(entry.getKey(), entry.getValue());
            }
        }
        // The dedicated read-only setting overrides the same driver option in the map.
        properties.setProperty("duckdb.read_only", Boolean.toString(readOnly));
        return properties;
    }
}
