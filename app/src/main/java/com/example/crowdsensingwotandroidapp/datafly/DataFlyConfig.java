package com.example.crowdsensingwotandroidapp.datafly;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Configurazione degli endpoint Data Fly e persistenza dei token.
 *
 * Sostituisce il vecchio schema (TDD pubblica + bearer = ObjectId Mongo) con:
 * - CORE_URL: identita' JWT e wallet (Marketplace Core);
 * - MASTER_TD_URL: la Thing Description del Master ("datafly-master"), da cui
 *   l'app risolve le azioni WoT (registerDevice, listCampaigns, ...).
 */
public final class DataFlyConfig {

	private static final String PREFS = "datafly";
	private static final String KEY_CORE_URL = "coreUrl";
	private static final String KEY_MASTER_TD_URL = "masterTdUrl";
	private static final String KEY_ACCESS_TOKEN = "accessToken";
	private static final String KEY_REFRESH_TOKEN = "refreshToken";
	private static final String KEY_EMAIL = "email";
	// M7: credenziali MQTT per-device consegnate dal Master alla registrazione
	private static final String KEY_MQTT_URL = "mqttUrl";
	private static final String KEY_MQTT_USERNAME = "mqttUsername";
	private static final String KEY_MQTT_PASSWORD = "mqttPassword";
	// M8: chiave pubblica (SPKI PEM) dei token spot firmati dal Core
	private static final String KEY_SPOT_TOKEN_KEY_PEM = "spotTokenKeyPem";

	// Default di sviluppo: emulatore Android -> host (10.0.2.2)
	private static final String DEFAULT_CORE_URL = "http://10.0.2.2:8080";
	private static final String DEFAULT_MASTER_TD_URL = "http://10.0.2.2:8082/datafly-master";

	private final SharedPreferences prefs;

	public DataFlyConfig(Context context) {
		this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
	}

	public String getCoreUrl() {
		return prefs.getString(KEY_CORE_URL, DEFAULT_CORE_URL);
	}

	public String getMasterTdUrl() {
		return prefs.getString(KEY_MASTER_TD_URL, DEFAULT_MASTER_TD_URL);
	}

	public void setEndpoints(String coreUrl, String masterTdUrl) {
		prefs.edit().putString(KEY_CORE_URL, coreUrl).putString(KEY_MASTER_TD_URL, masterTdUrl)
				.apply();
	}

	public String getAccessToken() {
		return prefs.getString(KEY_ACCESS_TOKEN, null);
	}

	public String getRefreshToken() {
		return prefs.getString(KEY_REFRESH_TOKEN, null);
	}

	public void saveTokens(String accessToken, String refreshToken) {
		prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken)
				.putString(KEY_REFRESH_TOKEN, refreshToken).apply();
	}

	public void clearTokens() {
		prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).apply();
	}

	/** Email dell'utente loggato: identificatore stabile mostrato/usato dalla UI. */
	public String getEmail() {
		return prefs.getString(KEY_EMAIL, null);
	}

	public void saveEmail(String email) {
		prefs.edit().putString(KEY_EMAIL, email).apply();
	}

	/**
	 * M7 — canale push protetto: il Master ruota la password a ogni
	 * registrazione, quindi questi valori vanno sovrascritti ogni volta.
	 * Il client MQTT dell'app li usera' per ASK/{deviceId} e ANSWER/{deviceId}.
	 */
	public void saveMqttCredentials(String url, String username, String password) {
		prefs.edit().putString(KEY_MQTT_URL, url).putString(KEY_MQTT_USERNAME, username)
				.putString(KEY_MQTT_PASSWORD, password).apply();
	}

	public String getMqttUrl() {
		return prefs.getString(KEY_MQTT_URL, null);
	}

	public String getMqttUsername() {
		return prefs.getString(KEY_MQTT_USERNAME, null);
	}

	public String getMqttPassword() {
		return prefs.getString(KEY_MQTT_PASSWORD, null);
	}

	public boolean hasMqttCredentials() {
		return getMqttUrl() != null && getMqttUsername() != null && getMqttPassword() != null;
	}

	/**
	 * M8 — chiave pubblica dei token spot (ES256): il device la usa per
	 * accettare SOLO gli ASK spot davvero firmati dalla piattaforma.
	 * Aggiornata a ogni registrazione (il Core puo' ruotarla).
	 */
	public void saveSpotTokenKey(String publicKeyPem) {
		prefs.edit().putString(KEY_SPOT_TOKEN_KEY_PEM, publicKeyPem).apply();
	}

	public String getSpotTokenKeyPem() {
		return prefs.getString(KEY_SPOT_TOKEN_KEY_PEM, null);
	}

	public boolean isLoggedIn() {
		return getAccessToken() != null;
	}
}
