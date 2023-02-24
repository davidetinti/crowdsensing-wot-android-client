package com.example.wot_servient.la_mqtt.lamqtt.simulator;

import java.util.ArrayList;

class GeofenceData {
    public ArrayList<ActiveUser> userList;
    public boolean active;
    public String id;

    public GeofenceData(String id) {
        this.userList = new ArrayList<>();
        this.active = true;
        this.id = id;
    }
}
