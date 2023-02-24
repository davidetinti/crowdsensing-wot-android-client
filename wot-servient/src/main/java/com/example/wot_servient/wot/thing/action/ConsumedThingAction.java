package com.example.wot_servient.wot.thing.action;

import android.util.Log;

import com.example.wot_servient.wot.binding.ProtocolClient;
import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentCodecException;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.ConsumedThing;
import com.example.wot_servient.wot.thing.ConsumedThingException;
import com.example.wot_servient.wot.thing.form.Form;
import com.example.wot_servient.wot.thing.form.Operation;
import com.example.wot_servient.wot.utilities.Pair;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Used in combination with {@link ConsumedThing} and allows consuming of a {@link ThingAction}.
 */
public class ConsumedThingAction<I, O> extends ThingAction<I, O> {

	private final ConsumedThing thing;

	public ConsumedThingAction(ThingAction<I, O> action, ConsumedThing thing) {
		this.thing = thing;
		name = action.getName();
		forms = action.getForms();
		input = action.getInput();
		output = action.getOutput();
		description = action.getDescription();
		descriptions = action.getDescriptions();
		forms = action.getForms();
		uriVariables = action.getUriVariables();
	}

	/**
	 * Invokes this action without parameters. Returns a future with the return result of the
	 * action.
	 *
	 * @return
	 */
	public CompletableFuture<O> invoke() {
		return invoke(Collections.emptyMap());
	}

	/**
	 * Invokes this action and passes <code>parameters</codes> to it. Returns a future with the
	 * return result of the action.
	 *
	 * @param parameters contains a map with the names of the uri variables as keys and
	 *                   corresponding values (ex. <code>Map.of("step", 3)</code>).
	 * @return
	 */
	public CompletableFuture<O> invoke(Map<String, Object> parameters) {
		try {
			Pair<ProtocolClient, Form> clientAndForm = thing.getClientFor(getForms(), Operation.INVOKE_ACTION);
			ProtocolClient client = clientAndForm.first();
			Form form = clientAndForm.second();
			Log.d("ConsumedThingAction", "Thing " + thing.getId() + " invoking Action " + name + " with form " + form.getHref() + " and parameters " + parameters);
			Content input = null;
			if (!parameters.isEmpty()) {
				input = ContentManager.valueToContent(parameters, form.getContentType());
			}
			form = ConsumedThing.handleUriVariables(form, parameters);
			CompletableFuture<Content> result = client.invokeResource(form, input);
			return result.thenApply(content -> {
				try {
					return ContentManager.contentToValue(content, getOutput());
				} catch (ContentCodecException e) {
					throw new CompletionException(new ConsumedThingException("Received invalid writeResource from Thing: " + e.getMessage()));
				}
			});
		} catch (ContentCodecException e) {
			throw new CompletionException(new ConsumedThingException("Received invalid input: " + e.getMessage()));
		} catch (ConsumedThingException | IOException e) {
			throw new CompletionException(e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), name, thing);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		ConsumedThingAction<?, ?> that = (ConsumedThingAction<?, ?>) o;
		return Objects.equals(name, that.name) && Objects.equals(thing, that.thing);
	}

	@Override
	public String toString() {
		return "ConsumedThingAction{" + "input=" + input + ", output=" + output + ", name='" + name + '\'' + ", description='" + description + '\'' + ", descriptions=" + descriptions + ", forms=" + forms + ", uriVariables=" + uriVariables + '}';
	}
}
