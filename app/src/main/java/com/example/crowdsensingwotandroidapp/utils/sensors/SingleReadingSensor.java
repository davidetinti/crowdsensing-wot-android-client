package com.example.crowdsensingwotandroidapp.utils.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base per i sensori hardware Android che producono il payload da una
 * singola lettura (barometro, accelerometro, ...): registra il listener,
 * attende il primo campione e si disiscrive, con un timeout di 5 secondi.
 */
abstract class SingleReadingSensor implements DeviceSensor {

	private static final long TIMEOUT_MILLIS = 5000;

	/** Il tipo di sensore Android da leggere (Sensor.TYPE_*). */
	abstract int getAndroidSensorType();

	/** Converte i valori del SensorEvent nel payload della campagna. */
	abstract Map<String, Object> toPayload(float[] values);

	@Override
	public boolean isAvailable(Context context) {
		SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		return sensorManager != null && sensorManager.getDefaultSensor(getAndroidSensorType()) != null;
	}

	@Override
	public CompletableFuture<Map<String, Object>> read(Context context) {
		SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		Sensor sensor = sensorManager != null ? sensorManager.getDefaultSensor(getAndroidSensorType()) : null;
		if (sensor == null)
			return CompletableFuture.completedFuture(null);
		CompletableFuture<Map<String, Object>> toReturn = new CompletableFuture<>();
		AtomicBoolean finished = new AtomicBoolean(false);
		SensorEventListener listener = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent event) {
				if (finished.compareAndSet(false, true)) {
					sensorManager.unregisterListener(this);
					toReturn.complete(toPayload(event.values));
				}
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
		sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
		new Handler(Looper.getMainLooper()).postDelayed(() -> {
			if (finished.compareAndSet(false, true)) {
				sensorManager.unregisterListener(listener);
				toReturn.complete(null);
			}
		}, TIMEOUT_MILLIS);
		return toReturn;
	}
}
