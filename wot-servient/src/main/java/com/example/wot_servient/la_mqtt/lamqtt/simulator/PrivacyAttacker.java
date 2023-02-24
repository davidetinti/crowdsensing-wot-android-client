package com.example.wot_servient.la_mqtt.lamqtt.simulator;

import com.example.wot_servient.la_mqtt.lamqtt.backend.GeoProcessor;
import com.example.wot_servient.la_mqtt.lamqtt.common.Position;

import java.util.ArrayList;

public class PrivacyAttacker {
    private static final double MAX_RADIUS = 50;
    private static final double MAX_HISTORY = 30;
    private final double dummy;
    private Position lastKnown;
    private ArrayList<Position> trajectory;

    PrivacyAttacker(double dummy) {
        this.lastKnown = null;
        this.trajectory = new ArrayList<>();
        this.dummy = dummy;
    }

    public boolean isFakeUpdate(Position pos) {
        this.addToTrajectory(pos);

        if (this.lastKnown == null) return true;

        if (this.isWithinMaxDistance(this.lastKnown, pos, PrivacyAttacker.MAX_RADIUS * this.dummy)) {
            this.lastKnown = pos;
            return true;
        } else return false;

    }

    private void addToTrajectory(Position dest) {
        if (this.trajectory == null) {
            ArrayList<Position> aPos = new ArrayList<>();
            aPos.add(dest);
            this.trajectory = aPos;
            return;
        }
        ArrayList<Position> aPos = this.trajectory;
        if (aPos.size() < PrivacyAttacker.MAX_HISTORY) aPos.add(dest);
        else {
            aPos.remove(0);
            aPos.add(dest);
            this.computeBestTrajectory();
        }

    }

    private void computeBestTrajectory() {
        Double bLength = null;
        Position candidate = null;
        for (int i = 0; i < this.trajectory.size(); i++) {
            Position start = this.trajectory.get(i);
            double cLength = 0.0;

            for (int j = (int) (i + this.dummy); j < this.trajectory.size(); j += this.dummy) {
                Position end = this.trajectory.get(j);
                boolean outcome = this.isWithinMaxDistance(start, end, PrivacyAttacker.MAX_RADIUS * this.dummy);
                if (outcome) cLength += 1;
                if ((bLength == null) || (cLength > bLength)) {
                    candidate = this.trajectory.get(j);
                    bLength = cLength;
                    start = this.trajectory.get(j);
                }
            }

        }
        if (candidate != null) {
            this.lastKnown = candidate;
        }
    }

    public boolean isWithinMaxDistance(Position start, Position dest, double radius) {
        double distance = GeoProcessor.computeDistanceGPS(start.latitude, start.longitude, dest.latitude, dest.longitude);
        return distance < radius;
    }
}
