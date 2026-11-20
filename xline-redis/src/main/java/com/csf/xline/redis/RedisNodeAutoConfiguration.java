package com.csf.xline.redis;

import com.csf.xline.core.XLineContext;
import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.autoconfigure.AbstractNodeAutoConfiguration;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.redis.handler.RedisDataHandler;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisNodeAutoConfiguration extends AbstractNodeAutoConfiguration<RedisNode, RedisDataHandler> {

    public RedisNodeAutoConfiguration(XLineProperties properties, XLineContext context) {
        super(properties, context);
    }

    @Override
    protected NodeType nodeType() {
        return NodeType.REDIS;
    }

    @Override
    protected Class<RedisNode> nodeClass() {
        return RedisNode.class;
    }

    @Override
    protected Class<RedisDataHandler> handlerClass() {
        return RedisDataHandler.class;
    }
}
