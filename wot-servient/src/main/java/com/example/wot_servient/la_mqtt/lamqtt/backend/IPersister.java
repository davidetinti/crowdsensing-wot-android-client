package com.example.wot_servient.la_mqtt.lamqtt.backend;

import com.example.wot_servient.la_mqtt.lamqtt.backend.model.Geofence;
import com.example.wot_servient.la_mqtt.lamqtt.backend.model.Subscription;

public interface IPersister {
    void connect();
    void disconnect();
    void addUserPosition(String userId, double lat, double lon);
    void addGeofence(String name, String idG, double lat, double lon, double rad, String msg);
    void addSubscription(String id, String top);
    Iterable<Subscription> getAllSubscriptions(String id);
    Iterable<Geofence> getGeofenceInfo(String top);
}
