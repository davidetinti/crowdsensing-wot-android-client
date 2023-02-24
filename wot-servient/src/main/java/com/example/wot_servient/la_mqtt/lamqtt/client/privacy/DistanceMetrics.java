package com.example.wot_servient.la_mqtt.lamqtt.client.privacy;

import com.example.wot_servient.la_mqtt.lamqtt.common.Direction;
import com.example.wot_servient.la_mqtt.lamqtt.common.Position;

import org.json.JSONException;
import org.json.JSONObject;

public class DistanceMetrics implements IPrivacyMetrics {

    private static final double MAX_SPEED = 5;
    private static final double MAX_PERTURBATION = 6000;
    private static final double SMOOTH_FACTOR = 0.5;
    private double maxSpace;


    public double compute(PrivacySet ps) {
        double distance = 0;
        for (Position pos : ps.dummySet) {
            distance += Direction.computeDistanceGPS(pos.latitude, pos.longitude, ps.realPosition.latitude, ps.realPosition.longitude);
        }
        distance = distance / ps.dummySet.size();
        // console.log("DIST "+distance+" "+ps.dummySet.length);
        distance = this.normalize(distance);
        return distance;
    }

    private double normalize(double distance) {
        double val = Math.pow(distance / this.maxSpace, DistanceMetrics.SMOOTH_FACTOR);
        if (val > 1.0)
            val = 1.0F;
        return val;
    }

    @Override
    public void update(PrivacySet ps) {

    }

    @Override
    public void setParameters(JSONObject parameters) throws JSONException {
        this.maxSpace = parameters.getDouble("interval") * parameters.getDouble("numdummy") * DistanceMetrics.MAX_SPEED;
        this.maxSpace += DistanceMetrics.MAX_PERTURBATION;

    }


}
