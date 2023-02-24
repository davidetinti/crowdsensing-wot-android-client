package com.example.wot_servient.la_mqtt.lamqtt.simulator;

import com.example.wot_servient.la_mqtt.lamqtt.backend.SpatialMQTTBackEnd;

import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Simulator {

    private Scenario scenario;
    private final SimConfig config;
    private float simTime;
    private SimEvaluator evaluator;
    private final SpatialMQTTBackEnd backend;

    public Simulator(SimConfig config, SpatialMQTTBackEnd backend) {
        this.config=config;
        this.simTime=0;
        this.backend=backend;
    }

    private void loadConfig() throws ExecutionException, InterruptedException {
        this.evaluator=new SimEvaluator(String.valueOf(this.config.numGeofences));
        this.scenario=new Scenario(this.config.scenarioLeftCorner, this.config.scenarioRightCorner, this.config.seed);
        this.scenario.setGeofences(this.config.numGeofences, this.config.radiusGeofence, this.config.numTopics, this.config.frequencyAdvPublish);
        this.scenario.setUsers(this.config.numUsers,this.config.maxSpeed,this.config.minSpeed,this.config.maxPauseTime, this.config.frequencyGPSPublish, this.config.privacyModel, this.config.privacyParameters);
        //await this.scenario.hackSubscriptions(this.backend);
        this.scenario.setEvaluator(this.evaluator);
    }


    public void run() throws ExecutionException, InterruptedException, IOException, JSONException {
        System.out.println("--- SIMULATION START ---");
        this.loadConfig();
        for ( ; this.simTime< this.config.simDuration; this.simTime+=this.config.simSlotLength) {
            this.scenario.update(this.config.simSlotLength, this.simTime);
            TimeUnit.SECONDS.sleep(1);
        }
        this.scenario.dispose();
        System.out.println("--- SIMULATION END ---");
        this.evaluator.printStat();
    }
}
