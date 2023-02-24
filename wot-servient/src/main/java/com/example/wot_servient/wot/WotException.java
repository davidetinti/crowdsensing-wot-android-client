package com.example.wot_servient.wot;

/**
 * An instance of WotException is thrown by an implementation of {@link Wot} when errors occur.
 */
public class WotException extends Exception {

	public WotException(String message) {
		super(message);
	}

	public WotException(Throwable cause) {
		super(cause);
	}

	public WotException() {
		super();
	}
}