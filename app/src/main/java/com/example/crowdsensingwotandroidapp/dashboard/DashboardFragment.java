package com.example.crowdsensingwotandroidapp.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.crowdsensingwotandroidapp.ForegroundServiceLauncher;
import com.example.crowdsensingwotandroidapp.MainViewModel;
import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.ServientService;
import com.example.crowdsensingwotandroidapp.databinding.FragmentDashboardBinding;
import com.example.crowdsensingwotandroidapp.utils.network.NetworkStateManager;

import java.util.Objects;

public class DashboardFragment extends Fragment {

	private FragmentDashboardBinding binding;

	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentDashboardBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		AppCompatActivity activity = (AppCompatActivity) requireActivity();
		// Navigation setup
		activity.setSupportActionBar(binding.topAppBar);
		AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.campaignsFragment, R.id.homeFragment, R.id.profileFragment).build();
		NavHostFragment navHostFragment = binding.fragmentContainerViewDashboard.getFragment();
		NavController navController = Objects.requireNonNull(navHostFragment).getNavController();
		NavigationUI.setupActionBarWithNavController(activity, navController, appBarConfiguration);
		NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
		// Back navigation listener
		OnBackPressedCallback callback = new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				if (Objects.requireNonNull(navController.getCurrentDestination())
						.getId() != navController.getGraph().getStartDestinationId())
					navController.navigateUp();
				else
					requireActivity().finish();
			}
		};
		activity.getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
		binding.topAppBar.setNavigationOnClickListener(v -> navController.navigateUp());
		// Dashboard view model setup
		DashboardViewModel dashboardViewModel = new ViewModelProvider(activity).get(DashboardViewModel.class);
		NetworkStateManager.getInstance().getNetworkConnectivityStatus()
				.observe(getViewLifecycleOwner(), connected -> {
					if (connected)
						dashboardViewModel.fetchDashboardData();
				});
		// Service initialization
		ForegroundServiceLauncher.getInstance().startService(requireContext());
		ServientService.getInstance(requireContext());
		// Invalid bearer handling
		MainViewModel.getBearerToken().observe(getViewLifecycleOwner(), bearer -> {
			if (bearer == null) {
				NavController controller = NavHostFragment.findNavController(this);
				controller.navigate(R.id.action_dashboardFragment_to_authFragment);
			}
		});
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}