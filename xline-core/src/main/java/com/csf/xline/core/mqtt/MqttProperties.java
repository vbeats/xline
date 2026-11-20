package com.csf.xline.core.mqtt;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
public class MqttProperties {

    private String broker = "tcp://127.0.0.1:1883";
    private String clientId;
    private String username;
    @ToString.Exclude
    private String password;
    private String charset = "UTF-8";
    private List<Subscription> subscriptions = new ArrayList<>();
    private String publishTopic;
    private int publishQos = 0;
    private boolean retained = false;
    private boolean cleanStart = true;
    private long sessionExpiryIntervalSeconds = 0;
    private int connectionTimeoutSeconds = 10;
    private int keepAliveIntervalSeconds = 60;
    private long operationTimeoutMillis = 10000;
    private boolean automaticReconnect = true;
    private int reconnectMinDelayMillis = 1000;
    private int reconnectMaxDelayMillis = 128000;
    private long disconnectQuiesceMillis = 1000;
    private Will will;

    @Data
    public static class Subscription {
        private String topic;
        private int qos = 0;
        private boolean noLocal = false;
        private boolean retainAsPublished = false;
        private int retainHandling = 0;
    }

    @Data
    public static class Will {
        private String topic;
        @ToString.Exclude
        private String payload = "";
        private int qos = 0;
        private boolean retained = false;
    }
}
