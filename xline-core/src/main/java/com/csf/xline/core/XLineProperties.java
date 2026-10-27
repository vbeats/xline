package com.csf.xline.core;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * xline 根配置（yml 前缀 {@code xline}）。
 *
 * <p>{@code nodes} 定义节点，{@code struct} 定义数据流拓扑：
 * <pre>
 * xline:
 *   nodes:
 *     - name: db
 *       mysql:
 *         host: 10.88.0.1
 *         port: 3306
 *         database-name: test
 *         username: root
 *         password: root
 *         flyway: false
 *   struct:
 *     db:
 *       - xx
 * </pre>
 * 上例中 struct 表示：节点 {@code db} 的数据单向推送给节点 {@code xx}。
 */
@Data
@ConfigurationProperties(prefix = XLine.PREFIX)
public class XLineProperties {

    /** 节点列表 */
    private List<Node> nodes = new ArrayList<>();

    /** 数据流拓扑：key 为上游节点名，value 为下游节点名列表 */
    private Map<String, List<String>> struct = new LinkedHashMap<>();
}
