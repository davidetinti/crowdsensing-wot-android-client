package com.example.crowdsensingwotandroidapp.dashboard.allCampaigns;

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
import com.example.crowdsensingwotandroidapp.databinding.DialogJoinCampaignSuccessBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class JoinCampaignSuccessDialog extends DialogFragment {

	private DashboardViewModel dashboardViewModel;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
		DialogJoinCampaignSuccessBinding binding = DialogJoinCampaignSuccessBinding.inflate(getLayoutInflater());
		builder.setView(binding.getRoot());
		AlertDialog dialog = builder.create();
		dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
		binding.applySuccessOkBtn.setOnClickListener(v -> dialog.cancel());
		return dialog;
	}

	@Override
	public void onCancel(@NonNull DialogInterface dialog) {
		NavHostFragment.findNavController(this)
				.navigate(R.id.action_campaignApplySuccessDialog_to_campaignsFragment);
		dashboardViewModel.getApplicationSelectedInterval().setValue(null);
		dashboardViewModel.getApplicationSelectedSubmissionMode().setValue(null);
		dashboardViewModel.getApplicationSelectedCampaign().setValue(null);
		super.onCancel(dialog);
	}
}
