package com.example.crowdsensingwotandroidapp.utils.campaign.schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Singolo campo di un data_schema di campagna: tipo, etichetta, unità,
 * limiti numerici, valori ammessi e obbligatorietà.
 */
public class DataSchemaField implements Serializable {

	public enum FieldType {
		NUMBER, INTEGER, STRING, BOOLEAN;

		static FieldType fromString(String raw) {
			if (raw == null)
				return STRING;
			switch (raw) {
				case "number":
					return NUMBER;
				case "integer":
					return INTEGER;
				case "boolean":
					return BOOLEAN;
				case "string":
				default:
					return STRING;
			}
		}
	}

	private final String name;
	private final FieldType type;
	private final String title;
	private final String description;
	private final String unit;
	private final Double minimum;
	private final Double maximum;
	private final ArrayList<String> allowedValues;
	private final boolean required;

	DataSchemaField(String name, HashMap property, boolean required) {
		this.name = name;
		this.type = FieldType.fromString((String) property.get("type"));
		this.title = (String) property.get("title");
		this.description = (String) property.get("description");
		this.unit = (String) property.get("unit");
		Number min = (Number) property.get("minimum");
		this.minimum = min != null ? min.doubleValue() : null;
		Number max = (Number) property.get("maximum");
		this.maximum = max != null ? max.doubleValue() : null;
		List rawAllowed = (List) property.get("enum");
		if (rawAllowed != null) {
			this.allowedValues = new ArrayList<>();
			for (Object value : rawAllowed) {
				this.allowedValues.add(String.valueOf(value));
			}
		} else {
			this.allowedValues = null;
		}
		this.required = required;
	}

	public String getName() {
		return name;
	}

	public FieldType getType() {
		return type;
	}

	public String getLabel() {
		return title != null ? title : name;
	}

	public String getDescription() {
		return description;
	}

	public String getUnit() {
		return unit;
	}

	public Double getMinimum() {
		return minimum;
	}

	public Double getMaximum() {
		return maximum;
	}

	public ArrayList<String> getAllowedValues() {
		return allowedValues;
	}

	public boolean isRequired() {
		return required;
	}
}
