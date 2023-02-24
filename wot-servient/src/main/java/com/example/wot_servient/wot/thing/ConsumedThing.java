package com.example.wot_servient.wot.thing;

import android.util.Log;

import com.damnhandy.uri.template.UriTemplate;
import com.example.wot_servient.wot.Servient;
import com.example.wot_servient.wot.binding.ProtocolClient;
import com.example.wot_servient.wot.binding.ProtocolClientException;
import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentCodecException;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.action.ConsumedThingAction;
import com.example.wot_servient.wot.thing.action.ThingAction;
import com.example.wot_servient.wot.thing.event.ConsumedThingEvent;
import com.example.wot_servient.wot.thing.event.ThingEvent;
import com.example.wot_servient.wot.thing.form.Form;
import com.example.wot_servient.wot.thing.form.FormBuilder;
import com.example.wot_servient.wot.thing.form.Operation;
import com.example.wot_servient.wot.thing.property.ConsumedThingProperty;
import com.example.wot_servient.wot.thing.property.ThingProperty;
import com.example.wot_servient.wot.thing.schema.ObjectSchema;
import com.example.wot_servient.wot.thing.security.SecurityScheme;
import com.example.wot_servient.wot.utilities.Pair;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
//import static java.util.concurrent.CompletableFuture.failedFuture; // From Java 9

/**
 * Represents an object that extends a Thing with methods for client interactions (send request for
 * reading and writing Properties), invoke Actions, subscribe and unsubscribe for Property changes
 * and Events. https://w3c.github.io/wot-scripting-api/#the-consumedthing-interface
 */
public class ConsumedThing extends Thing<ConsumedThingProperty<Object>, ConsumedThingAction<Object, Object>, ConsumedThingEvent<Object>> {

	private static final String TAG = "ConsumedThing";
	private static final String DEFAULT_OBJECT_TYPE = "Thing";
	private static final ThingContext DEFAULT_OBJECT_CONTEXT = new ThingContext("https://www.w3.org/2019/wot/td/v1");
	private final Servient servient;
	private final Map<String, ProtocolClient> clients = new HashMap<>();

	public ConsumedThing(Servient servient, Thing thing) {
		this.servient = servient;
		if (thing != null) {
			objectType = thing.getObjectType();
			if (objectType == null) {
				objectType = DEFAULT_OBJECT_TYPE;
			}
			objectContext = thing.getObjectContext();
			if (objectContext == null) {
				objectContext = DEFAULT_OBJECT_CONTEXT;
			}
			id = thing.getId();
			title = thing.getTitle();
			titles = thing.getTitles();
			description = thing.getDescription();
			descriptions = thing.getDescriptions();
			forms = thing.getForms();
			security = thing.getSecurity();
			securityDefinitions = thing.getSecurityDefinitions();
			base = thing.getBase();
			Map<String, ThingProperty> properties = thing.getProperties();
			properties.forEach((name, property) -> {
				if (!property.getType().equals("localActionsUtil")) {
					this.properties.put(name, new ConsumedThingProperty<>(property, this));
				}
			});
			Map<String, ThingAction<Object, Object>> actions = thing.getActions();
			actions.forEach((name, action) -> this.actions.put(name, new ConsumedThingAction(action, this)));
			Map<String, ThingEvent<Object>> events = thing.getEvents();
			events.forEach((name, event) -> this.events.put(name, new ConsumedThingEvent<>(event, this)));
		}
	}

	/**
	 * Creates new form (if needed) for URI Variables http://192.168.178.24:8080/counter/actions/increment{?step}
	 * with '{'step' : 3}' -&gt; http://192.168.178.24:8080/counter/actions/increment?step=3.<br>
	 * see RFC6570 (https://tools.ietf.org/html/rfc6570) for URI Template syntax
	 */
	public static Form handleUriVariables(Form form, Map<String, Object> parameters) {
		String href = form.getHref();
		UriTemplate uriTemplate = UriTemplate.fromTemplate(href);
		String updatedHref = uriTemplate.expand(parameters);
		if (!href.equals(updatedHref)) {
			// "clone" form to avoid modifying original form
			Form updatedForm = new FormBuilder(form).setHref(updatedHref).build();
			Log.d("ConsumedThing", href + " update URI to " + updatedHref);
			return updatedForm;
		}
		return form;
	}

	public Pair<ProtocolClient, Form> getClientFor(Form form, Operation op) throws ConsumedThingException {
		return getClientFor(Collections.singletonList(form), op);
	}

	/**
	 * Searches and returns a ProtocolClient in given <code>forms</code> that matches the given
	 * <code>op</code>. Throws an exception when no client can be found.
	 *
	 * @param forms
	 * @param op
	 * @return
	 * @throws ConsumedThingException
	 */
	public Pair<ProtocolClient, Form> getClientFor(List<Form> forms, Operation op) throws ConsumedThingException {
		if (forms.isEmpty()) {
			throw new NoFormForInteractionConsumedThingException(getId(), op);
		}
		// Rimuove i protocolli che non supportano quella operazione
		Set<String> schemes = forms.stream().map(Form::getHrefScheme).filter(Objects::nonNull)
				.filter(s -> servient.checkIfSchemeHasOverridenMethodForOperation(s, op))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		if (schemes.isEmpty()) {
			throw new NoFormForInteractionConsumedThingException("No schemes in forms found");
		}
		// Search client from cache
		String scheme = null;
		ProtocolClient client = null;
		for (String s : schemes) {
			ProtocolClient c = clients.get(s);
			if (c != null) {
				scheme = s;
				client = c;
				break;
			}
		}
		if (client != null) {
			// from cache
			Log.d("ConsumedThing", getId() + " chose cached client for scheme " + scheme);
		} else {
			// new client
			Log.d("ConsumedThing", getId() + " has no client in cache. Try to init client for one of the following schemes: " + schemes);
			Pair<String, ProtocolClient> protocolClient = initNewClientFor(schemes);
			scheme = protocolClient.first();
			client = protocolClient.second();
			Log.d("ConsumedThing", getId() + " got new client for scheme " + scheme);
			clients.put(scheme, client);
		}
		Form form = getFormForOpAndScheme(forms, op, scheme);
		return new Pair<>(client, form);
	}

	private Pair<String, ProtocolClient> initNewClientFor(Set<String> schemes) throws ConsumedThingException {
		try {
			for (String scheme : schemes) {
				if (servient.hasClientFor(scheme)) {
					ProtocolClient client = servient.getClientFor(scheme);
					// init client's security system
					List<String> security = getSecurity();
					if (!security.isEmpty()) {
						Log.d("ConsumedThing", getId() + " setting credentials for " + client);
						Map<String, SecurityScheme> securityDefinitions = getSecurityDefinitions();
						List<SecurityScheme> metadata = security.stream()
								.map(securityDefinitions::get).filter(Objects::nonNull)
								.collect(Collectors.toList());
						client.setSecurity(metadata, servient.getCredentials(id));
					}
					return new Pair<>(scheme, client);
				}
			}
			throw new NoClientFactoryForSchemesConsumedThingException(getId(), schemes);
		} catch (ProtocolClientException e) {
			throw new ConsumedThingException("Unable to create client: " + e.getMessage());
		}
	}

	private Form getFormForOpAndScheme(List<Form> forms, Operation op, String scheme) throws NoFormForInteractionConsumedThingException {
		// Find right operation and corresponding scheme in the array form
		Form form = null;
		for (Form f : forms) {
			Log.d(TAG, f.getHref() + f.getOp());
			if (f.getOp() != null && f.getOp().contains(op) && f.getHrefScheme().equals(scheme)) {
				form = f;
				break;
			}
		}
		if (form == null) {
			// if there no op was defined use default assignment
			final String finalScheme = scheme;
			Optional<Form> nonOpForm = forms.stream()
					.filter(f -> (f.getOp() == null || f.getOp().isEmpty()) && f.getHrefScheme()
							.equals(finalScheme)).findFirst();
			if (nonOpForm.isPresent()) {
				form = nonOpForm.get();
			} else {
				throw new NoFormForInteractionConsumedThingException(getId(), op);
			}
		}
		return form;
	}

	public CompletableFuture<Map<String, Object>> readProperties(String... names) {
		return readProperties(Arrays.asList(names));
	}

	/**
	 * Returns the values of the properties contained in <code>names</code>.
	 *
	 * @param names
	 * @return
	 */
	public CompletableFuture<Map<String, Object>> readProperties(List<String> names) {
		// TODO: read only requested properties
		return readProperties().thenApply(values -> {
			Stream<Map.Entry<String, Object>> stream = values.entrySet().stream()
					.filter(e -> names.contains(e.getKey()));
			return stream.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		});
	}

	/**
	 * Returns the values of all properties.
	 *
	 * @return
	 */
	public CompletableFuture<Map<String, Object>> readProperties() {
		try {
			Pair<ProtocolClient, Form> clientAndForm = getClientFor(getForms(), Operation.READ_ALL_PROPERTIES);
			ProtocolClient client = clientAndForm.first();
			Form form = clientAndForm.second();
			Log.d("ConsumedThing", getId() + " reading " + form.getHref());
			CompletableFuture<Content> result = client.readResource(form);
			return result.thenApply(content -> {
				try {
					return ContentManager.contentToValue(content, new ObjectSchema());
				} catch (ContentCodecException e) {
					throw new CompletionException(new ConsumedThingException("Received invalid writeResource from Thing: " + e.getMessage()));
				}
			});
		} catch (ConsumedThingException | IOException e) {
			//return failedFuture(e);// From Java 9
			// Workaround before Java 9
			CompletableFuture<Map<String, Object>> toReturn = new CompletableFuture<>();
			toReturn.completeExceptionally(e);
			return toReturn;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), servient);
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
		ConsumedThing that = (ConsumedThing) o;
		return Objects.equals(servient, that.servient);
	}

	@Override
	public String toString() {
		return "ConsumedThing{" + "objectType='" + objectType + '\'' + ", objectContext=" + objectContext + ", id='" + id + '\'' + ", title='" + title + '\'' + ", titles=" + titles + ", description='" + description + '\'' + ", descriptions=" + descriptions + ", properties=" + properties + ", actions=" + actions + ", events=" + events + ", forms=" + forms + ", security=" + security + ", securityDefinitions=" + securityDefinitions + ", base='" + base + '\'' + '}';
	}
}
