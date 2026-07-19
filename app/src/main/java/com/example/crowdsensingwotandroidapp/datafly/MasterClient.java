package com.example.crowdsensingwotandroidapp.datafly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Client WoT verso il Master Data Fly: consuma la TD "datafly-master" e
 * invoca le azioni risolvendo gli href dalle form (vero flusso WoT, come il
 * device-simulator del monorepo).
 *
 * Sostituisce il consumo della "Dashboard" della tesi: le azioni di identita'
 * non esistono piu' (CoreApiClient), applyToCampaignPull/Push diventano
 * joinCampaign con mode esplicita, sendPushCampaignData diventa submitData.
 * Ogni azione risponde con la busta { ok, ... | error } della tesi.
 */
public class MasterClient {

	private static final int TIMEOUT_MS = 15000;

	private final DataFlyConfig config;
	private final ObjectMapper mapper = new ObjectMapper();
	private JsonNode masterTd; // cache della TD consumata

	public MasterClient(DataFlyConfig config) {
		this.config = config;
	}

	/** (Ri)consuma la TD del Master. Chiamare fuori dal main thread. */
	public synchronized void consume() throws IOException {
		masterTd = get(config.getMasterTdUrl());
	}

	private synchronized JsonNode td() throws IOException {
		if (masterTd == null)
			consume();
		return masterTd;
	}

	/** Invoca un'azione del Master usando l'href dichiarato nella sua TD. */
	public JsonNode invoke(String action, JsonNode input) throws IOException {
		JsonNode actions = td().get("actions");
		JsonNode definition = actions == null ? null : actions.get(action);
		if (definition == null || !definition.has("forms")
				|| definition.get("forms").isEmpty()) {
			throw new IOException("azione \"" + action + "\" assente dalla TD del Master");
		}
		String href = definition.get("forms").get(0).get("href").asText();
		return post(href, input);
	}

	// --- azioni tipizzate (input come da TD del Master) ---

	/** Azione autenticata generica: {token, ...campi costruiti dal chiamante}. */
	public JsonNode invokeAuthorized(String action, String token,
			java.util.function.Consumer<ObjectNode> build) throws IOException {
		ObjectNode input = mapper.createObjectNode();
		input.put("token", token);
		build.accept(input);
		return invoke(action, input);
	}

	public JsonNode registerDevice(String token, JsonNode deviceTd) throws IOException {
		ObjectNode input = mapper.createObjectNode();
		input.put("token", token);
		input.set("td", deviceTd);
		return invoke("registerDevice", input);
	}

	public JsonNode listCampaigns(String token, String deviceId, Double lat, Double lon)
			throws IOException {
		ObjectNode input = mapper.createObjectNode();
		input.put("token", token).put("deviceId", deviceId);
		if (lat != null && lon != null) {
			input.put("lat", lat).put("lon", lon);
		}
		return invoke("listCampaigns", input);
	}

	public JsonNode joinCampaign(String token, String deviceId, String campaignId, String mode)
			throws IOException {
		ObjectNode input = mapper.createObjectNode();
		input.put("token", token).put("deviceId", deviceId).put("campaignId", campaignId)
				.put("mode", mode);
		return invoke("joinCampaign", input);
	}

	public JsonNode leaveCampaign(String token, String deviceId, String campaignId)
			throws IOException {
		ObjectNode input = mapper.createObjectNode();
		input.put("token", token).put("deviceId", deviceId).put("campaignId", campaignId);
		return invoke("leaveCampaign", input);
	}

	/** Invio dati: la risposta include status/score/netCents/reasons (M5). */
	public JsonNode submitData(String token, String deviceId, String campaignId, JsonNode payload)
			throws IOException {
		ObjectNode input = mapper.createObjectNode();
		input.put("token", token).put("deviceId", deviceId).put("campaignId", campaignId);
		input.set("payload", payload);
		return invoke("submitData", input);
	}

	public void ping(String deviceId) throws IOException {
		ObjectNode input = mapper.createObjectNode();
		input.put("deviceId", deviceId);
		invoke("ping", input);
	}

	// --- HTTP ---

	private JsonNode get(String url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		try {
			connection.setConnectTimeout(TIMEOUT_MS);
			connection.setReadTimeout(TIMEOUT_MS);
			int status = connection.getResponseCode();
			if (status >= 400)
				throw new IOException("GET " + url + " -> " + status);
			return mapper.readTree(readAll(connection.getInputStream()));
		} finally {
			connection.disconnect();
		}
	}

	private JsonNode post(String url, JsonNode body) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		try {
			connection.setRequestMethod("POST");
			connection.setConnectTimeout(TIMEOUT_MS);
			connection.setReadTimeout(TIMEOUT_MS);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);
			try (OutputStream out = connection.getOutputStream()) {
				out.write(mapper.writeValueAsBytes(body));
			}
			int status = connection.getResponseCode();
			InputStream stream =
					status >= 400 ? connection.getErrorStream() : connection.getInputStream();
			byte[] raw = stream == null ? new byte[0] : readAll(stream);
			if (raw.length == 0)
				return mapper.createObjectNode();
			return mapper.readTree(raw);
		} finally {
			connection.disconnect();
		}
	}

	private static byte[] readAll(InputStream stream) throws IOException {
		java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
		byte[] chunk = new byte[4096];
		int read;
		while ((read = stream.read(chunk)) != -1)
			buffer.write(chunk, 0, read);
		return buffer.toByteArray();
	}
}
