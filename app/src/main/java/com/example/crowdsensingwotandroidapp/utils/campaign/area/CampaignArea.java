package com.example.crowdsensingwotandroidapp.utils.campaign.area;

import android.location.Location;

import java.io.Serializable;

public interface CampaignArea extends Serializable {

	boolean isInsideArea(Location location);

	String getType();
}
