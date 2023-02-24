package com.example.crowdsensingwotandroidapp.utils.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

public class NetworkMonitoringUtil extends ConnectivityManager.NetworkCallback {

	private final NetworkRequest mNetworkRequest;
	private final ConnectivityManager mConnectivityManager;
	private final NetworkStateManager mNetworkStateManager;

	// Constructor
	public NetworkMonitoringUtil(Context context) {
		mNetworkRequest = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
				.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).build();
		mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		mNetworkStateManager = NetworkStateManager.getInstance();
	}

	/**
	 * Check current Network state
	 */
	public void checkNetworkState() {
		try {
			NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
			// Set the initial value for the live-data
			mNetworkStateManager.setNetworkConnectivityStatus(networkInfo != null && networkInfo.isConnected());
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	@Override
	public void onAvailable(@NonNull Network network) {
		super.onAvailable(network);
		// Setting Live-Data to 'true'
		mNetworkStateManager.setNetworkConnectivityStatus(true);
	}

	@Override
	public void onLost(@NonNull Network network) {
		super.onLost(network);
		// Setting Live-Data to 'false'
		mNetworkStateManager.setNetworkConnectivityStatus(false);
	}

	/**
	 * Registers the Network-Request callback
	 * (Note: Register only once to prevent duplicate callbacks)
	 */
	public void registerNetworkCallbackEvents() {
		mConnectivityManager.registerNetworkCallback(mNetworkRequest, this);
	}
}
