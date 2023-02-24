package com.example.crowdsensingwotandroidapp.utils.campaign.area;

import android.location.Location;

import java.util.HashMap;

public class CampaignCircleArea implements CampaignArea {

	private final String type;
	private final Number radius;
	private final Number centerLat;
	private final Number centerLng;

	public CampaignCircleArea(HashMap area) {
		type = "circle";
		radius = (Number) area.get("radius");
		centerLat = (Number) area.get("center_lat");
		centerLng = (Number) area.get("center_lng");
	}

	@Override
	public String getType() {
		return type;
	}

	public Number getRadius() {
		return radius;
	}

	public Number getCenterLat() {
		return centerLat;
	}

	public Number getCenterLng() {
		return centerLng;
	}

	@Override
	public boolean isInsideArea(Location location) {
		Location center = new Location("");
		center.setLatitude(centerLat.doubleValue());
		center.setLongitude(centerLng.doubleValue());
		return location.distanceTo(center) <= radius.floatValue();
	}
}