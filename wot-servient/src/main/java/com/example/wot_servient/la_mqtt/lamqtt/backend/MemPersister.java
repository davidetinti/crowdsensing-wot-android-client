package com.example.wot_servient.la_mqtt.lamqtt.backend;

import com.example.wot_servient.la_mqtt.lamqtt.backend.model.Geofence;
import com.example.wot_servient.la_mqtt.lamqtt.backend.model.Subscription;
import com.example.wot_servient.la_mqtt.lamqtt.backend.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class MemPersister implements IPersister {
    private final HashMap<String, User> memUser;
    private final HashMap<String, Geofence> memGeofence;
    private final HashMap<String, Subscription> memSubscription;

    MemPersister() {
        this.memUser = new HashMap<>();
        this.memGeofence = new HashMap<>();
        this.memSubscription = new HashMap<>();
    }

    public void connect() {
        CompletableFuture.supplyAsync(() -> true);
    }

    public void disconnect() {

    }

    public void addUserPosition(String userId, double lat, double lon) {
        User user = this.memUser.get(userId);
        if (user == null) {
            user = new User(userId, lat, lon);
            this.memUser.put(userId, user);
        } else {
            user.setLatitude(lat);
            user.setLongitude(lon);
        }
    }

    //HHH
    public void addGeofence(String name, String idG, double lat, double lon, double rad, String msg) {
        Geofence geofence = new Geofence(name, idG, lat, lon, rad, msg);
        this.memGeofence.put(idG, geofence);
    }

    public void addSubscription(String id, String top) {
        String key = id + "!" + top;
        Subscription subs = this.memSubscription.get(key);
        if (subs == null) {
            subs = new Subscription(id, top);
            this.memSubscription.put(key, subs);

        }
    }

    public ArrayList<Subscription> getAllSubscriptions(String id) {
        ArrayList<Subscription> res = new ArrayList<>();
        this.memSubscription.forEach((key, gEntry) -> {
            String idEntry = key.split("!")[0];
            if (Objects.equals(idEntry, id)) res.add(gEntry);
        });
        return res;
    }

    public ArrayList<Geofence> getGeofenceInfo(String top) {
        ArrayList<Geofence> res = new ArrayList<>();
        this.memGeofence.forEach((key, gEntry) -> {
            if (Objects.equals(gEntry.getTopic(), top)) res.add(gEntry);
        });
        return res;
    }
}
