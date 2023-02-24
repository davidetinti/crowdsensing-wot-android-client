package com.example.crowdsensingwotandroidapp.utils.network;

import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class NetworkStateManager {

	private static final MutableLiveData<Boolean> activeNetworkStatusMLD = new MutableLiveData<>();
	private static NetworkStateManager INSTANCE;

	private NetworkStateManager() {}

	public static synchronized NetworkStateManager getInstance() {
		if (INSTANCE == null) {
			Log.d("NetworkStateManager", "getInstance() called: Creating new instance");
			INSTANCE = new NetworkStateManager();
		}
		return INSTANCE;
	}

	/**
	 * Returns the current network status
	 */
	public LiveData<Boolean> getNetworkConnectivityStatus() {
		Log.d("NetworkStateManager", "getNetworkConnectivityStatus() called");
		return activeNetworkStatusMLD;
	}

	/**
	 * Updates the active network status live-data
	 */
	public void setNetworkConnectivityStatus(boolean connectivityStatus) {
		Log.d("NetworkStateManager", "setNetworkConnectivityStatus() called with: connectivityStatus = [" + connectivityStatus + "]");
		if (Looper.myLooper() == Looper.getMainLooper()) {
			activeNetworkStatusMLD.setValue(connectivityStatus);
		} else {
			activeNetworkStatusMLD.postValue(connectivityStatus);
		}
	}
}