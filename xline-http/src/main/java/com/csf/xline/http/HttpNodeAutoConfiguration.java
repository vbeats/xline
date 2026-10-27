package com.csf.xline.http;

import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.autoconfigure.AbstractNodeAutoConfiguration;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.http.handler.HttpDataHandler;
import com.dtflys.forest.config.ForestConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * HTTP 节点装配。
 *
 * <p>仅处理配置为 {@link NodeType#HTTP} 的节点，装配流程由父类统一完成。
 *
 * <p>Forest 全局配置由 starter 自动初始化并注册为 Bean，
 * 此处以 {@link ObjectProvider} 获取（未引入 starter 时为 null），
 * 初始化前交给节点，使节点未配置的项可沿用全局配置。
 */
@Configuration
@ConditionalOnClass(HttpNode.class)
public class HttpNodeAutoConfiguration extends AbstractNodeAutoConfiguration<HttpNode, HttpDataHandler> {

    private final ForestConfiguration forestConfiguration;

    public HttpNodeAutoConfiguration(XLineProperties xLineProperties,
                                     ObjectProvider<ForestConfiguration> forestConfiguration) {
        super(xLineProperties);
        this.forestConfiguration = forestConfiguration.getIfAvailable();
    }

    @Override
    protected NodeType nodeType() {
        return NodeType.HTTP;
    }

    @Override
    protected Class<HttpNode> nodeClass() {
        return HttpNode.class;
    }

    @Override
    protected Class<HttpDataHandler> handlerClass() {
        return HttpDataHandler.class;
    }

    @Override
    protected void beforeInit(HttpNode node) {
        node.setGlobalConfiguration(forestConfiguration);
    }
}
