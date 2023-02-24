package com.example.wot_servient.la_mqtt.lamqtt.client.privacy;

import com.example.wot_servient.la_mqtt.lamqtt.common.Position;

public class GridRegion {

    private static final double LEFT_CORNER_LAT = 44.482789890501586;
    private static final double LEFT_CORNER_LONG = 11.325016021728516;
    private static final double RIGHT_CORNER_LAT = 44.50715706370573;
    private static final double RIGHT_CORNER_LONG = 11.362781524658203;
    private static final double NUM_REGIONS_PER_SIZE = 2;
    private final double regionSizeLat;
    private final double regionSizeLong;

    public GridRegion() {
        this.regionSizeLat = (GridRegion.RIGHT_CORNER_LAT - GridRegion.LEFT_CORNER_LAT) / GridRegion.NUM_REGIONS_PER_SIZE;
        this.regionSizeLong = (GridRegion.RIGHT_CORNER_LONG - GridRegion.LEFT_CORNER_LONG) / GridRegion.NUM_REGIONS_PER_SIZE;
    }

    public double getNumRegions() {
        return GridRegion.NUM_REGIONS_PER_SIZE * GridRegion.NUM_REGIONS_PER_SIZE;
    }

    public double getCurrentRegion(Position cPosition) {
        double gridX = Math.floor((cPosition.latitude - GridRegion.LEFT_CORNER_LAT) / this.regionSizeLat);
        double gridY = Math.floor((cPosition.longitude - GridRegion.LEFT_CORNER_LONG) / this.regionSizeLong);
        return gridX * GridRegion.NUM_REGIONS_PER_SIZE + gridY;
    }
}
