package com.example.wot_servient.wot;

/**
 * An instance of ServientException is thrown by {@link Servient} when errors occur.
 */
public class ServientException extends WotException {

	public ServientException(String message) {
		super(message);
	}

	public ServientException(Throwable cause) {
		super(cause);
	}

	public ServientException() {
		super();
	}
}