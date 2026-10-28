package com.csf.xline.db.postgresql;

import com.csf.xline.core.XLineContext;
import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.autoconfigure.AbstractNodeAutoConfiguration;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.db.postgresql.handler.PostgresDataHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * PostgreSQL 节点装配。
 *
 * <p>仅处理配置为 {@link NodeType#POSTGRESQL} 的节点，装配流程由父类统一完成。
 */
@Configuration
@ConditionalOnClass(PostgresNode.class)
public class PostgresNodeAutoConfiguration extends AbstractNodeAutoConfiguration<PostgresNode, PostgresDataHandler> {

    public PostgresNodeAutoConfiguration(XLineProperties xLineProperties, XLineContext xLineContext) {
        super(xLineProperties, xLineContext);
    }

    @Override
    protected NodeType nodeType() {
        return NodeType.POSTGRESQL;
    }

    @Override
    protected Class<PostgresNode> nodeClass() {
        return PostgresNode.class;
    }

    @Override
    protected Class<PostgresDataHandler> handlerClass() {
        return PostgresDataHandler.class;
    }
}
