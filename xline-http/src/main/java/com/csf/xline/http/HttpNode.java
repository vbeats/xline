package com.csf.xline.http;

import com.csf.xline.core.Node;
import com.dtflys.forest.config.ForestConfiguration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.util.Assert;

/**
 * HTTP 节点。
 *
 * <p>每个节点持有一份独立的 Forest 配置（{@link HttpClients} 创建），
 * 生命周期与数据处理由 {@link Node} 的模板统一完成：
 * 业务处理器在 {@code rec} 中通过本节点发起 HTTP 请求。
 *
 * <p>业务自行声明 Forest 接口（{@code @Request} / {@code @BaseRequest}），
 * 经 {@link #client(Class)} 绑定到本节点配置后调用；
 * {@link #url(String)} 用于按 {@code base-url + path} 拼接地址。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HttpNode extends Node {

    /**
     * 本节点的 Forest 配置。
     *
     * <p>声明为 transient：仅存在于运行期。
     */
    @ToString.Exclude
    private transient ForestConfiguration configuration;

    /**
     * Forest 全局配置：由 starter 按 {@code forest.*} 自动初始化，装配阶段注入。
     *
     * <p>节点未配置的项沿用它；未引入 starter 时为 null。
     * 声明为 transient：仅存在于运行期。
     */
    @ToString.Exclude
    private transient ForestConfiguration globalConfiguration;

    @Override
    protected void initDataSource() throws Exception {
        configuration = HttpClients.create(this);
    }

    @Override
    protected void destroyDataSource() throws Exception {
        HttpClients.destroy(this);
    }

    /**
     * 取本节点的 Forest 配置。
     *
     * @return Forest 配置
     */
    public ForestConfiguration forest() {
        return configuration;
    }

    /**
     * 用本节点配置实例化业务声明的 Forest 接口。
     *
     * @param api 声明式接口类型
     * @param <T> 接口类型
     * @return 接口代理实例
     */
    public <T> T client(Class<T> api) {
        return configuration.client(api);
    }

    /**
     * 拼接完整地址：{@code base-url + path}。
     *
     * <p>{@code http} 配置段可选（未书写时地址由业务接口自带），
     * 但拼接地址必须有 {@code base-url}，故此处显式报错而非 NPE。
     *
     * @param path 路径，如 {@code /api/data} 或 {@code api/data}
     * @return 完整地址
     * @throws IllegalArgumentException 未配置 http 配置段或 base-url
     */
    public String url(String path) {
        Assert.notNull(getHttp(), String.format("【%s】 未配置 http 配置段，无法拼接地址", getName()));
        String base = getHttp().resolvedBaseUrl();
        if (path == null || path.isEmpty()) {
            return base;
        }
        return path.startsWith("/") ? base + path : base + "/" + path;
    }
}
