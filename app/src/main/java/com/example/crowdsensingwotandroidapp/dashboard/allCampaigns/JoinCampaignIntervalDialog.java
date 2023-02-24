package com.example.crowdsensingwotandroidapp.dashboard.allCampaigns;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.dashboard.DashboardViewModel;
import com.example.crowdsensingwotandroidapp.databinding.DialogJoinCampaignIntervalBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class JoinCampaignIntervalDialog extends DialogFragment {

	private DialogJoinCampaignIntervalBinding binding;
	private DashboardViewModel dashboardViewModel;
	private AlertDialog dialog;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
		binding = DialogJoinCampaignIntervalBinding.inflate(getLayoutInflater());
		builder.setView(binding.getRoot());
		dialog = builder.create();
		dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
		initDialog();
		return dialog;
	}

	private void initDialog() {
		binding.intervalRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		int[] intervals = getResources().getIntArray(R.array.sendingIntervals);
		binding.intervalRecyclerView.setAdapter(new JoinCampaignIntervalsAdapter(getContext(), intervals));
		binding.intervalCancelBtn.setOnClickListener(v -> dialog.cancel());
		binding.intervalOkBtn.setOnClickListener(v -> NavHostFragment.findNavController(this)
				.navigate(R.id.action_campaignApplyIntervalDialog_to_campaignApplyDialog));
	}

	@Override
	public void onCancel(@NonNull DialogInterface dialog) {
		dashboardViewModel.getApplicationSelectedInterval().setValue(null);
		NavHostFragment.findNavController(this)
				.navigate(R.id.action_campaignApplyIntervalDialog_to_campaignApplyDialog);
		super.onCancel(dialog);
	}
}
