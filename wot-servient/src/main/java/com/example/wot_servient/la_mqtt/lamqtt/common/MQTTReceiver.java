package com.example.wot_servient.la_mqtt.lamqtt.common;

import org.json.JSONException;

public interface MQTTReceiver {
    void messageRecv(MQTTMessage message) throws JSONException;
}