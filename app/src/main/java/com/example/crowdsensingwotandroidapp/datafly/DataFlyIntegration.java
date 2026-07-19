package com.example.crowdsensingwotandroidapp.datafly;

import android.content.Context;
import android.content.SharedPreferences;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Facciata unica del flusso worker Data Fly, pensata per essere chiamata da
 * ServientService/DashboardViewModel al posto delle azioni della Dashboard
 * della tesi (mappa completa in README-DATAFLY.md):
 *
 *   login → auto-discovery → TD → registerDevice → listCampaigns →
 *   joinCampaign(mode) → submitData → ping periodico.
 *
 * Tutte le chiamate sono bloccanti: usare l'executor gia' presente nel service.
 */
public class DataFlyIntegration {

	private static final String PREFS = "datafly";
	private static final String KEY_DEVICE_ID = "deviceId";

	private final Context context;
	private final DataFlyConfig config;
	private final CoreApiClient core;
	private final MasterClient master;

	public DataFlyIntegration(Context context) {
		this.context = context.getApplicationContext();
		this.config = new DataFlyConfig(this.context);
		this.core = new CoreApiClient(config);
		this.master = new MasterClient(config);
	}

	public CoreApiClient core() {
		return core;
	}

	public MasterClient master() {
		return master;
	}

	public DataFlyConfig config() {
		return config;
	}

	/** deviceId stabile "smartphone_<uuid>" (stesso schema della tesi). */
	public String deviceId() {
		SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
		String deviceId = prefs.getString(KEY_DEVICE_ID, null);
		if (deviceId == null) {
			deviceId = "smartphone_" + UUID.randomUUID();
			prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
		}
		return deviceId;
	}

	/**
	 * Onboarding device (da ripetere a ogni avvio: l'upsert e' idempotente e
	 * aggiorna le capability se l'hardware e' cambiato).
	 * @return le capability registrate (JsonNode array)
	 */
	public JsonNode registerDevice() throws IOException {
		String token = freshToken();
		List<SensorDiscovery.DiscoveredCapability> capabilities =
				SensorDiscovery.discover(context);
		if (capabilities.isEmpty()) {
			throw new IOException(
					"nessuna capability del registry rilevata su questo device");
		}
		ObjectNode td = DeviceTdFactory.build(deviceId(), capabilities);
		JsonNode result = master.registerDevice(token, td);
		requireOk(result, "registerDevice");
		// M7: se il broker ha dynsec, la risposta porta le credenziali MQTT
		// per-device (password ruotata a ogni registrazione): si salvano per
		// il client push (ASK/ANSWER sul canale protetto).
		JsonNode mqtt = result.get("mqtt");
		if (mqtt != null && mqtt.hasNonNull("url")) {
			config.saveMqttCredentials(mqtt.get("url").asText(),
					mqtt.get("username").asText(), mqtt.get("password").asText());
		}
		// M8: chiave pubblica dei token spot — serve a PushChannel per
		// verificare la firma degli ASK spot (SpotTokenVerifier).
		JsonNode tokenKey = result.get("tokenKey");
		if (tokenKey != null && tokenKey.hasNonNull("publicKeyPem")) {
			config.saveSpotTokenKey(tokenKey.get("publicKeyPem").asText());
		}
		return result.get("capabilities");
	}

	// --- Spot mode (M8): il worker vende letture on-demand ---

	/** Crea/aggiorna il listing spot del device (e lo riattiva se in pausa). */
	public JsonNode upsertSpotListing(String specKey, int priceCents) throws IOException {
		JsonNode result = master.invokeAuthorized("upsertSpotListing", freshToken(),
				body -> body.put("deviceId", deviceId()).put("specKey", specKey)
						.put("priceCents", priceCents));
		requireOk(result, "upsertSpotListing");
		return result;
	}

	/** Attiva o mette in pausa il listing spot. */
	public JsonNode setSpotListing(String specKey, boolean active) throws IOException {
		JsonNode result = master.invokeAuthorized("setSpotListing", freshToken(),
				body -> body.put("deviceId", deviceId()).put("specKey", specKey)
						.put("active", active));
		requireOk(result, "setSpotListing");
		return result;
	}

	/** I listing del worker con statistiche (letture vendute, guadagni). */
	public JsonNode listSpotListings() throws IOException {
		JsonNode result = master.invokeAuthorized("listSpotListings", freshToken(), body -> {
		});
		requireOk(result, "listSpotListings");
		return result.get("listings");
	}

	/** Campagne compatibili con questo device nella posizione data (opzionale). */
	public JsonNode listCampaigns(Double lat, Double lon) throws IOException {
		JsonNode result = master.listCampaigns(freshToken(), deviceId(), lat, lon);
		requireOk(result, "listCampaigns");
		return result.get("campaigns");
	}

	/** Adesione con un tap. mode: "pull_auto" | "pull_manual" ("push" da M7). */
	public String joinCampaign(String campaignId, String mode) throws IOException {
		JsonNode result = master.joinCampaign(freshToken(), deviceId(), campaignId, mode);
		requireOk(result, "joinCampaign");
		return result.get("participationId").asText();
	}

	public void leaveCampaign(String campaignId) throws IOException {
		requireOk(master.leaveCampaign(freshToken(), deviceId(), campaignId), "leaveCampaign");
	}

	/**
	 * Invio di un fix di posizione. La risposta M5 contiene l'esito della
	 * validazione: status accepted/rejected, score, reasons, netCents
	 * (crediti guadagnati, in pending fino al vesting).
	 */
	public JsonNode submitLocation(String campaignId, double lat, double lon, float accuracy)
			throws IOException {
		ObjectNode payload = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
		payload.put("lat", lat).put("lon", lon).put("accuracy", accuracy);
		payload.put("capturedAt", java.time.format.DateTimeFormatter.ISO_INSTANT
				.format(java.time.Instant.now()));
		JsonNode result = master.submitData(freshToken(), deviceId(), campaignId, payload);
		requireOk(result, "submitData");
		return result;
	}

	/** Ping di presenza (sostituisce il TimerTask della tesi, stesso periodo 10s). */
	public void ping() throws IOException {
		master.ping(deviceId());
	}

	/** Wallet worker: availableCents, pendingCents, transactions (sola lettura). */
	public JsonNode wallet() throws IOException {
		return core.getWallet();
	}

	// --- interni ---

	/**
	 * Access token pronto per un'azione WoT verso il Master: se sta per scadere
	 * (o e' gia' scaduto) viene rinnovato prima, perche' il Master si limita a
	 * verificarlo e non puo' fare refresh al posto del device.
	 */
	public String freshToken() throws IOException {
		String token = requireToken();
		if (isExpiringSoon(token) && core.refreshTokens()) {
			token = requireToken();
		}
		return token;
	}

	/** true se il JWT scade entro 60 secondi. */
	private static boolean isExpiringSoon(String jwt) {
		try {
			String[] parts = jwt.split("\\.");
			if (parts.length < 2)
				return false;
			byte[] payload = java.util.Base64.getUrlDecoder().decode(parts[1]);
			JsonNode claims = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
			long exp = claims.path("exp").asLong(0);
			return exp > 0 && exp * 1000L < System.currentTimeMillis() + 60_000L;
		} catch (RuntimeException | IOException e) {
			return false;
		}
	}

	private String requireToken() throws IOException {
		String token = config.getAccessToken();
		if (token == null)
			throw new IOException("utente non autenticato: eseguire il login sul Core");
		return token;
	}

	private static void requireOk(JsonNode result, String action) throws IOException {
		if (result == null || !result.path("ok").asBoolean(false)) {
			String error = result == null ? "risposta vuota" : result.path("error").asText("?");
			throw new IOException(action + " fallita: " + error);
		}
	}
}
