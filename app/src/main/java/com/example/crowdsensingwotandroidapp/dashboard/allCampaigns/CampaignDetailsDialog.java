package com.example.crowdsensingwotandroidapp.dashboard.allCampaigns;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.dashboard.DashboardViewModel;
import com.example.crowdsensingwotandroidapp.databinding.DialogCampaignDetailsBinding;
import com.example.crowdsensingwotandroidapp.utils.campaign.Campaign;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Map;

public class CampaignDetailsDialog extends DialogFragment {

	private Campaign campaign;
	private DashboardViewModel dashboardViewModel;
	private DialogCampaignDetailsBinding binding;
	private ActivityResultLauncher<String[]> activityResultLauncher;
	private AlertDialog dialog;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
		binding = DialogCampaignDetailsBinding.inflate(getLayoutInflater());
		builder.setView(binding.getRoot());
		dialog = builder.create();
		dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
		activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
			boolean permissionsGranted = true;
			for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
				permissionsGranted = permissionsGranted && entry.getValue();
			}
			if (permissionsGranted) {
				initDialog();
			} else {
				binding.campaignDetailsActionButton.setText(R.string.permissionNotGranted);
				binding.campaignDetailsActionButton.setTextColor(Color.WHITE);
				binding.campaignDetailsActionButton.setBackgroundColor(Color.parseColor("#707973"));
				binding.campaignDetailsActionButton.setEnabled(false);
			}
		});
		campaign = dashboardViewModel.getApplicationSelectedCampaign().getValue();
		if (campaign == null)
			dismiss();
		else
			initDialog();
		return dialog;
	}

	@SuppressLint("MissingPermission")
	private void initDialog() {
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
					NavHostFragment.findNavController(this)
							.navigate(R.id.action_campaignDetailsDialog_to_campaignApplyDialog);
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

	@Override
	public void onCancel(@NonNull DialogInterface dialog) {
		dashboardViewModel.getApplicationSelectedCampaign().setValue(null);
		NavHostFragment.findNavController(this)
				.navigate(R.id.action_campaignDetailsDialog_to_campaignsFragment);
		super.onCancel(dialog);
	}
}
