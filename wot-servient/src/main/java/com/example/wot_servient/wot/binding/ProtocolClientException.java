package com.example.wot_servient.wot.binding;

import com.example.wot_servient.wot.ServientException;

/**
 * A ProtocolClientException is thrown by {@link ProtocolClient} implementations when errors occur.
 */
public class ProtocolClientException extends ServientException {

	public ProtocolClientException(String message) {
		super(message);
	}

	public ProtocolClientException(Throwable cause) {
		super(cause);
	}
}
