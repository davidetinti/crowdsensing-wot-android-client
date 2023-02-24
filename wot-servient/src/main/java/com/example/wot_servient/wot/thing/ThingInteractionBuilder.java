package com.example.wot_servient.wot.thing;

import com.example.wot_servient.wot.thing.form.Form;
import com.example.wot_servient.wot.utilities.ObjectBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ThingInteractionBuilder<T extends ObjectBuilder> implements ObjectBuilder {

	String name;
	String description;
	Map<String, String> descriptions;
	List<Form> forms = new ArrayList<>();
	Map<String, Map<String, Object>> uriVariables = new HashMap<>();

	public T setName(String name) {
		this.name = name;
		return (T) this;
	}

	public T setDescription(String description) {
		this.description = description;
		return (T) this;
	}

	public T setDescription(Map<String, String> descriptions) {
		this.descriptions = descriptions;
		return (T) this;
	}

	public T setForms(List<Form> forms) {
		this.forms = forms;
		return (T) this;
	}

	public T addForm(Form form) {
		forms.add(form);
		return (T) this;
	}

	public T setUriVariables(Map<String, Map<String, Object>> uriVariables) {
		this.uriVariables = uriVariables;
		return (T) this;
	}

	protected void applyInteractionParameters(ThingInteraction interaction) {
		interaction.name = name;
		interaction.description = description;
		interaction.descriptions = descriptions;
		interaction.forms = forms;
		interaction.uriVariables = uriVariables;
	}
}
