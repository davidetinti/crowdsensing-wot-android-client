package com.example.wot_servient.wot.thing.event;

import com.example.wot_servient.wot.thing.ThingInteractionBuilder;
import com.example.wot_servient.wot.thing.schema.DataSchema;
import com.example.wot_servient.wot.thing.schema.StringSchema;

/**
 * Allows building new {@link ThingEvent} objects.
 */
public class ThingEventBuilder extends ThingInteractionBuilder<ThingEventBuilder> {

	private DataSchema data = new StringSchema();
	private String type;

	public ThingEventBuilder setData(DataSchema data) {
		this.data = data;
		return this;
	}

	public ThingEventBuilder setType(String type) {
		this.type = type;
		return this;
	}

	@Override
	public ThingEvent<Object> build() {
		ThingEvent<Object> event = new ThingEvent<>();
		event.data = data;
		event.type = type;
		applyInteractionParameters(event);
		return event;
	}
}
