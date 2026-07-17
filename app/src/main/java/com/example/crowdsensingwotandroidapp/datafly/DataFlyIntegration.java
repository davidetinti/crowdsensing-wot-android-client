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
		String token = requireToken();
		List<SensorDiscovery.DiscoveredCapability> capabilities =
				SensorDiscovery.discover(context);
		if (capabilities.isEmpty()) {
			throw new IOException(
					"nessuna capability del registry rilevata su questo device");
		}
		ObjectNode td = DeviceTdFactory.build(deviceId(), capabilities);
		JsonNode result = master.registerDevice(token, td);
		requireOk(result, "registerDevice");
		return result.get("capabilities");
	}

	/** Campagne compatibili con questo device nella posizione data (opzionale). */
	public JsonNode listCampaigns(Double lat, Double lon) throws IOException {
		JsonNode result = master.listCampaigns(requireToken(), deviceId(), lat, lon);
		requireOk(result, "listCampaigns");
		return result.get("campaigns");
	}

	/** Adesione con un tap. mode: "pull_auto" | "pull_manual" ("push" da M7). */
	public String joinCampaign(String campaignId, String mode) throws IOException {
		JsonNode result = master.joinCampaign(requireToken(), deviceId(), campaignId, mode);
		requireOk(result, "joinCampaign");
		return result.get("participationId").asText();
	}

	public void leaveCampaign(String campaignId) throws IOException {
		requireOk(master.leaveCampaign(requireToken(), deviceId(), campaignId), "leaveCampaign");
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
		JsonNode result = master.submitData(requireToken(), deviceId(), campaignId, payload);
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
