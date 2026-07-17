package com.example.crowdsensingwotandroidapp.utils.campaign.submission;

public class LocationSubmission implements DataSubmission {

	private final Double latitude;
	private final Double longitude;
	// Accuratezza del fix in metri: richiesta dalla spec core/location@1.0.0
	// di Data Fly (il validatore la usa per il gate accuracy-max).
	private final Float accuracy;

	public LocationSubmission(Double latitude, Double longitude) {
		this(latitude, longitude, 0f);
	}

	public LocationSubmission(Double latitude, Double longitude, Float accuracy) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.accuracy = accuracy;
	}

	public Double getLatitude() {
		return latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public Float getAccuracy() {
		return accuracy;
	}
}
