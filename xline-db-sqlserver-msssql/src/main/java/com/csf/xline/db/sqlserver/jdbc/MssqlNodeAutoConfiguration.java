package com.csf.xline.db.sqlserver.jdbc;

import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.autoconfigure.AbstractNodeAutoConfiguration;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.db.sqlserver.jdbc.handler.MssqlDataHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * SQL Server（mssql-jdbc）节点装配。
 *
 * <p>仅处理配置为 {@link NodeType#MSSQL} 的节点，装配流程由父类统一完成。
 */
@Configuration
@ConditionalOnClass(MssqlNode.class)
public class MssqlNodeAutoConfiguration extends AbstractNodeAutoConfiguration<MssqlNode, MssqlDataHandler> {

    public MssqlNodeAutoConfiguration(XLineProperties xLineProperties) {
        super(xLineProperties);
    }

    @Override
    protected NodeType nodeType() {
        return NodeType.MSSQL;
    }

    @Override
    protected Class<MssqlNode> nodeClass() {
        return MssqlNode.class;
    }

    @Override
    protected Class<MssqlDataHandler> handlerClass() {
        return MssqlDataHandler.class;
    }
}
