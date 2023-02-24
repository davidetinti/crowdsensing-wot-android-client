package com.example.wot_servient.wot.thing.property;

import static java.util.concurrent.CompletableFuture.completedFuture;

import android.util.Log;

import com.example.wot_servient.wot.thing.ExposedThing;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.reactivex.rxjava3.core.Observable;
//import static java.util.concurrent.CompletableFuture.failedFuture; From Java 9

/**
 * Used in combination with {@link ExposedThing} and allows exposing of a {@link ThingProperty}.
 */
public class ExposedThingProperty<T> extends ThingProperty<T> {

	//    private final String name;
	private final ExposedThing thing;
	@JsonIgnore private final PropertyState<T> state;

	@SuppressWarnings("squid:S107")
	public ExposedThingProperty(ExposedThing thing, PropertyState<T> state, String objectType, String name, String description, Map<String, String> descriptions, String type, boolean observable, boolean readOnly, boolean writeOnly, Map<String, Map<String, Object>> uriVariables, Map<String, Object> optionalProperties) {
		this.thing = thing;
		this.state = state;
		this.objectType = objectType;
		this.name = name;
		this.description = description;
		this.descriptions = descriptions;
		this.type = type;
		this.observable = observable;
		this.readOnly = readOnly;
		this.writeOnly = writeOnly;
		this.uriVariables = uriVariables;
		this.optionalProperties = optionalProperties;
	}

	public ExposedThingProperty(ThingProperty<T> property, ExposedThing thing) {
		this.thing = thing;
		state = new PropertyState<>();
		if (property != null) {
			name = property.getName();
			objectType = property.getObjectType();
			description = property.getDescription();
			descriptions = property.getDescriptions();
			type = property.getType();
			observable = property.isObservable();
			readOnly = property.isReadOnly();
			writeOnly = property.isWriteOnly();
			uriVariables = property.getUriVariables();
			optionalProperties = property.getOptionalProperties();
		}
	}

	public CompletableFuture<T> read() {
		// call read handler (if any)
		if (state.getReadHandler() != null) {
			Log.d("ExposedThingProperty", thing.getId() + " calls registered readHandler for Property " + name);
			// update internal state in case writeHandler wants to get the value
			try {
				return state.getReadHandler().get()
						.whenComplete((customValue, e) -> state.setValue(customValue));
			} catch (Exception e) {
				//return failedFuture(e);// From Java 9
				// Workaround before Java 9
				CompletableFuture<T> toReturn = new CompletableFuture<>();
				toReturn.completeExceptionally(e);
				return toReturn;
			}
		} else {
			CompletableFuture<T> future = new CompletableFuture<>();
			T value = state.getValue();
			Log.d("ExposedThingProperty", thing.getId() + " gets internal value " + value + " for Property " + name);
			future.complete(value);
			return future;
		}
	}

	public CompletableFuture<T> write(T value) {
		// call write handler (if any)
		if (state.getWriteHandler() != null) {
			Log.d("ExposedThingProperty", thing.getId() + " calls registered writeHandler for Property " + name);
			try {
				return state.getWriteHandler().apply(value).whenComplete((customValue, e) -> {
					Log.d("ExposedThingProperty", thing.getId() + " write handler for Property " + name + " sets custom value " + customValue);
					if (!Objects.equals(state.getValue(), customValue)) {
						state.setValue(customValue);
						// inform property observers
						state.getSubject().onNext(Optional.ofNullable(customValue));
					}
				});
			} catch (Exception e) {
				//return failedFuture(e);// From Java 9
				// Workaround before Java 9
				CompletableFuture<T> toReturn = new CompletableFuture<>();
				toReturn.completeExceptionally(e);
				return toReturn;
			}
		} else {
			if (!Objects.equals(state.getValue(), value)) {
				Log.d("ExposedThingProperty", thing.getId() + " sets Property " + name + " to internal value " + value);
				state.setValue(value);
				// inform property observers
				state.getSubject().onNext(Optional.ofNullable(value));
			}
			return completedFuture(null);
		}
	}

	public Observable<Optional<T>> observer() {
		return state.getSubject();
	}

	public PropertyState<T> getState() {
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
		ExposedThingProperty<?> that = (ExposedThingProperty<?>) o;
		return Objects.equals(name, that.name) && Objects.equals(thing, that.thing) && Objects.equals(state, that.state);
	}

	@Override
	public String toString() {
		return "ExposedThingProperty{" + "state=" + state + ", objectType='" + objectType + '\'' + ", type='" + type + '\'' + ", observable=" + observable + ", readOnly=" + readOnly + ", writeOnly=" + writeOnly + ", optionalProperties=" + optionalProperties + ", name='" + name + '\'' + ", description='" + description + '\'' + ", descriptions=" + descriptions + ", forms=" + forms + ", uriVariables=" + uriVariables + '}';
	}
}
