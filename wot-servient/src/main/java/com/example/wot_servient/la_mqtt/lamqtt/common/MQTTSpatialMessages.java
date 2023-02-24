package com.example.wot_servient.la_mqtt.lamqtt.common;

public enum MQTTSpatialMessages {
    TOPIC_PUBLISH_POSITION("PUBLISH_POSITION"),
    TOPIC_PUBLISH_GEOFENCE("PUBLISH_GEOFENCE"),
    TOPIC_PUBLISH_SUBSCRIPTION("PUBLISH_SUB");

    private final String action;

    public String getValue() {
        return this.action;
    }

    MQTTSpatialMessages(String action) {
        this.action = action;
    }
}