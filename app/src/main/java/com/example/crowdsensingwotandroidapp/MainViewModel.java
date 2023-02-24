package com.example.crowdsensingwotandroidapp;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MainViewModel extends ViewModel {

	// Error to be showed in a snack-bar
	private static final MutableLiveData<Throwable> errorToShow = new MutableLiveData<>(null);
	// Token used for user identification
	private static final MutableLiveData<String> bearerToken = new MutableLiveData<>(null);
	// Counter of loading request active
	private static final MutableLiveData<Integer> loadingCounter = new MutableLiveData<>(0);

	public static MutableLiveData<Integer> getLoadingCounter() {
		return loadingCounter;
	}

	public static MutableLiveData<String> getBearerToken() {
		return bearerToken;
	}

	public static MutableLiveData<Throwable> getErrorToShow() {
		return errorToShow;
	}

	public static void addLoading() {
		Log.d("LOADING", "ADDED");
		Integer loadingCounterValue = loadingCounter.getValue();
		if (loadingCounterValue != null)
			loadingCounter.setValue(loadingCounterValue + 1);
	}

	public static void removeLoading() {
		Log.d("LOADING", "REMOVED");
		Integer loadingCounterValue = loadingCounter.getValue();
		if (loadingCounterValue != null)
			loadingCounter.setValue(loadingCounterValue - 1);
	}
}
