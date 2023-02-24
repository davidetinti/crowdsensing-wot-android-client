package com.example.wot_servient.la_mqtt.lamqtt.backend.model;

public class Subscription {

    String clientId;
    String topic;

    public Subscription(String clientId, String topic) {
        this.clientId = clientId;
        this.topic = topic;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public String toString() {
        return "Subscription{" + "clientId='" + clientId + '\'' + ", topic='" + topic + '\'' + '}';
    }
}
