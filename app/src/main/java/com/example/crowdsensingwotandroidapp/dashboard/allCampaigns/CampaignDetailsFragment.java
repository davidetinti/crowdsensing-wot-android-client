package com.example.crowdsensingwotandroidapp.dashboard.allCampaigns;

import android.Manifest;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.crowdsensingwotandroidapp.MainViewModel;
import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.dashboard.DashboardViewModel;
import com.example.crowdsensingwotandroidapp.databinding.FragmentCampaignDetailsBinding;
import com.example.crowdsensingwotandroidapp.utils.campaign.Campaign;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class CampaignDetailsFragment extends DialogFragment {

	private Campaign campaign;
	private DashboardViewModel dashboardViewModel;
	private FragmentCampaignDetailsBinding binding;
	private ActivityResultLauncher<String[]> activityResultLauncher;

	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentCampaignDetailsBinding.inflate(inflater, container, false);
		activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
			boolean permissionsGranted = true;
			for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
				permissionsGranted = permissionsGranted && entry.getValue();
			}
			if (permissionsGranted) {
				initView();
			} else {
				binding.campaignDetailsActionButton.setText(R.string.permissionNotGranted);
				binding.campaignDetailsActionButton.setTextColor(Color.WHITE);
				binding.campaignDetailsActionButton.setBackgroundColor(Color.parseColor("#707973"));
				binding.campaignDetailsActionButton.setEnabled(false);
			}
		});
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar())
				.setTitle("");
		dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
		campaign = dashboardViewModel.getApplicationSelectedCampaign().getValue();
		if (campaign == null)
			MainViewModel.getErrorToShow().setValue(new Exception("Error with data"));
		else
			initView();
	}

	@SuppressLint("MissingPermission")
	private void initView() {
		binding.campaignDetailsTitle.setText(campaign.getTitle());
		binding.campaignDetailsOrganization.setText(campaign.getOrganization());
		binding.campaignDetailsDescription.setText(campaign.getDescription());
		String sensorText = "";
		switch (campaign.getType()) {
			case "location":
				sensorText += "\u2022 " + getString(R.string.campaignDetailsRequiredLocation);
				break;
			case "gps":
				sensorText += "\u2022 " + getString(R.string.campaignDetailsRequiredGPS);
				break;
		}
		binding.campaignDetailsSensor.setText(sensorText);
		String intervalText = getString(R.string.campaignDetailsInterval) + " " + dashboardViewModel.calculateTimeString(campaign.getIdealSubmissionInterval()) + ".";
		binding.campaignDetailsInterval.setText(intervalText);
		String submissionsText = getString(R.string.campaignDetailsSubmissions) + " " + campaign.getSubmissionRequired() + ".";
		binding.campaignDetailsSubmissions.setText(submissionsText);
		String pointsText = getString(R.string.campaignDetailsPoints) + " " + campaign.getPoints() + " " + getString(R.string.points) + " CRWSNS.";
		binding.campaignDetailsPoints.setText(pointsText);
		// Button
		if (dashboardViewModel.verifyDeviceCapability(campaign.getType())) {
			if (dashboardViewModel.verifyPermission(campaign.getType())) {
				binding.campaignDetailsActionButton.setText(R.string.participateButtonText);
				binding.campaignDetailsActionButton.setBackgroundColor(Color.parseColor("#254B5B"));
				binding.campaignDetailsActionButton.setOnClickListener(v -> {
					NavController controller = Navigation.findNavController(v);
					controller.navigate(R.id.action_campaignDetailsFragment_to_campaignApplyDialog);
				});
			} else {
				binding.campaignDetailsActionButton.setText(R.string.authorizeButtonText);
				binding.campaignDetailsActionButton.setBackgroundColor(Color.parseColor("#BA1A1A"));
				binding.campaignDetailsActionButton.setOnClickListener(v -> {
					ArrayList<String> permissions = new ArrayList<>();
					switch (campaign.getType()) {
						case "location":
							permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
							permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
							break;
						case "temperature":
						default:
					}
					activityResultLauncher.launch(permissions.toArray(new String[0]));
				});
			}
		} else {
			binding.campaignDetailsActionButton.setText(R.string.unsuitableButtonText);
			binding.campaignDetailsActionButton.setTextColor(Color.WHITE);
			binding.campaignDetailsActionButton.setBackgroundColor(Color.parseColor("#707973"));
			binding.campaignDetailsActionButton.setEnabled(false);
		}
	}
}
