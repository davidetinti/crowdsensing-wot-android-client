package com.example.wot_servient.wot.thing.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Defines the type of data. Used e.g. to define which input and output values a {@link
 * com.example.wot_servient.wot.thing.action.ThingAction} has or of which type a {@link
 * com.example.wot_servient.wot.thing.property.ThingProperty} is.<br> See also: https://www.w3.org/TR/wot-thing-description/#sec-data-schema-vocabulary-definition
 *
 * @param <T>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public interface DataSchema<T> {

	String getType();

	@JsonIgnore
	Class<T> getClassType();
}
