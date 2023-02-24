package com.example.wot_servient.la_mqtt.lamqtt.backend.model;

public class Geofence {

    String topic;
    String id;
    double latitude;
    double longitude;
    double radius;
    String message;

    public Geofence(String topic, String id, double latitude, double longitude, double radius, String message) {
        this.topic = topic;
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.message = message;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Geofence{" + "topic='" + topic + '\'' + ", id='" + id + '\'' + ", latitude=" + latitude + ", longitude=" + longitude + ", radius=" + radius + ", message='" + message + '\'' + '}';
    }
}
