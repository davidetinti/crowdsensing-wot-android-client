package com.example.wot_servient.wot.content;

import com.example.wot_servient.wot.ServientException;

/**
 * If errors occur during (de)serialization, this exception is thrown.
 */
public class ContentCodecException extends ServientException {

	public ContentCodecException(String message) {
		super(message);
	}

	public ContentCodecException(Throwable cause) {
		super(cause);
	}
}
