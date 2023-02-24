package com.example.wot_servient.wot.thing;

import android.util.Log;

import com.example.wot_servient.wot.thing.action.ThingAction;
import com.example.wot_servient.wot.thing.event.ThingEvent;
import com.example.wot_servient.wot.thing.form.Form;
import com.example.wot_servient.wot.thing.property.ThingProperty;
import com.example.wot_servient.wot.thing.security.SecurityScheme;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This class represents a read-only model of a thing.
 *
 * @param <P>
 * @param <A>
 * @param <E>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Thing<P extends ThingProperty<Object>, A extends ThingAction<Object, Object>, E extends ThingEvent<Object>> {

	private static final String TAG = "Thing";
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
	@JsonProperty("@type") @JsonInclude(JsonInclude.Include.NON_NULL) String objectType;
	@JsonProperty("@context") @JsonInclude(JsonInclude.Include.NON_NULL) ThingContext objectContext;
	@JsonInclude(JsonInclude.Include.NON_EMPTY) String id;
	@JsonInclude(JsonInclude.Include.NON_EMPTY) String title;
	@JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, String> titles;
	@JsonInclude(JsonInclude.Include.NON_EMPTY) String description;
	@JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, String> descriptions;
	@JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, P> properties = new HashMap<>();
	@JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, A> actions = new HashMap<>();
	@JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, E> events = new HashMap<>();
	@JsonInclude(JsonInclude.Include.NON_EMPTY) List<Form> forms = new ArrayList<>();
	@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
	@JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> security = new ArrayList<>();
	@JsonProperty("securityDefinitions") @JsonInclude(JsonInclude.Include.NON_EMPTY)
	Map<String, SecurityScheme> securityDefinitions = new HashMap<>();
	@JsonInclude(JsonInclude.Include.NON_EMPTY) String base;

	@SuppressWarnings("squid:S107")
	Thing(String objectType, ThingContext objectContext, String id, String title, Map<String, String> titles, String description, Map<String, String> descriptions, Map<String, P> properties, Map<String, A> actions, Map<String, E> events, List<Form> forms, List<String> security, Map<String, SecurityScheme> securityDefinitions, String base) {
		this.objectType = objectType;
		this.objectContext = objectContext;
		this.id = id;
		this.title = title;
		this.titles = titles;
		this.description = description;
		this.descriptions = descriptions;
		this.properties = properties;
		this.actions = actions;
		this.events = events;
		this.forms = forms;
		this.security = security;
		this.securityDefinitions = securityDefinitions;
		this.base = base;
	}

	public Thing() {
	}

	public static String randomId() {
		return "urn:uuid:" + UUID.randomUUID();
	}

	public static Thing fromJson(String json) {
		try {
			return JSON_MAPPER.readValue(json, Thing.class);
		} catch (IOException e) {
			Log.w("Thing", "Unable to read json " + e);
			return null;
		}
	}

	public static Thing fromJson(File json) {
		try {
			return JSON_MAPPER.readValue(json, Thing.class);
		} catch (IOException e) {
			Log.w("Thing", "Unable to read json " + e);
			return null;
		}
	}

	public static Thing fromMap(Map<String, Map> map) {
		return JSON_MAPPER.convertValue(map, Thing.class);
	}

	public String getObjectType() {
		return objectType;
	}

	public ThingContext getObjectContext() {
		return objectContext;
	}

	public String getTitle() {
		return title;
	}

	public Map<String, String> getTitles() {
		return titles;
	}

	public String getDescription() {
		return description;
	}

	public Map<String, String> getDescriptions() {
		return descriptions;
	}

	public List<Form> getForms() {
		return forms;
	}

	public P getProperty(String name) {
		return properties.get(name);
	}

	public Map<String, A> getActions() {
		return actions;
	}

	public A getAction(String name) {
		return actions.get(name);
	}

	public Map<String, E> getEvents() {
		return events;
	}

	public E getEvent(String name) {
		return events.get(name);
	}

	List<String> getSecurity() {
		return security;
	}

	Map<String, SecurityScheme> getSecurityDefinitions() {
		return securityDefinitions;
	}

	public String getBase() {
		return base;
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	public String getId() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Thing)) {
			return false;
		}
		return getId().equals(((Thing) obj).getId());
	}

	@Override
	public String toString() {
		return "Thing{" + "objectType='" + objectType + '\'' + ", objectContext=" + objectContext + ", id='" + id + '\'' + ", title='" + title + '\'' + ", titles=" + titles + ", description='" + description + '\'' + ", descriptions=" + descriptions + ", properties=" + properties + ", actions=" + actions + ", events=" + events + ", forms=" + forms + ", security=" + security + ", securityDefinitions=" + securityDefinitions + ", base='" + base + '\'' + '}';
	}

	public String toJson() {
		return toJson(false);
	}

	public String toJson(boolean prettyPrint) {
		try {
			if (prettyPrint) {
				return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
			} else {
				return JSON_MAPPER.writeValueAsString(this);
			}
		} catch (JsonProcessingException e) {
			Log.w("Thing", "Unable to create json " + e);
			return null;
		}
	}

	/**
	 * Returns a map of the properties and their keys that have the non-expanded JSON-LD type
	 * <code>objectType</code>.
	 *
	 * @param objectType
	 * @return
	 */
	public Map<String, P> getPropertiesByObjectType(String objectType) {
		return getPropertiesByExpandedObjectType(getExpandedObjectType(objectType));
	}

	/**
	 * Returns a map of the properties and their keys that have the expanded JSON-LD type
	 * <code>objectType</code>.
	 *
	 * @param objectType
	 * @return
	 */
	public Map<String, P> getPropertiesByExpandedObjectType(String objectType) {
		HashMap<String, P> results = new HashMap<>();
		getProperties().forEach((key, property) -> {
			if (getExpandedObjectType(property.getObjectType()) != null && getExpandedObjectType(property.getObjectType()).equals(objectType)) {
				results.put(key, property);
			}
		});
		return results;
	}

	public String getExpandedObjectType(String objectType) {
		if (objectType == null || objectContext == null) {
			return null;
		}
		String[] parts = objectType.split(":", 2);
		String prefix;
		String suffix;
		if (parts.length == 2) {
			prefix = parts[0];
			suffix = parts[1];
		} else {
			prefix = null;
			suffix = objectType;
		}
		String url = objectContext.getUrl(prefix);
		if (url != null) {
			return url + suffix;
		} else {
			return objectType;
		}
	}

	public Map<String, P> getProperties() {
		return properties;
	}

	public int getPropertiesNumber() {
		return properties.size();
	}

	public int getActionsNumber() {
		return actions.size();
	}
}
