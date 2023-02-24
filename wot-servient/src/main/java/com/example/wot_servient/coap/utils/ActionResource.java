package com.example.wot_servient.coap.utils;

import android.util.Log;

import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentCodecException;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.action.ExposedThingAction;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.server.resources.CoapExchange;

/**
 * Endpoint for interaction with a {@link com.example.wot_servient.wot.thing.action.ThingAction}.
 */
public class ActionResource extends AbstractResource {

	private static final String TAG = "ActionResource";
	private final ExposedThingAction<Object, Object> action;

	public ActionResource(String name, ExposedThingAction<Object, Object> action) {
		super(name);
		this.action = action;
	}

	@Override
	public void handlePOST(CoapExchange exchange) {
		Log.d(TAG, "Handle POST to " + getURI());
		String requestContentFormat = getOrDefaultRequestContentType(exchange);
		if (!ContentManager.isSupportedMediaType(requestContentFormat)) {
			Log.w(TAG, "Unsupported media type: " + requestContentFormat);
			String payload = "Unsupported Media Type (supported: " + String.join(", ", ContentManager.getSupportedMediaTypes()) + ")";
			exchange.respond(CoAP.ResponseCode.UNSUPPORTED_CONTENT_FORMAT, payload);
		}
		byte[] requestPayload = exchange.getRequestPayload();
		Content inputContent = new Content(requestContentFormat, requestPayload);
		try {
			Object input = ContentManager.contentToValue(inputContent, action.getInput());
			action.invoke(input).whenComplete((value, e) -> {
				if (e == null) {
					try {
						Content content = ContentManager.valueToContent(value, requestContentFormat);
						int contentFormat = MediaTypeRegistry.parse(content.getType());
						exchange.respond(CoAP.ResponseCode.CONTENT, content.getBody(), contentFormat);
					} catch (ContentCodecException ex) {
						exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE, ex.toString());
					}
				} else {
					exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE, e.toString());
				}
			});
		} catch (ContentCodecException e) {
			exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE, e.toString());
		}
	}
}
