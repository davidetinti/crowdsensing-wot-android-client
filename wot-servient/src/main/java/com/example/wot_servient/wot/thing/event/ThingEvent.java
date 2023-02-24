package com.example.wot_servient.wot.thing.event;

import com.example.wot_servient.wot.thing.ThingInteraction;
import com.example.wot_servient.wot.thing.schema.DataSchema;
import com.example.wot_servient.wot.thing.schema.VariableDataSchema;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Objects;

/**
 * This class represents a read-only model of a thing event. The class {@link Builder} can be used
 * to build new thing event models. Used in combination with {@link com.example.wot_servient.wot.thing.Thing}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThingEvent<T> extends ThingInteraction<ThingEvent<T>> {

	@JsonInclude(JsonInclude.Include.NON_NULL) @JsonDeserialize(as = VariableDataSchema.class)
	DataSchema<T> data;
	@JsonInclude(JsonInclude.Include.NON_NULL) String type;

	public String getType() {
		return type;
	}

	public DataSchema<T> getData() {
		return data;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), data, type);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ThingEvent)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		ThingEvent<Object> that = (ThingEvent<Object>) o;
		return Objects.equals(data, that.data) && Objects.equals(type, that.type);
	}

	@Override
	public String toString() {
		return "ThingEvent{" + "data=" + data + ", type='" + type + '\'' + ", name=" + name + ", description='" + description + '\'' + ", descriptions=" + descriptions + ", forms=" + forms + ", uriVariables=" + uriVariables + '}';
	}
}
