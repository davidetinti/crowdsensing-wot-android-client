package com.example.wot_servient.la_mqtt.lamqtt.common;

import android.content.Context;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class AndroidMQTTClient extends MQTTClient {

	private static final String DEFAULT_CLIENT_NAME = "tester";
	protected AndroidBrokerConf brokerConf;
	protected BrokerConnector mconnector;
	protected ArrayList<MQTTReceiver> mcallback;
	protected String clientId;

	public AndroidMQTTClient(String username, String password, String host, int port, String id, Context context) {
		super(username, password, host, port, id);
		if (id == null) {
			this.clientId = AndroidMQTTClient.DEFAULT_CLIENT_NAME;
		} else {
			this.clientId = id;
			this.brokerConf = new AndroidBrokerConf(username, password, host, port, this.clientId, context);
		}
		this.mconnector = new AndroidPahoConnector();
		this.mcallback = new ArrayList<>();
	}

	public void setCallback(MQTTReceiver mcallback) {
		this.mcallback.add(mcallback);
	}

	public boolean connect() throws ExecutionException, InterruptedException {
		return this.mconnector.connect(this.brokerConf).get();
	}

	public void publish(String topic, String message) throws ExecutionException, InterruptedException {
		this.mconnector.publish(topic, message).get();
	}

	public void subscribe(String topic) throws ExecutionException, InterruptedException {
		this.mconnector.subscribe(topic, this).get();
	}

	public void disconnect() throws ExecutionException, InterruptedException {
		this.mconnector.disconnect().get();
	}

	public void msgRecv(MQTTMessage msg) throws JSONException {
		for (MQTTReceiver mqttReceiver : this.mcallback) {
			mqttReceiver.messageRecv(msg);
		}
	}
}
