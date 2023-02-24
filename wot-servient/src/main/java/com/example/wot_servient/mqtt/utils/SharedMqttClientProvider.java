package com.example.wot_servient.mqtt.utils;

import android.content.Context;

import com.example.wot_servient.mqtt.client.MqttProtocolClient;
import com.example.wot_servient.mqtt.server.MqttProtocolServer;
import com.example.wot_servient.wot.utilities.Pair;
import com.example.wot_servient.wot.utilities.RefCountResource;
import com.typesafe.config.Config;

import org.eclipse.paho.android.service.MqttAndroidClient;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a Singleton class, which is used by {@link MqttProtocolClient} and {@link
 * MqttProtocolServer} to share a single MqttClient.
 */
public class SharedMqttClientProvider {

	private static final Map<Config, RefCountResource<Pair<MqttProtocolSettings, MqttAndroidClient>>> singletons = new HashMap<>();

	private SharedMqttClientProvider() {
		// singleton class
	}

	public static synchronized RefCountResource<Pair<MqttProtocolSettings, MqttAndroidClient>> singleton(Config config, Context context) {
		return singletons.computeIfAbsent(config, myConfig -> new RefCountResource<>(() -> {
			MqttProtocolSettings settings = new MqttProtocolSettings(myConfig);
			return new Pair<>(settings, settings.createConnectedMqttClient(context));
		}, pair -> pair.second().disconnect()));
	}
}
