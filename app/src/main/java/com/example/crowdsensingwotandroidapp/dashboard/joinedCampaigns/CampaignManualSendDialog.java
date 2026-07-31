package com.example.crowdsensingwotandroidapp.dashboard.joinedCampaigns;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.crowdsensingwotandroidapp.MainViewModel;
import com.example.crowdsensingwotandroidapp.R;
import com.example.crowdsensingwotandroidapp.dashboard.DashboardViewModel;
import com.example.crowdsensingwotandroidapp.databinding.DialogCampaignManualSendBinding;
import com.example.crowdsensingwotandroidapp.utils.campaign.AppliedCampaign;
import com.example.crowdsensingwotandroidapp.utils.campaign.schema.DataSchema;
import com.example.crowdsensingwotandroidapp.utils.campaign.schema.DataSchemaField;
import com.example.crowdsensingwotandroidapp.utils.campaign.submission.LocationSubmission;
import com.example.crowdsensingwotandroidapp.utils.sensors.SensorRegistry;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Invio manuale dei dati di una campagna. Tre casi:
 * - campagna con data_schema: form generato dinamicamente dallo schema;
 * - campagna "location": posizione corrente (comportamento originale);
 * - altri tipi con un sensore nel SensorRegistry: lettura del sensore.
 */
public class CampaignManualSendDialog extends DialogFragment {

	private DialogCampaignManualSendBinding binding;
	private DashboardViewModel dashboardViewModel;
	private AppliedCampaign campaign;
	private AlertDialog dialog;
	// Input generati dal data_schema, per nome campo
	private final Map<String, View> schemaInputs = new HashMap<>();

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity());
		binding = DialogCampaignManualSendBinding.inflate(getLayoutInflater());
		builder.setView(binding.getRoot());
		dashboardViewModel = new ViewModelProvider(requireActivity()).get(DashboardViewModel.class);
		campaign = dashboardViewModel.getUserSelectedCampaign().getValue();
		// Azzera eventuali dati residui di un dialog precedente: evita di
		// inviare il payload di un'altra campagna.
		dashboardViewModel.getManualData().setValue(null);
		dialog = builder.create();
		if (campaign != null) {
			initDialog();
		} else {
			dismiss();
		}
		return dialog;
	}

	private void initDialog() {
		if (campaign.getDataSchema() != null) {
			initSchemaForm(campaign.getDataSchema());
		} else if ("location".equals(campaign.getType())) {
			initLocation();
		} else if (SensorRegistry.sensorForType(requireContext(), campaign.getType()) != null) {
			initSensor();
		} else {
			// Nessun sensore e nessuno schema: cancel() prima di show() è un
			// no-op, quindi mostra l'errore e lascia solo il tasto annulla.
			binding.inputManualSendWrapper.setVisibility(View.GONE);
			binding.locationManualSendWrapper.setVisibility(View.VISIBLE);
			binding.locationTextField.setHint("Tipo di campagna non supportato");
			binding.manualSendConfirmBtn.setEnabled(false);
		}
		binding.manualSendCancelBtn.setOnClickListener(v -> dialog.cancel());
		binding.manualSendConfirmBtn.setOnClickListener(v -> {
			if (campaign.getDataSchema() != null) {
				Map<String, Object> values = collectSchemaValues();
				String error = campaign.getDataSchema().validate(values);
				if (error != null) {
					MainViewModel.getErrorToShow().setValue(new Exception(error));
					return;
				}
				dashboardViewModel.getManualData().setValue(values);
			}
			if (dashboardViewModel.getManualData().getValue() == null) {
				MainViewModel.getErrorToShow()
						.setValue(new Exception("Nessun dato disponibile da inviare"));
				return;
			}
			NavHostFragment.findNavController(this)
					.navigate(R.id.action_campaignManualSendDialog_to_homeDetailsFragment);
			dashboardViewModel.sendDataManual();
		});
	}

	/** Comportamento originale: posizione corrente mostrata e inviata. */
	private void initLocation() {
		binding.inputManualSendWrapper.setVisibility(View.GONE);
		binding.locationManualSendWrapper.setVisibility(View.VISIBLE);
		dashboardViewModel.getDeviceLocation().observe(this, location -> {
			if (location != null) {
				binding.locationTextField.setHint(location.getLatitude() + ", " + location.getLongitude());
				dashboardViewModel.getManualData()
						.setValue(new LocationSubmission(location.getLatitude(), location.getLongitude()));
			} else {
				binding.locationTextField.setHint("NULL Location");
			}
		});
	}

	/** Lettura una tantum dal sensore associato al tipo della campagna. */
	private void initSensor() {
		binding.inputManualSendWrapper.setVisibility(View.GONE);
		binding.locationManualSendWrapper.setVisibility(View.VISIBLE);
		binding.locationTextField.setHint("Lettura sensore...");
		Context context = requireContext().getApplicationContext();
		SensorRegistry.read(context, campaign.getType()).thenAccept(payload ->
				binding.getRoot().post(() -> {
					if (payload != null) {
						binding.locationTextField.setHint(payload.toString());
						dashboardViewModel.getManualData().setValue(payload);
					} else {
						binding.locationTextField.setHint("Sensore non disponibile");
					}
				}));
	}

	/** Costruisce il form dinamico a partire dal data_schema. */
	private void initSchemaForm(DataSchema schema) {
		binding.locationManualSendWrapper.setVisibility(View.GONE);
		LinearLayout wrapper = binding.inputManualSendWrapper;
		wrapper.setVisibility(View.VISIBLE);
		wrapper.removeAllViews();
		Context context = requireContext();
		for (DataSchemaField field : schema.getFields()) {
			TextView label = new TextView(context);
			label.setText(fieldLabel(field));
			wrapper.addView(label);
			if (field.getDescription() != null) {
				TextView description = new TextView(context);
				description.setText(field.getDescription());
				description.setTextSize(12);
				wrapper.addView(description);
			}
			View input = buildInput(context, field);
			schemaInputs.put(field.getName(), input);
			wrapper.addView(input);
		}
	}

	private String fieldLabel(DataSchemaField field) {
		String label = field.getLabel();
		if (field.getUnit() != null)
			label += " (" + field.getUnit() + ")";
		if (field.isRequired())
			label += " *";
		return label;
	}

	private View buildInput(Context context, DataSchemaField field) {
		if (field.getAllowedValues() != null && field.getType() != DataSchemaField.FieldType.BOOLEAN) {
			Spinner spinner = new Spinner(context);
			ArrayList<String> entries = new ArrayList<>();
			entries.add("—");
			entries.addAll(field.getAllowedValues());
			ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, entries);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(adapter);
			return spinner;
		}
		switch (field.getType()) {
			case BOOLEAN:
				Switch toggle = new Switch(context);
				toggle.setText(field.getLabel());
				return toggle;
			case NUMBER:
			case INTEGER:
				EditText numberInput = new EditText(context);
				int inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
				if (field.getType() == DataSchemaField.FieldType.NUMBER)
					inputType |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
				numberInput.setInputType(inputType);
				numberInput.setHint(numberHint(field));
				return numberInput;
			case STRING:
			default:
				EditText textInput = new EditText(context);
				textInput.setInputType(InputType.TYPE_CLASS_TEXT);
				textInput.setHint("Testo");
				return textInput;
		}
	}

	private String numberHint(DataSchemaField field) {
		if (field.getMinimum() != null && field.getMaximum() != null)
			return "Valore tra " + trimNumber(field.getMinimum()) + " e " + trimNumber(field.getMaximum());
		if (field.getMinimum() != null)
			return "Valore ≥ " + trimNumber(field.getMinimum());
		if (field.getMaximum() != null)
			return "Valore ≤ " + trimNumber(field.getMaximum());
		return "Valore";
	}

	private String trimNumber(Double value) {
		if (value == Math.rint(value) && Math.abs(value) < 1e15)
			return String.valueOf(value.longValue());
		return String.valueOf(value);
	}

	/** Raccoglie i valori inseriti nel form, convertiti al tipo del campo. */
	private Map<String, Object> collectSchemaValues() {
		Map<String, Object> values = new HashMap<>();
		DataSchema schema = campaign.getDataSchema();
		for (DataSchemaField field : schema.getFields()) {
			View input = schemaInputs.get(field.getName());
			if (input instanceof Spinner) {
				Object selected = ((Spinner) input).getSelectedItem();
				if (selected != null && !"—".equals(selected)) {
					values.put(field.getName(), convertEnumValue(field, (String) selected));
				}
			} else if (input instanceof Switch) {
				values.put(field.getName(), ((Switch) input).isChecked());
			} else if (input instanceof EditText) {
				String raw = ((EditText) input).getText().toString().trim();
				if (raw.isEmpty())
					continue;
				switch (field.getType()) {
					case NUMBER:
						try {
							values.put(field.getName(), Double.parseDouble(raw.replace(',', '.')));
						} catch (NumberFormatException e) {
							values.put(field.getName(), raw);
						}
						break;
					case INTEGER:
						try {
							values.put(field.getName(), Long.parseLong(raw));
						} catch (NumberFormatException e) {
							values.put(field.getName(), raw);
						}
						break;
					default:
						values.put(field.getName(), raw);
				}
			}
		}
		return values;
	}

	/** Riconverte il valore selezionato nello spinner al tipo dichiarato. */
	private Object convertEnumValue(DataSchemaField field, String selected) {
		switch (field.getType()) {
			case NUMBER:
				try {
					return Double.parseDouble(selected);
				} catch (NumberFormatException e) {
					return selected;
				}
			case INTEGER:
				try {
					return Long.parseLong(selected);
				} catch (NumberFormatException e) {
					return selected;
				}
			default:
				return selected;
		}
	}

	@Override
	public void onCancel(@NonNull DialogInterface dialog) {
		NavHostFragment.findNavController(this)
				.navigate(R.id.action_campaignManualSendDialog_to_homeDetailsFragment);
		super.onDismiss(dialog);
	}
}
