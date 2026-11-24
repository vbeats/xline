package com.csf.xline.db.oracle;

import com.csf.xline.core.XLineContext;
import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.autoconfigure.AbstractNodeAutoConfiguration;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.db.oracle.handler.OracleDataHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * Oracle 节点装配。
 *
 * <p>仅处理配置为 {@link NodeType#ORACLE} 的节点，装配流程由父类统一完成。
 */
@Configuration
@ConditionalOnClass(OracleNode.class)
public class OracleNodeAutoConfiguration extends AbstractNodeAutoConfiguration<OracleNode, OracleDataHandler> {

    public OracleNodeAutoConfiguration(XLineProperties xLineProperties, XLineContext xLineContext) {
        super(xLineProperties, xLineContext);
    }

    @Override
    protected NodeType nodeType() {
        return NodeType.ORACLE;
    }

    @Override
    protected Class<OracleNode> nodeClass() {
        return OracleNode.class;
    }

    @Override
    protected Class<OracleDataHandler> handlerClass() {
        return OracleDataHandler.class;
    }
}
