package com.csf.xline.db.sqlserver.jtds;

import com.csf.xline.core.XLineContext;
import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.autoconfigure.AbstractNodeAutoConfiguration;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.db.sqlserver.jtds.handler.JtdsDataHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * SQL Server（jTDS）节点装配。
 *
 * <p>仅处理配置为 {@link NodeType#MSSQL} 的节点，装配流程由父类统一完成。
 */
@Configuration
@ConditionalOnClass(JtdsNode.class)
public class JtdsNodeAutoConfiguration extends AbstractNodeAutoConfiguration<JtdsNode, JtdsDataHandler> {

    public JtdsNodeAutoConfiguration(XLineProperties xLineProperties, XLineContext xLineContext) {
        super(xLineProperties, xLineContext);
    }

    @Override
    protected NodeType nodeType() {
        return NodeType.MSSQL;
    }

    @Override
    protected Class<JtdsNode> nodeClass() {
        return JtdsNode.class;
    }

    @Override
    protected Class<JtdsDataHandler> handlerClass() {
        return JtdsDataHandler.class;
    }
}
