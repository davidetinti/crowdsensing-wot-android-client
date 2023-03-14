package com.example.crowdsensingwotandroidapp.dashboard.joinedCampaigns;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.dashboard.DashboardViewModel;
import com.example.crowdsensingwotandroidapp.databinding.FragmentCampaignDetailsBinding;
import com.example.crowdsensingwotandroidapp.utils.campaign.AppliedCampaign;

import java.util.Objects;

public class CampaignDetails extends Fragment {

	private AppliedCampaign campaign;
	private DashboardViewModel dashboardViewModel;
	private FragmentCampaignDetailsBinding binding;

	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentCampaignDetailsBinding.inflate(inflater, container, false);
		requireActivity().addMenuProvider(new MenuProvider() {
			@Override
			public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
				menuInflater.inflate(R.menu.toolbar_home_campaign_details, menu);
			}

			@Override
			public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
				if (menuItem.getItemId() == R.id.action_settings) {
					Navigation.findNavController(binding.getRoot())
							.navigate(R.id.action_homeDetailsFragment_to_homeDetailsSettings);
				}
				return false;
			}
		}, getViewLifecycleOwner());
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar())
				.setTitle("");
		dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
		campaign = dashboardViewModel.getUserSelectedCampaign().getValue();
		if (campaign == null)
			NavHostFragment.findNavController(this)
					.navigate(R.id.action_homeDetailsFragment_to_homeFragment);
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
		switch (campaign.getMode()) {
			case AUTOMATIC: {
				binding.campaignDetailsActionButton.setText(R.string.autoSendDataButtonText);
				binding.campaignDetailsActionButton.setBackgroundColor(Color.parseColor("#254B5B"));
				binding.campaignDetailsActionButton.setOnClickListener(v -> {
				});
				break;
			}
			case AUTO_WITH_PREF: {
				binding.campaignDetailsActionButton.setText(R.string.autoSendDataButtonText);
				binding.campaignDetailsActionButton.setBackgroundColor(Color.parseColor("#254B5B"));
				break;
			}
			case MANUAL: {
				binding.campaignDetailsActionButton.setText(R.string.manualSendDataButtonText);
				binding.campaignDetailsActionButton.setBackgroundColor(Color.parseColor("#254B5B"));
				binding.campaignDetailsActionButton.setOnClickListener(v -> {

					Navigation.findNavController(v)
							.navigate(R.id.action_homeDetailsFragment_to_campaignManualSendDialog);
				});
				break;
			}
			default: {
				binding.campaignDetailsActionButton.setTextColor(Color.WHITE);
				binding.campaignDetailsActionButton.setBackgroundColor(Color.parseColor("#707973"));
				binding.campaignDetailsActionButton.setEnabled(false);
			}
		}
		dashboardViewModel.getManualDataSubmission().observe(getViewLifecycleOwner(), result -> {
			if (result != null) {
				if (result == 0) {
					dashboardViewModel.getManualDataSubmission().setValue(null);
					dashboardViewModel.getUserSelectedCampaign().setValue(null);
					NavHostFragment.findNavController(this)
							.navigate(R.id.action_homeDetailsFragment_to_homeFragment);
				} else {
					NavHostFragment.findNavController(this)
							.navigate(R.id.action_homeDetailsFragment_to_campaignManualSendSuccessDialog);
				}
			}
		});
	}
}
