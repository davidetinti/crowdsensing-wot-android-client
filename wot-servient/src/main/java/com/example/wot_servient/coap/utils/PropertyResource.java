package com.example.wot_servient.coap.utils;

import android.util.Log;

import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentCodecException;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.property.ExposedThingProperty;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.server.resources.CoapExchange;

/**
 * Endpoint for interaction with a {@link com.example.wot_servient.wot.thing.property.ThingProperty}.
 */
public class PropertyResource extends AbstractResource {

	private static final String TAG = "PropertyResource";
	private final ExposedThingProperty<Object> property;

	public PropertyResource(String name, ExposedThingProperty<Object> property) {
		super(name);
		this.property = property;
	}

	@Override
	public void handleGET(CoapExchange exchange) {
		Log.d(TAG, "Handle GET to " + getURI());
		if (!property.isWriteOnly()) {
			String requestContentFormat = getOrDefaultRequestContentType(exchange);
			property.read().whenComplete((value, e) -> {
				if (e == null) {
					try {
						Content content = ContentManager.valueToContent(value, requestContentFormat);
						int contentFormat = MediaTypeRegistry.parse(content.getType());
						exchange.respond(CoAP.ResponseCode.CONTENT, content.getBody(), contentFormat);
					} catch (ContentCodecException ex) {
						Log.w(TAG, "Unable to serialize new property value " + ex);
						exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE, ex.toString());
					}
				} else {
					Log.w(TAG, "Unable to read property value " + e);
					exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE, e.toString());
				}
			});
		} else {
			exchange.respond(CoAP.ResponseCode.METHOD_NOT_ALLOWED, "Property writeOnly");
		}
	}

	@Override
	public void handlePUT(CoapExchange exchange) {
		Log.d(TAG, "WotCoapServer handles PUT to " + getURI());
		String requestContentFormat = getOrDefaultRequestContentType(exchange);
		if (!ContentManager.isSupportedMediaType(requestContentFormat)) {
			Log.w(TAG, "Unsupported media type: " + requestContentFormat);
			String payload = "Unsupported Media Type (supported: " + String.join(", ", ContentManager.getSupportedMediaTypes()) + ")";
			exchange.respond(CoAP.ResponseCode.UNSUPPORTED_CONTENT_FORMAT, payload);
		}
		if (!property.isReadOnly()) {
			writeProperty(exchange, requestContentFormat);
		} else {
			exchange.respond(CoAP.ResponseCode.METHOD_NOT_ALLOWED, "Property writeOnly");
		}
	}

	private void writeProperty(CoapExchange exchange, String requestContentFormat) {
		byte[] requestPayload = exchange.getRequestPayload();
		try {
			Object input = ContentManager.contentToValue(new Content(requestContentFormat, requestPayload), property);
			property.write(input).whenComplete((output, e) -> {
				if (e == null) {
					if (output != null) {
						try {
							Content outputContent = ContentManager.valueToContent(output, requestContentFormat);
							int outputContentFormat = MediaTypeRegistry.parse(outputContent.getType());
							exchange.respond(CoAP.ResponseCode.CHANGED, outputContent.getBody(), outputContentFormat);
						} catch (ContentCodecException ex) {
							exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE, ex.getMessage());
						}
					} else {
						exchange.respond(CoAP.ResponseCode.CHANGED, new byte[0], MediaTypeRegistry.parse(requestContentFormat));
					}
				} else {
					Log.w(TAG, "Unable to write property value " + e);
					exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE, e.getMessage());
				}
			});
		} catch (ContentCodecException ex) {
			exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE, ex.getMessage());
		}
	}
}
