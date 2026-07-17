package com.example.crowdsensingwotandroidapp.datafly;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-discovery delle capability del device (docs/03 M4 del monorepo):
 * PackageManager.hasSystemFeature + SensorManager.getSensorList, mappati sulle
 * specifiche del registry tramite gli stessi hint dichiarati in
 * `data_specs.discovery` (androidFeatures / androidSensorTypes).
 *
 * MVP: la mappa e' cablata su core/location@1.0.0 (unica spec del pilota).
 * Quando il registry crescera', la lista puo' essere scaricata da
 * GET {CORE_URL}/specs e la mappa costruita a runtime dagli hint.
 */
public final class SensorDiscovery {

	/** Capability rilevata, pronta per la TD (annotazioni dfy:spec / @type). */
	public static class DiscoveredCapability {
		public final String specKey;
		public final String specVersion;
		public final String semanticType;
		public final String affordanceName;

		public DiscoveredCapability(String specKey, String specVersion, String semanticType,
				String affordanceName) {
			this.specKey = specKey;
			this.specVersion = specVersion;
			this.semanticType = semanticType;
			this.affordanceName = affordanceName;
		}

		public String specRef() {
			return specKey + "@" + specVersion;
		}
	}

	private SensorDiscovery() {
	}

	/** Rileva le capability supportate da questo device. */
	public static List<DiscoveredCapability> discover(Context context) {
		List<DiscoveredCapability> capabilities = new ArrayList<>();
		PackageManager pm = context.getPackageManager();

		// core/location: hint androidFeatures = ["android.hardware.location.gps"]
		if (pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
			capabilities.add(new DiscoveredCapability("core/location", "1.0.0", "dfy:Location",
					"location"));
		}

		// Sensori inerziali/ambientali: pronti per le prossime spec del registry
		// (core/accelerometer, core/pressure, ...): il rilevamento c'e' gia',
		// l'aggiunta alla TD avverra' quando le spec saranno pubblicate.
		SensorManager sensorManager =
				(SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		if (sensorManager != null) {
			for (Sensor sensor : sensorManager.getSensorList(Sensor.TYPE_ALL)) {
				// riservato: mappa Sensor.TYPE_* -> spec key via registry hints
				// (vedi data_specs.discovery.androidSensorTypes)
				sensor.getType();
			}
		}
		return capabilities;
	}
}
