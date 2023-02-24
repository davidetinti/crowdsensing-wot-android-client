package com.example.wot_servient.la_mqtt.lamqtt.common;

import org.json.JSONException;
import org.json.JSONObject;

public class MQTTMessage {
    public String topic;
    public String message;

    public MQTTMessage(String topic, String message) {
        this.topic = topic;
        this.message = message;
    }

    public Position getPositionFromMessage() throws JSONException {
        JSONObject jsonOb = new JSONObject(this.message);
        return new Position(jsonOb.getDouble("latitude"), jsonOb.getDouble("longitude"));
    }

    public String getContent() throws JSONException {
        JSONObject jsonOb = new JSONObject(this.message);
        return jsonOb.getString("message");
    }
}
