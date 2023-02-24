package com.example.wot_servient.wot.thing;

import com.example.wot_servient.wot.thing.action.ThingAction;
import com.example.wot_servient.wot.thing.event.ThingEvent;
import com.example.wot_servient.wot.thing.form.Form;
import com.example.wot_servient.wot.thing.property.ThingProperty;
import com.example.wot_servient.wot.thing.security.SecurityScheme;
import com.example.wot_servient.wot.utilities.ObjectBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Allows building new {@link Thing} objects.
 */
public class ThingBuilder implements ObjectBuilder<Thing> {

	private String objectType;
	private ThingContext objectContext;
	private String id;
	private String title;
	private Map<String, String> titles;
	private String description;
	private Map<String, String> descriptions;
	private Map<String, ThingProperty<Object>> properties = new HashMap<>();
	private Map<String, ThingAction<Object, Object>> actions = new HashMap<>();
	private Map<String, ThingEvent<Object>> events = new HashMap<>();
	private List<Form> forms = new ArrayList<>();
	private List<String> security = new ArrayList<>();
	private Map<String, SecurityScheme> securityDefinitions = new HashMap<>();
	private String base;

	public ThingBuilder setObjectType(String objectType) {
		this.objectType = objectType;
		return this;
	}

	public ThingBuilder setObjectContext(ThingContext objectContext) {
		this.objectContext = objectContext;
		return this;
	}

	public ThingBuilder setId(String id) {
		this.id = id;
		return this;
	}

	public ThingBuilder setTitle(String title) {
		this.title = title;
		return this;
	}

	public ThingBuilder setTitles(Map<String, String> titles) {
		this.titles = titles;
		return this;
	}

	public ThingBuilder setDescription(String description) {
		this.description = description;
		return this;
	}

	public ThingBuilder setDescriptions(Map<String, String> descriptions) {
		this.descriptions = descriptions;
		return this;
	}

	public ThingBuilder addProperty(String name, ThingProperty<Object> property) {
		properties.put(name, property);
		return this;
	}

	public ThingBuilder addAction(String name, ThingAction<Object, Object> action) {
		actions.put(name, action);
		return this;
	}

	public ThingBuilder addEvent(String name, ThingEvent<Object> event) {
		events.put(name, event);
		return this;
	}

	public ThingBuilder addForm(Form form) {
		forms.add(form);
		return this;
	}

	public ThingBuilder setForms(List<Form> forms) {
		this.forms = forms;
		return this;
	}

	public ThingBuilder setProperties(Map<String, ThingProperty<Object>> properties) {
		this.properties = properties;
		return this;
	}

	public ThingBuilder setActions(Map<String, ThingAction<Object, Object>> actions) {
		this.actions = actions;
		return this;
	}

	public ThingBuilder setEvents(Map<String, ThingEvent<Object>> events) {
		this.events = events;
		return this;
	}

	public ThingBuilder setSecurity(List<String> security) {
		this.security = security;
		return this;
	}

	public ThingBuilder setSecurityDefinitions(Map<String, SecurityScheme> securityDefinitions) {
		this.securityDefinitions = securityDefinitions;
		return this;
	}

	public ThingBuilder setBase(String base) {
		this.base = base;
		return this;
	}

	@Override
	public Thing build() {
		return new Thing(objectType, objectContext, id, title, titles, description, descriptions, properties, actions, events, forms, security, securityDefinitions, base);
	}
}
