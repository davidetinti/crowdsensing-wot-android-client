package com.example.wot_servient.la_mqtt.client;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.runAsync;

import android.content.Context;
import android.util.Log;

import com.example.wot_servient.mqtt.client.MqttProtocolClient;
import com.example.wot_servient.mqtt.utils.MqttProtocolSettings;
import com.example.wot_servient.mqtt.utils.SharedMqttClientProvider;
import com.example.wot_servient.wot.ServientDiscoveryIgnore;
import com.example.wot_servient.wot.binding.ProtocolClientFactory;
import com.example.wot_servient.wot.utilities.Pair;
import com.example.wot_servient.wot.utilities.RefCountResource;
import com.example.wot_servient.wot.utilities.RefCountResourceException;
import com.typesafe.config.Config;

import org.eclipse.paho.android.service.MqttAndroidClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Creates new {@link MqttProtocolClient} instances.
 */
@ServientDiscoveryIgnore
public class LAMqttProtocolClientFactory implements ProtocolClientFactory {

	private static final String TAG = "MqttProtocolClientFacto";
	private final RefCountResource<Pair<MqttProtocolSettings, MqttAndroidClient>> mqttClientPairProvider;
	private Pair<MqttProtocolSettings, MqttAndroidClient> mqttClientPair;

	public LAMqttProtocolClientFactory(Config config, Context context) {
		mqttClientPairProvider = SharedMqttClientProvider.singleton(config, context);
	}

	@Override
	public String getScheme() {
		return "mqtt";
	}

	@Override
	public MqttProtocolClient getClient() {
		return new MqttProtocolClient(mqttClientPair);
	}

	@Override
	public CompletableFuture<Void> init() {
		Log.d(TAG, "Init MqttClient");
		if (mqttClientPair == null) {
			return runAsync(() -> {
				try {
					mqttClientPair = mqttClientPairProvider.retain();
				} catch (RefCountResourceException e) {
					throw new CompletionException(e);
				}
			});
		} else {
			return completedFuture(null);
		}
	}

	@Override
	public CompletableFuture<Void> destroy() {
		Log.d(TAG, "Disconnect MqttClient");
		if (mqttClientPair != null) {
			return runAsync(() -> {
				try {
					mqttClientPairProvider.release();
				} catch (RefCountResourceException e) {
					throw new CompletionException(e);
				}
			});
		} else {
			return completedFuture(null);
		}
	}
}
