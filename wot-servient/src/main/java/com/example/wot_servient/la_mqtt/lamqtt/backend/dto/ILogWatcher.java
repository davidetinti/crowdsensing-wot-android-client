package com.example.wot_servient.la_mqtt.lamqtt.backend.dto;

public interface ILogWatcher {
    void start(ILogCallback callback);
    void stop();
}
