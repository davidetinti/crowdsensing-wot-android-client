package com.example.wot_servient.coap.utils;

import com.example.wot_servient.wot.thing.event.ExposedThingEvent;

/**
 * Endpoint for interaction with a {@link com.example.wot_servient.wot.thing.event.ThingEvent}.
 */
public class EventResource extends AbstractSubscriptionResource {

	public EventResource(String name, ExposedThingEvent<Object> event) {
		super(name, name, event.observer());
	}
}
