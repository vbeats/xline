package com.csf.xline.db.duckdb;

import com.csf.xline.core.XLineContext;
import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.autoconfigure.AbstractNodeAutoConfiguration;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.db.duckdb.handler.DuckdbDataHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(DuckdbNode.class)
public class DuckdbNodeAutoConfiguration extends AbstractNodeAutoConfiguration<DuckdbNode, DuckdbDataHandler> {

    public DuckdbNodeAutoConfiguration(XLineProperties xLineProperties, XLineContext xLineContext) {
        super(xLineProperties, xLineContext);
    }

    @Override
    protected NodeType nodeType() {
        return NodeType.DUCKDB;
    }

    @Override
    protected Class<DuckdbNode> nodeClass() {
        return DuckdbNode.class;
    }

    @Override
    protected Class<DuckdbDataHandler> handlerClass() {
        return DuckdbDataHandler.class;
    }
}
