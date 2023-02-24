package com.example.wot_servient.la_mqtt.lamqtt.simulator;

import com.example.wot_servient.la_mqtt.lamqtt.backend.GeoProcessor;
import com.example.wot_servient.la_mqtt.lamqtt.client.SpatialMQTTClient;
import com.example.wot_servient.la_mqtt.lamqtt.common.Position;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SimulatedGeofence {
    private static final String MESSAGE_CONTENT_PREFIX = "CONTENT";
    //TBF
    private static final String MQTT_USERNAME = "XXX";
    private static final String MQTT_PASSWORD = "XXX";
    private static final String MQTT_URL = "tcp://vz5nc4zqbrgzpn13.myfritz.net:1883";
    private static double msgCounter = 0.0;
    private final double frequencyAdv;
    private final boolean verboseMode = true;
    private final SpatialMQTTClient sMQTTClient;
    public Position position;
    public double radius;
    public String topic;
    public String id;
    public Double lastAdvSent;
    private double seqNo;
    private SimEvaluator evaluator;


    public SimulatedGeofence(String id, String topic, double frequency) {
        this.id = id;
        this.topic = topic;
        this.lastAdvSent = null;
        this.frequencyAdv = frequency;
        this.sMQTTClient = null;
        //this.sMQTTClient = new SpatialMQTTClient(SimulatedGeofence.MQTT_USERNAME, SimulatedGeofence.MQTT_PASSWORD, SimulatedGeofence.MQTT_URL, 1883, this.id);
    }


    public void setEvaluator(SimEvaluator evaluator) {
        this.evaluator = evaluator;
    }


    public void initialize() {
        CompletableFuture.runAsync(() -> {
            try {
                this.sMQTTClient.connect();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void dispose() {
        CompletableFuture.runAsync(() -> {
            try {
                this.sMQTTClient.disconnect();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public double getSeqNo() {
        return this.seqNo;
    }

    public String getTopic() {
        return this.topic;
    }

    public void setFixedPosition(Position position, double radius) {
        this.position = position;
        this.radius = radius;
    }

    public void setRandomPosition(Scenario scenario, double radius) {
        this.radius = radius;
        this.position = scenario.createRandomPosition();
        //  this.position=new Position(44.49470097378098, 11.341306346499298);
        if (this.verboseMode)
            System.out.println("[CREATED GEOFENCE] " + this.id + "<" + this.position.latitude + "," + this.position.longitude + ">");
    }

    public void publishAdv(double ctime) {
        CompletableFuture.runAsync(() -> {
            if ((this.lastAdvSent == null) || ((ctime - this.lastAdvSent) >= this.frequencyAdv)) {
                String content = SimulatedGeofence.MESSAGE_CONTENT_PREFIX + "|" + SimulatedGeofence.msgCounter + "|" + this.id;
                this.evaluator.newAdvertisement(SimulatedGeofence.msgCounter, this.id);
                this.seqNo = SimulatedGeofence.msgCounter;
                SimulatedGeofence.msgCounter += 1;
                this.lastAdvSent = ctime;
                if (this.verboseMode) {
                    System.out.println("[SIM PUBLISH ADV] Geofence: " + this.id + " Topic: " + this.topic + " Time: " + ctime + " Content: " + content);
                }
                try {
                    this.sMQTTClient.publicGeofence(this.position.latitude, this.position.longitude, this.radius, this.topic, content, this.id);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public boolean isAdvSpatialRelevant(Position dest) {
        double distance = GeoProcessor.computeDistanceGPS(this.position.latitude, this.position.longitude, dest.latitude, dest.longitude);
        return distance < this.radius;
    }
}
