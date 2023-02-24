package com.example.crowdsensingwotandroidapp.dashboard.allCampaigns;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crowdsensingwotandroidapp.dashboard.DashboardViewModel;
import com.example.crowdsensingwotandroidapp.databinding.RadioItemBinding;

public class JoinCampaignIntervalsAdapter extends RecyclerView.Adapter<JoinCampaignIntervalsAdapter.ViewHolder> {

	private final int[] intervals;
	private final Context applicationContext;
	private final DashboardViewModel dashboardViewModel;

	public JoinCampaignIntervalsAdapter(Context context, int[] intervalArrayList) {
		intervals = intervalArrayList;
		applicationContext = context;
		dashboardViewModel = new ViewModelProvider((ViewModelStoreOwner) applicationContext).get(DashboardViewModel.class);
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		RadioItemBinding binding = RadioItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
		return new ViewHolder(binding);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		int interval = intervals[position];
		RadioItemBinding binding = holder.binding;
		binding.intervalItemLabel.setText(dashboardViewModel.calculateTimeString(interval));
		binding.intervalItemBtn.setOnClickListener(v -> dashboardViewModel.getApplicationSelectedInterval()
				.setValue(interval));
		dashboardViewModel.getApplicationSelectedInterval()
				.observe((LifecycleOwner) applicationContext, value -> {
					if (value != null) {
						binding.intervalItemRadioBtn.setChecked(value == interval);
					}
				});
	}

	@Override
	public int getItemCount() {
		if (intervals != null)
			return intervals.length;
		else
			return 0;
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {

		private final RadioItemBinding binding;

		public ViewHolder(@NonNull RadioItemBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
