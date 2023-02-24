package com.example.wot_servient.wot.thing.property;

import android.util.Log;

import com.example.wot_servient.wot.binding.ProtocolClient;
import com.example.wot_servient.wot.binding.ProtocolClientException;
import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentCodecException;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.ConsumedThing;
import com.example.wot_servient.wot.thing.ConsumedThingException;
import com.example.wot_servient.wot.thing.form.Form;
import com.example.wot_servient.wot.thing.form.FormBuilder;
import com.example.wot_servient.wot.thing.form.Operation;
import com.example.wot_servient.wot.utilities.Pair;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Observable;

/**
 * Used in combination with {@link ConsumedThing} and allows consuming of a {@link ThingProperty}.
 */
public class ConsumedThingProperty<T> extends ThingProperty<T> {

	private static final String TAG = "ConsumedThingProperty";
	//    private final String name;
	private final ConsumedThing thing;

	public ConsumedThingProperty(ThingProperty<T> property, ConsumedThing thing) {
		name = property.getName();
		objectType = property.getObjectType();
		description = property.getDescription();
		type = property.getType();
		observable = property.isObservable();
		readOnly = property.isReadOnly();
		writeOnly = property.isWriteOnly();
		forms = normalizeHrefs(property.getForms(), thing);
		uriVariables = property.getUriVariables();
		optionalProperties = property.getOptionalProperties();
		this.thing = thing;
	}

	private List<Form> normalizeHrefs(List<Form> forms, ConsumedThing thing) {
		return forms.stream().map(f -> normalizeHref(f, thing)).collect(Collectors.toList());
	}

	private Form normalizeHref(Form form, ConsumedThing thing) {
		String base = thing.getBase();
		if (base != null && !base.isEmpty() && !form.getHref().matches("^(?i:[a-z+]+:).*")) {
			String normalizedHref = base + form.getHref();
			return new FormBuilder(form).setHref(normalizedHref).build();
		} else {
			return form;
		}
	}

	public CompletableFuture<T> read() {
		try {
			Pair<ProtocolClient, Form> clientAndForm = thing.getClientFor(getForms(), Operation.READ_PROPERTY);
			ProtocolClient client = clientAndForm.first();
			Form form = clientAndForm.second();
			Log.d("ConsumedThingProperty", "Thing " + thing.getId() + " reading Property " + name + " from " + form.getHref());
			CompletableFuture<Content> result = client.readResource(form);
			Log.d(TAG, "read: " + result.isDone());
			return result.thenApply(content -> {
				try {
					return ContentManager.contentToValue(content, this);
				} catch (ContentCodecException e) {
					throw new CompletionException(new ConsumedThingException("Received invalid writeResource from Thing: " + e.getMessage()));
				}
			});
		} catch (ConsumedThingException | IOException e) {
			throw new CompletionException(e);
		}
	}

	public CompletableFuture<T> write(T value) {
		try {
			Pair<ProtocolClient, Form> clientAndForm = thing.getClientFor(getForms(), Operation.WRITE_PROPERTY);
			ProtocolClient client = clientAndForm.first();
			Form form = clientAndForm.second();
			Log.d("ConsumedThingProperty", "ConsumedThing " + thing.getId() + " writing " + form.getHref());
			Content input = ContentManager.valueToContent(value, form.getContentType());
			CompletableFuture<Content> result = client.writeResource(form, input);
			return result.thenApply(content -> {
				try {
					return ContentManager.contentToValue(content, this);
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

	public Observable<Optional<T>> observer() throws ConsumedThingException {
		try {
			Pair<ProtocolClient, Form> clientAndForm = thing.getClientFor(getForms(), Operation.OBSERVE_PROPERTY);
			ProtocolClient client = clientAndForm.first();
			Form form = clientAndForm.second();
			return client.observeResource(form)
					.map(content -> Optional.ofNullable(ContentManager.contentToValue(content, this)));
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
		ConsumedThingProperty<?> that = (ConsumedThingProperty<?>) o;
		return Objects.equals(name, that.name) && Objects.equals(thing, that.thing);
	}

	@Override
	public String toString() {
		return "ConsumedThingProperty{" + "objectType='" + objectType + '\'' + ", type='" + type + '\'' + ", observable=" + observable + ", readOnly=" + readOnly + ", writeOnly=" + writeOnly + ", optionalProperties=" + optionalProperties + ", name='" + name + '\'' + ", description='" + description + '\'' + ", descriptions=" + descriptions + ", forms=" + forms + ", uriVariables=" + uriVariables + '}';
	}
}
