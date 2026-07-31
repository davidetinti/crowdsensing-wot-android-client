package com.example.crowdsensingwotandroidapp.utils.sensors;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Livello di batteria (BatteryManager) — tipo "battery".
 */
public class BatterySensor implements DeviceSensor {

	@Override
	public List<String> getCampaignTypes() {
		return Collections.singletonList("battery");
	}

	@Override
	public boolean isAvailable(Context context) {
		return true;
	}

	@Override
	public CompletableFuture<Map<String, Object>> read(Context context) {
		BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
		if (batteryManager == null)
			return CompletableFuture.completedFuture(null);
		int level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
		if (level < 0)
			return CompletableFuture.completedFuture(null);
		Map<String, Object> payload = new HashMap<>();
		payload.put("level", level);
		payload.put("state", batteryState(context));
		return CompletableFuture.completedFuture(payload);
	}

	private String batteryState(Context context) {
		Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		if (intent == null)
			return "unknown";
		switch (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
			case BatteryManager.BATTERY_STATUS_CHARGING:
				return "charging";
			case BatteryManager.BATTERY_STATUS_FULL:
				return "full";
			case BatteryManager.BATTERY_STATUS_DISCHARGING:
			case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
				return "unplugged";
			default:
				return "unknown";
		}
	}
}
