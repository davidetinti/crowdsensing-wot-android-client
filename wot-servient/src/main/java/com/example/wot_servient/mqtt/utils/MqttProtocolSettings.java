package com.example.wot_servient.mqtt.utils;

import android.content.Context;
import android.util.Log;

import com.typesafe.config.Config;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttProtocolSettings {

	private static final String TAG = "MqttProtocolSettings";
	private final String broker;
	private final String clientId;
	private final String username;
	private final String password;

	public MqttProtocolSettings(Config config) {
		this(getItemFromConfig("wot.servient.mqtt.broker", config), getItemFromConfig("wot.servient.mqtt.client-id", config), getItemFromConfig("wot.servient.mqtt.username", config), getItemFromConfig("wot.servient.mqtt.password", config));
	}

	MqttProtocolSettings(String broker, String clientId, String username, String password) {
		this.broker = broker;
		this.clientId = clientId;
		this.username = username;
		this.password = password;
	}

	private static String getItemFromConfig(String configPath, Config config) {
		if (config.hasPath(configPath)) {
			String configItem = config.getString(configPath);
			if (!configItem.equals("")) {
				return configItem;
			}
		}
		if (configPath.equals("wot.servient.mqtt.client-id")) {
			return "wot" + System.nanoTime();
		} else {
			return null;
		}
	}

	public String getBroker() {
		return broker;
	}

	public String getClientId() {
		return clientId;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public MqttAndroidClient createConnectedMqttClient(Context context) throws MqttProtocolException {
		if (getBroker() == null || getBroker().isEmpty()) {
			throw new MqttProtocolException("No broker defined for MQTT server binding - skipping");
		}
		try (MqttClientPersistence persistence = new MemoryPersistence()) {
			MqttAndroidClient client = new MqttAndroidClient(context, getBroker(), getClientId(), persistence);
			MqttConnectOptions options = new MqttConnectOptions();
			options.setCleanSession(true);
			if (getUsername() != null) {
				options.setUserName(getUsername());
			}
			if (getPassword() != null) {
				options.setPassword(getPassword().toCharArray());
			}
			Log.i(TAG, "MqttClient trying to connect to broker at " + getBroker() + " with client ID " + getClientId());
			client.connect(options, null, new IMqttActionListener() {
				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					Log.i(TAG, "MqttClient connected to broker at " + getBroker());
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					Log.e(TAG, "MqttClient failed to connect to broker at " + getBroker());
				}
			});
			return client;
		} catch (MqttException e) {
			throw new MqttProtocolException(e);
		}
	}
}
