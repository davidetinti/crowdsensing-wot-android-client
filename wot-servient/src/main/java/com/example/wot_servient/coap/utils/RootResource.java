package com.example.wot_servient.coap.utils;

import android.util.Log;

import com.example.wot_servient.coap.server.WotCoapServer;
import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentCodecException;
import com.example.wot_servient.wot.content.ContentManager;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.server.resources.CoapExchange;

/**
 * Endpoint for listing all Things from the {@link com.example.wot_servient.wot.Servient}.
 */
public class RootResource extends AbstractResource {

	private static final String TAG = "RootResource";
	private final WotCoapServer server;

	public RootResource(WotCoapServer server) {
		super("");
		this.server = server;
	}

	@Override
	public void handleGET(CoapExchange exchange) {
		Log.d(TAG, "Handle GET to " + getURI());
		String requestContentFormat = getOrDefaultRequestContentType(exchange);
		if (ensureSupportedContentFormat(exchange, requestContentFormat)) {
			try {
				Content content = ContentManager.valueToContent(server.getProtocolServer()
																		.getThings(), requestContentFormat);
				int contentFormat = MediaTypeRegistry.parse(content.getType());
				exchange.respond(CoAP.ResponseCode.CONTENT, content.getBody(), contentFormat);
			} catch (ContentCodecException e) {
				Log.w(TAG, "Exception " + e);
				exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE, e.getMessage());
			}
		}
	}
}
