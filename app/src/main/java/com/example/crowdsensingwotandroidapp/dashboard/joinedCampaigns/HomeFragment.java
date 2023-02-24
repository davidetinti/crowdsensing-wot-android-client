package com.example.crowdsensingwotandroidapp.dashboard.joinedCampaigns;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.utils.campaign.AppliedCampaign;
import com.example.crowdsensingwotandroidapp.dashboard.DashboardViewModel;
import com.example.crowdsensingwotandroidapp.dashboard.allCampaigns.CampaignsAdapter;
import com.example.crowdsensingwotandroidapp.databinding.FragmentHomeBinding;
import com.example.crowdsensingwotandroidapp.utils.ui.DefaultItemDecorator;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

	private DashboardViewModel dashboardViewModel;
	private FragmentHomeBinding binding;
	private RecyclerView campaignsRecyclerView;

	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentHomeBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
		campaignsRecyclerView = binding.userCampaignsRecyclerView;
		campaignsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		campaignsRecyclerView.addItemDecoration(new DefaultItemDecorator(18, 15));
		campaignsRecyclerView.setAdapter(new CampaignsAdapter(getContext(), new ArrayList<>()));
		dashboardViewModel.getUserCampaigns()
				.observe(getViewLifecycleOwner(), this::reloadRecyclerView);
		dashboardViewModel.getCompletedCampaignPoints().observe(getViewLifecycleOwner(), points -> {
			if (points != null) {
				NavHostFragment.findNavController(this).navigate(R.id.action_homeFragment_to_campaignCompletedDialog);
			}
		});
	}

	private void reloadRecyclerView(ArrayList<AppliedCampaign> userCampaigns) {
		if (userCampaigns != null) {
			campaignsRecyclerView.setAdapter(new HomeAdapter(getContext(), userCampaigns));
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}