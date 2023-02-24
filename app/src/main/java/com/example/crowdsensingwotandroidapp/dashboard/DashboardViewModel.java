package com.example.crowdsensingwotandroidapp.dashboard;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.crowdsensingwotandroidapp.MainViewModel;
import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.ServientService;
import com.example.crowdsensingwotandroidapp.utils.User;
import com.example.crowdsensingwotandroidapp.utils.campaign.AppliedCampaign;
import com.example.crowdsensingwotandroidapp.utils.campaign.Campaign;
import com.example.crowdsensingwotandroidapp.utils.campaign.SubmissionMode;
import com.example.crowdsensingwotandroidapp.utils.campaign.submission.DataSubmission;
import com.example.crowdsensingwotandroidapp.utils.network.NetworkStateManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

public class DashboardViewModel extends AndroidViewModel {

	// List of all campaigns
	private final MutableLiveData<ArrayList<Campaign>> allCampaigns = new MutableLiveData<>(null);
	// Campaign currently selected for application
	private final MutableLiveData<Campaign> applicationSelectedCampaign = new MutableLiveData<>(null);
	// Interval currently selected during campaign application
	private final MutableLiveData<Integer> applicationSelectedInterval = new MutableLiveData<>(null);
	// Submission mode currently selected during campaign application
	private final MutableLiveData<SubmissionMode> applicationSelectedSubmissionMode = new MutableLiveData<>(null);
	// Boolean representing a successful application to a campaign
	private final MutableLiveData<Boolean> applicationResult = new MutableLiveData<>(null);
	// List of user's applied campaign IDs
	private final MutableLiveData<ArrayList<AppliedCampaign>> userCampaigns = new MutableLiveData<>(new ArrayList<>());
	// Campaign currently selected for data submission under user tab
	private final MutableLiveData<AppliedCampaign> userSelectedCampaign = new MutableLiveData<>(null);
	// Data to be submitted manual
	private final MutableLiveData<DataSubmission> manualData = new MutableLiveData<>(null);
	// Response from a manual data submission
	private final MutableLiveData<Integer> manualDataSubmission = new MutableLiveData<>(null);
	// Points earned from a completed campaign
	private final MutableLiveData<Integer> completedCampaignPoints = new MutableLiveData<>(null);
	// User data
	private final MutableLiveData<User> user = new MutableLiveData<>(null);
	// Reference to main ViewModel used to add or remove loading

	public DashboardViewModel(@NonNull Application application) {
		super(application);
	}

	public MutableLiveData<ArrayList<Campaign>> getAllCampaigns() {
		return allCampaigns;
	}

	public MutableLiveData<Campaign> getApplicationSelectedCampaign() {
		return applicationSelectedCampaign;
	}

	public MutableLiveData<Integer> getApplicationSelectedInterval() {
		return applicationSelectedInterval;
	}

	public MutableLiveData<SubmissionMode> getApplicationSelectedSubmissionMode() {
		return applicationSelectedSubmissionMode;
	}

	public MutableLiveData<Boolean> getApplicationResult() {
		return applicationResult;
	}

	public MutableLiveData<ArrayList<AppliedCampaign>> getUserCampaigns() {
		return userCampaigns;
	}

	public MutableLiveData<AppliedCampaign> getUserSelectedCampaign() {
		return userSelectedCampaign;
	}

	public MutableLiveData<DataSubmission> getManualData() {
		return manualData;
	}

	public MutableLiveData<Integer> getManualDataSubmission() {
		return manualDataSubmission;
	}

	public MutableLiveData<Integer> getCompletedCampaignPoints() {
		return completedCampaignPoints;
	}

	public MutableLiveData<User> getUser() {
		return user;
	}

	/**
	 * Logout the user. Called if the dashboard failed to initialize or if the user request the logout.
	 */
	public void logout(boolean isRequestedByUser) {
		if (Boolean.TRUE.equals(NetworkStateManager.getInstance().getNetworkConnectivityStatus()
										.getValue())) {
			MainViewModel.addLoading();
			ServientService.getInstance(getApplication().getApplicationContext())
					.thenCompose(ServientService::logout)
					.thenAccept(result -> new Handler(Looper.getMainLooper()).post(() -> {
						MainViewModel.getBearerToken().setValue(null);
						allCampaigns.setValue(null);
						applicationSelectedCampaign.setValue(null);
						applicationSelectedInterval.setValue(null);
						applicationSelectedSubmissionMode.setValue(null);
						userCampaigns.setValue(null);
						userSelectedCampaign.setValue(null);
						manualData.setValue(null);
						manualDataSubmission.setValue(null);
						user.setValue(null);
						if (isRequestedByUser) {
							// Clear login preferences
							SharedPreferences preferences = getApplication().getSharedPreferences("loginPreferences", Context.MODE_PRIVATE);
							preferences.edit().clear().apply();
						}
					})).exceptionally(throwable -> {
						new Handler(Looper.getMainLooper()).post(() -> MainViewModel.getErrorToShow()
								.setValue(throwable));
						return null;
					})
					.thenRun(() -> new Handler(Looper.getMainLooper()).post(MainViewModel::removeLoading));
		} else {
			MainViewModel.getErrorToShow()
					.setValue(new Exception("Impossible to connect. Try again later."));
		}
	}

	/**
	 * Used to get from the server user data, campaign data. It also update user joined and completed campaigns.
	 */
	public void fetchDashboardData() {
		if (Boolean.TRUE.equals(NetworkStateManager.getInstance().getNetworkConnectivityStatus()
										.getValue())) {
			MainViewModel.addLoading();
			ServientService.getInstance(getApplication().getApplicationContext())
					.thenAccept(servient -> {
						String bearer = MainViewModel.getBearerToken().getValue();
						servient.fetchData(bearer).thenCompose(userResult -> {
									new Handler(Looper.getMainLooper()).post(() -> user.setValue(userResult));
									updateCompletedCampaigns();
									return servient.fetchCampaigns();
								}).thenAccept(campaigns -> new Handler(Looper.getMainLooper()).post(() -> {
									allCampaigns.setValue(campaigns);
									fetchUserCampaigns();
								})).exceptionally(throwable -> {
									new Handler(Looper.getMainLooper()).post(() -> {
										MainViewModel.getErrorToShow().setValue(throwable);
										logout(false);
									});
									return null;
								})
								.thenRun(() -> new Handler(Looper.getMainLooper()).post(MainViewModel::removeLoading));
					});
		} else {
			MainViewModel.getErrorToShow()
					.setValue(new Exception("Impossible to connect. Try again later."));
		}
	}

	/**
	 * Remove the campaign ids saved in sharedPreferences under user bearer if these ids
	 * are present in user.completedCampaigns
	 */
	private void updateCompletedCampaigns() {
		String bearer = MainViewModel.getBearerToken().getValue();
		User _user = user.getValue();
		if (bearer != null && _user != null) {
			SharedPreferences preferences = getApplication().getSharedPreferences(bearer, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = preferences.edit();
			user.getValue().getCompletedCampaigns().forEach(editor::remove);
			editor.apply();
		}
	}

	/**
	 * Populate the userCampaign arrayList with the campaigns of allCampaigns whose ids are
	 * saved in the sharedPreferences under the user bearer.
	 */
	private void fetchUserCampaigns() {
		String bearer = MainViewModel.getBearerToken().getValue();
		if (bearer != null && allCampaigns.getValue() != null) {
			ArrayList<AppliedCampaign> newUserCampaigns = new ArrayList<>();
			// Get campaign ids saved under preferences at key "user bearer"
			SharedPreferences preferences = getApplication().getSharedPreferences(bearer, Context.MODE_PRIVATE);
			Map<String, ?> savedCampaignData = preferences.getAll();
			// Filter all campaign data taking only the campaign with ids in savedUserCampaignIds
			ArrayList<Campaign> rawUserCampaigns = (ArrayList<Campaign>) allCampaigns.getValue()
					.stream().filter(campaign -> savedCampaignData.containsKey(campaign.getId()))
					.collect(Collectors.toList());
			// Create all the new applied campaign instances and add them to newUserCampaigns array
			rawUserCampaigns.forEach(campaign -> {
				ObjectMapper mapper = new ObjectMapper();
				try {
					Map campaignData = mapper.readValue((String) savedCampaignData.get(campaign.getId()), Map.class);
					Integer modeRaw = (Integer) campaignData.get("mode");
					SubmissionMode mode = SubmissionMode.values()[Objects.requireNonNull(modeRaw)];
					Integer interval = (Integer) campaignData.get("interval");
					AppliedCampaign userCampaign = new AppliedCampaign(campaign, mode, interval);
					newUserCampaigns.add(userCampaign);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			// Set new value for userCampaigns
			userCampaigns.setValue(newUserCampaigns);
		}
	}

	/**
	 * Utility function used to convert a number into a string representing a time period,
	 * considering the number passed in seconds.
	 */
	public String calculateTimeString(@Nullable Number idealSubmissionInterval) {
		String result = "";
		if (idealSubmissionInterval == null)
			return result;
		int value = idealSubmissionInterval.intValue();
		int hours = value / 3600;
		int minutes = (value % 3600) / 60;
		int seconds = (value % 3600) % 60;
		if (hours == 1) {
			result += hours + " " + getApplication().getString(R.string.hour);
		} else if (hours > 1) {
			result += hours + " " + getApplication().getString(R.string.hours);
		}
		if (minutes == 1) {
			if (hours > 0 && seconds == 0)
				result += " " + getApplication().getString(R.string.and) + " ";
			else if (hours > 0 && seconds > 0)
				result += ", ";
			result += minutes + " " + getApplication().getString(R.string.minute);
		} else if (minutes > 1) {
			if (hours > 0 && seconds == 0)
				result += " " + getApplication().getString(R.string.and) + " ";
			else if (hours > 0 && seconds > 0)
				result += ", ";
			result += minutes + " " + getApplication().getString(R.string.minutes);
		}
		if (seconds == 1) {
			if (hours > 0 || minutes > 0)
				result += " " + getApplication().getString(R.string.and) + " ";
			result += seconds + " " + getApplication().getString(R.string.second);
		} else if (seconds > 1) {
			if (hours > 0 || minutes > 0)
				result += " " + getApplication().getString(R.string.and) + " ";
			result += seconds + " " + getApplication().getString(R.string.seconds);
		}
		return result;
	}

	public boolean verifyDeviceCapability(String type) {
		SensorManager sensorManager = (SensorManager) getApplication().getSystemService(Context.SENSOR_SERVICE);
		switch (type) {
			case "temperature":
				return sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null;
			case "location":
			default:
				return true;
		}
	}

	public boolean verifyPermission(String type) {
		ArrayList<String> permissions = new ArrayList<>();
		switch (type) {
			case "location":
				permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
				permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
				break;
			case "temperature":
			default:
		}
		return checkPermissionGranted(permissions);
	}

	private boolean checkPermissionGranted(ArrayList<String> permissions) {
		ArrayList<String> permissionsNotGranted = new ArrayList<>();
		for (int i = 0; i < permissions.size(); i++) {
			if (getApplication().checkSelfPermission(permissions.get(i)) != PackageManager.PERMISSION_GRANTED)
				permissionsNotGranted.add(permissions.get(i));
		}
		return permissionsNotGranted.isEmpty();
	}

	/**
	 * Called when a user join a campaign. Used to save the campaign data under sharedPreferences to enable
	 * continuity between session in the application.
	 */
	private void saveCampaignApplication(Campaign campaign, String bearer, SubmissionMode mode, Integer interval) throws CompletionException {
		SharedPreferences preferences = getApplication().getSharedPreferences(bearer, Context.MODE_PRIVATE);
		if (preferences.contains(campaign.getId()))
			throw new CompletionException(new Exception("User already applied to this campaign"));
		SharedPreferences.Editor editor = preferences.edit();
		Map<String, Object> campaignData = new HashMap<>();
		campaignData.put("mode", mode.ordinal());
		campaignData.put("interval", interval);
		String campaignDataAsString;
		try {
			campaignDataAsString = new ObjectMapper().writeValueAsString(campaignData);
		} catch (JsonProcessingException e) {
			throw new CompletionException(new Exception());
		}
		editor.putString(campaign.getId(), campaignDataAsString);
		editor.apply();
	}

	/**
	 * Called when a user request to apply to a campaign. Send the request to the server and save the
	 * campaign info.
	 */
	public void applyToCampaign() {
		if (Boolean.TRUE.equals(NetworkStateManager.getInstance().getNetworkConnectivityStatus()
										.getValue())) {
			Campaign campaign = applicationSelectedCampaign.getValue();
			applicationSelectedCampaign.setValue(null);
			if (campaign != null) {
				String bearer = MainViewModel.getBearerToken().getValue();
				SubmissionMode mode = applicationSelectedSubmissionMode.getValue();
				applicationSelectedSubmissionMode.setValue(null);
				Integer interval = null;
				if (mode != SubmissionMode.MANUAL) {
					boolean isAutomatic = mode == SubmissionMode.AUTOMATIC;
					Integer customInterval = applicationSelectedInterval.getValue();
					Integer idealInterval = (Integer) campaign.getIdealSubmissionInterval();
					interval = isAutomatic ? idealInterval : customInterval;
				}
				applicationSelectedInterval.setValue(null);
				Integer finalInterval = interval;
				if (!Objects.requireNonNull(user.getValue()).getCompletedCampaigns()
						.contains(campaign.getId())) {
					MainViewModel.addLoading();
					ServientService.getInstance(getApplication().getApplicationContext())
							.thenCompose(servient -> servient.applyToCampaign(bearer, campaign, mode, finalInterval))
							.thenAccept(result -> {
								Boolean isApplied = (Boolean) result.get("ok");
								if (Boolean.FALSE.equals(isApplied))
									throw new CompletionException(new Exception((String) result.get("error")));
								saveCampaignApplication(campaign, bearer, mode, finalInterval);
								new Handler(Looper.getMainLooper()).post(() -> {
									applicationResult.setValue(true);
									fetchUserCampaigns();
								});
							}).exceptionally(throwable -> {
								new Handler(Looper.getMainLooper()).post(() -> MainViewModel.getErrorToShow()
										.setValue(throwable));
								return null;
							})
							.thenRun(() -> new Handler(Looper.getMainLooper()).post(MainViewModel::removeLoading));
				} else {
					MainViewModel.getErrorToShow()
							.setValue(new Exception("User already completed this campaign!"));
				}
			} else {
				MainViewModel.getErrorToShow()
						.setValue(new Exception("Error applying to campaign"));
			}
		} else {
			MainViewModel.getErrorToShow()
					.setValue(new Exception("Impossible to connect. Try again later."));
		}
	}

	public void sendDataManual() {
		String bearer = MainViewModel.getBearerToken().getValue();
		Campaign campaign = userSelectedCampaign.getValue();
		DataSubmission data = manualData.getValue();
		if (campaign != null) {
			MainViewModel.addLoading();
			ServientService.getInstance(getApplication().getApplicationContext())
					.thenCompose(servient -> servient.sendManualData(bearer, campaign.getId(), data))
					.thenAccept(result -> {
						Boolean isOk = (Boolean) result.get("ok");
						if (Boolean.FALSE.equals(isOk))
							throw new CompletionException(new Exception((String) result.get("error")));
						Integer remaining = (Integer) result.get("remaining");
						if (remaining != null) {
							if (remaining == 0) {
								SharedPreferences preferences = getApplication().getSharedPreferences(bearer, Context.MODE_PRIVATE);
								SharedPreferences.Editor editor = preferences.edit();
								editor.remove(campaign.getId());
								editor.apply();
								new Handler(Looper.getMainLooper()).post(() -> {
									completedCampaignPoints.setValue(campaign.getPoints()
																			 .intValue());
									fetchDashboardData();
								});
							}
							new Handler(Looper.getMainLooper()).post(() -> manualDataSubmission.setValue(remaining));
						} else
							throw new CompletionException(new Exception("Error getting remaining submissions"));
					}).exceptionally(throwable -> {
						new Handler(Looper.getMainLooper()).post(() -> MainViewModel.getErrorToShow()
								.setValue(throwable));
						return null;
					})
					.thenRun(() -> new Handler(Looper.getMainLooper()).post(MainViewModel::removeLoading));
		} else {
			MainViewModel.getErrorToShow().setValue(new Exception("Error sending data"));
		}
	}

	public void abandonUserSelectedCampaign() {
		String bearer = MainViewModel.getBearerToken().getValue();
		AppliedCampaign campaign = userSelectedCampaign.getValue();
		if (campaign != null && bearer != null) {
			MainViewModel.addLoading();
			ServientService.getInstance(getApplication().getApplicationContext())
					.thenCompose(servient -> servient.leaveCampaign(bearer, campaign))
					.thenAccept(result -> {
						Boolean isOk = (Boolean) result.get("ok");
						if (Boolean.FALSE.equals(isOk))
							throw new CompletionException(new Exception((String) result.get("error")));
						SharedPreferences preferences = getApplication().getSharedPreferences(bearer, Context.MODE_PRIVATE);
						SharedPreferences.Editor editor = preferences.edit();
						editor.remove(campaign.getId());
						editor.apply();
						new Handler(Looper.getMainLooper()).post(this::fetchUserCampaigns);
					}).exceptionally(throwable -> {
						new Handler(Looper.getMainLooper()).post(() -> MainViewModel.getErrorToShow()
								.setValue(throwable));
						return null;
					})
					.thenRun(() -> new Handler(Looper.getMainLooper()).post(MainViewModel::removeLoading));
		} else {
			MainViewModel.getErrorToShow().setValue(new Exception("Error abandoning to campaign"));
		}
	}
}