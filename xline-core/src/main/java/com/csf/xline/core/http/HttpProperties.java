package com.csf.xline.core.http;

import lombok.Data;

/**
 * HTTP 节点配置段（{@code xline.nodes[].http}）。
 *
 * <p>本类只承载「请求到哪、超时多久」，不承载具体接口定义：
 * 具体请求由业务声明 Forest 接口（{@code @Request} /
 * {@code @BaseRequest}），经 {@code HttpNode.client()} 绑定到本节点配置后使用。
 *
 * <p>本段整体可选：节点只写 {@code type: http} 而不写 {@code http} 段时，
 * 全部沿用 Forest 全局配置（由 starter 按 {@code forest.*} 初始化），
 * 请求地址由业务接口自带的 {@code @Address} / {@code @Request(url)} 决定。
 * 写了本段后节点值优先，未配置的项仍沿用全局配置。
 *
 * <p>{@code base-url} 仅在使用 {@code node.url(path)} 拼接地址、
 * 或接口未自带地址时才需要。
 */
@Data
public class HttpProperties {

    /**
     * 服务地址，如 {@code http://192.168.1.10:8080}。
     *
     * <p>可选：未配置时地址由业务接口自带的 {@code @Address} / {@code @Request(url)} 决定。
     * 业务可用 {@code node.url("/api/x")} 拼接出完整地址。
     */
    private String baseUrl;

    /** 字符编码：未配置沿用全局配置 */
    private String charset;

    /** 连接超时（毫秒）：未配置沿用全局配置 */
    private Integer connectTimeout;

    /** 读取超时（毫秒）：未配置沿用全局配置 */
    private Integer readTimeout;

    /** 连接池最大连接数：未配置沿用全局配置 */
    private Integer maxConnections;

    /** 失败重试次数：未配置沿用全局配置 */
    private Integer retryCount;

    /** 是否打印 Forest 请求日志 */
    private Boolean logEnabled;

    /**
     * HTTP 后端（{@code httpclient} / {@code okhttp3}）：未配置沿用全局配置，
     * 全局也未配置时由 Forest 自动选择。
     */
    private String backendName;

    /**
     * 取生效的服务地址，末尾不带斜杠。
     *
     * @return 服务地址
     * @throws IllegalStateException 未配置 base-url
     */
    public String resolvedBaseUrl() {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalStateException("http 节点必须配置 base-url");
        }
        String url = baseUrl.trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
