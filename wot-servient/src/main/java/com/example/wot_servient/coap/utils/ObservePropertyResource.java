package com.example.wot_servient.coap.utils;

import com.example.wot_servient.wot.thing.property.ExposedThingProperty;

/**
 * Endpoint for subscribing to value changes for a {@link com.example.wot_servient.wot.thing.property.ThingProperty}.
 */
public class ObservePropertyResource extends AbstractSubscriptionResource {

	public ObservePropertyResource(String name, ExposedThingProperty<Object> property) {
		super("observable", name, property.observer());
	}
}
