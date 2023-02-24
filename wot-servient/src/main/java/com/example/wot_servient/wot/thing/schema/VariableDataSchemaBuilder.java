package com.example.wot_servient.wot.thing.schema;

import com.example.wot_servient.wot.utilities.ObjectBuilder;

/**
 * Allows building new {@link VariableDataSchema} objects.
 */
public class VariableDataSchemaBuilder implements ObjectBuilder<VariableDataSchema> {

	private String type;

	public VariableDataSchemaBuilder setType(String type) {
		this.type = type;
		return this;
	}

	@Override
	public VariableDataSchema build() {
		VariableDataSchema schema = new VariableDataSchema();
		schema.type = type;
		return schema;
	}
}