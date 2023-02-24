package com.example.crowdsensingwotandroidapp;

import android.content.Context;
import android.content.Intent;

public class ForegroundServiceLauncher {

	private static volatile ForegroundServiceLauncher foregroundServiceLauncher;

	private ForegroundServiceLauncher() {
		//Prevent form the reflection api.
		if (foregroundServiceLauncher != null) {
			throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
		}
	}

	public static ForegroundServiceLauncher getInstance() {
		if (foregroundServiceLauncher == null) {
			synchronized (ForegroundServiceLauncher.class) {
				if (foregroundServiceLauncher == null) {
					foregroundServiceLauncher = new ForegroundServiceLauncher();
				}
			}
		}
		return foregroundServiceLauncher;
	}

	public synchronized void startService(Context context) {
		if (!ServientService.IS_ACTIVITY_RUNNING) {
			context.startForegroundService(new Intent(context, ServientService.class));
		}
	}

	public synchronized void stopService(Context context) {
		context.stopService(new Intent(context, ServientService.class));
	}
}
