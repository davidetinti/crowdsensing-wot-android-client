package com.example.crowdsensingwotandroidapp;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.crowdsensingwotandroidapp.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

	private ActivityMainBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Handle the splash screen transition.
		SplashScreen.installSplashScreen(this);
		super.onCreate(savedInstanceState);
		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		MainViewModel.getLoadingCounter().observe(this, counter -> {
			if (counter > 0)
				binding.loadingWrapper.setVisibility(View.VISIBLE);
			else
				binding.loadingWrapper.setVisibility(View.GONE);
		});
		MainViewModel.getErrorToShow().observe(this, error -> {
			if (error != null) {
				error.printStackTrace();
				while (error.getCause() != null)
					error = error.getCause();
				String message = error.getMessage() != null ? error.getMessage() : "Error";
				Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
				MainViewModel.getErrorToShow().setValue(null);
			}
		});
	}
}