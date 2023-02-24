package com.example.crowdsensingwotandroidapp.dashboard.joinedCampaigns;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.dashboard.DashboardViewModel;
import com.example.crowdsensingwotandroidapp.databinding.DialogCampaignManualSendSuccessBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CampaignManualSendSuccessDialog extends DialogFragment {

	private DialogCampaignManualSendSuccessBinding binding;
	private DashboardViewModel dashboardViewModel;
	private AlertDialog dialog;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
		binding = DialogCampaignManualSendSuccessBinding.inflate(getLayoutInflater());
		builder.setView(binding.getRoot());
		dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
		initDialog();
		dialog = builder.create();
		return dialog;
	}

	private void initDialog() {
		Integer result = dashboardViewModel.getManualDataSubmission().getValue();
		if (result == null) {
			dashboardViewModel.getManualDataSubmission().setValue(null);
			NavHostFragment.findNavController(this)
					.navigate(R.id.action_campaignManualSendSuccessDialog_to_homeDetailsFragment);
		} else {
			binding.manualSendSuccessDigit.setText(String.valueOf(result));
			binding.manualSendOkBtn.setOnClickListener(v -> {
				dialog.cancel();
			});
		}
	}

	@Override
	public void onCancel(@NonNull DialogInterface dialog) {
		dashboardViewModel.getManualDataSubmission().setValue(null);
		NavHostFragment.findNavController(this)
				.navigate(R.id.action_campaignManualSendSuccessDialog_to_homeDetailsFragment);
		super.onDismiss(dialog);
	}
}
