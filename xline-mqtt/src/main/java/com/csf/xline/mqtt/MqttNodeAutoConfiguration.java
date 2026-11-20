package com.csf.xline.mqtt;

import com.csf.xline.core.XLineContext;
import com.csf.xline.core.XLineProperties;
import com.csf.xline.core.autoconfigure.AbstractNodeAutoConfiguration;
import com.csf.xline.core.enums.NodeType;
import com.csf.xline.mqtt.handler.MqttDataHandler;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttNodeAutoConfiguration extends AbstractNodeAutoConfiguration<MqttNode, MqttDataHandler> {

    public MqttNodeAutoConfiguration(XLineProperties properties, XLineContext context) {
        super(properties, context);
    }

    @Override
    protected NodeType nodeType() {
        return NodeType.MQTT;
    }

    @Override
    protected Class<MqttNode> nodeClass() {
        return MqttNode.class;
    }

    @Override
    protected Class<MqttDataHandler> handlerClass() {
        return MqttDataHandler.class;
    }
}
