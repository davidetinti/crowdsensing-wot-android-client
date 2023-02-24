package com.example.crowdsensingwotandroidapp.dashboard.profile;

import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.crowdsensingwotandroidapp.ForegroundServiceLauncher;
import com.example.crowdsensingwotandroidapp.MainViewModel;
import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.dashboard.DashboardViewModel;
import com.example.crowdsensingwotandroidapp.utils.User;

import java.util.Objects;

public class ProfilePreferenceFragment extends PreferenceFragmentCompat {

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.profile_preferences, rootKey);
		DashboardViewModel dashboardViewModel = new ViewModelProvider((ViewModelStoreOwner) requireContext()).get(DashboardViewModel.class);
		Preference profileEmail = findPreference("profileEmail");
		User user = dashboardViewModel.getUser().getValue();
		if (user == null) {
			MainViewModel.getErrorToShow().setValue(new Exception("Error fetching data"));
		} else {
			String email = user.getEmail();
			Objects.requireNonNull(profileEmail).setSummary(email);
		}
		Preference profileLogout = findPreference("profileLogout");
		if (profileLogout != null) {
			profileLogout.setOnPreferenceClickListener(preference -> {
				dashboardViewModel.logout(true);
				ForegroundServiceLauncher.getInstance().stopService(requireContext());
				return false;
			});
		}
		Preference profileCampaignHistory = findPreference("profileCampaignHistory");
		if (profileCampaignHistory != null) {
			profileCampaignHistory.setOnPreferenceClickListener(preference -> {
				NavHostFragment.findNavController(this)
						.navigate(R.id.action_profileFragment_to_historyPreferenceFragment);
				return false;
			});
		}
		Preference profileSettings = findPreference("profileSettings");
		if (profileSettings != null) {
			profileSettings.setOnPreferenceClickListener(preference -> {
				NavHostFragment.findNavController(this)
						.navigate(R.id.action_profileFragment_to_settingsPreferenceFragment);
				return false;
			});
		}
	}
}