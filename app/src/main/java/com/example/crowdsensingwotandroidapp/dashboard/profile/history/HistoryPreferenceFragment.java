package com.example.crowdsensingwotandroidapp.dashboard.profile.history;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.example.crowdsensingwotandroidapp.R;

public class HistoryPreferenceFragment extends PreferenceFragmentCompat {

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.history_preferences, rootKey);
	}
}
