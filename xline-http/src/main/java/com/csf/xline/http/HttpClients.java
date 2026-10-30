package com.csf.xline.http;

import com.csf.xline.core.http.HttpProperties;
import com.dtflys.forest.config.ForestConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP 节点的 Forest 配置登记。
 *
 * <p>每个节点一份独立配置（地址、超时、重试、连接池互不干扰）。
 * 节点未配置的项沿用 Forest 全局配置 —— 全局配置由 starter 按 {@code forest.*} 自动初始化，
 * 由装配阶段注入（未引入 starter 时为 null，则完全使用节点配置与 Forest 默认值）。
 */
@Slf4j
public class HttpClients {

    /** 已创建的节点配置：key 为节点名 */
    private static final Map<String, ForestConfiguration> CONFIGS = new ConcurrentHashMap<>();

    /**
     * 按节点配置创建 Forest 配置。
     *
     * <p>{@code http} 配置段可选：未书写时全部沿用 Forest 全局配置
     * （接口自带 {@code @Address} 即可，无需节点级 {@code base-url}）。
     *
     * @param node HTTP 节点
     * @return 该节点独立的 Forest 配置
     */
    public static ForestConfiguration create(HttpNode node) {
        HttpProperties properties = node.getHttp() == null ? new HttpProperties() : node.getHttp();

        ForestConfiguration global = node.getGlobalConfiguration();
        ForestConfiguration configuration = ForestConfiguration.configuration();

        String charset = first(properties.getCharset(), global == null ? null : global.getCharset());
        if (charset != null) {
            configuration.setCharset(charset);
        }

        Integer connectTimeout = first(properties.getConnectTimeout(), global == null ? null : global.getConnectTimeout());
        if (connectTimeout != null) {
            configuration.setConnectTimeout(connectTimeout);
        }

        Integer readTimeout = first(properties.getReadTimeout(), global == null ? null : global.getReadTimeout());
        if (readTimeout != null) {
            configuration.setReadTimeout(readTimeout);
        }

        Integer maxConnections = first(properties.getMaxConnections(), global == null ? null : global.getMaxConnections());
        if (maxConnections != null) {
            configuration.setMaxConnections(maxConnections);
        }

        Integer retryCount = first(properties.getRetryCount(), global == null ? null : global.getRetryCount());
        if (retryCount != null) {
            configuration.setRetryCount(retryCount);
        }

        String backendName = StringUtils.hasText(properties.getBackendName())
                ? properties.getBackendName() : (global == null ? null : global.getBackendName());
        if (StringUtils.hasText(backendName)) {
            configuration.setBackendName(backendName);
        }

        if (properties.getLogEnabled() != null) {
            configuration.setLogEnabled(properties.getLogEnabled());
        }

        CONFIGS.put(node.getName(), configuration);
        log.info("【Http - Forest】created: {} -> {}（{}）",
                node.getName(), describeBaseUrl(properties),
                global == null ? "仅节点配置" : "节点配置优先，未配项沿用全局");
        return configuration;
    }

    /**
     * 移除节点配置。
     *
     * @param node HTTP 节点
     */
    public static void destroy(HttpNode node) {
        if (CONFIGS.remove(node.getName()) != null) {
            log.info("【Http - Forest】destroyed: {}", node.getName());
        }
    }

    /** 节点值优先，其次全局值 */
    private static <T> T first(T nodeValue, T globalValue) {
        return nodeValue != null ? nodeValue : globalValue;
    }

    /** 日志用的地址描述：未配置 base-url 时地址由接口自带的 {@code @Address} 决定 */
    private static String describeBaseUrl(HttpProperties properties) {
        return StringUtils.hasText(properties.getBaseUrl())
                ? properties.resolvedBaseUrl()
                : "未配置 base-url（地址由接口 @Address 决定）";
    }
}
