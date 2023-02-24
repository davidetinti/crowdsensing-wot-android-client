package com.example.wot_servient.wot.thing.event;

import com.example.wot_servient.wot.binding.ProtocolClient;
import com.example.wot_servient.wot.binding.ProtocolClientException;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.ConsumedThing;
import com.example.wot_servient.wot.thing.ConsumedThingException;
import com.example.wot_servient.wot.thing.form.Form;
import com.example.wot_servient.wot.thing.form.Operation;
import com.example.wot_servient.wot.utilities.Pair;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import io.reactivex.rxjava3.core.Observable;

/**
 * Used in combination with {@link ConsumedThing} and allows consuming of a {@link ThingEvent}.
 */
public class ConsumedThingEvent<T> extends ThingEvent<T> {

	//    private final String name;
	private final ConsumedThing thing;

	public ConsumedThingEvent(ThingEvent<T> event, ConsumedThing thing) {
		this.thing = thing;
		name = event.getName();
		forms = event.getForms();
		type = event.getType();
		data = event.getData();
	}

	public Observable<Optional<T>> observer() throws ConsumedThingException {
		try {
			Pair<ProtocolClient, Form> clientAndForm = thing.getClientFor(getForms(), Operation.SUBSCRIBE_EVENT);
			ProtocolClient client = clientAndForm.first();
			Form form = clientAndForm.second();
			return client.observeResource(form)
					.map(content -> Optional.ofNullable(ContentManager.contentToValue(content, getData())));
		} catch (ProtocolClientException | IOException e) {
			throw new ConsumedThingException(e);
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
		ConsumedThingEvent<?> that = (ConsumedThingEvent<?>) o;
		return Objects.equals(name, that.name) && Objects.equals(thing, that.thing);
	}

	@Override
	public String toString() {
		return "ConsumedThingEvent{" + "data=" + data + ", type='" + type + '\'' + ", name='" + name + '\'' + ", description='" + description + '\'' + ", descriptions=" + descriptions + ", forms=" + forms + ", uriVariables=" + uriVariables + '}';
	}
}
