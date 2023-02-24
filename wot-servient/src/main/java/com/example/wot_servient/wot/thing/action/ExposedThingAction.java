package com.example.wot_servient.wot.thing.action;

import static java.util.concurrent.CompletableFuture.completedFuture;

import android.util.Log;

import com.example.wot_servient.wot.thing.ExposedThing;
import com.example.wot_servient.wot.thing.schema.DataSchema;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
//import static java.util.concurrent.CompletableFuture.failedFuture; From Java 9

/**
 * Used in combination with {@link ExposedThing} and allows exposing of a {@link ThingAction}.
 */
public class ExposedThingAction<I, O> extends ThingAction<I, O> {

	//    private final String name;
	private final ExposedThing thing;
	@JsonIgnore private final ActionState<I, O> state;

	public ExposedThingAction(ThingAction<I, O> action, ExposedThing thing) {
		this(thing, new ActionState<>(), action.getName(), action.getDescription(), action.getDescriptions(), action.getUriVariables(), action.getInput(), action.getOutput());
	}

	@SuppressWarnings("squid:S107")
	ExposedThingAction(ExposedThing thing, ActionState<I, O> state, String name, String description, Map<String, String> descriptions, Map<String, Map<String, Object>> uriVariables, DataSchema input, DataSchema output) {
		this.name = name;
		this.thing = thing;
		this.state = state;
		this.description = description;
		this.descriptions = descriptions;
		this.uriVariables = uriVariables;
		this.input = input;
		this.output = output;
	}

	/**
	 * Invokes the method and executes the handler defined in {@link #state}.
	 *
	 * @return
	 */
	public CompletableFuture<O> invoke() {
		return invoke(null);
	}

	/**
	 * Invokes the method and executes the handler defined in {@link #state}. <code>input</code>
	 * contains the request payload.
	 *
	 * @param input
	 * @return
	 */
	public CompletableFuture<O> invoke(I input) {
		return invoke(input, Collections.emptyMap());
	}

	/**
	 * Invokes the method and executes the handler defined in {@link #state}. <code>input</code>
	 * contains the request payload. <code>options</code> can contain additional data (for example,
	 * the query parameters when using COAP/HTTP).
	 *
	 * @param input
	 * @param options
	 * @return
	 */
	public CompletableFuture<O> invoke(I input, Map<String, Map<String, Object>> options) {
		Log.d("ExposedThingAction", thing.getId() + " has Action state of " + name + ": " + getState());
		if (getState().getHandler() != null) {
			Log.d("ExposedThingAction", thing.getId() + " calls registered handler for Action " + name + " with input " + input + " and options " + options);
			try {
				CompletableFuture<O> output = getState().getHandler().apply(input, options);
				if (output == null) {
					Log.w("ExposedThingAction", thing.getId() + ": Called registered handler for Action " + name + " returned null. This can cause problems. Give Future with null result back.");
					output = completedFuture(null);
				}
				return output;
			} catch (Exception e) {
				//return failedFuture(e);// From Java 9
				// Workaround before Java 9
				CompletableFuture<O> toReturn = new CompletableFuture<>();
				toReturn.completeExceptionally(e);
				return toReturn;
			}
		} else {
			Log.d("ExposedThingAction", thing.getId() + " has no handler for Action " + name);
			return completedFuture(null);
		}
	}

	public ActionState<I, O> getState() {
		return state;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), name, thing, state);
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
		ExposedThingAction<?, ?> that = (ExposedThingAction<?, ?>) o;
		return Objects.equals(name, that.name) && Objects.equals(thing, that.thing) && Objects.equals(state, that.state);
	}

	@Override
	public String toString() {
		return "ExposedThingAction{" + "state=" + state + ", input=" + input + ", output=" + output + ", name='" + name + '\'' + ", description='" + description + '\'' + ", descriptions=" + descriptions + ", forms=" + forms + ", uriVariables=" + uriVariables + '}';
	}
}
