package com.csf.xline.db.h2;

import com.csf.xline.core.XLineContext;
import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.autoconfigure.AbstractNodeAutoConfiguration;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.db.h2.handler.H2DataHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(H2Node.class)
public class H2NodeAutoConfiguration extends AbstractNodeAutoConfiguration<H2Node, H2DataHandler> {

    public H2NodeAutoConfiguration(XLineProperties xLineProperties, XLineContext xLineContext) {
        super(xLineProperties, xLineContext);
    }

    @Override
    protected NodeType nodeType() {
        return NodeType.H2;
    }

    @Override
    protected Class<H2Node> nodeClass() {
        return H2Node.class;
    }

    @Override
    protected Class<H2DataHandler> handlerClass() {
        return H2DataHandler.class;
    }
}
