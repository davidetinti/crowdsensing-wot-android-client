package com.example.crowdsensingwotandroidapp;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;

import com.example.crowdsensingwotandroidapp.utils.User;
import com.example.crowdsensingwotandroidapp.utils.campaign.Campaign;
import com.example.crowdsensingwotandroidapp.utils.campaign.SubmissionMode;
import com.example.crowdsensingwotandroidapp.utils.campaign.submission.DataSubmission;
import com.example.crowdsensingwotandroidapp.utils.network.NetworkStateManager;
import com.example.wot_servient.la_mqtt.utils.LAMqttLocationProvider;
import com.example.wot_servient.la_mqtt.utils.Location;
import com.example.wot_servient.wot.DefaultWot;
import com.example.wot_servient.wot.Wot;
import com.example.wot_servient.wot.WotException;
import com.example.wot_servient.wot.thing.ConsumedThing;
import com.example.wot_servient.wot.thing.ExposedThing;
import com.example.wot_servient.wot.thing.Thing;
import com.example.wot_servient.wot.thing.ThingBuilder;
import com.example.wot_servient.wot.thing.property.ThingPropertyBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class ServientService extends Service {

	// Boolean indicating if the service is running
	public static boolean IS_ACTIVITY_RUNNING = false;
	// Unique instance used to access to service
	private static ServientService instance;
	// Needed to extract values, strings, settings, parameters
	private static Resources resources;
	// Used to manage location
	private static FusedLocationProviderClient fusedLocationClient;
	// Shared preferences to store persistent data
	private static SharedPreferences sharedPreferences;
	// Directory URL
	private static String directoryUrl;
	// Timer that run ping device task
	private static Timer timer;
	// Task used to ping device
	private static TimerTask pingTask;
	// Reference to exposed device
	private static ExposedThing exposedDevice;
	// Reference to exposed device
	private static ExposedThing backupExposedDevice;
	// WOT instance
	private static Wot wot;
	// Reference to consumed master thing
	private ConsumedThing consumedDashboard;

	public ServientService() {
	}

	private ServientService(Context context) throws WotException, JSONException, URISyntaxException, ExecutionException, InterruptedException {
		wot = startWot(context);
		consumedDashboard = wot.fetch(directoryUrl + "/things/crwsns:dashboard")
				.thenApply(thing -> wot.consume(thing)).get();
		Observer<Boolean> connectivityObserver = connected -> {
			MainViewModel.addLoading();
			if (connected) {
				//Connected
				if (instance == null) {
					try {
						wot = startWot(context);
						consumedDashboard = wot.fetch(directoryUrl + "/things/dashboard")
								.thenApply(thing -> wot.consume(thing)).get();
						if (backupExposedDevice != null) {
							exposeDevice().get();
							//TODO
						}
						instance = this;
					} catch (WotException | JSONException | ExecutionException |
							 URISyntaxException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			} else {
				//Disconnected
				instance = null;
				pingTask.cancel();
				backupExposedDevice = exposedDevice;
				try {
					removeExposedDevice().get();
				} catch (ExecutionException | InterruptedException e) {
					e.printStackTrace();
				}
				consumedDashboard = null;
				exposedDevice = null;
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
				} catch (WotException | JSONException | ExecutionException | URISyntaxException |
						 InterruptedException e) {
					throw new CompletionException(new Exception("Error initializing servient"));
				}
			}
			return instance;
		});
	}

	private static void setupStatic(Context context) {
		resources = context.getResources();
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		directoryUrl = "http://vz5nc4zqbrgzpn13.myfritz.net:8081";
		fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
		timer = new Timer();
		pingTask = null;
	}

	private CompletableFuture<Void> removeExposedDevice() {
		if (exposedDevice != null) {
			CompletableFuture<Void> toReturn = new CompletableFuture<>();
			wot.destroyExposedThingById(exposedDevice.getId()).thenRun(() -> {
				toReturn.complete(null);
				exposedDevice = null;
			}).exceptionally(throwable -> {
				toReturn.completeExceptionally(throwable);
				return null;
			});
			return toReturn;
		}
		return CompletableFuture.completedFuture(null);
	}

	public CompletableFuture<Void> logout() {
		CompletableFuture<Void> toReturn = new CompletableFuture<>();
		try {
			wot.unregister(directoryUrl, exposedDevice).thenApply((_result) -> {
				pingTask.cancel();
				backupExposedDevice = null;
				return removeExposedDevice();
			}).thenRun(() -> toReturn.complete(null)).exceptionally(throwable -> {
				toReturn.completeExceptionally(throwable);
				return null;
			});
		} catch (URISyntaxException e) {
			toReturn.completeExceptionally(e);
		}
		return toReturn;
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

	private Wot startWot(Context context) throws WotException, JSONException {
		return startWot(generateConfig(), context);
	}

	private Wot startWot(Config config, Context context) throws WotException {
		return new DefaultWot(config, context);
	}

	private Config generateConfig() throws JSONException {
		// Servers
		JSONArray servers = new JSONArray();
		// LA-MQTT
		/*
		JSONObject lamqtt = new JSONObject();
		servers.put("com.example.wot_servient.la_mqtt.server.LAMqttProtocolServer");
		lamqtt.put("uri", "mqtt://vz5nc4zqbrgzpn13.myfritz.net:1883");
		lamqtt.put("clientId", "testClientSmartphone");
		//lamqtt.put("locationProvider", (LAMqttLocationProvider) () -> new Location(5, 3));
		*/
		// MQTT
		JSONObject mqtt = new JSONObject();
		servers.put("com.example.wot_servient.mqtt.server.MqttProtocolServer");
		mqtt.put("broker", sharedPreferences.getString("mqttServerBroker", resources.getString(R.string.mqttBroker)));
		mqtt.put("bind-port", sharedPreferences.getString("mqttServerPort", resources.getString(R.string.mqttPort)));
		mqtt.put("client-id", sharedPreferences.getString("mqttServerClientId", resources.getString(R.string.mqttClientId)));
		mqtt.put("username", sharedPreferences.getString("mqttServerUsername", resources.getString(R.string.mqttUsername)));
		mqtt.put("password", sharedPreferences.getString("mqttServerPassword ", resources.getString(R.string.mqttPassword)));
		mqtt.put("all-TD-topic", sharedPreferences.getString("mqttServerAllTDTopic", resources.getString(R.string.mqttAllTDTopic)));

		// Client factories
		JSONArray clientFactories = new JSONArray();
		// HTTP
		clientFactories.put("com.example.wot_servient.http.client.HttpProtocolClientFactory");
		// COAP
		clientFactories.put("com.example.wot_servient.coap.client.CoapProtocolClientFactory");
		// Finalization
		JSONObject servient = new JSONObject();
		servient.put("servers", servers);
		servient.put("credentials", new JSONObject());
		servient.put("client-factories", clientFactories);
		servient.put("mqtt", mqtt);
		//servient.put("la_mqtt", lamqtt);
		JSONObject wot = new JSONObject();
		wot.put("servient", servient);
		JSONObject config = new JSONObject();
		config.put("wot", wot);
		return generateConfig(config);
	}

	private Config generateConfig(JSONObject config) {
		return ConfigFactory.parseString(config.toString());
	}

	public CompletableFuture<String> register(String email, String password) {
		return access(email, password, false);
	}

	public CompletableFuture<String> login(String email, String password) {
		return access(email, password, true);
	}

	private CompletableFuture<String> access(String email, String password, Boolean isLogin) {
		CompletableFuture<String> toReturn = new CompletableFuture<>();
		Map<String, Object> params = new HashMap<>();
		params.put("email", email);
		params.put("password", password);
		String action = isLogin ? "loginUser" : "registerNewUser";
		if (consumedDashboard != null && consumedDashboard.getActions().containsKey(action)) {
			consumedDashboard.getAction(action).invoke(params).thenAcceptAsync(result -> {
				Boolean authenticated = (Boolean) ((HashMap) result).get("authenticated");
				if (Boolean.TRUE.equals(authenticated)) {
					String bearer = (String) ((HashMap) result).get("bearer");
					if (!IS_ACTIVITY_RUNNING) {
						exposeDevice().thenRun(() -> toReturn.complete(bearer))
								.exceptionally(throwable -> {
									toReturn.completeExceptionally(throwable);
									return null;
								});
					} else {
						toReturn.complete(bearer);
					}
				} else {
					String error = (String) ((HashMap) result).get("error");
					toReturn.completeExceptionally(new Exception(error));
				}
			}).exceptionally(throwable -> {
				toReturn.completeExceptionally(new Exception("Error connecting to server"));
				return null;
			});
		} else {
			toReturn.completeExceptionally(new Exception("Consumed dashboard null, or not containing action " + action));
		}
		return toReturn;
	}

	public CompletableFuture<User> fetchData(String bearer) {
		return CompletableFuture.supplyAsync(() -> {
			if (consumedDashboard == null)
				throw new CompletionException(new Exception("Servient not available"));
			Map<String, Object> params = new HashMap<>();
			params.put("bearer", bearer);
			String action = "retrieveUserData";
			HashMap result;
			try {
				result = (HashMap) consumedDashboard.getAction(action).invoke(params).get();
			} catch (ExecutionException | InterruptedException e) {
				throw new CompletionException(e);
			}
			if (result != null) {
				String email = (String) result.get("email");
				Integer points = (Integer) result.get("points");
				ArrayList completedCampaigns = (ArrayList) result.get("completed_campaigns");
				return new User(email, points, completedCampaigns);
			} else
				throw new CompletionException(new Exception("Error during the request"));
		});
	}

	public CompletableFuture<ArrayList<Campaign>> fetchCampaigns() {
		return CompletableFuture.supplyAsync(() -> {
			if (consumedDashboard == null)
				throw new CompletionException(new Exception("Servient not available"));
			String property = "campaigns";
			ArrayList result;
			try {
				result = (ArrayList) consumedDashboard.getProperty(property).read().get();
			} catch (Exception e) {
				throw new CompletionException(e);
			}
			if (result != null) {
				ArrayList<Campaign> campaigns = new ArrayList<>();
				for (int i = 0; i < result.size(); i++) {
					Campaign campaign = new Campaign((HashMap) result.get(i));
					campaigns.add(campaign);
				}
				return campaigns;
			} else
				throw new CompletionException(new Exception("Error during the request"));
		});
	}

	private CompletableFuture<Void> exposeDevice() {
		return CompletableFuture.runAsync(() -> {
			if (wot == null || consumedDashboard == null)
				throw new CompletionException(new Exception("Servient not available"));
			try {
				String deviceId = sharedPreferences.getString("deviceId", null);
				if (deviceId == null) {
					deviceId = "smartphone_" + UUID.randomUUID().toString();
					sharedPreferences.edit().putString("deviceId", deviceId).apply();
				}
				if (backupExposedDevice == null) {
					Thing thing = new ThingBuilder().setTitle(deviceId).setId(deviceId).build();
					exposedDevice = wot.produce(thing);
					exposedDevice.addProperty("ping", new ThingPropertyBuilder().setReadOnly(true)
							.setWriteOnly(false).setObservable(false).setType(null).build(), null);
				} else {
					exposedDevice = wot.produce(backupExposedDevice);
				}
				exposedDevice = exposedDevice.expose().get();
				wot.register(directoryUrl, exposedDevice).get();
				Log.d("MainServient", "Correctly registered thing");
				Map<String, Object> params = new HashMap<>();
				params.put("deviceId", deviceId);
				pingTask = new TimerTask() {
					@Override
					public void run() {
						try {
							consumedDashboard.getAction("ping").invoke(params).get();
						} catch (ExecutionException | InterruptedException e) {
							e.printStackTrace();
						}
					}
				};
				timer.scheduleAtFixedRate(pingTask, 0, 10000);
			} catch (WotException | URISyntaxException | ExecutionException |
					 InterruptedException e) {
				throw new CompletionException(e);
			}
		});
	}

	public CompletableFuture<HashMap> applyToCampaign(String bearer, Campaign campaign, SubmissionMode mode, Integer interval) {
		return CompletableFuture.supplyAsync(() -> {
			if (consumedDashboard == null)
				throw new CompletionException(new Exception("Servient not available"));
			try {
				String deviceId = sharedPreferences.getString("deviceId", null);
				Map<String, Object> params = new HashMap<>();
				params.put("bearer", bearer);
				params.put("deviceId", deviceId);
				params.put("campaignId", campaign.getId());
				params.put("interval", interval);
				String actionName = "";
				exposeForCampaign(campaign, mode).get();
				switch (mode) {
					case AUTOMATIC:
						actionName = "applyToCampaignPull";
						break;
					case AUTO_WITH_PREF:
					case MANUAL:
						actionName = "applyToCampaignPush";
						break;
				}
				return (HashMap) consumedDashboard.getAction(actionName).invoke(params).get();
			} catch (ExecutionException | InterruptedException e) {
				throw new CompletionException(e);
			}
		});
	}

	@SuppressLint("MissingPermission")
	private CompletableFuture<Void> exposeForCampaign(Campaign campaign, SubmissionMode mode) {
		return CompletableFuture.runAsync(() -> {
			if (exposedDevice == null || wot == null)
				throw new CompletionException(new Exception("Servient not available"));
			String propertyName = campaign.getId();
			Supplier<CompletableFuture<Object>> supplier = null;
			if (mode == SubmissionMode.AUTOMATIC) {
				switch (campaign.getType()) {
					case "location":
						supplier = () -> {
							CompletableFuture<Object> completableFuture = new CompletableFuture<>();
							fusedLocationClient.getCurrentLocation(new CurrentLocationRequest.Builder().setMaxUpdateAgeMillis(0)
																		   .build(), null)
									.addOnSuccessListener(completableFuture::complete);
							return completableFuture;
						};
						break;
					case "gps":
						supplier = () -> CompletableFuture.supplyAsync(() -> {
							Map<String, Object> result = new HashMap<>();
							String json = "";
							try {
								json = new ObjectMapper().writeValueAsString(result);
							} catch (JsonProcessingException e) {
								e.printStackTrace();
							}
							return json;
						});
						break;
					default:
						break;
				}
			}
			try {
				exposedDevice.addProperty(propertyName, new ThingPropertyBuilder().setReadOnly(true)
						.setWriteOnly(false).setObservable(false).setName(propertyName)
						.setType("object").build(), supplier, null);
				exposedDevice = exposedDevice.expose().get();
				wot.register(directoryUrl, exposedDevice).get();
			} catch (ExecutionException | InterruptedException | URISyntaxException e) {
				throw new CompletionException(e);
			}
		});
	}

	public CompletableFuture<HashMap> sendManualData(String bearer, String campaignId, DataSubmission data) {
		return CompletableFuture.supplyAsync(() -> {
			if (consumedDashboard == null)
				throw new CompletionException(new Exception("Servient not available"));
			Map<String, Object> params = new HashMap<>();
			String deviceId = sharedPreferences.getString("deviceId", null);
			params.put("bearer", bearer);
			params.put("deviceId", deviceId);
			params.put("campaignId", campaignId);
			params.put("data", data);
			try {
				return (HashMap) consumedDashboard.getAction("sendPushCampaignData").invoke(params)
						.get();
			} catch (ExecutionException | InterruptedException e) {
				throw new CompletionException(e);
			}
		});
	}

	public CompletableFuture<HashMap> leaveCampaign(String bearer, Campaign campaign) {
		return CompletableFuture.supplyAsync(() -> {
			if (consumedDashboard == null)
				throw new CompletionException(new Exception("Servient not available"));
			try {
				String deviceId = sharedPreferences.getString("deviceId", null);
				Map<String, Object> params = new HashMap<>();
				params.put("bearer", bearer);
				params.put("deviceId", deviceId);
				params.put("campaignId", campaign.getId());
				String actionName = "leaveCampaign";
				exposedDevice.removeProperty(campaign.getId());
				exposedDevice = exposedDevice.expose().get();
				wot.register(directoryUrl, exposedDevice).get();
				return (HashMap) consumedDashboard.getAction(actionName).invoke(params).get();
			} catch (ExecutionException | InterruptedException | URISyntaxException e) {
				throw new CompletionException(e);
			}
		});
	}
}
