package com.example.wot_servient.coap.utils;

import android.util.Log;

import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentCodecException;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.ExposedThing;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.server.resources.CoapExchange;

/**
 * Endpoint for reading all properties from a Thing
 */
public class AllPropertiesResource extends AbstractResource {

	private static final String TAG = "AllPropertiesResource";
	private final ExposedThing thing;

	public AllPropertiesResource(ExposedThing thing) {
		super("properties");
		this.thing = thing;
	}

	@Override
	public void handleGET(CoapExchange exchange) {
		Log.d(TAG, "Handle GET to " + getURI());
		String requestContentFormat = getOrDefaultRequestContentType(exchange);
		if (ensureSupportedContentFormat(exchange, requestContentFormat)) {
			thing.readProperties().whenComplete((values, e) -> {
				if (e == null) {
					// remove writeOnly properties
					values.entrySet()
							.removeIf(entry -> thing.getProperty(entry.getKey()).isWriteOnly());
					try {
						Content content = ContentManager.valueToContent(values, requestContentFormat);
						int contentFormat = MediaTypeRegistry.parse(content.getType());
						exchange.respond(CoAP.ResponseCode.CONTENT, content.getBody(), contentFormat);
					} catch (ContentCodecException ex) {
						Log.w(TAG, "Exception " + ex);
						exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE, ex.toString());
					}
				} else {
					Log.w(TAG, "Exception " + e);
					exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE, e.toString());
				}
			});
		}
	}
}
