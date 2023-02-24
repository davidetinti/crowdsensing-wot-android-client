package com.example.wot_servient.wot.binding;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletableFuture;

/**
 * A ProtocolClientFactory is responsible for creating new {@link ProtocolClient} instances. There
 * is a separate client instance for each {@link com.example.wot_servient.wot.thing.ConsumedThing}.
 */
public interface ProtocolClientFactory {

	String getScheme();

	ProtocolClient getClient() throws ProtocolClientException;

	/**
	 * Is called on servient start.
	 *
	 * @return
	 */
	default CompletableFuture<Void> init() {
		return completedFuture(null);
	}

	/**
	 * Is called on servient shutdown.
	 *
	 * @return
	 */
	default CompletableFuture<Void> destroy() {
		return completedFuture(null);
	}
}
