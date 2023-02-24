package com.example.wot_servient.la_mqtt.lamqtt.common;

public class BrokerConf {

	String username;
	String password;
	String url;
	int port;
	String id;

	public BrokerConf(String username, String password, String url, int port, String id) {
		this.username = username;
		this.password = password;
		this.url = url;
		this.port = port;
		this.id = id;
	}
}