package com.example.crowdsensingwotandroidapp.dashboard.allCampaigns;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.dashboard.DashboardViewModel;
import com.example.crowdsensingwotandroidapp.databinding.DialogJoinCampaignReviewBinding;
import com.example.crowdsensingwotandroidapp.utils.campaign.Campaign;
import com.example.crowdsensingwotandroidapp.utils.campaign.SubmissionMode;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class JoinCampaignReviewDialog extends DialogFragment {

	private DialogJoinCampaignReviewBinding binding;
	private DashboardViewModel dashboardViewModel;
	private Campaign campaign;
	private AlertDialog dialog;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
		binding = DialogJoinCampaignReviewBinding.inflate(getLayoutInflater());
		builder.setView(binding.getRoot());
		dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
		campaign = dashboardViewModel.getApplicationSelectedCampaign().getValue();
		if (campaign != null)
			initDialog();
		else
			dismiss();
		dialog = builder.create();
		return dialog;
	}

	private void initDialog() {
		SubmissionMode submissionMode = dashboardViewModel.getApplicationSelectedSubmissionMode()
				.getValue();
		if (submissionMode == SubmissionMode.MANUAL) {
			binding.reviewTitle.setText(R.string.applyReviewTitleManual);
			binding.sendingInfo.setVisibility(View.VISIBLE);
			binding.reviewIntervalLabel.setText(R.string.applyReviewIntervalLabelManual);
		} else {
			binding.reviewTitle.setText(R.string.applyReviewTitleAuto);
			binding.sendingInfo.setVisibility(View.GONE);
			binding.reviewIntervalLabel.setText(R.string.applyReviewIntervalLabelAuto);
		}
		Integer interval;
		if (submissionMode == SubmissionMode.AUTO_WITH_PREF) {
			interval = dashboardViewModel.getApplicationSelectedInterval().getValue();
		} else {
			interval = (Integer) campaign.getIdealSubmissionInterval();
		}
		String intervalString = dashboardViewModel.calculateTimeString(interval);
		String[] intervalSplit = intervalString.split(" ");
		if (intervalSplit.length > 1) {
			binding.intervalDigit.setText(intervalSplit[0]);
			binding.intervalUnit.setText(intervalSplit[1]);
		}
		Number submissionRequired = campaign.getSubmissionRequired();
		if (submissionRequired != null) {
			binding.sendingTimesDigit.setText(String.valueOf(submissionRequired.intValue()));
			binding.sendingTimesUnit.setText(submissionRequired.intValue() == 1 ? R.string.time : R.string.times);
		}
		binding.reviewConfirmBtn.setOnClickListener(v -> {
			NavHostFragment.findNavController(this)
					.navigate(R.id.action_campaignApplyReviewDialog_to_campaignsFragment);
			dashboardViewModel.applyToCampaign();
		});
		binding.reviewCancelBtn.setOnClickListener(v -> dialog.cancel());
	}

	@Override
	public void onCancel(@NonNull DialogInterface dialog) {
		NavHostFragment.findNavController(this)
				.navigate(R.id.action_campaignApplyReviewDialog_to_campaignApplyDialog);
		super.onDismiss(dialog);
	}
}
