package com.example.wot_servient.la_mqtt.lamqtt.client;

import android.content.Context;

import com.example.wot_servient.la_mqtt.lamqtt.client.privacy.*;
import com.example.wot_servient.la_mqtt.lamqtt.common.AndroidMQTTClient;
import com.example.wot_servient.la_mqtt.lamqtt.common.MQTTClient;
import com.example.wot_servient.la_mqtt.lamqtt.common.MQTTReceiver;
import com.example.wot_servient.la_mqtt.lamqtt.common.MQTTSpatialMessages;
import com.example.wot_servient.la_mqtt.lamqtt.common.Position;
import com.example.wot_servient.la_mqtt.lamqtt.simulator.RNG;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SpatialMQTTClient extends AndroidMQTTClient {

    private final MQTTClientMeasurer mqttMeasurer;
    private final ArrayList<Double> dummyOptions = new ArrayList<>(Arrays.asList(1.0, 2.0, 4.0, 8.0));
    private final ArrayList<Double> perturbationOptions = new ArrayList<>(Arrays.asList(1.0, 2.0, 4.0));
    private IPrivacyManager pManager;
    private PrivacyModel pModel;
    private RNG rng;
    private boolean selfTuneMode;
    private ParameterTuner tunerAlgorithm;

    /**
     * @param username: MQTT broker access credential
     * @param password: MQTT broker access credential
     * @param host:     MQTT broker IP
     * @param port:     MQTT broker port number (default 1883)
     * @param clientId: MQTT client name
     */
    public SpatialMQTTClient(String username, String password, String host, int port, String clientId, Context context) {
        super(username, password, host, port, clientId, context);
        this.mqttMeasurer = new MQTTClientMeasurer();
        this.pModel = PrivacyModel.NONE;
        this.pManager = null;
        this.tunerAlgorithm = null;
        this.selfTuneMode = false;
    }

    /**
     * Publish the GPS position on the MQTT broker (used by MCs only)
     *
     * @param latitude:  Current GPS latitude value (real one, no perturbation)
     * @param longitude: Current GPS latitude value (real one, no perturbation)
     */
    public CompletableFuture<Position> publicPosition(double latitude, double longitude) throws JSONException {
        Position positionOrig = new Position(latitude, longitude);
        Position positionTsf = positionOrig;

        if (this.pModel != PrivacyModel.NONE) positionTsf = this.pManager.transform(positionOrig);

        this.mqttMeasurer.trackGPSPublish(positionOrig);

        if ((this.pModel != PrivacyModel.NONE) && (this.selfTuneMode)) this.tunerAlgorithm.update(positionOrig);


        String topic = MQTTSpatialMessages.TOPIC_PUBLISH_POSITION.getValue();
        String message = "{ " + positionTsf.toString() + ", \"id\": \"" + this.clientId + "\" }";
        Position finalPositionTsf = positionTsf;
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.publish(topic, message);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            return finalPositionTsf;
        });

    }


    /**
     * Publish a content update from the LDS (IoT data producer)
     * NOTE: This method assumes circular geofence region
     *
     * @param latitude:   Current GPS latitude value of the producer
     * @param longitude:  Current GPS longitude value of the producer
     * @param radius:     Radius (in meters) of the dissemination area
     * @param topic:      Channel name/source data type (e.g. temperature)
     * @param message:    Content to be disseminated (string)
     * @param geofenceId: unique id  of the LDS/data producer (e.g. sensor #10)
     */
    public void publicGeofence(double latitude, double longitude, double radius, String topic, String message, String geofenceId) throws ExecutionException, InterruptedException {
        Position position = new Position(latitude, longitude);
        String topicN = MQTTSpatialMessages.TOPIC_PUBLISH_GEOFENCE.getValue();
        String messageN = "{ " + position + ", \"id\": \"" + geofenceId + "\"";
        messageN = messageN + ", \"radius\": " + radius + ", \"message\": \"" + message + "\", \"topicGeofence\": \"" + topic + "\" }";
        //this.publish(topic, messageN);
        //HHH
        this.publish(topicN, messageN);
        //this.mqttMeasurer.trackGeofencePublish();
    }


    /**
     * Topic subscription (used by MC only)
     *
     * @param topic:    LDS channel name to subscribe to (e.g. temperature)
     * @param callback: MQTTReceiver object to notify when a new MQTT message has been received
     */
    public void subscribeGeofence(String topic, MQTTReceiver callback) throws ExecutionException, InterruptedException {
        String topicNew = this.generateGeofenceTopic(topic);
        this.setCallback(callback);
        this.setCallback(this.mqttMeasurer);
        //this.subscribe(topic);
        //HHH
        this.subscribe(topicNew);
        String messageN = "{ \"topic\": \"" + topic + "\", \"id\": \"" + this.clientId + "\"}";
        this.publish(MQTTSpatialMessages.TOPIC_PUBLISH_SUBSCRIPTION.getValue(), messageN);
    }


    public void setPrivacyModel(PrivacyModel privacyModel, RNG rng, String params) {
        /*
        this.pModel = privacyModel;
        this.rng = rng;
        switch (privacyModel) {
            case PERTURBATION -> {
                this.pManager = new GeoPerturbation(this.rng);
                this.pManager.setParameters(new JSONObject(params));
            }
            case DUMMY_UPDATES -> {
                this.pManager = new DummyUpdates(this.rng);
                this.pManager.setParameters(new JSONObject(params));
            }
            //this.pAttacker=new PrivacyAttacker(JSON.parse(params)["numDummy"]);
            case DUMMY_UPDATES_WITH_PERCOLATION -> {
                this.pManager = new Percolation(this.rng);
                this.pManager.setParameters(new JSONObject(params));
            }
            //this.pAttacker=new PrivacyAttacker(JSON.parse(params)["numDummy"]);
        }

        this.mqttMeasurer.setPrivacyModel(this.pManager);

        if (new JSONObject(params).getBoolean("selfTune")) {
            this.selfTuneMode = true;
            double alpha = new JSONObject(params).getDouble("alpha");
            this.tunerAlgorithm = new ParameterTuner(this.dummyOptions, this.perturbationOptions, alpha, this.pManager, this.mqttMeasurer, this.rng, new JSONObject(params).getDouble("interval"), new JSONObject(params).getDouble("explorationFactor"));
        }
         */
    }

    public MQTTClientMeasurer getMQTTStat() {
        return this.mqttMeasurer;
    }

    public void setTrajectory(Position cPosition, Position dPosition) {
        if (this.pModel == PrivacyModel.DUMMY_UPDATES_WITH_PERCOLATION)
            this.pManager.setTrajectory(cPosition, dPosition);
    }


    private String generateGeofenceTopic(String topic) {
        return topic + "_" + this.clientId;
    }

}
