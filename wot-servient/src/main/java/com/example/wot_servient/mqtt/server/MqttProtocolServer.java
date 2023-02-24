package com.example.wot_servient.mqtt.server;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.runAsync;

import android.content.Context;
import android.util.Log;

import com.example.wot_servient.mqtt.utils.MqttProtocolSettings;
import com.example.wot_servient.mqtt.utils.SharedMqttClientProvider;
import com.example.wot_servient.wot.Servient;
import com.example.wot_servient.wot.ServientDiscoveryIgnore;
import com.example.wot_servient.wot.binding.ProtocolServer;
import com.example.wot_servient.wot.binding.ProtocolServerException;
import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.ExposedThing;
import com.example.wot_servient.wot.thing.action.ExposedThingAction;
import com.example.wot_servient.wot.thing.event.ExposedThingEvent;
import com.example.wot_servient.wot.thing.form.Form;
import com.example.wot_servient.wot.thing.form.FormBuilder;
import com.example.wot_servient.wot.thing.form.Operation;
import com.example.wot_servient.wot.thing.property.ExposedThingProperty;
import com.example.wot_servient.wot.utilities.Pair;
import com.example.wot_servient.wot.utilities.RefCountResource;
import com.example.wot_servient.wot.utilities.RefCountResourceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.typesafe.config.Config;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Allows exposing Things via MQTT.
 */
@ServientDiscoveryIgnore
public class MqttProtocolServer implements ProtocolServer {

	private static final String TAG = "MqttProtocolServer";
	private final Map<String, ExposedThing> things = new HashMap<>();
	private final Multimap<String, Disposable> subscriptions;
	private final RefCountResource<Pair<MqttProtocolSettings, MqttAndroidClient>> mqttClientProvider;
	private Pair<MqttProtocolSettings, MqttAndroidClient> mqttClientPair;

	MqttProtocolServer(RefCountResource<Pair<MqttProtocolSettings, MqttAndroidClient>> mqttClientProvider, Multimap<String, Disposable> subscriptions, Pair<MqttProtocolSettings, MqttAndroidClient> mqttClientPair) {
		this.mqttClientProvider = mqttClientProvider;
		this.subscriptions = subscriptions;
		this.mqttClientPair = mqttClientPair;
	}

	public MqttProtocolServer(Config config, Context context) {
		this(SharedMqttClientProvider.singleton(config, context), HashMultimap.create(), null);
	}

	@Override
	public CompletableFuture<Void> start(Servient servient) {
		Log.i(TAG, "Start MqttServer");
		if (mqttClientPair != null) {
			return completedFuture(null);
		}
		return runAsync(() -> {
			try {
				mqttClientPair = mqttClientProvider.retain();
			} catch (RefCountResourceException e) {
				throw new CompletionException(e);
			}
		});
	}

	@Override
	public CompletableFuture<Void> stop() {
		Log.i(TAG, "Stop MqttServer");
		if (mqttClientPair != null) {
			// Rimuovi tutte le ExposedThings e poi rimuovi il client
			@SuppressWarnings("unchecked") CompletableFuture<Void>[] destroyFutures = new ArrayList<>(things.values()).stream()
					.map(this::destroy).toArray(CompletableFuture[]::new);
			return CompletableFuture.allOf(destroyFutures).thenRunAsync(() -> {
				try {
					mqttClientPair = null;
					mqttClientProvider.release();
				} catch (RefCountResourceException e) {
					throw new CompletionException(e);
				}
			});
		} else {
			return completedFuture(null);
		}
	}

	@Override
	public CompletableFuture<Void> expose(ExposedThing thing) {
		String baseUrl = createUrl();
		Log.i(TAG, "MqttServer exposes " + thing.getId() + " at " + baseUrl + thing.getId() + "/*'");
		if (mqttClientPair == null) {
			// From Java 9
			// return failedFuture(new ProtocolServerException("Unable to expose thing before MqttServer has been started"));
			// Workaround before Java 9
			CompletableFuture<Void> toReturn = new CompletableFuture<>();
			toReturn.completeExceptionally(new ProtocolServerException("Unable to expose thing before MqttServer has been started"));
			return toReturn;
		}
		things.put(thing.getId(), thing);
		exposeProperties(thing, baseUrl);
		exposeActions(thing, baseUrl);
		exposeEvents(thing, baseUrl);
		listenOnMqttMessages();
		return completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> destroy(ExposedThing thing) {
		// if the server is not running, nothing needs to be done
		if (mqttClientPair == null) {
			return completedFuture(null);
		}
		Log.i(TAG, "MqttServer stop exposing " + thing.getId() + " as unique '/" + thing.getId() + "/*'");
		unexposeTD(thing);
		// dispose all created subscriptions
		Collection<Disposable> thingSubscriptions = subscriptions.removeAll(thing.getId());
		for (Disposable subscription : thingSubscriptions) {
			subscription.dispose();
		}
		things.remove(thing.getId());
		return completedFuture(null);
	}

	@Override
	public String getTDIdentifier(String id) throws ProtocolServerException {
		return id;
	}

	private String createUrl() {
		String base = "mqtt" + mqttClientPair.first().getBroker()
				.substring(mqttClientPair.first().getBroker().indexOf("://"));
		if (!base.endsWith("/")) {
			base = base + "/";
		}
		return base;
	}

	private void exposeProperties(ExposedThing thing, String baseUrl) {
		Map<String, ExposedThingProperty<Object>> properties = thing.getProperties();
		properties.forEach((name, property) -> {
			String rootTopic = thing.getId() + "/properties/" + name;
			String href = baseUrl + rootTopic;
			if (!property.isWriteOnly()) {
				String askTopic = rootTopic + "/readAsk";
				String askHref = href + "/readAsk";
				String answerTopic = rootTopic + "/readAnswer";
				try (MqttAndroidClient client = mqttClientPair.second()) {
					client.subscribe(askTopic, 2, ((topic, message) -> {
						Log.d(TAG, "Received Message with topic " + topic + ": " + message);
						byte[] data = new ObjectMapper().writeValueAsBytes(property.read().get());
						client.publish(answerTopic, new MqttMessage(data));
					}));
				} catch (MqttException e) {
					Log.w(TAG, "Exception occurred while trying to subscribe to broker " + mqttClientPair.first()
							.getBroker() + " and topic " + askTopic + ": " + e.getMessage());
				}
				FormBuilder readForm = new FormBuilder();
				readForm.setHref(askHref).setContentType(ContentManager.DEFAULT);
				readForm.setOp(Operation.READ_PROPERTY);
				readForm.setOptional("mqv:answerTopic", answerTopic);
				property.addForm(readForm.build());
				Log.d(TAG, "Assign " + href + " to Property " + name);
			}
			if (!property.isReadOnly()) {
				String writeTopic = rootTopic + "/writeProperty";
				String writeHref = href + "/writeProperty";
				try (MqttAndroidClient client = mqttClientPair.second()) {
					client.subscribe(writeTopic, 2, ((topic, message) -> {
						Log.d(TAG, "Received Message with topic " + topic + ": " + message);
						Content content = new Content(message.getPayload());
						Object input = ContentManager.contentToValue(content, property);
						property.write(input).get();
					}));
				} catch (MqttException e) {
					Log.w(TAG, "Exception occurred while trying to subscribe to broker " + mqttClientPair.first()
							.getBroker() + " and topic " + writeTopic + ": " + e.getMessage());
				}
				FormBuilder writeForm = new FormBuilder();
				writeForm.setHref(writeHref).setContentType(ContentManager.DEFAULT);
				writeForm.setOp(Operation.WRITE_PROPERTY);
				property.addForm(writeForm.build());
				Log.d(TAG, "Assign " + writeHref + " to Property " + name);
			}
			if (property.isObservable()) {
				// TODO: Need to figure why its here
				Disposable subscription = property.observer()
						.map(optional -> ContentManager.valueToContent(optional.orElse(null)))
						.map(content -> new MqttMessage(content.getBody()))
						.subscribe(mqttMessage -> {
							try (MqttAndroidClient client = mqttClientPair.second()) {
								client.publish(rootTopic, mqttMessage);
							}
						}, e -> Log.w(TAG, "MqttServer cannot publish data for topic " + rootTopic + ": " + e.getMessage()), () -> {});
				subscriptions.put(thing.getId(), subscription);
				FormBuilder observeForm = new FormBuilder().setHref(href)
						.setContentType(ContentManager.DEFAULT)
						.setOp(Operation.OBSERVE_PROPERTY, Operation.UNOBSERVE_PROPERTY);
				property.addForm(observeForm.build());
				Log.d(TAG, "Assign " + href + " to Property " + name);
			}
		});
	}

	private void exposeActions(ExposedThing thing, String baseUrl) {
		Map<String, ExposedThingAction<Object, Object>> actions = thing.getActions();
		actions.forEach((name, action) -> {
			String rootTopic = thing.getId() + "/actions/" + name;
			String href = baseUrl + rootTopic;
			try (MqttAndroidClient client = mqttClientPair.second()) {
				client.subscribe(rootTopic, 2, (topic, message) -> {
					Log.d(TAG, "Received Message with topic " + topic + ": " + message);
					Content content = new Content(message.getPayload());
					Object input = ContentManager.contentToValue(content, action.getInput());
					action.invoke(input);
				});
			} catch (MqttException e) {
				Log.w(TAG, "Exception occurred while trying to subscribe to broker " + mqttClientPair.first()
						.getBroker() + " and topic " + rootTopic + ": " + e.getMessage());
			}
			FormBuilder form = new FormBuilder();
			form.setHref(href).setContentType(ContentManager.DEFAULT);
			form.setOp(Operation.INVOKE_ACTION);
			action.addForm(form.build());
			Log.d(TAG, "Assign " + href + " to Action " + name);
		});
	}

	private void exposeEvents(ExposedThing thing, String baseUrl) {
		Map<String, ExposedThingEvent<Object>> events = thing.getEvents();
		events.forEach((name, event) -> {
			String topic = thing.getId() + "/events/" + name;
			Disposable subscription = event.observer()
					.map(optional -> ContentManager.valueToContent(optional.orElse(null)))
					.map(content -> new MqttMessage(content.getBody())).subscribe(mqttMessage -> {
						try (MqttAndroidClient client = mqttClientPair.second()) {
							client.publish(topic, mqttMessage);
						}
					}, e -> Log.w(TAG, "MqttServer cannot publish data for topic " + topic + ": " + e.getMessage()), () -> {
					});
			subscriptions.put(thing.getId(), subscription);
			String href = baseUrl + topic;
			Form form = new FormBuilder().setHref(href).setContentType(ContentManager.DEFAULT)
					.setOp(Operation.SUBSCRIBE_EVENT, Operation.UNSUBSCRIBE_EVENT)
					.setOptional("mqtt:qos", 0).setOptional("mqtt:retain", false).build();
			event.addForm(form);
			Log.d(TAG, "Assign " + href + " to Event " + name);
		});
	}

	/**
	 * To "delete" a retained message from the broker, we need to publish an empty message under the
	 * same topic.
	 */
	private void unexposeTD(ExposedThing thing) {
		String topic = thing.getId();
		Log.d(TAG, "Remove published " + thing.getId() + " Thing Description at topic " + topic);
		try (MqttAndroidClient client = mqttClientPair.second()) {
			MqttMessage mqttMessage = new MqttMessage();
			mqttMessage.setRetained(true);
			client.publish(topic, mqttMessage);
		} catch (MqttException e) {
			Log.w(TAG, "Unable to remove published thing description at topic " + topic + ": " + e.getMessage());
		}
	}

	private void listenOnMqttMessages() {
		// connect incoming messages to Thing
		try (MqttAndroidClient client = mqttClientPair.second()) {
			client.setCallback(new MqttCallback() {
				@Override
				public void connectionLost(Throwable cause) {
					Log.i(TAG, "MqttServer lost connection to broker");
				}

				@Override
				public void messageArrived(String topic, MqttMessage message) {
					Log.i(TAG, "MqttServer received message for " + topic);
				}

				@Override
				public void deliveryComplete(IMqttDeliveryToken token) {
					// do nothing
				}
			});
		}
	}
}
