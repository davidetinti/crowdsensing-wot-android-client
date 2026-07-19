package com.example.crowdsensingwotandroidapp.dashboard.profile.spot;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.crowdsensingwotandroidapp.MainViewModel;
import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.databinding.FragmentSpotBinding;
import com.example.crowdsensingwotandroidapp.datafly.DataFlyIntegration;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Modalità spot (M8): il worker vende letture on-demand. Da qui imposta il
 * prezzo per lettura, attiva/mette in pausa l'annuncio e vede le statistiche
 * (letture vendute, crediti guadagnati — il netto entra nel wallet in pending
 * come per le campagne). Il pilota espone la sola spec core/location: la
 * scelta della spec arriverà quando il registry ne avrà più d'una sul device.
 *
 * Tutte le chiamate passano dal layer datafly (azioni WoT del Master) su
 * thread di background; la UI si aggiorna sul main thread.
 */
public class SpotFragment extends Fragment {

	private static final String SPEC_KEY = "core/location";

	private FragmentSpotBinding binding;
	private DataFlyIntegration integration;
	// stato corrente del listing (null = mai pubblicato)
	private Boolean listingActive;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		binding = FragmentSpotBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		integration = new DataFlyIntegration(requireContext());

		binding.spotSaveButton.setOnClickListener(v -> publishListing());
		binding.spotActiveSwitch.setOnCheckedChangeListener((button, checked) -> {
			// reagire solo ai tap dell'utente, non ai setChecked programmatici
			if (button.isPressed() && listingActive != null && checked != listingActive) {
				toggleActive(checked);
			}
		});
		refresh();
	}

	/** Carica il listing corrente e popola la UI. */
	private void refresh() {
		setBusy(true);
		CompletableFuture.supplyAsync(() -> {
			try {
				return integration.listSpotListings();
			} catch (Exception e) {
				throw new java.util.concurrent.CompletionException(e);
			}
		}).whenComplete((listings, err) -> post(() -> {
			setBusy(false);
			if (err != null) {
				showError(err);
				return;
			}
			JsonNode listing = findListing(listings);
			if (listing == null) {
				listingActive = null;
				binding.spotActiveSwitch.setChecked(false);
				binding.spotActiveSwitch.setEnabled(false);
				binding.spotStatsLabel.setText(R.string.spot_stats_none);
				return;
			}
			listingActive = listing.path("active").asBoolean(false);
			binding.spotActiveSwitch.setEnabled(true);
			binding.spotActiveSwitch.setChecked(listingActive);
			binding.spotPriceInput.setText(String.format(Locale.US, "%.2f",
					listing.path("priceCents").asInt(0) / 100.0));
			JsonNode stats = listing.path("stats");
			binding.spotStatsLabel.setText(getString(R.string.spot_stats_format,
					stats.path("retrieves").asInt(0),
					stats.path("earnedCents").asInt(0) / 100.0));
		}));
	}

	private void publishListing() {
		int priceCents;
		try {
			String raw = String.valueOf(binding.spotPriceInput.getText()).replace(',', '.');
			priceCents = Math.round(Float.parseFloat(raw) * 100f);
		} catch (NumberFormatException e) {
			priceCents = 0;
		}
		if (priceCents <= 0) {
			binding.spotPriceLayout.setError(getString(R.string.spot_error_price));
			return;
		}
		binding.spotPriceLayout.setError(null);
		int finalPrice = priceCents;
		setBusy(true);
		CompletableFuture.runAsync(() -> {
			try {
				integration.upsertSpotListing(SPEC_KEY, finalPrice);
			} catch (Exception e) {
				throw new java.util.concurrent.CompletionException(e);
			}
		}).whenComplete((unused, err) -> post(() -> {
			setBusy(false);
			if (err != null) {
				showError(err);
			} else {
				MainViewModel.getErrorToShow()
						.setValue(new Exception(getString(R.string.spot_saved)));
				refresh();
			}
		}));
	}

	private void toggleActive(boolean active) {
		setBusy(true);
		CompletableFuture.runAsync(() -> {
			try {
				integration.setSpotListing(SPEC_KEY, active);
			} catch (Exception e) {
				throw new java.util.concurrent.CompletionException(e);
			}
		}).whenComplete((unused, err) -> post(() -> {
			setBusy(false);
			if (err != null) {
				showError(err);
			}
			refresh();
		}));
	}

	// --- interni ---

	private JsonNode findListing(JsonNode listings) {
		if (listings == null)
			return null;
		String deviceId = integration.deviceId();
		for (JsonNode listing : listings) {
			if (deviceId.equals(listing.path("deviceId").asText())
					&& SPEC_KEY.equals(listing.path("spec").path("key").asText())) {
				return listing;
			}
		}
		return null;
	}

	private void setBusy(boolean busy) {
		if (binding == null)
			return;
		binding.spotProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
		binding.spotSaveButton.setEnabled(!busy);
	}

	private void showError(Throwable err) {
		Throwable cause = err.getCause() != null ? err.getCause() : err;
		MainViewModel.getErrorToShow().setValue(new Exception(cause.getMessage()));
	}

	private void post(Runnable action) {
		new Handler(Looper.getMainLooper()).post(() -> {
			if (binding != null)
				action.run();
		});
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}
