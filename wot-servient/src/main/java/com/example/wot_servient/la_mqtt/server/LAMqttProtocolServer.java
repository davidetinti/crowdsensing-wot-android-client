package com.example.wot_servient.la_mqtt.server;

import static java.util.concurrent.CompletableFuture.completedFuture;

import android.content.Context;
import android.util.Log;

import com.example.wot_servient.la_mqtt.lamqtt.client.SpatialMQTTClient;
import com.example.wot_servient.la_mqtt.lamqtt.common.MQTTReceiver;
import com.example.wot_servient.la_mqtt.utils.LAMqttBrokerServerConfig;
import com.example.wot_servient.la_mqtt.utils.LAMqttLocationProvider;
import com.example.wot_servient.la_mqtt.utils.Location;
import com.example.wot_servient.wot.Servient;
import com.example.wot_servient.wot.ServientDiscoveryIgnore;
import com.example.wot_servient.wot.binding.ProtocolServer;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.ExposedThing;
import com.example.wot_servient.wot.thing.action.ExposedThingAction;
import com.example.wot_servient.wot.thing.event.ExposedThingEvent;
import com.example.wot_servient.wot.thing.form.FormBuilder;
import com.example.wot_servient.wot.thing.form.Operation;
import com.example.wot_servient.wot.thing.property.ExposedThingProperty;
import com.typesafe.config.Config;

import org.json.JSONException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Allows exposing Things via MQTT.
 */
@ServientDiscoveryIgnore
public class LAMqttProtocolServer implements ProtocolServer {

	private static final String TAG = "LAMqttProtocolServer";
	private final Map<String, ExposedThing> things = new HashMap<>();
	private Timer locationUpdateHandler;
	private TimerTask locationUpdate;
	private Integer port = -1;
	private final String scheme = "mqtt";
	private String address;
	private final String brokerURI;
	private LAMqttLocationProvider locationProvider;
	private final LAMqttBrokerServerConfig config;
	private SpatialMQTTClient broker;
	private Context context;

	LAMqttProtocolServer(LAMqttBrokerServerConfig config) {
		Log.d("AAAAAAAA", "111111111");
		if (config.uri == null)
			config.uri = "mqtt://localhost:1883";
		if (config.clientId == null)
			config.clientId = "localClient";
		if (config.locationProvider == null)
			config.locationProvider = () -> new Location(0, 0);
		// if there is a MQTT protocol indicator missing, add this
		if (!config.uri.contains("://")) {
			config.uri = this.scheme + "://" + config.uri;
		}
		this.config = config;
		this.brokerURI = config.uri;
	}

	public LAMqttProtocolServer(Config config, Context context) {
		this(new LAMqttBrokerServerConfig((String) getItemFromConfig("wot.servient.la_mqtt.uri", config), (String) getItemFromConfig("wot.servient.la_mqtt.clientId", config), () -> new Location(5, 3)));
		Log.d("AAAAAAAA", "222222222");
		this.locationUpdateHandler = new Timer();
		this.context = context;
	}

	private static Object getItemFromConfig(String configPath, Config config) {
		Log.d("AAAAAAAA", configPath);
		if (config.hasPath(configPath)) {
			Log.d("AAAAAAAA", "5555");
			String configItem = config.getString(configPath);
			Log.d("AAAAAAAA", "6666666");
			if (!configItem.equals("")) {
				Log.d("AAAAAAAA", "777777777");
				return configItem;
			}
		}
		if (configPath.equals("wot.servient.la_mqtt.clientId")) {
			return "wot" + System.nanoTime();
		} else {
			return null;
		}
	}

	@Override
	public CompletableFuture<Void> start(Servient servient) {
		return CompletableFuture.runAsync(() -> {
			Log.i(TAG, "Start LAMqttServer");
			if (this.brokerURI == null) {
				Log.i(TAG, "No broker defined for MQTT server binding - skipping");
			} else {
				Log.i(TAG, "LAMqttBrokerServer trying to connect to secured broker at " + this.brokerURI + " with client ID " + this.config.clientId);
				// TODO: verify password and username
				// TODO: generate random clientID if not provided
				try {
					URI parsed = new URI(this.brokerURI);
					String address = parsed.getScheme() + "://" + parsed.getHost();
					int port = parsed.getPort() > 0 ? parsed.getPort() : 1883;
					this.broker = new SpatialMQTTClient("33333", "11111111", "mqtt://vz5nc4zqbrgzpn13.myfritz.net", 1883, this.config.clientId, context);
					if (this.broker.connect()) {
						Log.i(TAG, "LAMqttBrokerServer connected to broker at " + this.brokerURI);
						this.locationProvider = this.config.locationProvider;
						this.port = port;
						this.address = address;
						this.locationUpdate = new TimerTask() {
							@Override
							public void run() {
								Location location = locationProvider.provideLocation();
								try {
									broker.publicPosition(location.latitude.doubleValue(), location.longitude.doubleValue());
								} catch (JSONException e) {
									throw new RuntimeException(e);
								}
							}
						};
						this.locationUpdateHandler.scheduleAtFixedRate(this.locationUpdate, 0, 5000);
					} else {
						throw new CompletionException(new Exception("LAMqttBrokerServer could not connect to broker at " + this.brokerURI));
					}
				} catch (ExecutionException | InterruptedException | URISyntaxException e) {
					throw new CompletionException(e);
				}
			}
		});
	}

	@Override
	public CompletableFuture<Void> stop() {
		Log.i(TAG, "Stop MqttServer");
		if (this.broker != null) {
			// Rimuovi tutte le ExposedThings e poi rimuovi il client
			@SuppressWarnings("unchecked") CompletableFuture<Void>[] destroyFutures = new ArrayList<>(things.values()).stream()
					.map(this::destroy).toArray(CompletableFuture[]::new);
			return CompletableFuture.allOf(destroyFutures).thenRunAsync(() -> {
				try {
					this.broker.disconnect();
					this.broker = null;
					this.locationUpdate.cancel();
				} catch (ExecutionException | InterruptedException e) {
					throw new CompletionException(e);
				}
			});
		} else {
			return completedFuture(null);
		}
	}

	@Override
	public CompletableFuture<Void> expose(ExposedThing thing) {
		if (this.broker == null) {
			return completedFuture(null);
		}
		String name = thing.getTitle();
		Log.d(TAG, "LAMqttBrokerServer at " + this.brokerURI + " exposes " + thing.getTitle() + " as unique " + name);
		this.things.put(name, thing);
		thing.getProperties()
				.forEach((propertyName, property) -> this.exposeProperty(thing, propertyName, property));
		thing.getActions()
				.forEach((actionName, action) -> this.exposeAction(thing, actionName, action));
		thing.getEvents().forEach((eventName, event) -> this.exposeEvent(thing, eventName, event));
		//TODO
		//this.broker.publish(name, JSON.stringify(thing.getThingDescription()), {
		//  retain: true,
		//});
		return completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> destroy(ExposedThing thing) {
		// if the server is not running, nothing needs to be done
		if (this.broker == null) {
			return completedFuture(null);
		}
		Log.i(TAG, "MqttServer stop exposing " + thing.getId() + " as unique '/" + thing.getId() + "/*'");
		things.remove(thing.getId());
		return completedFuture(null);
	}

	@Override
	public String getTDIdentifier(String id) {
		return id;
	}

	private void exposeProperty(ExposedThing thing, String propertyName, ExposedThingProperty<Object> property) {
		String rootTopic = thing.getTitle() + "/properties/" + propertyName;
		if (!property.isWriteOnly()) {
			String askTopic = rootTopic + "/readAsk";
			String answerTopic = rootTopic + "/readAnswer";
			MQTTReceiver askCallback = mqttMessage -> {
				CompletableFuture.runAsync(() -> {
					Log.d(TAG, "LAMqttBrokerServer at " + this.brokerURI + " received message for " + mqttMessage.topic);
					try {
						Object read = property.read().get();
						this.broker.publish(answerTopic, read.toString());
					} catch (ExecutionException | InterruptedException e) {
						throw new RuntimeException(e);
					}
				});
			};
			try {
				this.broker.subscribeGeofence(askTopic, askCallback);
			} catch (ExecutionException | InterruptedException e) {
				throw new RuntimeException(e);
			}
			String askHref = this.brokerURI + "/" + askTopic;
			FormBuilder form = new FormBuilder();
			form.setHref(askHref).setContentType(ContentManager.DEFAULT);
			form.setOp(Operation.READ_PROPERTY);
			form.setOptional("lamqv:answerTopic", answerTopic);
			property.addForm(form.build());
			Log.d(TAG, "Assign " + askHref + " to Property " + propertyName);
		}
		if (!property.isReadOnly()) {
			String askTopic = rootTopic + "/writeAsk";
			String answerTopic = rootTopic + "/writeAnswer";
			MQTTReceiver askCallback = mqttMessage -> {
				CompletableFuture.runAsync(() -> {
					Log.d(TAG, "LAMqttBrokerServer at " + this.brokerURI + " received message for " + mqttMessage.topic);
					try {
						Object read = property.write(mqttMessage.getContent()).get();
						this.broker.publish(answerTopic, read.toString());
					} catch (ExecutionException | InterruptedException | JSONException e) {
						throw new RuntimeException(e);
					}
				});
			};
			try {
				this.broker.subscribeGeofence(askTopic, askCallback);
			} catch (ExecutionException | InterruptedException e) {
				throw new RuntimeException(e);
			}
			String askHref = this.brokerURI + "/" + askTopic;
			FormBuilder form = new FormBuilder();
			form.setHref(askHref).setContentType(ContentManager.DEFAULT);
			form.setOp(Operation.WRITE_PROPERTY);
			form.setOptional("lamqv:answerTopic", answerTopic);
			property.addForm(form.build());
			Log.d(TAG, "Assign " + askHref + " to Property " + propertyName);
		}
	}

	private void exposeAction(ExposedThing thing, String actionName, ExposedThingAction<Object, Object> action) {
	}

	private void exposeEvent(ExposedThing thing, String eventName, ExposedThingEvent<Object> event) {
	}

	/*
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

	 */
}
