package com.example.wot_servient.la_mqtt.lamqtt.simulator;

class ActiveUser {
    public String id;
    public double timeEnter;
    public Double timeNotified;
    public boolean notified;

    public ActiveUser(String id, double time) {
        this.id = id;
        this.timeEnter = time;
        this.timeNotified = null;
        this.notified = false;
    }
}
