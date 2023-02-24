package com.example.crowdsensingwotandroidapp.dashboard.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.crowdsensingwotandroidapp.dashboard.DashboardViewModel;
import com.example.crowdsensingwotandroidapp.databinding.FragmentProfileBinding;
import com.example.crowdsensingwotandroidapp.utils.User;

public class ProfileFragment extends Fragment {

	private FragmentProfileBinding binding;

	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentProfileBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		DashboardViewModel dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
		User initUser = dashboardViewModel.getUser().getValue();
		if (initUser != null) {
			Integer points = initUser.getPoints();
			binding.profilePointsLabel.setText(String.valueOf(points));
		}
		dashboardViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
			if (user != null) {
				binding.profilePointsLabel.setText(String.valueOf(user.getPoints()));
			}
		});
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}