package com.example.crowdsensingwotandroidapp.datafly;

import android.location.Location;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Canale push protetto (M7, docs/04 §1 del monorepo): il client MQTT del
 * device sul broker dynsec.
 *
 * - connessione con le credenziali per-device consegnate alla registrazione
 *   (username = deviceId, password ruotata a ogni registerDevice);
 * - will LWT/{deviceId} = "offline" e publish "online" a ogni (ri)connessione:
 *   lo stato presenza arriva al Master senza polling;
 * - subscribe di ASK/{deviceId}: per ogni richiesta del Master si risponde su
 *   ANSWER/{deviceId} riflettendo il {@code jti} (monouso, scade in ~30 s:
 *   rispondere subito e una volta sola) con il fix corrente.
 *
 * Le ACL deny-default del broker garantiscono che NESSUN altro possa leggere
 * questi topic: e' la risposta al problema "chiunque conosca il topic legge i
 * dati" della tesi. Gli ASK vengono processati su un executor dedicato: mai
 * bloccare (o ri-pubblicare dal) thread di callback di Paho.
 */
public class PushChannel {

	/** Sorgente del fix di posizione (bloccante, chiamata fuori dal main thread). */
	public interface FixProvider {
		/** @return il fix corrente, o null se non disponibile (permessi, GPS off). */
		Location currentFix();
	}

	private static final String TAG = "DataFlyPush";
	private static final int KEEP_ALIVE_SEC = 30;

	private final DataFlyConfig config;
	private final String deviceId;
	private final FixProvider fixProvider;
	private final ObjectMapper mapper = new ObjectMapper();
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private MqttClient client;

	public PushChannel(DataFlyConfig config, String deviceId, FixProvider fixProvider) {
		this.config = config;
		this.deviceId = deviceId;
		this.fixProvider = fixProvider;
	}

	public synchronized boolean isRunning() {
		return client != null && client.isConnected();
	}

	/**
	 * Connette il canale. Richiede le credenziali salvate da registerDevice
	 * (DataFlyConfig.hasMqttCredentials): senza, il canale push non esiste e
	 * l'app resta in sola modalita' pull.
	 */
	public synchronized void start() throws MqttException {
		if (isRunning())
			return;
		if (!config.hasMqttCredentials()) {
			throw new IllegalStateException(
					"credenziali MQTT assenti: registrare prima il device sul Master");
		}
		// Paho vuole tcp:// o ssl://, la piattaforma consegna mqtt:// o mqtts://
		String url = config.getMqttUrl().replaceFirst("^mqtt://", "tcp://")
				.replaceFirst("^mqtts://", "ssl://");
		// clientId = deviceId: una sola sessione per device (una nuova connessione
		// soppianta la vecchia, coerente con la rotazione delle credenziali)
		client = new MqttClient(url, deviceId, new MemoryPersistence());
		MqttConnectOptions options = new MqttConnectOptions();
		options.setUserName(config.getMqttUsername());
		options.setPassword(config.getMqttPassword().toCharArray());
		options.setCleanSession(true);
		options.setAutomaticReconnect(true);
		options.setKeepAliveInterval(KEEP_ALIVE_SEC);
		options.setWill("LWT/" + deviceId, "offline".getBytes(StandardCharsets.UTF_8), 0, false);

		client.setCallback(new MqttCallbackExtended() {
			@Override
			public void connectComplete(boolean reconnect, String serverURI) {
				// (ri)sottoscrizione e presenza: vale sia alla prima connect sia
				// dopo una riconnessione automatica (cleanSession=true)
				try {
					client.subscribe("ASK/" + deviceId, 0);
					client.publish("LWT/" + deviceId,
							"online".getBytes(StandardCharsets.UTF_8), 0, false);
					Log.i(TAG, "canale push connesso (" + (reconnect ? "riconnesso" : "nuovo") + ")");
				} catch (MqttException e) {
					Log.w(TAG, "subscribe/online fallita", e);
				}
			}

			@Override
			public void connectionLost(Throwable cause) {
				Log.w(TAG, "connessione push persa (riconnessione automatica)", cause);
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) {
				// mai bloccare il thread di Paho: l'ASK si processa sull'executor
				byte[] payload = message.getPayload();
				executor.execute(() -> answerAsk(payload));
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}
		});
		client.connect(options);
	}

	/**
	 * Chiusura definitiva: "offline" esplicito, disconnect e stop dell'executor
	 * (best effort). L'istanza non e' riavviabile: ricrearla (ServientService
	 * lo fa a ogni registerDevice, quando le credenziali ruotano).
	 */
	public synchronized void stop() {
		executor.shutdown();
		if (client == null)
			return;
		try {
			if (client.isConnected()) {
				client.publish("LWT/" + deviceId,
						"offline".getBytes(StandardCharsets.UTF_8), 0, false);
				client.disconnect(2000);
			}
			client.close();
		} catch (MqttException e) {
			Log.w(TAG, "stop del canale push non pulito", e);
		} finally {
			client = null;
		}
	}

	// --- interni ---

	private void answerAsk(byte[] rawAsk) {
		try {
			JsonNode ask = mapper.readTree(rawAsk);
			String jti = ask.path("jti").asText(null);
			if (jti == null) {
				Log.w(TAG, "ASK senza jti: ignorato");
				return;
			}
			Location fix = fixProvider.currentFix();
			if (fix == null) {
				// niente fix ⇒ niente ANSWER: il jti scade da solo lato Master
				Log.w(TAG, "ASK " + jti + ": nessun fix disponibile, non rispondo");
				return;
			}
			ObjectNode payload = mapper.createObjectNode();
			payload.put("lat", fix.getLatitude()).put("lon", fix.getLongitude())
					.put("accuracy", fix.getAccuracy());
			long fixTime = fix.getTime();
			payload.put("capturedAt", DateTimeFormatter.ISO_INSTANT
					.format(fixTime > 0 ? Instant.ofEpochMilli(fixTime) : Instant.now()));
			ObjectNode answer = mapper.createObjectNode();
			answer.put("jti", jti);
			answer.set("payload", payload);
			MqttClient current;
			synchronized (this) {
				current = client;
			}
			if (current != null && current.isConnected()) {
				current.publish("ANSWER/" + deviceId, mapper.writeValueAsBytes(answer), 0, false);
				Log.i(TAG, "ANSWER inviata per jti " + jti);
			}
		} catch (Exception e) {
			Log.w(TAG, "gestione ASK fallita", e);
		}
	}
}
