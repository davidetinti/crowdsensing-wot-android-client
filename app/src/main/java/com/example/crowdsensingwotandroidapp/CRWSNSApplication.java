package com.example.crowdsensingwotandroidapp;

import android.app.Application;

import com.example.crowdsensingwotandroidapp.utils.network.NetworkMonitoringUtil;

public class CRWSNSApplication extends Application {

	public NetworkMonitoringUtil mNetworkMonitoringUtil;

	@Override
	public void onCreate() {
		super.onCreate();
		mNetworkMonitoringUtil = new NetworkMonitoringUtil(getApplicationContext());
		mNetworkMonitoringUtil.checkNetworkState();
		mNetworkMonitoringUtil.registerNetworkCallbackEvents();
	}
}
