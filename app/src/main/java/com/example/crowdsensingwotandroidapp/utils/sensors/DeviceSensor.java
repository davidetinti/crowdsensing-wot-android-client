package com.example.crowdsensingwotandroidapp.utils.sensors;

import android.content.Context;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Un sensore del dispositivo in grado di produrre il payload per uno o più
 * tipi di campagna. Per rendere condivisibile un nuovo tipo di dato con
 * lettura automatica basta implementare questa interfaccia e registrare
 * l'istanza nel {@link SensorRegistry}.
 */
public interface DeviceSensor {

	/** Valori del campo type delle campagne gestite da questo sensore. */
	List<String> getCampaignTypes();

	/** True se il sensore esiste su questo dispositivo. */
	boolean isAvailable(Context context);

	/**
	 * Legge il valore corrente; il future completa con null se la lettura
	 * non è possibile.
	 */
	CompletableFuture<Map<String, Object>> read(Context context);
}
