package com.example.wot_servient.la_mqtt.lamqtt.common;

public class Position {
    public double latitude;
    public double longitude;

    public Position(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "\"latitude\": " + this.latitude + ", \"longitude\": " + this.longitude;
    }
}
