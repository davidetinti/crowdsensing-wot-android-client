package com.example.wot_servient.wot;

import android.content.Context;

import com.example.wot_servient.wot.binding.ProtocolServerException;
import com.example.wot_servient.wot.thing.ConsumedThing;
import com.example.wot_servient.wot.thing.ExposedThing;
import com.example.wot_servient.wot.thing.Thing;
import com.example.wot_servient.wot.thing.filter.DiscoveryMethod;
import com.example.wot_servient.wot.thing.filter.ThingFilter;
import com.typesafe.config.Config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import io.reactivex.rxjava3.core.Observable;

/**
 * Standard implementation of {@link Wot}.
 */
public class DefaultWot implements Wot {

	private static final String TAG = "DefaultWot";
	protected final Servient servient;

	/**
	 * Creates a new DefaultWot instance for given <code>servient</code>.
	 *
	 * @param servient
	 */
	public DefaultWot(Servient servient) {
		this.servient = servient;
	}

	/**
	 * Creates and starts a {@link Servient} with the given <code>config</code>.
	 *
	 * @param config
	 */
	public DefaultWot(Config config, Context context) throws WotException {
		servient = new Servient(config, context);
		servient.start().join();
	}

	/**
	 * Creates and starts a {@link Servient} with the given <code>config</code>. The servient will
	 * not start any servers and can therefore only consume things and not expose any things.
	 *
	 * @param config
	 */
	public static Wot clientOnly(Config config, Context context) throws WotException {
		Servient servient = Servient.clientOnly(config, context);
		servient.start().join();
		return new DefaultWot(servient);
	}

	/**
	 * Creates and starts a {@link Servient} with the given <code>config</code>. The servient will
	 * not start any clients and can therefore only produce and expose things.
	 *
	 * @param config
	 */
	public static Wot serverOnly(Config config, Context context) throws WotException {
		Servient servient = Servient.serverOnly(config, context);
		servient.start().join();
		return new DefaultWot(servient);
	}

	@Override
	public String toString() {
		return "DefaultWot{" + "servient=" + servient + '}';
	}

	@Override
	public CompletableFuture<Void> unregister(String directory, ExposedThing thing) throws URISyntaxException {
		return servient.unregister(directory, thing);
	}

	@Override
	public CompletableFuture<Void> register(String directory, ExposedThing thing) throws URISyntaxException {
		return servient.register(directory, thing);
	}

	@Override
	public Observable<Thing> discover(ThingFilter filter) throws WotException {
		return servient.discover(filter);
	}

	@Override
	public Observable<Thing> discover() throws WotException {
		return discover(new ThingFilter(DiscoveryMethod.ANY));
	}

	@Override
	public ExposedThing produce(Thing thing) throws WotException {
		ExposedThing exposedThing = new ExposedThing(servient, thing);
		if (servient.addThing(exposedThing)) {
			return exposedThing;
		} else {
			throw new WotException("Thing already exists: " + thing.getId());
		}
	}

	@Override
	public ConsumedThing consume(Thing thing) {
		return new ConsumedThing(servient, thing);
	}

	@Override
	public ConsumedThing consume(String thing) {
		return consume(Thing.fromJson(thing));
	}

	@Override
	public CompletableFuture<Thing> fetch(URI url) {
		return servient.fetch(url);
	}

	@Override
	public CompletableFuture<Thing> fetch(String url) throws URISyntaxException {
		return servient.fetch(url);
	}

	@Override
	public CompletableFuture<Void> destroy() {
		return servient.shutdown();
	}

	@Override
	public CompletableFuture<ExposedThing> destroyExposedThingById(String exposedThingId) {
		return servient.destroy(exposedThingId);
	}

	@Override
	public Integer getNumberOfActiveProtocolsServer() {
		return servient.getNumberOfActiveProtocolsServer();
	}

	@Override
	public ExposedThing getExistingExposedThing(String exposedThingId) {
		return servient.getExposedThing(exposedThingId);
	}

	@Override
	public ArrayList<String> getTDIdentifiersForExposedThing(String exposedThingId) throws ProtocolServerException {
		return servient.getTDIdentifiersForExposedThing(exposedThingId);
	}
}
