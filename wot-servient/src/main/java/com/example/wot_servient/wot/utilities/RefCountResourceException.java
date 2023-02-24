package com.example.wot_servient.wot.utilities;

/**
 * A RefCountResourceException is thrown by the {@link RefCountResource} when errors occur.
 */
public class RefCountResourceException extends Exception {

	public RefCountResourceException(String message) {
		super(message);
	}

	public RefCountResourceException(Throwable cause) {
		super(cause);
	}
}
