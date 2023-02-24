package com.example.crowdsensingwotandroidapp.utils.campaign.submission;

public class LocationSubmission implements DataSubmission {

	private final Double latitude;
	private final Double longitude;

	public LocationSubmission(Double latitude, Double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public Double getLatitude() {
		return latitude;
	}

	public Double getLongitude() {
		return longitude;
	}
}
