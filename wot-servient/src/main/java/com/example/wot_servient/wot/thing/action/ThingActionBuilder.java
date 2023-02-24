package com.example.wot_servient.wot.thing.action;

import com.example.wot_servient.wot.thing.ThingInteractionBuilder;
import com.example.wot_servient.wot.thing.schema.DataSchema;
import com.example.wot_servient.wot.thing.schema.StringSchema;

/**
 * Allows building new {@link ThingAction} objects.
 */
public class ThingActionBuilder extends ThingInteractionBuilder<ThingActionBuilder> {

	private DataSchema input = new StringSchema();
	private DataSchema output = new StringSchema();

	public ThingActionBuilder setInput(DataSchema input) {
		this.input = input;
		return this;
	}

	public ThingActionBuilder setOutput(DataSchema output) {
		this.output = output;
		return this;
	}

	@Override
	public ThingAction<Object, Object> build() {
		ThingAction<Object, Object> action = new ThingAction<>(input, output);
		applyInteractionParameters(action);
		return action;
	}
}