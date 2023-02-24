package com.example.wot_servient.coap.utils;

import android.util.Log;

import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentCodecException;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.utilities.Pair;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;

import java.util.Optional;

import io.reactivex.rxjava3.core.Observable;

abstract class AbstractSubscriptionResource extends AbstractResource {

	private static final String TAG = "AbstractSubscriptionRes";
	private final String name;
	private final Observable<Optional<Object>> observable;
	private Pair<Optional, Throwable> last;

	AbstractSubscriptionResource(String resourceName, String name, Observable<Optional<Object>> observable) {
		super(resourceName);
		this.name = name;
		this.observable = observable;
		setObservable(true); // enable observing
		setObserveType(CoAP.Type.CON); // configure the notification type to CONs
		getAttributes().setObservable(); // mark observable in the Link-Format
		observable.subscribe(optional -> {
			last = new Pair<>(optional, null);
			changed();
		}, e -> {
			last = new Pair<>(null, e);
			changed();
		});
	}

	@Override
	public void handleGET(CoapExchange exchange) {
		Log.d(TAG, "Handle GET to " + getURI());
		if (!exchange.advanced().getRequest().isAcknowledged()) {
			// The requestor should only be informed about new values.
			// send acknowledgement
			exchange.accept();
			ObserveRelation relation = exchange.advanced().getRelation();
			//            relation.setEstablished(true);
			relation.setEstablished();
			addObserveRelation(relation);
		} else {
			Optional optional = last.first();
			Throwable e = last.second();
			String requestContentFormat = getOrDefaultRequestContentType(exchange);
			String subscribableType = observable.getClass().getSimpleName();
			if (e == null) {
				try {
					Object data = optional.orElse(null);
					Log.d(TAG, "New data received for " + subscribableType + " " + name + ": " + data);
					Content content = ContentManager.valueToContent(data, requestContentFormat);
					int contentFormat = MediaTypeRegistry.parse(content.getType());
					byte[] body = content.getBody();
					if (body.length > 0) {
						exchange.respond(CoAP.ResponseCode.CONTENT, body, contentFormat);
					} else {
						exchange.respond(CoAP.ResponseCode.CONTENT);
					}
				} catch (ContentCodecException ex) {
					Log.w(TAG, "Cannot process data for " + subscribableType + " " + name + ": " + ex.getMessage());
					exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE, "Invalid " + subscribableType + " Data");
				}
			} else {
				Log.w(TAG, "Cannot process data for " + subscribableType + " " + name + ": " + e.getMessage());
				exchange.respond(CoAP.ResponseCode.SERVICE_UNAVAILABLE, "Invalid " + subscribableType + " Data");
			}
		}
	}
}
