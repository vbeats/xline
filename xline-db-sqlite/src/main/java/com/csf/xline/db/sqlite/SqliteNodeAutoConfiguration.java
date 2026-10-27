package com.csf.xline.db.sqlite;

import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.autoconfigure.AbstractNodeAutoConfiguration;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.db.sqlite.handler.SqliteDataHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * SQLite 节点装配。
 *
 * <p>仅处理配置为 {@link NodeType#SQLITE} 的节点，装配流程由父类统一完成。
 */
@Configuration
@ConditionalOnClass(SqliteNode.class)
public class SqliteNodeAutoConfiguration extends AbstractNodeAutoConfiguration<SqliteNode, SqliteDataHandler> {

    public SqliteNodeAutoConfiguration(XLineProperties xLineProperties) {
        super(xLineProperties);
    }

    @Override
    protected NodeType nodeType() {
        return NodeType.SQLITE;
    }

    @Override
    protected Class<SqliteNode> nodeClass() {
        return SqliteNode.class;
    }

    @Override
    protected Class<SqliteDataHandler> handlerClass() {
        return SqliteDataHandler.class;
    }
}
