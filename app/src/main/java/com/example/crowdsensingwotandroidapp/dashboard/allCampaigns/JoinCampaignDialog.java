package com.example.crowdsensingwotandroidapp.dashboard.allCampaigns;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.utils.campaign.Campaign;
import com.example.crowdsensingwotandroidapp.dashboard.DashboardViewModel;
import com.example.crowdsensingwotandroidapp.databinding.DialogJoinCampaignBinding;
import com.example.crowdsensingwotandroidapp.utils.campaign.SubmissionMode;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class JoinCampaignDialog extends DialogFragment {

	private DialogJoinCampaignBinding binding;
	private DashboardViewModel dashboardViewModel;
	private Campaign campaign;
	private AlertDialog dialog;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
		binding = DialogJoinCampaignBinding.inflate(getLayoutInflater());
		builder.setView(binding.getRoot());
		dialog = builder.create();
		dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
		campaign = dashboardViewModel.getApplicationSelectedCampaign().getValue();
		if (campaign == null)
			dismiss();
		else
			initDialog();
		return dialog;
	}

	private void initDialog() {
		// Radio buttons logic
		dashboardViewModel.getApplicationSelectedSubmissionMode()
				.observe((LifecycleOwner) requireContext(), submissionMode -> {
					binding.autoRadioBtn.setChecked(submissionMode == SubmissionMode.AUTOMATIC);
					binding.autoPrefRadioBtn.setChecked(submissionMode == SubmissionMode.AUTO_WITH_PREF);
					binding.manualRadioBtn.setChecked(submissionMode == SubmissionMode.MANUAL);
				});
		dashboardViewModel.getApplicationSelectedInterval()
				.observe((LifecycleOwner) requireContext(), interval -> {
					if (interval != null) {
						binding.intervalLabel.setText(dashboardViewModel.calculateTimeString(interval));
					}
				});
		binding.autoBtn.setEnabled(campaign.getPullEnabled());
		binding.autoRadioBtn.setEnabled(campaign.getPullEnabled());
		binding.autoBtn.setOnClickListener(v -> dashboardViewModel.getApplicationSelectedSubmissionMode()
				.setValue(SubmissionMode.AUTOMATIC));
		binding.autoPrefBtn.setEnabled(campaign.getPushAutoEnabled());
		binding.autoPrefRadioBtn.setEnabled(campaign.getPushAutoEnabled());
		binding.autoPrefBtn.setOnClickListener(v -> dashboardViewModel.getApplicationSelectedSubmissionMode()
				.setValue(SubmissionMode.AUTO_WITH_PREF));
		binding.manualBtn.setEnabled(campaign.getPushInputEnabled());
		binding.manualRadioBtn.setEnabled(campaign.getPushInputEnabled());
		binding.manualBtn.setOnClickListener(v -> dashboardViewModel.getApplicationSelectedSubmissionMode()
				.setValue(SubmissionMode.MANUAL));
		binding.intervalBtn.setOnClickListener(v -> {
			if (dashboardViewModel.getApplicationSelectedSubmissionMode()
					.getValue() == SubmissionMode.AUTO_WITH_PREF) {
				NavHostFragment.findNavController(this)
						.navigate(R.id.action_campaignApplyDialog_to_campaignApplyIntervalDialog);
			}
		});
		// Action buttons logic
		binding.applyNextBtn.setOnClickListener(v -> {
			SubmissionMode submissionMode = dashboardViewModel.getApplicationSelectedSubmissionMode()
					.getValue();
			if (submissionMode != null) {
				if (submissionMode != SubmissionMode.AUTO_WITH_PREF || dashboardViewModel.getApplicationSelectedInterval()
						.getValue() != null) {
					NavHostFragment.findNavController(this)
							.navigate(R.id.action_campaignApplyDialog_to_campaignApplyReviewDialog);
				}
			}
		});
		binding.applyCancelBtn.setOnClickListener(v -> dialog.cancel());
	}

	@Override
	public void onCancel(@NonNull DialogInterface dialog) {
		dashboardViewModel.getApplicationSelectedInterval().setValue(null);
		dashboardViewModel.getApplicationSelectedSubmissionMode().setValue(null);
		NavHostFragment.findNavController(this)
				.navigate(R.id.action_campaignApplyDialog_to_campaignDetailsDialog);
		super.onCancel(dialog);
	}
}
