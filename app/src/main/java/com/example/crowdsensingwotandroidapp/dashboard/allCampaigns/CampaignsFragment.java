package com.example.crowdsensingwotandroidapp.dashboard.allCampaigns;

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
import com.example.crowdsensingwotandroidapp.dashboard.DashboardViewModel;
import com.example.crowdsensingwotandroidapp.databinding.FragmentCampaignsBinding;
import com.example.crowdsensingwotandroidapp.utils.campaign.AppliedCampaign;
import com.example.crowdsensingwotandroidapp.utils.campaign.Campaign;
import com.example.crowdsensingwotandroidapp.utils.ui.DefaultItemDecorator;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class CampaignsFragment extends Fragment {

	private DashboardViewModel dashboardViewModel;
	private FragmentCampaignsBinding binding;
	private RecyclerView campaignsRecyclerView;

	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentCampaignsBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
		campaignsRecyclerView = binding.availableCampaignsRecyclerView;
		campaignsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		campaignsRecyclerView.addItemDecoration(new DefaultItemDecorator(18, 15));
		campaignsRecyclerView.setAdapter(new CampaignsAdapter(getContext(), new ArrayList<>()));
		dashboardViewModel.getAllCampaigns()
				.observe(getViewLifecycleOwner(), campaigns -> reloadRecyclerView());
		dashboardViewModel.getUserCampaigns()
				.observe(getViewLifecycleOwner(), userCampaigns -> reloadRecyclerView());
		dashboardViewModel.getApplicationResult().observe(getViewLifecycleOwner(), result -> {
			if (result != null) {
				NavHostFragment.findNavController(this)
						.navigate(R.id.action_campaignsFragment_to_campaignApplySuccessDialog);
				dashboardViewModel.getApplicationResult().setValue(null);
			}
		});
	}

	private void reloadRecyclerView() {
		ArrayList<AppliedCampaign> userCampaigns = dashboardViewModel.getUserCampaigns().getValue();
		ArrayList<Campaign> allCampaigns = dashboardViewModel.getAllCampaigns().getValue();
		if (userCampaigns != null && allCampaigns != null) {
			ArrayList<Campaign> filtered = new ArrayList<>();
			allCampaigns.forEach(campaign -> {
				AtomicBoolean found = new AtomicBoolean(false);
				userCampaigns.forEach(userCampaign -> {
					if (Objects.equals(campaign.getId(), userCampaign.getId()))
						found.set(true);
				});
				if (!found.get())
					filtered.add(campaign);
			});
			campaignsRecyclerView.setAdapter(new CampaignsAdapter(getContext(), filtered));
		} else {
			campaignsRecyclerView.setAdapter(new CampaignsAdapter(getContext(), new ArrayList<>()));
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}