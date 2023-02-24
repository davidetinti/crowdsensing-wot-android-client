package com.example.wot_servient.wot.binding;

import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.thing.Thing;
import com.example.wot_servient.wot.thing.filter.ThingFilter;
import com.example.wot_servient.wot.thing.form.Form;
import com.example.wot_servient.wot.thing.security.SecurityScheme;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.reactivex.rxjava3.core.Observable;
//import static java.util.concurrent.CompletableFuture.failedFuture;// From Java 9

/**
 * A ProtocolClient defines how to interact with a Thing via a specific protocol (e.g. HTTP, MQTT,
 * etc.).
 */
public interface ProtocolClient {

	/**
	 * Reads the resource defined in <code>form</code>. This can be a {@link
	 * com.example.wot_servient.wot.thing.property.ThingProperty}, a {@link Thing} or a Thing Directory.
	 *
	 * @param form
	 * @return
	 */
	default CompletableFuture<Content> readResource(Form form) throws IOException {
		//return failedFuture(new ProtocolClientNotImplementedException(getClass(), "read"));// From Java 9
		// Workaround before Java 9
		CompletableFuture<Content> toReturn = new CompletableFuture<>();
		toReturn.completeExceptionally(new ProtocolClientNotImplementedException(getClass(), "read"));
		return toReturn;
	}

	/**
	 * Writes <code>content</code> to the resource defined in <code>form</code>. This can be, for
	 * example, a {@link com.example.wot_servient.wot.thing.property.ThingProperty}.
	 *
	 * @param form
	 * @param content
	 * @return
	 */
	default CompletableFuture<Content> writeResource(Form form, Content content) throws IOException {
		//return failedFuture(new ProtocolClientNotImplementedException(getClass(), "write"));// From Java 9
		// Workaround before Java 9
		CompletableFuture<Content> toReturn = new CompletableFuture<>();
		toReturn.completeExceptionally(new ProtocolClientNotImplementedException(getClass(), "write"));
		return toReturn;
	}

	/**
	 * Invokes the resource defined in the <code>form</code>. This can be a {@link
	 * com.example.wot_servient.wot.thing.action.ThingAction}, for example.
	 *
	 * @param form
	 * @return
	 */
	default CompletableFuture<Content> invokeResource(Form form) throws IOException {
		return invokeResource(form, null);
	}

	/**
	 * Invokes the resource defined in the <code>form</code> with the payload defined in
	 * <code>content</code>. This can be a {@link com.example.wot_servient.wot.thing.action.ThingAction}, for
	 * example.
	 *
	 * @param form
	 * @param content
	 * @return
	 */
	default CompletableFuture<Content> invokeResource(Form form, Content content) throws IOException {
		//return failedFuture(new ProtocolClientNotImplementedException(getClass(), "invoke"));// From Java 9
		// Workaround before Java 9
		CompletableFuture<Content> toReturn = new CompletableFuture<>();
		toReturn.completeExceptionally(new ProtocolClientNotImplementedException(getClass(), "invoke"));
		return toReturn;
	}

	/**
	 * Create an observable for the resource defined in <code>form</code>. This resource can be, for
	 * example, an {@link com.example.wot_servient.wot.thing.event.ThingEvent} or an observable {@link
	 * com.example.wot_servient.wot.thing.property.ThingProperty}.
	 *
	 * @param form
	 * @return
	 */
	default Observable<Content> observeResource(Form form) throws ProtocolClientException, IOException {
		throw new ProtocolClientNotImplementedException(getClass(), "observe");
	}

	/**
	 * Adds the <code>metadata</code> with security mechanisms (e.g. use password authentication)
	 * and <code>credentials</code>credentials (e.g. password and username) of a things to the
	 * client.
	 *
	 * @param metadata
	 * @param credentials
	 * @return
	 */
	default boolean setSecurity(List<SecurityScheme> metadata, Object credentials) {
		return false;
	}

	/**
	 * Starts the discovery process that will provide Things that match the <code>filter</code>
	 * argument.
	 *
	 * @param filter
	 * @return
	 */
	default Observable<Thing> discover(ThingFilter filter) throws ProtocolClientNotImplementedException {
		throw new ProtocolClientNotImplementedException(getClass(), "discover");
	}
}
