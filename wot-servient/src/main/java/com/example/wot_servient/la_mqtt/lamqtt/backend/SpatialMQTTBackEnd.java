package com.example.wot_servient.la_mqtt.lamqtt.backend;

import com.example.wot_servient.la_mqtt.lamqtt.backend.dto.ILogCallback;
import com.example.wot_servient.la_mqtt.lamqtt.common.MQTTClient;
import com.example.wot_servient.la_mqtt.lamqtt.common.MQTTMessage;
import com.example.wot_servient.la_mqtt.lamqtt.common.MQTTReceiver;
import com.example.wot_servient.la_mqtt.lamqtt.common.MQTTSpatialMessages;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SpatialMQTTBackEnd extends MQTTClient implements MQTTReceiver, ILogCallback {

    private static final String DEFAULT_NAME = "MQTT_BACKEND";
    private static final String STORAGE_NAME = "smqtt";


    //HHH
    private static final boolean GEOFENCE_PERSISTENCE = true; //false
    private static final boolean NOTIFY_ONLY_ONCE = false; //true

    private final boolean verboseMode;
    private final PersisterType persisterType;
    private final IPersister persister;
    private final GeoProcessor geoProcessor;
    private final HashMap<String, Integer> historyGeofence;
    private final HashMap<String, Integer> historyNotificationSent;

    public SpatialMQTTBackEnd(String username, String password, String host, Integer port) {
        super(username, password, host, port, SpatialMQTTBackEnd.DEFAULT_NAME);
        this.persisterType = PersisterType.MEMORY;
        this.persister = new MemPersister();

        this.geoProcessor = new GeoProcessor(this.persister, this);
        this.historyGeofence = new HashMap<>();
        this.historyNotificationSent = new HashMap<>();
        this.verboseMode = false;
    }


    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            try {
                boolean res = this.connect();
                if (res) {
                    this.setCallback(this);
                    this.subscribe(MQTTSpatialMessages.TOPIC_PUBLISH_POSITION.getValue());
                    this.subscribe(MQTTSpatialMessages.TOPIC_PUBLISH_GEOFENCE.getValue());
                    this.subscribe(MQTTSpatialMessages.TOPIC_PUBLISH_SUBSCRIPTION.getValue());
                    this.persister.connect();
                    //this.logWatcher.start(this);
                }
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void stop() throws ExecutionException, InterruptedException {
        this.disconnect();
        this.persister.disconnect();
        //this.logWatcher.stop();
        System.out.println("Connection to DB closed");
    }

    public void messageRecv(MQTTMessage msg) throws JSONException {
        if (Objects.equals(msg.topic, MQTTSpatialMessages.TOPIC_PUBLISH_POSITION.getValue()))
            this.handlePositionUpdate(msg.message);
        else if (Objects.equals(msg.topic, MQTTSpatialMessages.TOPIC_PUBLISH_GEOFENCE.getValue()))
            this.handleGeofenceUpdate(msg.message);
        else if (Objects.equals(msg.topic, MQTTSpatialMessages.TOPIC_PUBLISH_SUBSCRIPTION.getValue()))
            this.handleSubscriptionUpdate(msg.message);
    }

    private void handlePositionUpdate(String payload) throws JSONException {
        if (this.verboseMode) {
            System.out.println("[BACKEND] Received UPDATE POSITION: " + payload);
        }
        JSONObject objJSON = new JSONObject(payload);
        this.persister.addUserPosition(objJSON.getString("id"), objJSON.getDouble("latitude"), objJSON.getDouble("longitude"));
        if (SpatialMQTTBackEnd.GEOFENCE_PERSISTENCE)
            this.geoProcessor.processUpdate(objJSON.getString("id"), objJSON.getDouble("latitude"), objJSON.getDouble("longitude"));
    }

    private void handleGeofenceUpdate(String payload) throws JSONException {
        if (this.verboseMode) {
            System.out.println("[BACKEND] Received UPDATE GEOFENCE: " + payload);
        }
        JSONObject objJSON = new JSONObject(payload);
        Integer seqNo = this.historyGeofence.get(objJSON.getString("id"));
        if (seqNo == null) {
            seqNo = 1;
        } else {
            seqNo += 1;
        }
        this.historyGeofence.put(objJSON.getString("id"), seqNo);
        this.persister.addGeofence(objJSON.getString("topicGeofence"), objJSON.getString("id"), objJSON.getDouble("latitude"), objJSON.getDouble("longitude"), objJSON.getDouble("radius"), objJSON.getString("message"));
    }

    private void handleSubscriptionUpdate(String payload) throws JSONException {
        if (this.verboseMode) {
            System.out.println("[BACKEND] Received SUBSCRIPTION: " + payload);
        }
        JSONObject objJSON = new JSONObject(payload);
        this.newSubscribeEvent(objJSON.getString("id"), objJSON.getString("topic"));
    }

    public void newSubscribeEvent(String clientId, String topic) {
        //if (this.verboseMode==true) {
        System.out.println("[BACKEND] New subscription " + clientId + " " + topic);
        //}
        this.persister.addSubscription(clientId, topic);
    }

    public void advertiseClient(String geofenceId, String topic, String clientId, String message) throws ExecutionException, InterruptedException {
        String correctTopic = topic + "_" + clientId;
        if (SpatialMQTTBackEnd.NOTIFY_ONLY_ONCE) {
            String signature = geofenceId + "_" + clientId;
            Integer currentSeqNo = this.historyNotificationSent.get(signature);
            Integer lastSeqNo = this.historyGeofence.get(geofenceId);
            if ((currentSeqNo == null) || (currentSeqNo < lastSeqNo)) {
                this.historyNotificationSent.put(signature, lastSeqNo);
                if (this.verboseMode)
                    System.out.println("[BACKEND] Publish GEO_ADVERTISEMENT to: " + clientId + " topic: " + correctTopic);
                //this.publish(topic, message);
                //HHH
                this.publish(correctTopic, message);
            }
        } else {
            if (this.verboseMode)
                System.out.println("[BACKEND] Publish GEO_ADVERTISEMENT to: " + clientId + " topic: " + correctTopic);
            //HHH
            this.publish(correctTopic, message);
            //this.publish(topic, message);
        }

    }
}
