package com.example.wot_servient.la_mqtt.utils;

public class LAMqttBrokerServerConfig {

	public String uri;
	public String clientId;
	public LAMqttLocationProvider locationProvider;

	public LAMqttBrokerServerConfig(String uri, String clientId, LAMqttLocationProvider locationProvider) {
		this.uri = uri;
		this.clientId = clientId;
		this.locationProvider = locationProvider;
	}
}
