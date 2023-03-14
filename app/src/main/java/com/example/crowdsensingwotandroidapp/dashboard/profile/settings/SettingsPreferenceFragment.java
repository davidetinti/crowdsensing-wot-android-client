package com.example.crowdsensingwotandroidapp.dashboard.profile.settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.example.crowdsensingwotandroidapp.R;

public class SettingsPreferenceFragment extends PreferenceFragmentCompat {


	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.profile_settings, rootKey);
	}
}
