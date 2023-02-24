package com.example.wot_servient.wot.thing.event;

import android.util.Log;

import com.example.wot_servient.wot.thing.ExposedThing;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;
import java.util.Optional;

import io.reactivex.rxjava3.core.Observable;

/**
 * Used in combination with {@link ExposedThing} and allows exposing of a {@link ThingEvent}.
 */
public class ExposedThingEvent<T> extends ThingEvent<T> {

	//    private final String name;
	@JsonIgnore private final EventState<T> state;

	public ExposedThingEvent(ThingEvent<T> event) {
		this(new EventState<>());
		name = event.getName();
		description = event.getDescription();
		descriptions = event.getDescriptions();
		uriVariables = event.getUriVariables();
		type = event.getType();
		data = event.getData();
	}

	ExposedThingEvent(EventState<T> state) {
		this.state = state;
	}

	public EventState<T> getState() {
		return state;
	}

	public void emit() {
		emit(null);
	}

	public void emit(T data) {
		Log.d("ExposedThingEvent", "Event " + name + " has been emitted");
		state.getSubject().onNext(Optional.ofNullable(data));
	}

	public Observable<Optional<T>> observer() {
		return state.getSubject();
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), name, state);
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
		ExposedThingEvent<?> that = (ExposedThingEvent<?>) o;
		return Objects.equals(name, that.name) && Objects.equals(state, that.state);
	}

	@Override
	public String toString() {
		return "ExposedThingEvent{" + "state=" + state + ", data=" + data + ", type='" + type + '\'' + ", name='" + name + '\'' + ", description='" + description + '\'' + ", descriptions=" + descriptions + ", forms=" + forms + ", uriVariables=" + uriVariables + '}';
	}
}
