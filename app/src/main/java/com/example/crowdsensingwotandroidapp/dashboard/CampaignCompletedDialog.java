package com.example.crowdsensingwotandroidapp.dashboard;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.databinding.DialogCampaignCompletedBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CampaignCompletedDialog extends DialogFragment {

	private DialogCampaignCompletedBinding binding;
	private DashboardViewModel dashboardViewModel;
	private Integer points;
	private AlertDialog dialog;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
		binding = DialogCampaignCompletedBinding.inflate(getLayoutInflater());
		builder.setView(binding.getRoot());
		dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
		points = dashboardViewModel.getCompletedCampaignPoints().getValue();
		if (points != null)
			initDialog();
		else
			dismiss();
		dialog = builder.create();
		return dialog;
	}

	private void initDialog() {
		binding.completedCampaignPointsDigit.setText(String.valueOf(points));
		binding.campaignCompletedOkBtn.setOnClickListener(v -> dialog.cancel());
	}

	@Override
	public void onCancel(@NonNull DialogInterface dialog) {
		dashboardViewModel.getCompletedCampaignPoints().setValue(null);
		NavHostFragment.findNavController(this)
				.navigate(R.id.action_campaignCompletedDialog_to_homeFragment);
		super.onDismiss(dialog);
	}
}
