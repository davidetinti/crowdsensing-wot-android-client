package com.example.crowdsensingwotandroidapp.utils.sensors;

import android.hardware.Sensor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pressione atmosferica dal barometro — tipo "pressure".
 * Android riporta hPa; il payload è in kPa per coerenza con l'app iOS
 * (CMAltitudeData.pressure).
 */
public class BarometerSensor extends SingleReadingSensor {

	@Override
	public List<String> getCampaignTypes() {
		return Collections.singletonList("pressure");
	}

	@Override
	int getAndroidSensorType() {
		return Sensor.TYPE_PRESSURE;
	}

	@Override
	Map<String, Object> toPayload(float[] values) {
		if (values == null || values.length == 0)
			return null;
		Map<String, Object> payload = new HashMap<>();
		payload.put("pressure", values[0] / 10.0);
		return payload;
	}
}
