package com.example.crowdsensingwotandroidapp.utils.sensors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Posizione (FusedLocationProviderClient) — tipi "location" e "gps".
 * Il payload usa gli stessi campi dell'app iOS, così le campagne con
 * data_schema validano allo stesso modo su entrambe le piattaforme.
 */
public class LocationSensor implements DeviceSensor {

	@Override
	public List<String> getCampaignTypes() {
		return Arrays.asList("location", "gps");
	}

	@Override
	public boolean isAvailable(Context context) {
		return true;
	}

	@SuppressLint("MissingPermission")
	@Override
	public CompletableFuture<Map<String, Object>> read(Context context) {
		CompletableFuture<Map<String, Object>> toReturn = new CompletableFuture<>();
		LocationServices.getFusedLocationProviderClient(context)
				.getCurrentLocation(new CurrentLocationRequest.Builder().setMaxUpdateAgeMillis(10000)
											.build(), null)
				.addOnSuccessListener(location -> toReturn.complete(payload(location)))
				.addOnFailureListener(e -> toReturn.complete(null));
		return toReturn;
	}

	private Map<String, Object> payload(Location location) {
		if (location == null)
			return null;
		Map<String, Object> payload = new HashMap<>();
		payload.put("latitude", location.getLatitude());
		payload.put("longitude", location.getLongitude());
		payload.put("accuracy", (double) location.getAccuracy());
		payload.put("altitude", location.getAltitude());
		payload.put("speed", (double) location.getSpeed());
		payload.put("time", location.getTime());
		payload.put("provider", "android-fused");
		return payload;
	}
}
