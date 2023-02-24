package com.example.wot_servient.la_mqtt.lamqtt.client.privacy;

import com.example.wot_servient.la_mqtt.lamqtt.common.Position;

import org.json.JSONException;
import org.json.JSONObject;

public interface IPrivacyManager {
    Position transform(Position cPosition);
    void setParameters(JSONObject parameters) throws JSONException;
    void setTrajectory(Position cPosition, Position dPosition);
    double getPrivacyMetricValue();
}
