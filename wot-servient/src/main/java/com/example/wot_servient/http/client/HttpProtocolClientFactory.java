package com.example.wot_servient.http.client;

import android.content.Context;

import com.example.wot_servient.wot.binding.ProtocolClientFactory;
import com.typesafe.config.Config;

/**
 * Creates new {@link HttpProtocolClient} instances.
 */
public class HttpProtocolClientFactory implements ProtocolClientFactory {

    public HttpProtocolClientFactory(Config config, Context context) {}

    @Override
    public String toString() {
        return "HttpClient";
    }

    @Override
    public String getScheme() {
        return "http";
    }

    @Override
    public HttpProtocolClient getClient() {
        return new HttpProtocolClient();
    }
}
