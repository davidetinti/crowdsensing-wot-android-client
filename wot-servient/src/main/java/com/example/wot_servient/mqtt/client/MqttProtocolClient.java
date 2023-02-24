package com.example.wot_servient.mqtt.client;

import android.util.Log;

import com.example.wot_servient.mqtt.utils.MqttProtocolSettings;
import com.example.wot_servient.wot.binding.ProtocolClient;
import com.example.wot_servient.wot.binding.ProtocolClientException;
import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.Thing;
import com.example.wot_servient.wot.thing.filter.ThingFilter;
import com.example.wot_servient.wot.thing.form.Form;
import com.example.wot_servient.wot.thing.schema.StringSchema;
import com.example.wot_servient.wot.utilities.Pair;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Allows consuming Things via MQTT.
 */
public class MqttProtocolClient implements ProtocolClient {

	private static final String TAG = "MqttProtocolClient";
	private final Map<String, Observable<Content>> topicSubjects;
	private final Pair<MqttProtocolSettings, MqttAndroidClient> mqttClientPair;

	public MqttProtocolClient(Pair<MqttProtocolSettings, MqttAndroidClient> mqttClientPair) {
		this(mqttClientPair, new HashMap<>());
	}

	MqttProtocolClient(Pair<MqttProtocolSettings, MqttAndroidClient> mqttClientPair, Map<String, Observable<Content>> topicSubjects) {
		this.mqttClientPair = mqttClientPair;
		this.topicSubjects = topicSubjects;
	}

	@Override
	public CompletableFuture<Content> invokeResource(Form form) {
		return invokeResource(form, null);
	}

	@Override
	public CompletableFuture<Content> invokeResource(Form form, Content content) {
		CompletableFuture<Content> future = new CompletableFuture<>();
		try {
			String topic = new URI(form.getHref()).getPath().substring(1);
			publishToTopic(content, future, topic);
		} catch (URISyntaxException e) {
			future.completeExceptionally(new ProtocolClientException("Unable to extract topic from href '" + form.getHref() + "'"));
		}
		return future;
	}

	@Override
	public Observable<Content> observeResource(Form form) throws ProtocolClientException {
		try {
			String topic = new URI(form.getHref()).getPath().substring(1);
			Log.d(TAG, "observeResource: " + topic);
			return topicSubjects.computeIfAbsent(topic, key -> topicObserver(form, topic));
		} catch (URISyntaxException e) {
			throw new ProtocolClientException("Unable to subscribe resource: " + e.getMessage());
		}
	}

	@Override
	public Observable<Thing> discover(ThingFilter filter) {
		String allTDTopic = "AndroidWotServientAllTD";
		return Observable.create(emitter -> {
					Log.d(TAG, "Subscribe to topic " + allTDTopic + " to receive all Thing Descriptions.");
					try {
						mqttClientPair.second().subscribe(allTDTopic, 2, (topic, message) -> {
							Log.d(TAG, "Received Message for Discovery with topic " + topic + ": " + message);
							Content content = new Content(message.getPayload());
							String json = ContentManager.contentToValue(content, new StringSchema());
							Thing thing = Thing.fromJson(json);
							emitter.onNext(thing);
						});
					} catch (Exception e) {
						Log.d(TAG, "Discovery is completed. Unsubscribe from topic " + allTDTopic);
						mqttClientPair.second().unsubscribe(allTDTopic);
						emitter.onError(e);
					}
				}).takeUntil(Observable.timer(5, TimeUnit.SECONDS)).map(n -> (Thing) n)
				.subscribeOn(Schedulers.io());
	}

	@NonNull
	private Observable<Content> topicObserver(Form form, String topic) {
		//        return Observable.using(
		//                () -> null,
		//                ignore -> Observable.<Content>create(source -> {
		//                    Log.d(TAG, "MqttClient connected to broker at " + mqttClientPair.first().getBroker() + " subscribe to topic " + topic);
		//
		//                    try {
		//                        mqttClientPair.second().subscribe(topic, 2, (receivedTopic, message) -> {
		//                            Log.d(TAG, "MqttClient received message from broker " + mqttClientPair.first().getBroker() + " for topic " + receivedTopic);
		//                            Content content = new Content(form.getContentType(), message.getPayload());
		//                            source.onNext(content);
		//                        });
		//                    }
		//                    catch (MqttException e) {
		//                        Log.w(TAG, "Exception occured while trying to subscribe to broker " + mqttClientPair.first().getBroker() + " and topic " + topic + ": " + e.getMessage());
		//                        source.onError(e);
		//                    }
		//                }),
		//                ignore -> {
		//                    Log.d(TAG, "MqttClient subscriptions of broker " + mqttClientPair.first().getBroker() + " and topic " + topic + " has no more observers. Remove subscription.");
		//
		//                    try {
		//                        mqttClientPair.second().unsubscribe(topic);
		//                    }
		//                    catch (MqttException e) {
		//                        Log.w(TAG, "Exception occured while trying to unsubscribe from broker " + mqttClientPair.first().getBroker() + " and topic " + topic + ": " + e.getMessage());
		//                    }
		//                }
		//        ).share();
		return Observable.<Content>create(emitter -> {
			Log.d(TAG, "MqttClient connected to broker at " + mqttClientPair.first()
					.getBroker() + " subscribe to topic " + topic);
			try {
				mqttClientPair.second().subscribe(topic, 2, (receivedTopic, message) -> {
					Log.d(TAG, "MqttClient received message from broker " + mqttClientPair.first()
							.getBroker() + " for topic " + receivedTopic);
					Content content = new Content(form.getContentType(), message.getPayload());
					emitter.onNext(content);
				});
			} catch (MqttException e) {
				Log.w(TAG, "Exception occured while trying to subscribe to broker " + mqttClientPair.first()
						.getBroker() + " and topic " + topic + ": " + e.getMessage());
				emitter.onError(e);
			}
		}).subscribeOn(Schedulers.io());
	}

	private void publishToTopic(Content content, CompletableFuture<Content> future, String topic) {
		try {
			Log.d(TAG, "MqttClient at " + mqttClientPair.first()
					.getBroker() + " publishing to topic " + topic);
			byte[] payload;
			if (content != null) {
				payload = content.getBody();
			} else {
				payload = new byte[0];
			}
			mqttClientPair.second().publish(topic, new MqttMessage(payload));
			// MQTT does not support the request-response pattern. return empty message
			future.complete(Content.EMPTY_CONTENT);
		} catch (MqttException e) {
			future.completeExceptionally(new ProtocolClientException("MqttClient at '" + mqttClientPair.first()
					.getBroker() + "' cannot publish data for topic '" + topic + "': " + e.getMessage()));
		}
	}
}