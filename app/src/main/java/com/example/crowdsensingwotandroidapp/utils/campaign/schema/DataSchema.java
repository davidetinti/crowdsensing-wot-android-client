package com.example.crowdsensingwotandroidapp.utils.campaign.schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Schema dei dati di una campagna (campo data_schema del documento),
 * sottoinsieme di JSON Schema allineato al DataSchema delle WoT Thing
 * Description. Permette di definire nuovi tipi di dato condivisibili
 * senza modificare l'app: il form di invio manuale viene generato da qui
 * e la stessa validazione è replicata lato proxy.
 */
public class DataSchema implements Serializable {

	private final ArrayList<DataSchemaField> fields = new ArrayList<>();

	/**
	 * Restituisce lo schema decodificato dalla HashMap del documento
	 * campagna, o null se assente o non interpretabile.
	 */
	public static DataSchema fromMap(HashMap raw) {
		if (raw == null)
			return null;
		Object type = raw.get("type");
		if (type != null && !"object".equals(type))
			return null;
		Object properties = raw.get("properties");
		if (!(properties instanceof HashMap) || ((HashMap) properties).isEmpty())
			return null;
		return new DataSchema(raw, (HashMap) properties);
	}

	private DataSchema(HashMap raw, HashMap properties) {
		List required = raw.get("required") instanceof List ? (List) raw.get("required") : Collections.emptyList();
		for (Object key : properties.keySet()) {
			Object property = properties.get(key);
			if (property instanceof HashMap) {
				fields.add(new DataSchemaField((String) key, (HashMap) property, required.contains(key)));
			}
		}
		// L'ordine delle HashMap non è stabile: prima i campi obbligatori,
		// poi gli altri, in ordine alfabetico.
		Collections.sort(fields, (a, b) -> {
			if (a.isRequired() != b.isRequired())
				return a.isRequired() ? -1 : 1;
			return a.getName().compareTo(b.getName());
		});
	}

	public ArrayList<DataSchemaField> getFields() {
		return fields;
	}

	/**
	 * Valida i valori raccolti dal form prima dell'invio (la stessa
	 * validazione viene rifatta dal proxy). Restituisce null se tutto è
	 * valido, altrimenti il messaggio di errore da mostrare.
	 */
	public String validate(Map<String, Object> values) {
		for (DataSchemaField field : fields) {
			Object value = values.get(field.getName());
			if (value == null) {
				if (field.isRequired())
					return "Il campo \"" + field.getLabel() + "\" è obbligatorio";
				continue;
			}
			switch (field.getType()) {
				case NUMBER:
				case INTEGER:
					if (!(value instanceof Number) || value instanceof Boolean)
						return "Il campo \"" + field.getLabel() + "\" deve essere un numero";
					double number = ((Number) value).doubleValue();
					if (field.getType() == DataSchemaField.FieldType.INTEGER && number != Math.rint(number))
						return "Il campo \"" + field.getLabel() + "\" deve essere un numero intero";
					if (field.getMinimum() != null && number < field.getMinimum())
						return "Il campo \"" + field.getLabel() + "\" deve essere ≥ " + formatBound(field.getMinimum());
					if (field.getMaximum() != null && number > field.getMaximum())
						return "Il campo \"" + field.getLabel() + "\" deve essere ≤ " + formatBound(field.getMaximum());
					break;
				case STRING:
					if (!(value instanceof String))
						return "Il campo \"" + field.getLabel() + "\" deve essere un testo";
					break;
				case BOOLEAN:
					if (!(value instanceof Boolean))
						return "Il campo \"" + field.getLabel() + "\" deve essere vero o falso";
					break;
			}
			if (field.getAllowedValues() != null && field.getType() != DataSchemaField.FieldType.BOOLEAN
					&& !matchesAllowed(value, field.getAllowedValues()))
				return "Il campo \"" + field.getLabel() + "\" deve essere uno tra: " + String.join(", ", field.getAllowedValues());
		}
		return null;
	}

	/**
	 * Confronto con i valori ammessi tollerante ai numeri: "1" nello schema
	 * deve corrispondere sia a 1 sia a 1.0 nel payload.
	 */
	private boolean matchesAllowed(Object value, List<String> allowed) {
		for (String allowedString : allowed) {
			if (value instanceof Number && !(value instanceof Boolean)) {
				try {
					if (((Number) value).doubleValue() == Double.parseDouble(allowedString))
						return true;
				} catch (NumberFormatException ignored) {
				}
			}
			if (allowedString.equals(String.valueOf(value)))
				return true;
		}
		return false;
	}

	static String formatBound(double value) {
		if (value == Math.rint(value) && Math.abs(value) < 1e15)
			return String.valueOf((long) value);
		return String.valueOf(value);
	}
}
