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

	public boolean isLoggedIn() {
		return getAccessToken() != null;
	}
}
