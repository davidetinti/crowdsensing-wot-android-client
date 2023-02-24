package com.example.wot_servient.la_mqtt.lamqtt.common;

import android.content.Context;

public class AndroidBrokerConf extends BrokerConf {

	Context context;

	public AndroidBrokerConf(String username, String password, String url, int port, String id, Context context) {
		super(username, password, url, port, id);
		this.context = context;
	}
}