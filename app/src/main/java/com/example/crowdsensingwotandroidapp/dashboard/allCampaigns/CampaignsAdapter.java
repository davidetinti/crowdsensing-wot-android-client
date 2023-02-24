package com.example.crowdsensingwotandroidapp.dashboard.allCampaigns;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.utils.campaign.Campaign;
import com.example.crowdsensingwotandroidapp.dashboard.DashboardViewModel;
import com.example.crowdsensingwotandroidapp.databinding.CardCampaignBinding;

import java.util.ArrayList;
import java.util.Objects;

public class CampaignsAdapter extends RecyclerView.Adapter<CampaignsAdapter.ViewHolder> {

	private final ArrayList<Campaign> activeCampaigns;
	private final DashboardViewModel dashboardViewModel;

	public CampaignsAdapter(Context context, ArrayList<Campaign> campaigns) {
		activeCampaigns = campaigns;
		dashboardViewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(DashboardViewModel.class);
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		CardCampaignBinding binding = CardCampaignBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new ViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		Campaign campaign = activeCampaigns.get(position);
		CardCampaignBinding binding = holder.binding;
		boolean isCameraRequired = Objects.equals(campaign.getType(), "camera");
		boolean isMicRequired = Objects.equals(campaign.getType(), "mic");
		boolean isLocationRequired = Objects.equals(campaign.getType(), "location");
		boolean isGpsRequired = Objects.equals(campaign.getType(), "gps");
		boolean isOtherRequired = Objects.equals(campaign.getType(), "other");
		binding.campaignTitle.setText(campaign.getTitle());
		binding.campaignOrganization.setText(campaign.getOrganization());
		binding.sensorCamera.setVisibility(isCameraRequired ? View.VISIBLE : View.GONE);
		binding.sensorMic.setVisibility(isMicRequired ? View.VISIBLE : View.GONE);
		binding.sensorPosition.setVisibility(isLocationRequired ? View.VISIBLE : View.GONE);
		binding.sensorSatellite.setVisibility(isGpsRequired ? View.VISIBLE : View.GONE);
		binding.sensorUnknown.setVisibility(isOtherRequired ? View.VISIBLE : View.GONE);
		int points = campaign.getPoints().intValue();
		binding.points.setText(points > 0 ? points + " PTS" : "NO POINTS");
		binding.getRoot().setOnClickListener(v -> {
			dashboardViewModel.getApplicationSelectedCampaign().setValue(campaign);
			NavController controller = Navigation.findNavController(v);
			controller.navigate(R.id.action_campaignsFragment_to_campaignDetailsFragment);
		});
	}

	@Override
	public int getItemCount() {
		if (activeCampaigns != null)
			return activeCampaigns.size();
		else
			return 0;
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {

		private final CardCampaignBinding binding;

		public ViewHolder(@NonNull CardCampaignBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
