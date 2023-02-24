package com.example.wot_servient.la_mqtt.lamqtt.simulator;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class God {
    private final HashMap<String, GeofenceData> snapshotAdv;
    private final boolean debugMode;
    private final boolean OPTIMIZED_MODE;

    public God() {
        this.snapshotAdv = new HashMap<>();
        this.debugMode = false;
        this.OPTIMIZED_MODE = false;
    }

    public void newAdvertisement(double messageNo, String geofenceId) {
        String messageS = String.valueOf(messageNo);
        GeofenceData gData = new GeofenceData(geofenceId);
        this.snapshotAdv.put(messageS, gData);
        //System.out.println(this.snapshotAdv.keys());
        this.snapshotAdv.forEach((key, gEntry) -> {
            if ((Objects.equals(gEntry.id, geofenceId)) && (!Objects.equals(key, messageS))) {
                gEntry.active = false;
                if (this.OPTIMIZED_MODE) this.snapshotAdv.remove(key);
            }
        });
        if (this.debugMode) this.printSnapshot();
    }

    public void setActiveUser(double messageNo, String userId, double ctime) {
        String messageS = String.valueOf(messageNo);
        GeofenceData gData = this.snapshotAdv.get(messageS);
        if ((gData != null) && (gData.active)) {
            boolean found = false;
            for (int i = 0; i < gData.userList.size(); i++) {
                if (Objects.equals(gData.userList.get(i).id, userId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                gData.userList.add(new ActiveUser(userId, ctime));
            }
        }

        if (this.debugMode) this.printSnapshot();
    }


    public void setNotifiedUser(double messageNo, String userId, double ctime) {
        String messageS = String.valueOf(messageNo);
        GeofenceData gData = this.snapshotAdv.get(messageS);
        if ((gData != null) && (gData.active)) {
            for (int i = 0; i < gData.userList.size(); i++) {
                if (Objects.equals(gData.userList.get(i).id, userId)) {
                    gData.userList.get(i).notified = true;
                    gData.userList.get(i).timeNotified = ctime;
                }
            }
        }

        if (this.debugMode) this.printSnapshot();

    }

    public void printSnapshot() {
        System.out.println("------GOD------");
        this.snapshotAdv.forEach((key, gEntry) -> {
            System.out.println("Key: " + key + "GFence: " + gEntry.id + " active: " + gEntry.active + " numUsers: " + gEntry.userList.size());
            for (int i = 0; i < gEntry.userList.size(); i++) {
                System.out.println("User " + gEntry.userList.get(i).id + " Notified: " + gEntry.userList.get(i).notified + " Time enter: " + gEntry.userList.get(i).timeEnter + " Time notified: " + gEntry.userList.get(i).timeNotified);
            }
        });
    }

    public double computePrecision() {
        AtomicInteger totUserEntered = new AtomicInteger();
        AtomicInteger totUserNotified = new AtomicInteger();
        this.snapshotAdv.forEach((key, gEntry) -> {
            totUserEntered.set(gEntry.userList.size());
            for (int i = 0; i < gEntry.userList.size(); i++) {
                if (gEntry.userList.get(i).notified) totUserNotified.addAndGet(1);
            }
        });
        return (double) totUserNotified.get() / totUserEntered.get();
    }


    public double computeDelay() {
        AtomicReference<Double> meanDelay = new AtomicReference<>(0.0);
        AtomicReference<Double> totUserNotified = new AtomicReference<>(0.0);
        this.snapshotAdv.forEach((key, gEntry) -> {
            for (int i = 0; i < gEntry.userList.size(); i++) {
                if (gEntry.userList.get(i).notified) {
                    totUserNotified.updateAndGet(v -> v + 1);
                    System.out.println(gEntry.userList.get(i).timeNotified);
                    if (gEntry.userList.get(i).timeNotified >= gEntry.userList.get(i).timeEnter) {
                        int finalI = i;
                        meanDelay.updateAndGet(v -> v + (gEntry.userList.get(finalI).timeNotified - gEntry.userList.get(finalI).timeEnter));
                    }
                }
            }
        });
        return meanDelay.get() / totUserNotified.get();
    }
}
