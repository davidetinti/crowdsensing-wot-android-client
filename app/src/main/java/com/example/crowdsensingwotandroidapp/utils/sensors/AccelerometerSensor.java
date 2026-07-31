package com.example.crowdsensingwotandroidapp.utils.sensors;

import android.hardware.Sensor;
import android.hardware.SensorManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Accelerazione — tipo "motion".
 * Android riporta m/s²; il payload è in g per coerenza con l'app iOS
 * (CMAccelerometerData).
 */
public class AccelerometerSensor extends SingleReadingSensor {

	@Override
	public List<String> getCampaignTypes() {
		return Collections.singletonList("motion");
	}

	@Override
	int getAndroidSensorType() {
		return Sensor.TYPE_ACCELEROMETER;
	}

	@Override
	Map<String, Object> toPayload(float[] values) {
		if (values == null || values.length < 3)
			return null;
		Map<String, Object> payload = new HashMap<>();
		payload.put("x", values[0] / SensorManager.GRAVITY_EARTH);
		payload.put("y", values[1] / SensorManager.GRAVITY_EARTH);
		payload.put("z", values[2] / SensorManager.GRAVITY_EARTH);
		return payload;
	}
}
