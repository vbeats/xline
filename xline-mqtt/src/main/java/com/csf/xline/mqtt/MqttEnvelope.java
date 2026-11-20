package com.csf.xline.mqtt;

import lombok.Getter;
import org.eclipse.paho.mqttv5.common.MqttMessage;

@Getter
public final class MqttEnvelope {

    private final String topic;
    private final byte[] payload;
    private final int qos;
    private final boolean retained;
    private final boolean duplicate;

    public MqttEnvelope(String topic, MqttMessage message) {
        this.topic = topic;
        this.payload = message.getPayload().clone();
        this.qos = message.getQos();
        this.retained = message.isRetained();
        this.duplicate = message.isDuplicate();
    }

    public byte[] getPayload() {
        return payload.clone();
    }
}
