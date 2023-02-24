package com.example.wot_servient.la_mqtt.lamqtt.common;

import org.eclipse.paho.client.mqttv3.*;
import org.json.JSONException;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class PahoConnector implements BrokerConnector {

    private boolean connected;
    private MqttClient client;
    private boolean eventInitialized;
    private MQTTClient sClient;

    public PahoConnector() {
        this.connected = false;
        this.eventInitialized = false;
        this.sClient = null;
    }

    public CompletableFuture<Boolean> connect(BrokerConf conf) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.client = new MqttClient(conf.url, conf.id);
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setMqttVersion(4);
                connOpts.setConnectionTimeout(5000);
                connOpts.setUserName(conf.username);
                connOpts.setPassword(conf.password.toCharArray());
                this.client.connect(connOpts);
                this.client.connect(new MqttConnectOptions());
                System.out.println("Connected to MQTT Broker. Client id: " + conf.id);
                this.connected = true;
                return true;
            } catch (MqttException e) {
                System.out.println("Connection Error" + e);
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> publish(String topic, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (this.connected) {
                    this.client.publish(topic, new MqttMessage(message.getBytes()));
                    return true;
                } else {
                    return false;
                }
            } catch (MqttException e) {
                System.out.println("Connection Error" + e);
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> subscribe(String topic, MQTTClient mClient) {
        return CompletableFuture.supplyAsync(() -> {
            this.sClient = mClient;
            try {
                if (this.connected) {
                    this.client.subscribe(topic);
                    if (!this.eventInitialized) this.recvMessage();
                    this.eventInitialized = true;
                    return true;
                } else {
                    return false;
                }
            } catch (MqttException e) {
                System.out.println("Connection Error" + e);
                return false;
            }
        });
    }

    private void recvMessage() {
        PahoConnector that = this;
        this.client.setCallback(new MqttCallback() {

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws JSONException {
                if (that.sClient != null) {
                    that.sClient.msgRecv(new MQTTMessage(topic, Arrays.toString(message.getPayload())));
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public CompletableFuture<Boolean> disconnect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (this.connected) {
                    this.client.disconnect();
                }
                return true;
            } catch (MqttException e) {
                System.out.println("Connection Error" + e);
                return false;
            }
        });
    }
}
