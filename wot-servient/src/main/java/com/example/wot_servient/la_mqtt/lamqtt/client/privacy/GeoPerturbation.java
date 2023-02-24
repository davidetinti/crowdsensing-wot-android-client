package com.example.wot_servient.la_mqtt.lamqtt.client.privacy;

import com.example.wot_servient.la_mqtt.lamqtt.common.Position;
import com.example.wot_servient.la_mqtt.lamqtt.simulator.RNG;

import org.json.JSONException;
import org.json.JSONObject;

public class GeoPerturbation implements IPrivacyManager {
    private final RNG rng;
    private int perturbationDigit;

    public GeoPerturbation(RNG rng) {
        this.rng = rng;
    }

    private double generateRandomDigit() {
        return this.rng.nextInt(0.0, 10.0);
    }


    @Override
    public Position transform(Position cPosition) {
        String latString = String.valueOf(cPosition.latitude);
        StringBuilder finalLat = new StringBuilder(latString.split("\\.")[0] + ".");
        latString = latString.split("\\.")[1];
        double numDigit = latString.length();

        String longString = String.valueOf(cPosition.longitude);
        StringBuilder finalLong = new StringBuilder(longString.split("\\.")[0] + ".");
        longString = longString.split("\\.")[1];

        String prefixString = latString.substring(0, this.perturbationDigit);
        finalLat.append(prefixString);
        prefixString = longString.substring(0, this.perturbationDigit);
        finalLong.append(prefixString);

        for (int i = 0; i < (numDigit - prefixString.length()); i++) {
            finalLat.append(this.generateRandomDigit());
            finalLong.append(this.generateRandomDigit());
        }
        return new Position(Double.parseDouble(finalLat.toString()), Double.parseDouble(finalLong.toString()));
    }

    @Override
    public void setParameters(JSONObject parameters) throws JSONException {
        this.perturbationDigit = parameters.getInt("digit");
        System.out.println("Changed: " + this.perturbationDigit);

    }

    @Override
    public void setTrajectory(Position cPosition, Position dPosition) {

    }

    @Override
    public double getPrivacyMetricValue() {
        return PrivacySet.NO_METRIC_VALUE;
    }
}
