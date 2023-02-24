package com.example.crowdsensingwotandroidapp.dashboard.joinedCampaigns;

import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.crowdsensingwotandroidapp.MainViewModel;
import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.utils.campaign.AppliedCampaign;
import com.example.crowdsensingwotandroidapp.dashboard.DashboardViewModel;

import java.util.Objects;

public class HomeDetailsSettings extends PreferenceFragmentCompat {

	private DashboardViewModel dashboardViewModel;

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.home_details_settings, rootKey);
		dashboardViewModel = new ViewModelProvider((ViewModelStoreOwner) requireContext()).get(DashboardViewModel.class);
		Preference campaignMode = findPreference("campaignMode");
		AppliedCampaign campaign = dashboardViewModel.getUserSelectedCampaign().getValue();
		if (campaign == null) {
			MainViewModel.getErrorToShow().setValue(new Exception("Campaign null"));
		} else {
			switch (campaign.getMode()) {
				case MANUAL:
					Objects.requireNonNull(campaignMode).setSummary(R.string.manualSubmissionTitle);
					break;
				case AUTO_WITH_PREF:
					Objects.requireNonNull(campaignMode)
							.setSummary(R.string.preferencesSubmissionTitle);
					break;
				case AUTOMATIC:
					Objects.requireNonNull(campaignMode)
							.setSummary(R.string.defaultSubmissionTitle);
					break;
			}
		}
		Preference abandonCampaign = findPreference("abandonCampaign");
		if (abandonCampaign != null) {
			abandonCampaign.setOnPreferenceClickListener(preference -> {
				dashboardViewModel.abandonUserSelectedCampaign();
				Navigation.findNavController(requireView())
						.navigate(R.id.action_homeDetailsSettings_to_homeFragment);
				return false;
			});
		}
	}
}
