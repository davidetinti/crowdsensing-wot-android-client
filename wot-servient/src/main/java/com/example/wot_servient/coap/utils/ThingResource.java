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
 * Endpoint for displaying a Thing Description.
 */
public class ThingResource extends AbstractResource {

	private static final String TAG = "ThingResource";
	private final ExposedThing thing;

	public ThingResource(ExposedThing thing) {
		super(thing.getId());
		this.thing = thing;
	}

	@Override
	public void handleGET(CoapExchange exchange) {
		Log.d(TAG, "Handles GET to " + getURI());
		String requestContentFormat = getOrDefaultRequestContentType(exchange);
		if (!ContentManager.isSupportedMediaType(requestContentFormat)) {
			Log.w(TAG, "Unsupported media type: " + requestContentFormat);
			String payload = "Unsupported Media Type (supported: " + String.join(", ", ContentManager.getSupportedMediaTypes()) + ")";
			exchange.respond(CoAP.ResponseCode.UNSUPPORTED_CONTENT_FORMAT, payload);
		}
		try {
			Content content = ContentManager.valueToContent(thing, requestContentFormat);
			int contentFormat = MediaTypeRegistry.parse(content.getType());
			exchange.respond(CoAP.ResponseCode.CONTENT, content.getBody(), contentFormat);
		} catch (ContentCodecException e) {
			Log.w(TAG, "Exception " + e);
			exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE, e.toString());
		}
	}
}
