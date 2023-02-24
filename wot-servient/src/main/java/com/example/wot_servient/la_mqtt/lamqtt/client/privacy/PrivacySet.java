package com.example.wot_servient.la_mqtt.lamqtt.client.privacy;

import com.example.wot_servient.la_mqtt.lamqtt.common.Position;

import java.util.ArrayList;

public class PrivacySet {
    public Position realPosition;
    public ArrayList<Position> dummySet;
    public static double NO_METRIC_VALUE = -1;

    public PrivacySet(){
        this.dummySet = new ArrayList<>();
    }

    public void add(Position pos){
        this.dummySet.add(pos);
    }
}
