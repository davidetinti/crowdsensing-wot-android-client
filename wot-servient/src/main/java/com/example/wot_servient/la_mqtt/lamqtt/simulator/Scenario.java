package com.example.wot_servient.la_mqtt.lamqtt.simulator;

import com.example.wot_servient.la_mqtt.lamqtt.backend.SpatialMQTTBackEnd;
import com.example.wot_servient.la_mqtt.lamqtt.client.privacy.PrivacyModel;
import com.example.wot_servient.la_mqtt.lamqtt.common.MQTTMessage;
import com.example.wot_servient.la_mqtt.lamqtt.common.MQTTReceiver;
import com.example.wot_servient.la_mqtt.lamqtt.common.Position;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class Scenario implements MQTTReceiver {

    private static final String DEFAULT_TOPIC_PREFIX = "T";
    private static final String DEFAULT_GEOFENCE_PREFIX = "GF";
    private static final String DEFAULT_USER_PREFIX = "U";
    public Position leftCorner;
    public Position rightCorner;
    public ArrayList<SimulatedGeofence> listGeofence;
    public ArrayList<SimulatedUser> listUser;
    public RNG rng;
    public double cTime;
    public double seed;
    private double numTopics;
    private SimEvaluator evaluator;


    public Scenario(Position left, Position right, double seed) {
        this.leftCorner = left;
        this.rightCorner = right;
        this.numTopics = 0.0;
        this.rng = new RNG(seed);
        this.seed = seed;
        this.listGeofence = new ArrayList<>();
        this.listUser = new ArrayList<>();

    }


    public void setEvaluator(SimEvaluator evaluator) {
        this.evaluator = evaluator;
        if (this.listUser != null)
            for (SimulatedUser simulatedUser : this.listUser) simulatedUser.setEvaluator(evaluator);
        if (this.listGeofence != null)
            for (SimulatedGeofence simulatedGeofence : this.listGeofence) simulatedGeofence.setEvaluator(evaluator);

    }


    public void setGeofences(double numGeofence, double radius, double numTopic, double frequency) {
        this.numTopics = numTopic;
        double geofencePerTopic = Math.floor(numGeofence / numTopic);
        double cTopic = 0;
        double counterTopic = 0;
        String topicName;
        for (int i = 0; i < numGeofence; i++) {
            String id = Scenario.DEFAULT_GEOFENCE_PREFIX + "_" + i;
            if (geofencePerTopic == counterTopic) {
                counterTopic = 0;
                cTopic += 1;
            } else {
                counterTopic++;
            }
            topicName = Scenario.DEFAULT_TOPIC_PREFIX + "_" + cTopic;
            SimulatedGeofence geofence = new SimulatedGeofence(id, topicName, frequency);
            geofence.setRandomPosition(this, radius);
            this.listGeofence.add(geofence);
            geofence.initialize();
        }
    }

    public void setUsers(double numUser, double maxSpeed, double minSpeed, double pauseTime, double frequency, PrivacyModel privacyModel, String privacyParameters) throws ExecutionException, InterruptedException {
        for (int i = 0; i < numUser; i++) {
            String id = Scenario.DEFAULT_USER_PREFIX + "_" + i;
            SimulatedUser user = new SimulatedUser(id, maxSpeed, minSpeed, pauseTime, frequency, privacyModel, privacyParameters, this);
            user.setRandomPosition();
            this.listUser.add(user);
            user.initialize();
        }
    }

    public void hackSubscriptions(SpatialMQTTBackEnd backend) {
        for (SimulatedUser simulatedUser : this.listUser) {
            String topic = simulatedUser.getTopicSubscribed();
            String id = simulatedUser.getId();
            backend.newSubscribeEvent(id, topic);
        }
    }

    public void dispose() throws ExecutionException, InterruptedException {
        for (SimulatedUser simulatedUser : this.listUser) simulatedUser.dispose();
        for (SimulatedGeofence simulatedGeofence : this.listGeofence) simulatedGeofence.dispose();

    }


    public Position createRandomPosition() {
        double maxLat = Math.abs(this.leftCorner.latitude - this.rightCorner.latitude);
        double maxLong = Math.abs(this.leftCorner.longitude - this.rightCorner.longitude);
        double randomLat = this.rng.nextDouble() * maxLat + Math.min(this.leftCorner.latitude, this.rightCorner.latitude);
        double randomLong = this.rng.nextDouble() * maxLong + Math.min(this.leftCorner.longitude, this.rightCorner.longitude);
        return new Position(randomLat, randomLong);

    }

    public void update(double timeAdvance, double currentTime) throws ExecutionException, InterruptedException, JSONException {
        for (SimulatedGeofence simulatedGeofence : this.listGeofence) simulatedGeofence.publishAdv(currentTime);
        for (SimulatedUser simulatedUser : this.listUser) simulatedUser.move(timeAdvance, currentTime);

        this.cTime = currentTime;
        // FOR STATS ONLY
        for (SimulatedUser simulatedUser : this.listUser) {
            for (SimulatedGeofence simulatedGeofence : this.listGeofence) {
                boolean isEntered = simulatedGeofence.isAdvSpatialRelevant(simulatedUser.getPosition());
                if (isEntered && (Objects.equals(simulatedGeofence.getTopic(), simulatedUser.getTopicSubscribed())))
                    this.evaluator.setActiveUser(simulatedGeofence.getSeqNo(), simulatedUser.getId(), currentTime);
            }
        }

    }

    public String generateRandomTopic() {
        double topicNo = this.rng.nextInt(0.0, this.numTopics);
        return (Scenario.DEFAULT_TOPIC_PREFIX + "_" + topicNo);
    }

    @Override
    public void messageRecv(MQTTMessage msg) throws JSONException {
        //String payload= msg.message;
        String payload = msg.getContent();
        this.evaluator.eventAdvReceived(msg);
        int gfId = Integer.parseInt(payload.split("\\|")[2].split("_")[1]);
        int userId = Integer.parseInt(msg.topic.split("_")[3]);
        if ((userId < this.listUser.size()) && (gfId < this.listGeofence.size())) {
            this.evaluator.setNotifiedUser(this.listGeofence.get(gfId).getSeqNo(), this.listUser.get(userId).getId(), this.cTime);
            boolean isRelevant = this.listGeofence.get(gfId).isAdvSpatialRelevant(this.listUser.get(userId).getPosition());
            this.evaluator.eventAdvRelevant(isRelevant);
        }
    }
}
