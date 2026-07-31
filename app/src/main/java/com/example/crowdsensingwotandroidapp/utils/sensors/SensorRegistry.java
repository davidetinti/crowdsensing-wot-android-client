package com.example.crowdsensingwotandroidapp.utils.sensors;

import android.content.Context;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Registro dei sensori disponibili sul dispositivo, consultato per le
 * letture pull, per l'invio manuale e per stabilire quali modalità di
 * partecipazione sono proponibili per una campagna.
 */
public final class SensorRegistry {

	private static final List<DeviceSensor> SENSORS = Arrays.asList(
			new LocationSensor(),
			new BatterySensor(),
			new BarometerSensor(),
			new AccelerometerSensor());

	private SensorRegistry() {
	}

	/** Il sensore disponibile per il tipo di campagna, o null. */
	public static DeviceSensor sensorForType(Context context, String campaignType) {
		for (DeviceSensor sensor : SENSORS) {
			if (sensor.getCampaignTypes().contains(campaignType) && sensor.isAvailable(context))
				return sensor;
		}
		return null;
	}

	/** Legge il valore corrente per il tipo di campagna (null se impossibile). */
	public static CompletableFuture<Map<String, Object>> read(Context context, String campaignType) {
		DeviceSensor sensor = sensorForType(context, campaignType);
		if (sensor == null)
			return CompletableFuture.completedFuture(null);
		return sensor.read(context);
	}
}
