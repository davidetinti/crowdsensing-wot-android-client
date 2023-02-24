package com.example.wot_servient.la_mqtt.utils;

/**
 * A MqttProtocolException is thrown by mqtt binding when errors occur.
 */
public class LAMqttProtocolException extends Exception {

	public LAMqttProtocolException(String message) {
		super(message);
	}

	public LAMqttProtocolException(Throwable cause) {
		super(cause);
	}
}
