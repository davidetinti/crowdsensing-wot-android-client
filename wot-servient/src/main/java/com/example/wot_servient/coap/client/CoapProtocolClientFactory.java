package com.example.wot_servient.coap.client;

import android.content.Context;

import com.example.wot_servient.wot.binding.ProtocolClientFactory;
import com.typesafe.config.Config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Creates new {@link CoapProtocolClient} instances.
 */
public class CoapProtocolClientFactory implements ProtocolClientFactory {

	private final ExecutorService executor;
	private final ScheduledThreadPoolExecutor secondaryExecutor;

	public CoapProtocolClientFactory(Config config, Context context) {
		executor = Executors.newFixedThreadPool(10);
		secondaryExecutor = new ScheduledThreadPoolExecutor(1);
	}

	@Override
	public String toString() {
		return "CoapClient";
	}

	@Override
	public String getScheme() {
		return "coap";
	}

	@Override
	public CoapProtocolClient getClient() {
		return new CoapProtocolClient(executor, secondaryExecutor);
	}
}
