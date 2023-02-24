package com.example.wot_servient.mqtt.utils;

/**
 * A MqttProtocolException is thrown by mqtt binding when errors occur.
 */
public class MqttProtocolException extends Exception {

	public MqttProtocolException(String message) {
		super(message);
	}

	public MqttProtocolException(Throwable cause) {
		super(cause);
	}
}
