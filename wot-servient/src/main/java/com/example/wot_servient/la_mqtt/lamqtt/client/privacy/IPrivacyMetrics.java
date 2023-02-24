package com.example.wot_servient.la_mqtt.lamqtt.client.privacy;

import org.json.JSONException;
import org.json.JSONObject;

public interface IPrivacyMetrics {
    double compute(PrivacySet ps);
    void update(PrivacySet ps);
    void setParameters(JSONObject parameters) throws JSONException;
}
