package com.example.wot_servient.wot.thing.property;

import com.example.wot_servient.wot.thing.ThingInteractionBuilder;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows building new {@link ThingProperty} objects.
 */
public class ThingPropertyBuilder extends ThingInteractionBuilder<ThingPropertyBuilder> {

	private String objectType;
	private String type = "string";
	private boolean observable;
	private boolean readOnly;
	private boolean writeOnly;
	private Map<String, Object> optionalProperties = new HashMap<>();

	public ThingPropertyBuilder setObjectType(String objectType) {
		this.objectType = objectType;
		return this;
	}

	public ThingPropertyBuilder setType(String type) {
		this.type = type;
		return this;
	}

	public ThingPropertyBuilder setObservable(boolean observable) {
		this.observable = observable;
		return this;
	}

	public ThingPropertyBuilder setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

	public ThingPropertyBuilder setWriteOnly(boolean writeOnly) {
		this.writeOnly = writeOnly;
		return this;
	}

	public ThingPropertyBuilder setOptionalProperties(Map<String, Object> optionalProperties) {
		this.optionalProperties = optionalProperties;
		return this;
	}

	@JsonAnySetter
	public ThingPropertyBuilder setOptional(String name, String value) {
		optionalProperties.put(name, value);
		return this;
	}

	@Override
	public ThingProperty<Object> build() {
		ThingProperty<Object> property = new ThingProperty<>();
		property.objectType = objectType;
		property.type = type;
		property.observable = observable;
		property.readOnly = readOnly;
		property.writeOnly = writeOnly;
		property.optionalProperties = optionalProperties;
		applyInteractionParameters(property);
		return property;
	}
}