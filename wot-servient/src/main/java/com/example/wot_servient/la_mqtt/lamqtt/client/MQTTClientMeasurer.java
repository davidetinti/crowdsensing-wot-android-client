package com.example.wot_servient.la_mqtt.lamqtt.client;

import com.example.wot_servient.la_mqtt.lamqtt.client.privacy.IPrivacyManager;
import com.example.wot_servient.la_mqtt.lamqtt.client.privacy.PrivacySet;
import com.example.wot_servient.la_mqtt.lamqtt.common.Direction;
import com.example.wot_servient.la_mqtt.lamqtt.common.MQTTMessage;
import com.example.wot_servient.la_mqtt.lamqtt.common.MQTTReceiver;
import com.example.wot_servient.la_mqtt.lamqtt.common.Position;

import org.json.JSONException;

import java.util.Objects;

public class MQTTClientMeasurer implements MQTTReceiver {

    private static final double RELEVANCE_DISTANCE = 300;
    private static final double DEFAULT_SPATIAL_ACCURACY = 0.3;

    private double numPublishSent;
    private double numNotificationRecv;
    private double numNotificationRelevant;
    private double spatialAccuracy;
    private IPrivacyManager pManager;

    private double numNotificationRecvPerSlot;
    private double numNotificationRelevantPerSlot;
    private double spatialAccuracyPerSlot;
    private double privacyPerSlot;
    private double numPrivacySamples;

    private Position cPosition;

    public MQTTClientMeasurer() {
        this.numPublishSent = 0.0;
        this.numNotificationRecv = 0.0;
        this.numNotificationRelevant = 0.0;
        this.spatialAccuracy = 0.0;
        this.cPosition = null;
        this.numNotificationRecvPerSlot = 0.0;
        this.numNotificationRelevantPerSlot = 0.0;
        this.spatialAccuracyPerSlot = 0.0;
        this.privacyPerSlot = 0.0;
        this.numPrivacySamples = 0.0;
        this.pManager = null;
    }


    public void setPrivacyModel(IPrivacyManager pm) {
        this.pManager = pm;
    }

    @Override
    public void messageRecv(MQTTMessage message) throws JSONException {
        this.numNotificationRecv += 1;
        this.numNotificationRecvPerSlot += 1;
        Position posGf = message.getPositionFromMessage();
        if (this.cPosition != null) {
            double distance = Direction.computeDistanceGPS(posGf.latitude, posGf.longitude, this.cPosition.latitude, this.cPosition.longitude);
            if (distance < MQTTClientMeasurer.RELEVANCE_DISTANCE) {
                this.numNotificationRelevant += 1;
                this.numNotificationRelevantPerSlot += 1;
                this.spatialAccuracy = (this.numNotificationRelevant) / this.numNotificationRecv;
                this.spatialAccuracyPerSlot = (this.numNotificationRelevantPerSlot) / this.numNotificationRecvPerSlot;
            }
        }
    }

    public void trackGPSPublish(Position cPos) {
        this.numPublishSent += 1;
        this.cPosition = cPos;
        if (this.pManager != null) this.updatePrivacyPerSlot();
    }

    public void trackGeofencePublish() {
        this.numPublishSent += 1;
    }

    public double getNumberPublishSent() {
        return this.numPublishSent;
    }

    public double getNumberNotificationRecv() {
        return this.numNotificationRecv;
    }

    public double getSpatialAccuracy() {
        return this.spatialAccuracy;
    }

    public double getNumberNotificationRecvPerSlot() {
        return this.numNotificationRecvPerSlot;
    }

    public double getSpatialAccuracyPerSlot() {
        if (this.numNotificationRecvPerSlot > 0) return this.spatialAccuracyPerSlot;
        else return MQTTClientMeasurer.DEFAULT_SPATIAL_ACCURACY;
    }

    public double getPrivacyPerSlot() {
        if (this.numPrivacySamples > 0) return this.privacyPerSlot / this.numPrivacySamples;
        else return PrivacySet.NO_METRIC_VALUE;
    }


    public void updatePrivacyPerSlot() {
        double value = this.pManager.getPrivacyMetricValue();
        if (!Objects.equals(value, PrivacySet.NO_METRIC_VALUE)) {
            this.privacyPerSlot += value;
            this.numPrivacySamples += 1;
        }
    }

    public double getInstantaneousPrivacyMetric() {
        if (this.pManager != null) return this.pManager.getPrivacyMetricValue();
        else return PrivacySet.NO_METRIC_VALUE;
    }


    public void resetLatest() {
        this.numNotificationRecvPerSlot = 0.0;
        this.spatialAccuracyPerSlot = 0.0;
        this.numNotificationRelevantPerSlot = 0.0;
        this.privacyPerSlot = 0.0;
        this.numPrivacySamples = 0.0;
    }
}
