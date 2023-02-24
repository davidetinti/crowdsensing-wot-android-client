package com.example.wot_servient.la_mqtt.lamqtt.simulator;

import com.example.wot_servient.la_mqtt.lamqtt.client.SpatialMQTTClient;
import com.example.wot_servient.la_mqtt.lamqtt.client.privacy.IPrivacyManager;
import com.example.wot_servient.la_mqtt.lamqtt.client.privacy.PrivacyModel;
import com.example.wot_servient.la_mqtt.lamqtt.client.privacy.PrivacySet;
import com.example.wot_servient.la_mqtt.lamqtt.common.Direction;
import com.example.wot_servient.la_mqtt.lamqtt.common.MQTTMessage;
import com.example.wot_servient.la_mqtt.lamqtt.common.MQTTReceiver;
import com.example.wot_servient.la_mqtt.lamqtt.common.Position;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class SimulatedUser implements MQTTReceiver {


    //TBF
    private static final String MQTT_USERNAME = "XXX";
    private static final String MQTT_PASSWORD = "XXX";
    private static final String MQTT_URL = "tcp://vz5nc4zqbrgzpn13.myfritz.net:1883";
    private final String id;
    private final Double maxSpeed;
    private final Double minSpeed;
    private final Double pauseTime;
    private final Scenario scenario;
    private Double cSpeed;
    private Position cPosition;
    private Position cDestination;
    private Direction cDirection;
    private Double cPauseTime;
    private final Double frequencyGPSPublish;
    private Double lastGPSPublish;
    private MobilityState cState;
    private final SpatialMQTTClient sMQTTClient;
    private SimEvaluator evaluator;
    private String topicSubscribed;
    private final IPrivacyManager pManager;
    private PrivacyAttacker pAttacker;
    private PrivacyModel pModel;
    private final Boolean verboseMode = true;

    SimulatedUser(String id, Double maxSpeed, Double minSpeed, Double pauseTime, Double frequency, PrivacyModel privacyModel, String privacyParameters, Scenario scenario) {
        this.id = id;
        this.maxSpeed = maxSpeed;
        this.minSpeed = minSpeed;
        this.pauseTime = pauseTime;
        this.scenario = scenario;
        this.frequencyGPSPublish = frequency;
        this.lastGPSPublish = null;
        this.evaluator = null;
        this.cState = MobilityState.IDLE;
        this.sMQTTClient = null;
        //this.sMQTTClient = new SpatialMQTTClient(SimulatedUser.MQTT_USERNAME, SimulatedUser.MQTT_PASSWORD, SimulatedUser.MQTT_URL, 1883, this.id);

        this.pManager = null;
        this.setPrivacyModel(privacyModel, privacyParameters);
    }

    public void setPrivacyModel(PrivacyModel privacyModel, String params) {
        this.pModel = privacyModel;
        this.sMQTTClient.setPrivacyModel(privacyModel, this.scenario.rng, params);
        /*
        switch(privacyModel) {
            case PrivacyModel.PERTURBATION:
                this.pManager=new GeoPerturbation(this.scenario.rng);
                this.pManager.setParameters(JSON.parse(params));
                break;
            case PrivacyModel.DUMMY_UPDATES:
                this.pManager=new DummyUpdates(this.scenario.rng);
                this.pManager.setParameters(JSON.parse(params));
                this.pAttacker=new PrivacyAttacker(JSON.parse(params)["numDummy"]);
                break;
            case PrivacyModel.DUMMY_UPDATES_WITH_PERCOLATION:
                this.pManager=new Percolation(this.scenario.rng);
                this.pManager.setParameters(JSON.parse(params));
                this.pAttacker=new PrivacyAttacker(JSON.parse(params)["numDummy"]);
                break;
        }*/
    }

    public void initialize() throws ExecutionException, InterruptedException {
        this.sMQTTClient.connect();
        String topicName = this.scenario.generateRandomTopic();
        //HHH
        this.sMQTTClient.subscribeGeofence(topicName, this);
        //this.sMQTTClient.subscribeGeofence(topicName, this.scenario);
        this.topicSubscribed = topicName;
        if (this.verboseMode)
            System.out.println("[SIM SUBSCRIBE] User: " + this.id + " Topic: " + this.topicSubscribed);
    }


    public String getTopicSubscribed() {
        return this.topicSubscribed;
    }

    public String getId() {
        return this.id;
    }

    public Position getPosition() {
        return this.cPosition;
    }

    public void dispose() throws ExecutionException, InterruptedException {
        this.sMQTTClient.disconnect();
    }

    public void setEvaluator(SimEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public void publishPosition(Double ctime) throws ExecutionException, InterruptedException, JSONException {
        if ((this.lastGPSPublish == null) || ((ctime - this.lastGPSPublish) >= this.frequencyGPSPublish)) {
            this.lastGPSPublish = ctime;
            Position posPrivacy;
            if (this.pModel != PrivacyModel.NONE) {
                posPrivacy = this.sMQTTClient.publicPosition(this.cPosition.latitude, this.cPosition.longitude).get();
                if (this.evaluator != null) {
                    this.evaluator.eventGPSSent();
                    this.evaluator.computePositionPrivacy(this.cPosition, posPrivacy);
                    if (this.pAttacker != null) {
                        if (!this.pAttacker.isFakeUpdate(posPrivacy))
                            this.evaluator.computePositionPrivacySmart(this.cPosition, posPrivacy);
                    }
                    double privacyValue = this.sMQTTClient.getMQTTStat().getInstantaneousPrivacyMetric();
                    if (privacyValue != PrivacySet.NO_METRIC_VALUE) this.evaluator.updatePrivacyMetric(privacyValue);

                }
            } else {
                this.sMQTTClient.publicPosition(this.cPosition.latitude, this.cPosition.longitude);
                if (this.evaluator != null) this.evaluator.eventGPSSent();
            }
        }
    }


    /*public async publishPosition(ctime: number) {
        if ((this.lastGPSPublish==undefined) || ((ctime-this.lastGPSPublish) >= this.frequencyGPSPublish)) {
            this.lastGPSPublish=ctime;
            let posPrivacy: Position;
            if (this.pManager!=undefined) {
                posPrivacy=this.pManager.transform(this.cPosition);
                await this.sMQTTClient.publicPosition(posPrivacy.latitude, posPrivacy.longitude);
                if (this.evaluator!=undefined) {
                    this.evaluator.eventGPSSent();
                    this.evaluator.computePositionPrivacy(this.cPosition, posPrivacy);
                    if (this.pAttacker!=undefined) {
                        if (this.pAttacker.isFakeUpdate(posPrivacy)==false)
                            this.evaluator.computePositionPrivacySmart(this.cPosition, posPrivacy);
                    }
                    let privacyValue: number=this.pManager.getPrivacyMetricValue();
                    if (privacyValue!=PrivacySet.NO_METRIC_VALUE)
                        this.evaluator.updatePrivacyMetric(privacyValue);

                }
            } else  {
                await this.sMQTTClient.publicPosition(this.cPosition.latitude, this.cPosition.longitude);
                if (this.evaluator!=undefined)
                    this.evaluator.eventGPSSent();
            }
        }
    }
*/
    public void setRandomPosition() {
        this.cPosition = this.scenario.createRandomPosition();
        //this.cPosition=new Position(44.50374616853628, 11.345168727453526);
        if (this.verboseMode)
            System.out.println("[CREATED USER] Id: " + this.id + " <" + this.cPosition.latitude + "," + this.cPosition.longitude + ">");
    }

    private void setRandomDestination() {
        this.cSpeed = this.scenario.rng.nextDouble() * Math.abs(this.maxSpeed - this.minSpeed) + this.minSpeed;
        this.cDestination = this.scenario.createRandomPosition();
        this.cDirection = new Direction(this.cPosition, this.cDestination, this.cSpeed);
        //    if (this.pModel==PrivacyModel.DUMMY_UPDATES_WITH_PERCOLATION)
        //      this.pManager.setTrajectory(this.cPosition,this.cDestination);
        if (this.pModel == PrivacyModel.DUMMY_UPDATES_WITH_PERCOLATION)
            this.sMQTTClient.setTrajectory(this.cPosition, this.cDestination);

    }


    public void move(Double timeAdvance, Double currentTime) throws ExecutionException, InterruptedException, JSONException {
        if (this.cState == MobilityState.IDLE) {
            this.setRandomDestination();
            this.cPosition = this.cDirection.computeAdvance(this.cSpeed, timeAdvance);
            this.cState = MobilityState.MOVING;
        } else if (this.cState == MobilityState.MOVING) {
            if (this.cDirection.isDestinationReached()) {
                this.cState = MobilityState.PAUSED;
                this.cPauseTime = Math.floor(this.scenario.rng.nextDouble() * this.pauseTime) + timeAdvance;
            } else {
                this.cPosition = this.cDirection.computeAdvance(this.cSpeed, timeAdvance);
                this.publishPosition(currentTime);
            }
        } else if (this.cState == MobilityState.PAUSED) {
            this.cPauseTime -= timeAdvance;
            if (this.cPauseTime <= 0.0) {
                this.setRandomDestination();
                this.cState = MobilityState.MOVING;
                this.cPauseTime = 0.0;
            }
        }
    }

    @Override
    public void messageRecv(MQTTMessage message) throws JSONException {
        String payload = message.message;
        this.evaluator.eventAdvReceived(message);
        JSONObject jsonOb = new JSONObject(payload);
        int gfId = Integer.parseInt((jsonOb.getString("message").split("\\|")[2].split("_")[1]));
        this.evaluator.setNotifiedUser(this.scenario.listGeofence.get(gfId).getSeqNo(), this.getId(), this.scenario.cTime);
        boolean isRelevant = this.scenario.listGeofence.get(gfId).isAdvSpatialRelevant(this.getPosition());
        this.evaluator.eventAdvRelevant(isRelevant);
    }
}
