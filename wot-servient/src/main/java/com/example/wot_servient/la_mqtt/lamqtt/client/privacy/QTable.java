package com.example.wot_servient.la_mqtt.lamqtt.client.privacy;

import com.example.wot_servient.la_mqtt.lamqtt.common.Position;
import com.example.wot_servient.la_mqtt.lamqtt.simulator.RNG;

import java.util.ArrayList;
import java.util.HashMap;

public class QTable {

    private static final double DEFAULT_VALUE = -1;
    private static final double GAMMA = 0.5;
    private final HashMap<String, Double> qTable;
    private final RNG rng;
    private final GridRegion gr;


    public QTable(Configuration conf, ArrayList<Double> dummyUpdateValues, ArrayList<Double> perturbationValues, RNG rng) {
        this.qTable = new HashMap<>();
        this.gr = new GridRegion();
        this.rng = rng;
        for (int i = 0; i < conf.getNumConfigurations(); i++) {
            for (int k = 0; k < this.gr.getNumRegions(); k++) {
                String stateVal = this.encode(k, i);
                this.qTable.put(stateVal, QTable.DEFAULT_VALUE);
            }
        }

    }


    public double getNumEntries(Configuration conf) {
        return this.gr.getNumRegions() * conf.getNumConfigurations();
    }

    private String encode(double gridCell, double configNumber) {
        return gridCell + "_" + configNumber;
    }


    public void update(Configuration conf, double action, Position cPosition, double reward) {
        double gridCell = this.gr.getCurrentRegion(cPosition);
        String state = this.encode(gridCell, conf.getConfigNumber());
        double val;
        if (this.qTable.get(state) == QTable.DEFAULT_VALUE)
            val = reward;
        else
            val = QTable.GAMMA * reward + (1 - QTable.GAMMA) * this.qTable.get(state);
        System.out.println("State: " + state + " QTable: " + val + " " + "Reward: " + reward);
        this.qTable.put(state, val);
    }


    public double getBestAction(Position cPosition, Configuration conf, double temperature) {
        double gridCell = this.gr.getCurrentRegion(cPosition);
        ArrayList<Double> probAction = new ArrayList<>();
        int bestAction = 0;
        double probTot = 0;
        for (int action = 0; action < conf.getNumConfigurations(); action++) {
            String state = this.encode(gridCell, action);
            double value = this.qTable.get(state);
            probAction.add(action, Math.exp(value / temperature));
            probTot += probAction.get(action);
        }
        for (int action = 0; action < conf.getNumConfigurations(); action++) {
            probAction.set(action, probAction.get(action) / probTot);
        }

        double ranValue = this.rng.nextDouble();
        boolean end = false;
        double base = 0;
        for (int action = 0; ((action < conf.getNumConfigurations()) && (!end)); action++) {
            base = base + probAction.get(action);
            if (ranValue <= base) {
                end = true;
                bestAction = action;
            }
        }
        System.out.println("BestAction: " + bestAction + " DU: " + conf.getDummiesFromConfigNumber(bestAction) + " PE: " + conf.getPerturbationFromConfigNumber(bestAction));
        return bestAction;
    }

}
