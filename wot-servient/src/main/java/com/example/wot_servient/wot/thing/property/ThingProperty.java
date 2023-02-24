package com.example.wot_servient.wot.thing.property;

import com.example.wot_servient.wot.thing.ThingInteraction;
import com.example.wot_servient.wot.thing.schema.DataSchema;
import com.example.wot_servient.wot.thing.schema.VariableDataSchemaBuilder;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class represents a read-only model of a thing property. The class {@link ThingPropertyBuilder} can be
 * used to build new thing property models. Used in combination with {@link
 * com.example.wot_servient.wot.thing.Thing}
 */
public class ThingProperty<T> extends ThingInteraction<ThingProperty<T>> implements DataSchema<T> {

	@JsonProperty("@type") @JsonInclude(JsonInclude.Include.NON_NULL) String objectType;
	@JsonInclude(JsonInclude.Include.NON_NULL) String type;
	@JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean observable;
	@JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean readOnly;
	@JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean writeOnly;
	Map<String, Object> optionalProperties = new HashMap<>();

	public ThingProperty() {
	}

	public String getObjectType() {
		return objectType;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public Class<T> getClassType() {
		return new VariableDataSchemaBuilder().setType(type).build().getClassType();
	}

	public boolean isObservable() {
		return observable;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public boolean isWriteOnly() {
		return writeOnly;
	}

	@JsonAnyGetter
	public Map<String, Object> getOptionalProperties() {
		return optionalProperties;
	}

	public Object getOptional(String name) {
		return optionalProperties.get(name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), objectType, type, observable, readOnly, writeOnly, optionalProperties);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ThingProperty)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		ThingProperty<Object> that = (ThingProperty<Object>) o;
		return observable == that.observable && readOnly == that.readOnly && writeOnly == that.writeOnly && Objects.equals(objectType, that.objectType) && Objects.equals(type, that.type) && Objects.equals(optionalProperties, that.optionalProperties);
	}

	@Override
	public String toString() {
		return "ThingProperty{" + "objectType='" + objectType + '\'' + ", type='" + type + '\'' + ", observable=" + observable + ", readOnly=" + readOnly + ", writeOnly=" + writeOnly + ", optionalProperties=" + optionalProperties + ", name='" + name + '\'' + ", description='" + description + '\'' + ", descriptions=" + descriptions + ", forms=" + forms + ", uriVariables=" + uriVariables + '}';
	}
}
