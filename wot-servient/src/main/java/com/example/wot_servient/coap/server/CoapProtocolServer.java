package com.example.wot_servient.coap.server;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.runAsync;

import android.content.Context;
import android.util.Log;

import com.example.wot_servient.coap.utils.ActionResource;
import com.example.wot_servient.coap.utils.AllPropertiesResource;
import com.example.wot_servient.coap.utils.EventResource;
import com.example.wot_servient.coap.utils.ObservePropertyResource;
import com.example.wot_servient.coap.utils.PropertyResource;
import com.example.wot_servient.coap.utils.ThingResource;
import com.example.wot_servient.wot.Servient;
import com.example.wot_servient.wot.binding.ProtocolServer;
import com.example.wot_servient.wot.binding.ProtocolServerException;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.ExposedThing;
import com.example.wot_servient.wot.thing.action.ExposedThingAction;
import com.example.wot_servient.wot.thing.event.ExposedThingEvent;
import com.example.wot_servient.wot.thing.form.Form;
import com.example.wot_servient.wot.thing.form.FormBuilder;
import com.example.wot_servient.wot.thing.form.Operation;
import com.example.wot_servient.wot.thing.property.ExposedThingProperty;
import com.typesafe.config.Config;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.Resource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Allows exposing Things via CoAP.
 */
public class CoapProtocolServer implements ProtocolServer {

	private static final String TAG = "CoapProtocolServer";
	private final String bindHost;
	private final int bindPort;
	private final List<String> addresses;
	private final Map<String, ExposedThing> things;
	private final Map<String, CoapResource> resources;
	private final Supplier<WotCoapServer> serverSupplier;
	private WotCoapServer server;
	private int actualPort;
	private List<String> actualAddresses;

	public CoapProtocolServer(Config config, Context context) {
		bindHost = "0.0.0.0";
		bindPort = config.getInt("wot.servient.coap.bind-port");
		if (!config.getStringList("wot.servient.coap.addresses").isEmpty()) {
			addresses = config.getStringList("wot.servient.coap.addresses");
		} else {
			addresses = Servient.getAddresses().stream().map(a -> "coap://" + a + ":" + bindPort)
					.collect(Collectors.toList());
		}
		things = new HashMap<>();
		resources = new HashMap<>();
		serverSupplier = () -> new WotCoapServer(this);
	}

	@SuppressWarnings({"java:S107"})
	CoapProtocolServer(String bindHost, int bindPort, List<String> addresses, Map<String, ExposedThing> things, Map<String, CoapResource> resources, Supplier<WotCoapServer> serverSupplier, WotCoapServer server, int actualPort, List<String> actualAddresses) {
		this.bindHost = bindHost;
		this.bindPort = bindPort;
		this.addresses = addresses;
		this.things = things;
		this.resources = resources;
		this.serverSupplier = serverSupplier;
		this.server = server;
		this.actualPort = actualPort;
		this.actualAddresses = actualAddresses;
	}

	@Override
	public String toString() {
		return "WotCoapServer";
	}

	@Override
	public CompletableFuture<Void> start(Servient servient) {
		Log.i(TAG, "Starting on " + bindHost + " port " + bindPort);
		if (server != null) {
			return completedFuture(null);
		}
		return runAsync(() -> {
			server = serverSupplier.get();
			server.start();
			actualPort = server.getPort();
			actualAddresses = addresses.stream()
					.map(a -> a.replace(":" + bindPort, ":" + actualPort))
					.collect(Collectors.toList());
		});
	}

	@Override
	public CompletableFuture<Void> stop() {
		Log.i(TAG, "Stopping on " + bindHost + " port " + actualPort);
		if (server == null) {
			return completedFuture(null);
		}
		return runAsync(() -> {
			server.stop();
			server.destroy();
			Log.d(TAG, "Server stopped");
		});
	}

	@Override
	public CompletableFuture<Void> expose(ExposedThing thing) {
		if (server == null) {
			// From Java 9
			//return failedFuture(new ProtocolServerException("Unable to expose thing before CoapServer has been started"));
			// Workaround before Java 9
			CompletableFuture<Void> toReturn = new CompletableFuture<>();
			toReturn.completeExceptionally(new ProtocolServerException("Unable to expose thing before CoapServer has been started"));
			return toReturn;
		}
		Log.i(TAG, "CoapServer on " + bindHost + " port " + actualPort + " exposes " + thing.getId() + " at coap://" + bindHost + ":" + actualPort + "/" + thing.getId());
		things.put(thing.getId(), thing);
		CoapResource thingResource = new ThingResource(thing);
		resources.put(thing.getId(), thingResource);
		Resource root = server.getRoot();
		if (root == null) {
			// From Java 9
			//return failedFuture();
			// Workaround before Java 9
			CompletableFuture<Void> toReturn = new CompletableFuture<>();
			toReturn.completeExceptionally(new Exception("Unable to expose thing before CoapServer has been started"));
			return toReturn;
		}
		root.add(thingResource);
		exposeProperties(thing, thingResource, actualAddresses, ContentManager.getOfferedMediaTypes());
		exposeActions(thing, thingResource, actualAddresses, ContentManager.getOfferedMediaTypes());
		exposeEvents(thing, thingResource, actualAddresses, ContentManager.getOfferedMediaTypes());
		return completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> destroy(ExposedThing thing) {
		// if the server is not running, nothing needs to be done
		if (server == null) {
			return completedFuture(null);
		}
		Log.i(TAG, "CoapServer on " + bindHost + " port " + actualPort + " stop exposing " + thing.getId() + " at coap://" + bindHost + ":" + actualPort + "/" + thing.getId());
		things.remove(thing.getId());
		CoapResource resource = resources.remove(thing.getId());
		if (resource != null) {
			server.getRoot().delete(resource);
		}
		return completedFuture(null);
	}

	@Override
	public URI getDirectoryUrl() {
		try {
			return new URI(actualAddresses.get(0));
		} catch (URISyntaxException e) {
			Log.w(TAG, "Unable to create directory url " + e);
			return null;
		}
	}

	@Override
	public String getTDIdentifier(String id) {
		try {
			return new URI(actualAddresses.get(0)).resolve("/" + id).toString();
		} catch (URISyntaxException e) {
			Log.w(TAG, "Unable to thing url " + e);
			return null;
		}
	}

	private void exposeProperties(ExposedThing thing, CoapResource thingResource, List<String> addresses, Set<String> contentTypes) {
		Map<String, ExposedThingProperty<Object>> properties = thing.getProperties();
		if (!properties.isEmpty()) {
			// make reporting of all properties optional?
			if (true) {
				addAllPropertiesForm(thing, thingResource, addresses, contentTypes);
			}
			CoapResource propertiesResource = new CoapResource("properties");
			thingResource.add(propertiesResource);
			properties.forEach((name, property) -> addPropertyForm(thing, addresses, contentTypes, propertiesResource, name, property));
		}
	}

	private void addPropertyForm(ExposedThing thing, List<String> addresses, Set<String> contentTypes, CoapResource propertiesResource, String name, ExposedThingProperty<Object> property) {
		for (String address : addresses) {
			for (String contentType : contentTypes) {
				String href = address + "/" + thing.getId() + "/properties/" + name;
				FormBuilder form = new FormBuilder().setHref(href).setContentType(contentType);
				if (property.isReadOnly()) {
					form.setOp(Operation.READ_PROPERTY);
				} else if (property.isWriteOnly()) {
					form.setOp(Operation.WRITE_PROPERTY);
				} else {
					form.setOp(Operation.READ_PROPERTY, Operation.WRITE_PROPERTY);
				}
				property.addForm(form.build());
				Log.d(TAG, "Assign " + href + " to Property " + name);
				// if property is observable add an additional form with a observable href
				if (property.isObservable()) {
					String observableHref = href + "/observable";
					Form observableForm = new FormBuilder().setHref(observableHref)
							.setContentType(contentType).setOp(Operation.OBSERVE_PROPERTY)
							.setSubprotocol("longpoll").build();
					property.addForm(observableForm);
					Log.d(TAG, "Assign " + observableHref + " to observe Property " + name);
				}
			}
		}
		PropertyResource propertyResource = new PropertyResource(name, property);
		propertiesResource.add(propertyResource);
		if (property.isObservable()) {
			propertyResource.add(new ObservePropertyResource(name, property));
		}
	}

	private void addAllPropertiesForm(ExposedThing thing, CoapResource thingResource, List<String> addresses, Set<String> contentTypes) {
		CoapResource allResource = new CoapResource("all");
		thingResource.add(allResource);
		for (String address : addresses) {
			for (String contentType : contentTypes) {
				String href = address + "/" + thing.getId() + "/all/properties";
				Form form = new FormBuilder().setHref(href).setContentType(contentType)
						.setOp(Operation.READ_ALL_PROPERTIES, Operation.READ_MULTIPLE_PROPERTIES/*, Operation.writeallproperties, Operation.writemultipleproperties*/)
						.build();
				thing.addForm(form);
				Log.d(TAG, "Assign " + href + " for reading all properties");
			}
		}
		allResource.add(new AllPropertiesResource(thing));
	}

	private void exposeActions(ExposedThing thing, CoapResource thingResource, List<String> addresses, Set<String> contentTypes) {
		Map<String, ExposedThingAction<Object, Object>> actions = thing.getActions();
		if (!actions.isEmpty()) {
			CoapResource actionsResource = new CoapResource("actions");
			thingResource.add(actionsResource);
			actions.forEach((name, action) -> {
				for (String address : addresses) {
					for (String contentType : contentTypes) {
						String href = address + "/" + thing.getId() + "/actions/" + name;
						Form form = new FormBuilder().setHref(href).setOp(Operation.INVOKE_ACTION)
								.setContentType(contentType).build();
						action.addForm(form);
						Log.d(TAG, "Assign " + href + " to Action " + name);
					}
				}
				actionsResource.add(new ActionResource(name, action));
			});
		}
	}

	private void exposeEvents(ExposedThing thing, CoapResource thingResource, List<String> addresses, Set<String> contentTypes) {
		Map<String, ExposedThingEvent<Object>> events = thing.getEvents();
		if (!events.isEmpty()) {
			CoapResource eventsResource = new CoapResource("events");
			thingResource.add(eventsResource);
			events.forEach((name, event) -> {
				for (String address : addresses) {
					for (String contentType : contentTypes) {
						String href = address + "/" + thing.getId() + "/events/" + name;
						Form form = new FormBuilder().setHref(href).setOp(Operation.SUBSCRIBE_EVENT)
								.setContentType(contentType).build();
						event.addForm(form);
						Log.d(TAG, "Assign " + href + " to Event " + name);
					}
				}
				eventsResource.add(new EventResource(name, event));
			});
		}
	}

	public int getBindPort() {
		return bindPort;
	}

	public Map<String, ExposedThing> getThings() {
		return things;
	}
}
