package com.example.crowdsensingwotandroidapp.datafly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Client REST verso il Marketplace Core (identita' JWT + wallet).
 *
 * Sostituisce le azioni registerNewUser/loginUser della dashboard della tesi
 * (docs/01 par.2 del monorepo): le credenziali non transitano piu' su
 * affordances WoT. Access token breve + refresh token con rotazione:
 * request() ritenta UNA volta dopo un 401 rinnovando i token.
 *
 * Tutte le chiamate sono bloccanti: invocarle fuori dal main thread
 * (l'app usa gia' CompletableFuture/executor per il servient).
 */
public class CoreApiClient {

	public static class CoreApiException extends IOException {
		public final int status;

		public CoreApiException(int status, String message) {
			super(message);
			this.status = status;
		}
	}

	private static final int TIMEOUT_MS = 15000;

	private final DataFlyConfig config;
	private final ObjectMapper mapper = new ObjectMapper();

	public CoreApiClient(DataFlyConfig config) {
		this.config = config;
	}

	/** Registra un worker e salva i token. */
	public JsonNode signup(String email, String password, String displayName) throws IOException {
		ObjectNode body = mapper.createObjectNode();
		body.put("email", email).put("password", password).put("displayName", displayName);
		JsonNode response = request("POST", "/auth/signup", body, false);
		saveTokens(response);
		return response.get("user");
	}

	/** Login worker: salva i token e restituisce l'utente. */
	public JsonNode login(String email, String password) throws IOException {
		ObjectNode body = mapper.createObjectNode();
		body.put("email", email).put("password", password);
		JsonNode response = request("POST", "/auth/login", body, false);
		saveTokens(response);
		return response.get("user");
	}

	public void logout() {
		String refreshToken = config.getRefreshToken();
		if (refreshToken != null) {
			try {
				ObjectNode body = mapper.createObjectNode();
				body.put("refreshToken", refreshToken);
				request("POST", "/auth/logout", body, false);
			} catch (IOException ignored) {
				// best effort: i token locali vengono comunque cancellati
			}
		}
		config.clearTokens();
	}

	/** Wallet del worker: availableCents, pendingCents, transactions. */
	public JsonNode getWallet() throws IOException {
		return request("GET", "/wallet", null, true);
	}

	/** Access token corrente (per le azioni WoT verso il Master). */
	public String currentAccessToken() {
		return config.getAccessToken();
	}

	// --- interni ---

	private void saveTokens(JsonNode response) throws IOException {
		JsonNode tokens = response.get("tokens");
		if (tokens == null || tokens.get("accessToken") == null) {
			throw new IOException("risposta di autenticazione senza token");
		}
		config.saveTokens(tokens.get("accessToken").asText(),
				tokens.get("refreshToken").asText());
	}

	private synchronized boolean tryRefresh() {
		String refreshToken = config.getRefreshToken();
		if (refreshToken == null)
			return false;
		try {
			ObjectNode body = mapper.createObjectNode();
			body.put("refreshToken", refreshToken);
			JsonNode response = rawRequest("POST", "/auth/refresh", body, null);
			config.saveTokens(response.get("accessToken").asText(),
					response.get("refreshToken").asText());
			return true;
		} catch (IOException e) {
			config.clearTokens();
			return false;
		}
	}

	private JsonNode request(String method, String path, JsonNode body, boolean authenticated)
			throws IOException {
		try {
			return rawRequest(method, path, body,
					authenticated ? config.getAccessToken() : null);
		} catch (CoreApiException e) {
			// access token scaduto: un solo retry dopo il refresh (rotazione)
			if (e.status == 401 && authenticated && tryRefresh()) {
				return rawRequest(method, path, body, config.getAccessToken());
			}
			throw e;
		}
	}

	private JsonNode rawRequest(String method, String path, JsonNode body, String bearer)
			throws IOException {
		HttpURLConnection connection =
				(HttpURLConnection) new URL(config.getCoreUrl() + path).openConnection();
		try {
			connection.setRequestMethod(method);
			connection.setConnectTimeout(TIMEOUT_MS);
			connection.setReadTimeout(TIMEOUT_MS);
			connection.setRequestProperty("Content-Type", "application/json");
			if (bearer != null)
				connection.setRequestProperty("Authorization", "Bearer " + bearer);
			if (body != null) {
				connection.setDoOutput(true);
				try (OutputStream out = connection.getOutputStream()) {
					out.write(mapper.writeValueAsBytes(body));
				}
			}
			int status = connection.getResponseCode();
			InputStream stream =
					status >= 400 ? connection.getErrorStream() : connection.getInputStream();
			byte[] raw = stream == null ? new byte[0] : readAll(stream);
			if (status >= 400) {
				String message = "errore " + status;
				if (raw.length > 0) {
					JsonNode error = mapper.readTree(raw);
					if (error.has("message"))
						message = error.get("message").asText();
				}
				throw new CoreApiException(status, message);
			}
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
