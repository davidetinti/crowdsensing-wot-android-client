package com.example.wot_servient.wot.thing;

import com.example.wot_servient.wot.ServientException;

/**
 * A ConsumedThingException is thrown by {@link ConsumedThing} when errors occur.
 */
public class ConsumedThingException extends ServientException {

	public ConsumedThingException(String message) {
		super(message);
	}

	public ConsumedThingException(Throwable cause) {
		super(cause);
	}
}