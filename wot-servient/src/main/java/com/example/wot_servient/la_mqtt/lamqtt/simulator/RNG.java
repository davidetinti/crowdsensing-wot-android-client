package com.example.wot_servient.la_mqtt.lamqtt.simulator;
import java.util.ArrayList;

public class RNG {
    private double seed;

    public RNG(double seed) {
        this.seed = seed;
    }

    public double getSeed() {
        return this.seed;
    }

    private double next(Double min, Double max) {
        if (max == null) max = 0.0;
        if (min == null) min = 0.0;

        this.seed = (this.seed * 9301 + 49297) % 233280;
        double rnd = this.seed / 233281;

        return min + rnd * (max - min);
    }

    public int nextInt(double min, double max) {
        return (int) Math.floor(this.next(min, max));
    }

    public double nextDouble() {
        return this.next(0.0, 1.0);
    }

    public Object pick(ArrayList<Object> collection) {
        return collection.get(this.nextInt(0.0, collection.size() - 1));
    }
}
