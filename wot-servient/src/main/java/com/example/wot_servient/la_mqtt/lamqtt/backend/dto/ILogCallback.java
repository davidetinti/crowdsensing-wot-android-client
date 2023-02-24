package com.example.wot_servient.la_mqtt.lamqtt.backend.dto;

public interface ILogCallback {
    void newSubscribeEvent(String clientId, String topic);
}
