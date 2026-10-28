package com.csf.xline.core.autoconfigure;

import cn.hutool.core.bean.BeanUtil;
import com.csf.xline.core.Node;
import com.csf.xline.core.XLineContext;
import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.core.handler.DataHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 节点装配模板。
 *
 * <p>装配流程在各数据库模块中完全一致，故上提到本类：
 * {@code 筛选本类型的节点 → 拷贝配置到具体节点类 → 匹配业务处理器
 * → 注入运行时上下文 → 模块专属装配 → 初始化 → 登记到上下文}。
 * 各模块只需声明节点类型、节点实现类与处理器契约三类信息。
 *
 * <p>节点在 {@link ApplicationReadyEvent} 阶段初始化：
 * 此时容器已就绪，业务处理器 Bean 可正常获取。
 *
 * @param <N> 节点实现类
 * @param <H> 该数据库的数据处理契约
 */
@Slf4j
public abstract class AbstractNodeAutoConfiguration<N extends Node, H extends DataHandler<? extends Node>>
        implements ApplicationListener<ApplicationReadyEvent> {

    private final XLineProperties xLineProperties;

    private final XLineContext xLineContext;

    protected AbstractNodeAutoConfiguration(XLineProperties xLineProperties, XLineContext xLineContext) {
        this.xLineProperties = xLineProperties;
        this.xLineContext = xLineContext;
    }

    /**
     * 本模块负责的节点类型
     */
    protected abstract NodeType nodeType();

    /**
     * 本模块的节点实现类
     */
    protected abstract Class<N> nodeClass();

    /**
     * 本模块的数据处理契约
     */
    protected abstract Class<H> handlerClass();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        List<Node> nodes = xLineProperties.getNodes().stream()
                .filter(node -> nodeType().equals(node.getType()))
                .collect(Collectors.toList());
        if (nodes.isEmpty()) {
            return;
        }

        Map<String, H> handlers = event.getApplicationContext().getBeansOfType(handlerClass());
        for (Node node : nodes) {
            // 同名节点只初始化一次：SQL Server 有 jTDS / mssql-jdbc 两个驱动模块，
            // 同时引入时先装配的模块生效，避免重复建连与重复注册数据源
            if (XLineContext.NODES.containsKey(node.getName())) {
                log.warn("节点 [{}] 已由其它模块初始化，本次跳过：同一节点请勿引入多个同类驱动模块", node.getName());
                continue;
            }

            N target = BeanUtil.copyProperties(node, nodeClass());
            target.setDataHandler(resolveHandler(handlers, node.getName()));
            target.setContext(xLineContext);
            beforeInit(target);
            try {
                target.init();
                XLineContext.NODES.put(node.getName(), target);
            } catch (Exception e) {
                log.error("【{}】 Init Failed : ", node.getName(), e);
            }
        }
    }

    /**
     * 初始化前的模块专属装配钩子，默认无操作。
     *
     * <p>供需要额外运行期依赖的节点模块覆写（如 tcp 节点注入线程工厂）。
     *
     * @param node 待初始化的节点
     */
    protected void beforeInit(N node) {
        // 默认无额外装配
    }

    /**
     * 解析节点使用的业务处理器：优先按 Bean 名与节点名匹配，单实现时所有节点共用。
     *
     * @param handlers 容器中该契约的全部实现
     * @param nodeName 节点名
     * @return 匹配到的处理器，无实现时为 {@code null}
     */
    private H resolveHandler(Map<String, H> handlers, String nodeName) {
        if (handlers.isEmpty()) {
            return null;
        }
        H matched = handlers.get(nodeName);
        return matched != null ? matched : handlers.values().iterator().next();
    }
}
