package com.example.crowdsensingwotandroidapp.utils;

import java.util.ArrayList;

public class User {

	private final String email;
	private final Integer points;
	private final ArrayList<String> completedCampaigns;

	public User(String email, Integer points, ArrayList completedCampaigns) {
		this.email = email;
		this.points = points;
		this.completedCampaigns = new ArrayList<>();
		if (completedCampaigns != null){
			for (int i = 0; i < completedCampaigns.size(); i++) {
				this.completedCampaigns.add((String) completedCampaigns.get(i));
			}
		}

	}

	public String getEmail() {
		return email;
	}

	public Integer getPoints() {
		return points;
	}

	public ArrayList<String> getCompletedCampaigns() {
		return completedCampaigns;
	}
}
