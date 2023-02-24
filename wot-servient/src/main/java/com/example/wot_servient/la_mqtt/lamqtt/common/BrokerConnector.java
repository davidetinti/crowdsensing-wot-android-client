package com.example.wot_servient.la_mqtt.lamqtt.common;

import java.util.concurrent.CompletableFuture;

public interface BrokerConnector {
    CompletableFuture<Boolean> connect(BrokerConf conf);

    CompletableFuture<Boolean> publish(String topic, String message);

    CompletableFuture<Boolean> subscribe(String topic, MQTTClient client);

    CompletableFuture<Boolean> disconnect();
}
