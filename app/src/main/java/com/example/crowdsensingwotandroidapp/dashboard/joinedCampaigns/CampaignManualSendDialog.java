package com.example.crowdsensingwotandroidapp.dashboard.joinedCampaigns;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.dashboard.DashboardViewModel;
import com.example.crowdsensingwotandroidapp.databinding.DialogCampaignManualSendBinding;
import com.example.crowdsensingwotandroidapp.utils.campaign.AppliedCampaign;
import com.example.crowdsensingwotandroidapp.utils.campaign.submission.LocationSubmission;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CampaignManualSendDialog extends DialogFragment {

	private DialogCampaignManualSendBinding binding;
	private DashboardViewModel dashboardViewModel;
	private AppliedCampaign campaign;
	private AlertDialog dialog;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
		binding = DialogCampaignManualSendBinding.inflate(getLayoutInflater());
		builder.setView(binding.getRoot());
		dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
		campaign = dashboardViewModel.getUserSelectedCampaign().getValue();
		if (campaign != null) {
			initDialog();
		} else {
			dismiss();
		}
		dialog = builder.create();
		return dialog;
	}

	private void initDialog() {
		switch (campaign.getType()) {
			case "location":
				binding.inputManualSendWrapper.setVisibility(View.GONE);
				binding.locationManualSendWrapper.setVisibility(View.VISIBLE);
				binding.manualSendLatitude.setText("Current Latitude");
				binding.manualSendLongitude.setText("Current Longitude");
				dashboardViewModel.getManualData().setValue(new LocationSubmission(300.0, 200.0));
				break;
			default: {
				dialog.cancel();
			}
		}
		binding.manualSendCancelBtn.setOnClickListener(v -> dialog.cancel());
		binding.manualSendConfirmBtn.setOnClickListener(v -> {
			NavHostFragment.findNavController(this)
					.navigate(R.id.action_campaignManualSendDialog_to_homeDetailsFragment);
			dashboardViewModel.sendDataManual();
		});
	}

	@Override
	public void onCancel(@NonNull DialogInterface dialog) {
		NavHostFragment.findNavController(this)
				.navigate(R.id.action_campaignManualSendDialog_to_homeDetailsFragment);
		super.onDismiss(dialog);
	}
}

