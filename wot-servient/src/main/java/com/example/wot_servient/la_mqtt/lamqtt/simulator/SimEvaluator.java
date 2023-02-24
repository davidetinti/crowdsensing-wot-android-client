package com.example.wot_servient.la_mqtt.lamqtt.simulator;

import com.example.wot_servient.la_mqtt.lamqtt.backend.GeoProcessor;
import com.example.wot_servient.la_mqtt.lamqtt.common.MQTTMessage;
import com.example.wot_servient.la_mqtt.lamqtt.common.Position;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SimEvaluator {
    private static final String SIM_RESULT_FILE = "mqttStat.txt";
    private static final boolean SAVE_TO_FILE_MODE = true;
    private final String label;
    private final God god;
    private double mqttTotMessages;
    private double mqttAdvMessages;
    private double mqttRelevantAdvMessages;
    private double delay;
    private double precision;
    private double meanDistance;
    private double meanDistanceSmart;
    private double privacyMetric;
    private double privacyMetricUpdate;

    public SimEvaluator(String label) {
        this.mqttTotMessages = 0;
        this.mqttAdvMessages = 0;
        this.mqttRelevantAdvMessages = 0;
        this.delay = 0;
        this.precision = 0;
        this.god = new God();
        this.label = label;
        this.meanDistance = 0;
        this.meanDistanceSmart = 0;
        this.privacyMetric = 0;
        this.privacyMetricUpdate = 0;
    }

    public void eventGPSSent() {
        this.mqttTotMessages = this.mqttTotMessages + 1;
    }

    public void eventAdvReceived(MQTTMessage message) {
        this.mqttAdvMessages = this.mqttAdvMessages + 1;
    }

    public void eventAdvRelevant(boolean isRelevant) {
        if (isRelevant) this.mqttRelevantAdvMessages += 1;
    }


    public void newAdvertisement(double messageNo, String geofenceId) {
        this.god.newAdvertisement(messageNo, geofenceId);
    }


    public void setActiveUser(double messageNo, String userId, double ctime) {
        this.god.setActiveUser(messageNo, userId, ctime);
    }


    public void setNotifiedUser(double messageNo, String userId, double ctime) {
        this.god.setNotifiedUser(messageNo, userId, ctime);
    }

    public void updatePrivacyMetric(double value) {
        this.privacyMetric += value;
        this.privacyMetricUpdate++;
    }

    public void computeMean() {
        this.mqttRelevantAdvMessages = (this.mqttRelevantAdvMessages * 100 / this.mqttAdvMessages);
        this.precision = (this.god.computePrecision() * 100);
        this.delay = this.god.computeDelay();
        this.meanDistance = this.meanDistance / this.mqttTotMessages;
        this.meanDistanceSmart = this.meanDistanceSmart / this.mqttTotMessages;
        this.privacyMetric = this.privacyMetric / this.privacyMetricUpdate;
    }


    public void computePositionPrivacy(Position realPos, Position sentPos) {
        double distance = GeoProcessor.computeDistanceGPS(realPos.latitude, realPos.longitude, sentPos.latitude, sentPos.longitude);
        this.meanDistance += distance;
    }

    public void computePositionPrivacySmart(Position realPos, Position sentPos) {
        double distance = GeoProcessor.computeDistanceGPS(realPos.latitude, realPos.longitude, sentPos.latitude, sentPos.longitude);
        this.meanDistanceSmart += distance;
    }


    public void printStat() throws IOException {
        this.computeMean();
        System.out.println("Simulation Run Statistics");
        System.out.println("#MQTT_Messages_sent(total): " + this.mqttTotMessages);
        System.out.println("#MQTT_Advertisement_recv(total): " + this.mqttAdvMessages);
        System.out.println("#MQTT_Advertisement_relevant(%): " + this.mqttRelevantAdvMessages);
        System.out.println("#MQTT_Precision (%): " + this.precision);
        System.out.println("#MQTT_Delay (sec): " + this.delay);
        System.out.println("Privacy_distance_Mean_model (met): " + this.meanDistance);
        System.out.println("Privacy_distance_Trajectory_model (met): " + this.meanDistanceSmart);
        System.out.println("Privacy_metric_value: " + this.privacyMetric);

        if (SimEvaluator.SAVE_TO_FILE_MODE) this.saveResult();
    }


    private String getStatRow() {
        return this.label + "," + this.mqttTotMessages + "," + this.mqttAdvMessages + "," + this.mqttRelevantAdvMessages + "," + this.precision + "," + this.delay + "," + this.meanDistance + "," + this.meanDistanceSmart + "," + this.privacyMetric + "\n";
    }


    private void saveResult() throws IOException {
        if (new File(SimEvaluator.SIM_RESULT_FILE).createNewFile()) {
            System.out.println("Created new result file.");
        }
        try (FileWriter fileWriter = new FileWriter(SimEvaluator.SIM_RESULT_FILE)) {
            fileWriter.write(this.getStatRow());
        } catch (Exception e) {
            System.out.println("Error saving result");
        }
    }

}
