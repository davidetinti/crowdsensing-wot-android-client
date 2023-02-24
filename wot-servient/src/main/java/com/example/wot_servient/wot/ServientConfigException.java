package com.example.wot_servient.wot;

/**
 * Un'istanza di ServientConfigException è lanciata dal {@link ServientConfig} quando si verificano errori.
 */
public class ServientConfigException extends ServientException {

	public ServientConfigException(Exception cause) {
		super(cause);
	}
}
