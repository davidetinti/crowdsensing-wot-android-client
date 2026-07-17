package com.example.crowdsensingwotandroidapp;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Observer;

import com.example.crowdsensingwotandroidapp.datafly.DataFlyIntegration;
import com.example.crowdsensingwotandroidapp.utils.User;
import com.example.crowdsensingwotandroidapp.utils.campaign.Campaign;
import com.example.crowdsensingwotandroidapp.utils.campaign.SubmissionMode;
import com.example.crowdsensingwotandroidapp.utils.campaign.submission.DataSubmission;
import com.example.crowdsensingwotandroidapp.utils.campaign.submission.LocationSubmission;
import com.example.crowdsensingwotandroidapp.utils.network.NetworkStateManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Tasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * Ponte tra la UI della tesi e il marketplace Data Fly.
 *
 * L'API pubblica (getInstance, register/login, fetchData, fetchCampaigns,
 * applyToCampaign, sendManualData, leaveCampaign, logout) e la gestione del
 * foreground service restano identiche: cambia solo cosa c'e' dietro.
 * Le azioni della vecchia Dashboard sono sostituite dal layer
 * {@link DataFlyIntegration} (Core per identita'/wallet, Master per il flusso
 * WoT: registerDevice/listCampaigns/join/submit/ping). L'esposizione del
 * servient MQTT del device tornera' in M7 con il canale push protetto.
 *
 * Nota di compatibilita': il "bearer" che la UI passa e usa come namespace
 * delle SharedPreferences ora e' l'email dell'utente (stabile tra sessioni,
 * a differenza dell'access token JWT che ruota col refresh). I metodi lo
 * accettano ancora per firma ma i token reali vivono in DataFlyConfig.
 */
public class ServientService extends Service {

	private static final String TAG = "ServientService";
	private static final long PING_PERIOD_MS = 10000;

	// Boolean indicating if the service is running
	public static boolean IS_ACTIVITY_RUNNING = false;
	// Unique instance used to access to service
	private static ServientService instance;
	// Used to manage location
	private static FusedLocationProviderClient fusedLocationClient;
	// Timer that run ping device task
	private static Timer timer;
	// Task used to ping device
	private static TimerTask pingTask;
	// Facade towards Data Fly (Core + Master)
	private static DataFlyIntegration integration;

	public ServientService() {
	}

	private ServientService(Context context) throws IOException {
		integration = new DataFlyIntegration(context);
		// Consumo subito la TD del Master: se non e' raggiungibile l'init
		// fallisce, come falliva la fetch della dashboard nella tesi.
		integration.master().consume();
		Observer<Boolean> connectivityObserver = connected -> {
			MainViewModel.addLoading();
			if (connected) {
				if (instance == null) {
					try {
						integration.master().consume();
						if (integration.config().isLoggedIn()) {
							integration.registerDevice();
							startPingTask();
						}
						instance = this;
					} catch (IOException e) {
						Log.w(TAG, "riconnessione al Master fallita", e);
					}
				}
			} else {
				instance = null;
				stopPingTask();
			}
			MainViewModel.removeLoading();
		};
		new Handler(Looper.getMainLooper()).post(() -> NetworkStateManager.getInstance()
				.getNetworkConnectivityStatus().observeForever(connectivityObserver));
	}

	public static CompletableFuture<ServientService> getInstance(Context context) {
		return CompletableFuture.supplyAsync(() -> {
			if (instance == null) {
				setupStatic(context);
				try {
					instance = new ServientService(context);
				} catch (IOException e) {
					throw new CompletionException(new Exception("Error initializing servient"));
				}
			}
			return instance;
		});
	}

	private static void setupStatic(Context context) {
		fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
		timer = new Timer();
		pingTask = null;
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		startForeground(62318, builtNotification());
		IS_ACTIVITY_RUNNING = true;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// This return is needed because if the Service is killed due to low RAM,
		// it'll be reactivated by itself
		if (!IS_ACTIVITY_RUNNING)
			stopSelf();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		ForegroundServiceLauncher.getInstance().startService(this);
		IS_ACTIVITY_RUNNING = false;
	}

	public Notification builtNotification() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		assert notificationManager != null;
		NotificationCompat.Builder builder = null;
		int importance = NotificationManager.IMPORTANCE_HIGH;
		NotificationChannel notificationChannel = new NotificationChannel("ID", "Name", importance);
		// Creating an Audio Attribute
		notificationManager.createNotificationChannel(notificationChannel);
		builder = new NotificationCompat.Builder(this, notificationChannel.getId());
		builder.setDefaults(Notification.DEFAULT_LIGHTS);
		String message = "La raccolta dati è attiva";
		builder.setSmallIcon(R.mipmap.ic_launcher).setAutoCancel(true)
				.setPriority(Notification.PRIORITY_MAX).setOngoing(false).setOnlyAlertOnce(true)
				.setColor(Color.parseColor("#0f9595")).setContentTitle(getString(R.string.app_name))
				.setContentText(message);
		Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
		launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(contentIntent);
		Notification notification = builder.build();
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		return notification;
	}

	public CompletableFuture<String> register(String email, String password) {
		return access(email, password, false);
	}

	public CompletableFuture<String> login(String email, String password) {
		return access(email, password, true);
	}

	/**
	 * Login/signup sul Core (JWT al posto delle azioni WoT della tesi), poi
	 * onboarding del device sul Master (discovery -> TD -> registerDevice, come
	 * l'exposeDevice della tesi) e avvio del ping di presenza.
	 * Restituisce l'email come "bearer" per la UI: e' stabile tra sessioni,
	 * quindi le campagne salvate nelle SharedPreferences non si perdono.
	 */
	private CompletableFuture<String> access(String email, String password, Boolean isLogin) {
		return CompletableFuture.supplyAsync(() -> {
			if (integration == null)
				throw new CompletionException(new Exception("Servient not available"));
			try {
				if (isLogin) {
					integration.core().login(email, password);
				} else {
					String displayName = email.contains("@")
							? email.substring(0, email.indexOf('@')) : email;
					integration.core().signup(email, password, displayName);
				}
				integration.config().saveEmail(email);
				integration.registerDevice();
				startPingTask();
				return email;
			} catch (IOException e) {
				throw new CompletionException(new Exception(e.getMessage()));
			}
		});
	}

	/**
	 * Dati utente per la dashboard: al posto di retrieveUserData legge il
	 * wallet Data Fly. I "punti" della UI sono i crediti totali (disponibili +
	 * in vesting) espressi in unita' intere.
	 */
	public CompletableFuture<User> fetchData(String bearer) {
		return CompletableFuture.supplyAsync(() -> {
			if (integration == null)
				throw new CompletionException(new Exception("Servient not available"));
			try {
				JsonNode wallet = integration.wallet();
				long cents = wallet.path("availableCents").asLong(0)
						+ wallet.path("pendingCents").asLong(0);
				String email = integration.config().getEmail();
				// La piattaforma non traccia "campagne completate" per utente:
				// la pulizia locale usa questa lista, quindi vuota = nessuna rimozione.
				return new User(email != null ? email : bearer, (int) (cents / 100),
						new ArrayList());
			} catch (IOException e) {
				throw new CompletionException(new Exception(e.getMessage()));
			}
		});
	}

	/**
	 * Catalogo campagne: gia' filtrate dal Core per capability del device e
	 * area (se la posizione e' disponibile). Le risposte sono mappate sulle
	 * chiavi che Campaign(HashMap) si aspetta dalla tesi.
	 */
	public CompletableFuture<ArrayList<Campaign>> fetchCampaigns() {
		return CompletableFuture.supplyAsync(() -> {
			if (integration == null)
				throw new CompletionException(new Exception("Servient not available"));
			try {
				Location location = lastKnownLocation();
				Double lat = location != null ? location.getLatitude() : null;
				Double lon = location != null ? location.getLongitude() : null;
				JsonNode result = integration.listCampaigns(lat, lon);
				ArrayList<Campaign> campaigns = new ArrayList<>();
				for (JsonNode node : result) {
					campaigns.add(new Campaign(toLegacyCampaign(node)));
				}
				return campaigns;
			} catch (IOException e) {
				throw new CompletionException(new Exception(e.getMessage()));
			}
		});
	}

	/**
	 * Adesione a una campagna. Le modalita' della tesi diventano le mode di
	 * Data Fly: AUTOMATIC -> "push" (lettura dal Master, canale protetto da
	 * M7: oggi il Core risponde con un errore), AUTO_WITH_PREF -> "pull_auto",
	 * MANUAL -> "pull_manual". L'interval resta una preferenza locale.
	 */
	public CompletableFuture<HashMap> applyToCampaign(String bearer, Campaign campaign, SubmissionMode mode, Integer interval) {
		return CompletableFuture.supplyAsync(() -> {
			if (integration == null)
				throw new CompletionException(new Exception("Servient not available"));
			try {
				JsonNode result = integration.master().joinCampaign(integration.freshToken(),
						integration.deviceId(), campaign.getId(), toDataFlyMode(mode));
				return toLegacyEnvelope(result);
			} catch (IOException e) {
				return failureEnvelope(e);
			}
		});
	}

	/**
	 * Invio manuale: sendPushCampaignData diventa submitData sul Master. La
	 * risposta M5 contiene l'esito della validazione: se accettata la busta
	 * legacy include "remaining" (per la logica di completamento della UI),
	 * se respinta diventa { ok: false, error: <motivi> }.
	 */
	public CompletableFuture<HashMap> sendManualData(String bearer, String campaignId, DataSubmission data) {
		return CompletableFuture.supplyAsync(() -> {
			if (integration == null)
				throw new CompletionException(new Exception("Servient not available"));
			if (!(data instanceof LocationSubmission))
				return failureEnvelope(new Exception("Tipo di dato non supportato"));
			LocationSubmission submission = (LocationSubmission) data;
			try {
				JsonNode result = integration.submitLocation(campaignId,
						submission.getLatitude(), submission.getLongitude(),
						submission.getAccuracy() != null ? submission.getAccuracy() : 0f);
				String status = result.path("status").asText("");
				if (!"accepted".equals(status)) {
					StringBuilder reasons = new StringBuilder();
					for (JsonNode reason : result.path("reasons")) {
						if (reasons.length() > 0)
							reasons.append(", ");
						reasons.append(reason.asText());
					}
					return failureEnvelope(new Exception(
							"Dati respinti dalla validazione: " + reasons));
				}
				HashMap<String, Object> envelope = new HashMap<>();
				envelope.put("ok", Boolean.TRUE);
				int target = result.path("target").asInt(0);
				int received = result.path("receivedCount").asInt(0);
				envelope.put("remaining", Integer.valueOf(Math.max(0, target - received)));
				envelope.put("score", Double.valueOf(result.path("score").asDouble(0)));
				envelope.put("netCents", Integer.valueOf(result.path("netCents").asInt(0)));
				return envelope;
			} catch (IOException e) {
				return failureEnvelope(e);
			}
		});
	}

	public CompletableFuture<HashMap> leaveCampaign(String bearer, Campaign campaign) {
		return CompletableFuture.supplyAsync(() -> {
			if (integration == null)
				throw new CompletionException(new Exception("Servient not available"));
			try {
				JsonNode result = integration.master().leaveCampaign(integration.freshToken(),
						integration.deviceId(), campaign.getId());
				return toLegacyEnvelope(result);
			} catch (IOException e) {
				return failureEnvelope(e);
			}
		});
	}

	/** Logout: ferma il ping e revoca il refresh token sul Core (best effort). */
	public CompletableFuture<Void> logout() {
		return CompletableFuture.runAsync(() -> {
			stopPingTask();
			if (integration != null)
				integration.core().logout();
		});
	}

	// --- interni ---

	/** Ping di presenza verso il Master, stesso periodo della tesi (10 s). */
	private static void startPingTask() {
		stopPingTask();
		pingTask = new TimerTask() {
			@Override
			public void run() {
				try {
					integration.ping();
				} catch (IOException e) {
					Log.w(TAG, "ping fallito", e);
				}
			}
		};
		timer.scheduleAtFixedRate(pingTask, 0, PING_PERIOD_MS);
	}

	private static void stopPingTask() {
		if (pingTask != null) {
			pingTask.cancel();
			pingTask = null;
		}
	}

	/** Ultima posizione nota (best effort: null senza permessi o fix). */
	@SuppressLint("MissingPermission")
	private static Location lastKnownLocation() {
		try {
			return Tasks.await(fusedLocationClient.getLastLocation(), 5, TimeUnit.SECONDS);
		} catch (Exception e) {
			return null;
		}
	}

	private static String toDataFlyMode(SubmissionMode mode) {
		switch (mode) {
			case AUTOMATIC:
				return "push";
			case AUTO_WITH_PREF:
				return "pull_auto";
			case MANUAL:
			default:
				return "pull_manual";
		}
	}

	/**
	 * Converte una CompatibleCampaign di Data Fly nella HashMap con le chiavi
	 * legacy attese da Campaign(HashMap). I "points" sono la stima dei crediti
	 * netti per submission; il tipo e' la parte finale della spec key
	 * ("core/location" -> "location", come i type della tesi).
	 */
	private static HashMap<String, Object> toLegacyCampaign(JsonNode node) {
		HashMap<String, Object> campaign = new HashMap<>();
		campaign.put("_id", node.path("id").asText());
		campaign.put("title", node.path("title").asText());
		campaign.put("description", node.path("description").asText());
		campaign.put("organization", node.path("organization").asText(""));
		campaign.put("ideal_submission_interval",
				Integer.valueOf(node.path("idealIntervalSec").asInt(0)));
		campaign.put("points",
				Double.valueOf(node.path("netPerSubmissionCents").asInt(0) / 100.0));
		boolean push = false;
		boolean pullAuto = false;
		boolean pullManual = false;
		for (JsonNode mode : node.path("modes")) {
			switch (mode.asText()) {
				case "push":
					push = true;
					break;
				case "pull_auto":
					pullAuto = true;
					break;
				case "pull_manual":
					pullManual = true;
					break;
			}
		}
		// Mappa inversa di toDataFlyMode: pull_enabled abilita AUTOMATIC,
		// push_auto_enabled AUTO_WITH_PREF, push_input_enabled MANUAL.
		campaign.put("pull_enabled", Boolean.valueOf(push));
		campaign.put("push_auto_enabled", Boolean.valueOf(pullAuto));
		campaign.put("push_input_enabled", Boolean.valueOf(pullManual));
		campaign.put("submission_required",
				Integer.valueOf(node.path("targetSubmissions").asInt(0)));
		String specKey = node.path("spec").path("key").asText("");
		campaign.put("type", specKey.contains("/")
				? specKey.substring(specKey.lastIndexOf('/') + 1) : specKey);
		JsonNode area = node.path("area");
		if (area.isObject()) {
			HashMap<String, Object> legacyArea = new HashMap<>();
			legacyArea.put("type", "circle");
			legacyArea.put("radius", Double.valueOf(area.path("radiusM").asDouble(0)));
			legacyArea.put("center_lat",
					Double.valueOf(area.path("center").path("lat").asDouble(0)));
			legacyArea.put("center_lng",
					Double.valueOf(area.path("center").path("lon").asDouble(0)));
			campaign.put("area", legacyArea);
		}
		return campaign;
	}

	/** Busta { ok | error } del Master -> HashMap legacy per la UI. */
	private static HashMap<String, Object> toLegacyEnvelope(JsonNode result) {
		HashMap<String, Object> envelope = new HashMap<>();
		boolean ok = result != null && result.path("ok").asBoolean(false);
		envelope.put("ok", Boolean.valueOf(ok));
		if (!ok) {
			envelope.put("error", result == null ? "Risposta vuota dal Master"
					: result.path("error").asText("Errore sconosciuto"));
		}
		return envelope;
	}

	private static HashMap<String, Object> failureEnvelope(Exception e) {
		HashMap<String, Object> envelope = new HashMap<>();
		envelope.put("ok", Boolean.FALSE);
		envelope.put("error", e.getMessage() != null ? e.getMessage() : "Errore di rete");
		return envelope;
	}
}
