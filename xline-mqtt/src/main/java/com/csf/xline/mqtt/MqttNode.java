package com.csf.xline.mqtt;

import com.csf.xline.core.Node;
import com.csf.xline.core.mqtt.MqttProperties;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Slf4j
public class MqttNode extends Node {

    private transient volatile MqttAsyncClient client;
    private transient MqttProperties settings;
    private transient Charset charset;
    private transient MqttSubscription[] subscriptions;
    private transient volatile boolean closing;

    @Override
    protected void initDataSource() throws Exception {
        settings = getMqtt() == null ? new MqttProperties() : getMqtt();
        validate(settings);
        charset = Charset.forName(settings.getCharset());
        subscriptions = settings.getSubscriptions().stream().map(subscription -> {
            MqttSubscription target = new MqttSubscription(subscription.getTopic(), subscription.getQos());
            target.setNoLocal(subscription.isNoLocal());
            target.setRetainAsPublished(subscription.isRetainAsPublished());
            target.setRetainHandling(subscription.getRetainHandling());
            return target;
        }).toArray(MqttSubscription[]::new);
        String clientId = StringUtils.hasText(settings.getClientId()) ? settings.getClientId() : getName();
        Assert.hasText(clientId, "mqtt 必须配置 client-id 或节点 name");
        closing = false;
        MqttAsyncClient created = new MqttAsyncClient(settings.getBroker(), clientId, new MemoryPersistence());
        client = created;
        created.setCallback(callback());
        created.connect(connectionOptions()).waitForCompletion(settings.getOperationTimeoutMillis());
        if (subscriptions.length > 0) {
            IMqttToken token = created.subscribe(subscriptions);
            token.waitForCompletion(settings.getOperationTimeoutMillis());
            checkSubscription(token);
        }
        log.info("【MQTT】reg success: {}", getName());
    }

    public MqttAsyncClient client() {
        MqttAsyncClient current = client;
        Assert.state(current != null && !closing, "mqtt 节点尚未初始化或已关闭");
        return current;
    }

    public IMqttToken publish(Object data) throws MqttException {
        client();
        Assert.hasText(settings.getPublishTopic(), "mqtt 发布必须配置 publish-topic 或显式传入主题");
        return publish(settings.getPublishTopic(), data);
    }

    public IMqttToken publish(String topic, Object data) throws MqttException {
        client();
        Assert.notNull(data, "mqtt 发布数据不能为 null");
        byte[] payload = data instanceof MqttEnvelope ? ((MqttEnvelope) data).getPayload()
                : data instanceof byte[] ? (byte[]) data : String.valueOf(data).getBytes(charset);
        return publish(topic, payload, settings.getPublishQos(), settings.isRetained());
    }

    public IMqttToken publish(String topic, byte[] payload, int qos, boolean retained) throws MqttException {
        validatePublishTopic(topic);
        Assert.notNull(payload, "mqtt 发布数据不能为 null");
        MqttMessage.validateQos(qos);
        return client().publish(topic, payload, qos, retained);
    }

    @Override
    protected void destroyDataSource() throws Exception {
        closing = true;
        MqttAsyncClient current = client;
        client = null;
        if (current == null) {
            return;
        }
        try {
            if (current.isConnected()) {
                current.disconnect(settings.getDisconnectQuiesceMillis())
                        .waitForCompletion(settings.getOperationTimeoutMillis());
            }
        } finally {
            current.close(true);
        }
    }

    private MqttConnectionOptions connectionOptions() {
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(settings.isCleanStart());
        options.setSessionExpiryInterval(settings.getSessionExpiryIntervalSeconds());
        options.setConnectionTimeout(settings.getConnectionTimeoutSeconds());
        options.setKeepAliveInterval(settings.getKeepAliveIntervalSeconds());
        options.setAutomaticReconnect(settings.isAutomaticReconnect());
        options.setAutomaticReconnectDelay(settings.getReconnectMinDelayMillis(), settings.getReconnectMaxDelayMillis());
        if (settings.getUsername() != null) {
            options.setUserName(settings.getUsername());
        }
        if (settings.getPassword() != null) {
            options.setPassword(settings.getPassword().getBytes(StandardCharsets.UTF_8));
        }
        MqttProperties.Will will = settings.getWill();
        if (will != null) {
            MqttMessage message = new MqttMessage(will.getPayload().getBytes(charset));
            message.setQos(will.getQos());
            message.setRetained(will.isRetained());
            options.setWill(will.getTopic(), message);
        }
        return options;
    }

    private MqttCallback callback() {
        return new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse response) {
                if (!closing) {
                    log.warn("【{}】 MQTT 连接断开", getName());
                }
            }

            @Override
            public void mqttErrorOccurred(MqttException exception) {
                if (!closing) {
                    log.error("【{}】 MQTT 客户端异常", getName(), exception);
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                if (!closing) {
                    pushMsg(new MqttEnvelope(topic, message));
                }
            }

            @Override
            public void deliveryComplete(IMqttToken token) {
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (!reconnect || closing || subscriptions.length == 0) {
                    return;
                }
                MqttAsyncClient current = client;
                if (current == null) {
                    return;
                }
                try {
                    // Paho 回调线程不能阻塞等待 SUBACK，否则接收回调无法继续。
                    current.subscribe(subscriptions, null, new MqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken token) {
                            try {
                                checkSubscription(token);
                            } catch (IllegalStateException e) {
                                log.error("【{}】 MQTT 重连后订阅被拒绝", getName(), e);
                            }
                        }

                        @Override
                        public void onFailure(IMqttToken token, Throwable exception) {
                            log.error("【{}】 MQTT 重连后订阅失败", getName(), exception);
                        }
                    }, null);
                } catch (MqttException e) {
                    log.error("【{}】 MQTT 重连后订阅失败", getName(), e);
                }
            }

            @Override
            public void authPacketArrived(int reasonCode, org.eclipse.paho.mqttv5.common.packet.MqttProperties properties) {
            }
        };
    }

    private void checkSubscription(IMqttToken token) {
        int[] codes = token.getReasonCodes();
        Assert.state(codes != null && codes.length == subscriptions.length, "mqtt 订阅未返回完整确认");
        for (int code : codes) {
            Assert.state(code >= 0 && code <= 2, "mqtt 订阅被拒绝，原因码: " + code);
        }
    }

    private static void validate(MqttProperties properties) {
        Assert.hasText(properties.getBroker(), "mqtt broker 不能为空");
        Assert.hasText(properties.getCharset(), "mqtt charset 不能为空");
        Assert.isTrue(properties.getConnectionTimeoutSeconds() > 0, "mqtt connection-timeout-seconds 必须大于 0");
        Assert.isTrue(properties.getOperationTimeoutMillis() > 0, "mqtt operation-timeout-millis 必须大于 0");
        Assert.isTrue(properties.getKeepAliveIntervalSeconds() >= 0 && properties.getKeepAliveIntervalSeconds() <= 65535,
                "mqtt keep-alive-interval-seconds 必须在 0~65535 之间");
        Assert.isTrue(properties.getSessionExpiryIntervalSeconds() >= 0 && properties.getSessionExpiryIntervalSeconds() <= 0xFFFFFFFFL,
                "mqtt session-expiry-interval-seconds 超出范围");
        Assert.isTrue(properties.getReconnectMinDelayMillis() > 0
                && properties.getReconnectMaxDelayMillis() >= properties.getReconnectMinDelayMillis(), "mqtt 重连间隔配置无效");
        Assert.isTrue(properties.getDisconnectQuiesceMillis() >= 0, "mqtt disconnect-quiesce-millis 不能为负数");
        MqttMessage.validateQos(properties.getPublishQos());
        if (properties.getPublishTopic() != null) {
            validatePublishTopic(properties.getPublishTopic());
        }
        Assert.notNull(properties.getSubscriptions(), "mqtt subscriptions 不能为 null");
        for (MqttProperties.Subscription subscription : properties.getSubscriptions()) {
            Assert.notNull(subscription, "mqtt 订阅项不能为空");
            Assert.hasText(subscription.getTopic(), "mqtt 订阅主题不能为空");
            MqttSubscription.validateQos(subscription.getQos());
            MqttSubscription.validateRetainHandling(subscription.getRetainHandling());
        }
        if (properties.getWill() != null) {
            validatePublishTopic(properties.getWill().getTopic());
            Assert.notNull(properties.getWill().getPayload(), "mqtt 遗嘱 payload 不能为 null");
            MqttMessage.validateQos(properties.getWill().getQos());
        }
    }

    private static void validatePublishTopic(String topic) {
        Assert.hasText(topic, "mqtt 发布主题不能为空");
        Assert.isTrue(topic.indexOf('#') < 0 && topic.indexOf('+') < 0, "mqtt 发布主题不能包含通配符");
    }
}
