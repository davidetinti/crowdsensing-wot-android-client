package com.example.wot_servient.la_mqtt.lamqtt.client.privacy;

import java.util.ArrayList;

public class Configuration {
    private final ArrayList<Double> dummyUpdateValues;
    private final ArrayList<Double> perturbationValues;
    public double cDummyValue;
    public int cDummyIndex;
    public double cPerturbationValue;
    public int cPerturbationIndex;

    public Configuration(ArrayList<Double> dummyUpdateValues, ArrayList<Double> perturbationValues) {
        this.dummyUpdateValues = dummyUpdateValues;
        this.perturbationValues = perturbationValues;
        this.cDummyIndex = 0;
        this.cPerturbationIndex = 0;
        this.cDummyValue = this.dummyUpdateValues.get(0);
        this.cPerturbationValue = this.perturbationValues.get(0);
    }

    public Integer getNumConfigurations() {
        return this.dummyUpdateValues.size() * this.perturbationValues.size();
    }

    public void setConfiguration(double action) {
        this.cDummyIndex = (int) Math.round(action % this.dummyUpdateValues.size());
        this.cPerturbationIndex = (int) Math.floor(action / this.dummyUpdateValues.size());
        this.cDummyValue = this.dummyUpdateValues.get(this.cDummyIndex);
        this.cPerturbationValue = this.perturbationValues.get(this.cPerturbationIndex);
    }

    public Integer getConfigNumber() {
        return ((this.cPerturbationIndex * this.dummyUpdateValues.size()) + this.cDummyIndex);
    }

    public double getDummiesFromConfigNumber(Integer val) {
        int index = val % this.dummyUpdateValues.size();
        return this.dummyUpdateValues.get(index);
    }

    public double getPerturbationFromConfigNumber(int val) {
        int index = (int) Math.floor((double) val / this.dummyUpdateValues.size());
        return this.perturbationValues.get(index);
    }
}
