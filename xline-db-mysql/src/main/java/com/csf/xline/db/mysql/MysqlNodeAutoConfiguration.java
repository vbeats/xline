package com.csf.xline.db.mysql;

import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.autoconfigure.AbstractNodeAutoConfiguration;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.db.mysql.handler.MysqlDataHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * MySQL 节点装配。
 *
 * <p>仅处理配置为 {@link NodeType#MYSQL} 的节点，装配流程由父类统一完成。
 */
@Configuration
@ConditionalOnClass(MysqlNode.class)
public class MysqlNodeAutoConfiguration extends AbstractNodeAutoConfiguration<MysqlNode, MysqlDataHandler> {

    public MysqlNodeAutoConfiguration(XLineProperties xLineProperties) {
        super(xLineProperties);
    }

    @Override
    protected NodeType nodeType() {
        return NodeType.MYSQL;
    }

    @Override
    protected Class<MysqlNode> nodeClass() {
        return MysqlNode.class;
    }

    @Override
    protected Class<MysqlDataHandler> handlerClass() {
        return MysqlDataHandler.class;
    }
}
