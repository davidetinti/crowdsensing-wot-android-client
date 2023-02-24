package com.example.wot_servient.wot.thing.form;

import com.example.wot_servient.wot.utilities.ObjectBuilder;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Allows building new {@link Form} objects.
 */
public class FormBuilder implements ObjectBuilder<Form> {

	private String href;
	private List<Operation> op;
	private String subprotocol;
	private String contentType;
	private Map<String, Object> optionalProperties = new HashMap<>();

	public FormBuilder(Form form) {
		href = form.getHref();
		op = form.getOp();
		subprotocol = form.getSubprotocol();
		contentType = form.getContentType();
		optionalProperties = form.getOptionalProperties();
	}

	public FormBuilder() {
		op = new ArrayList<>();
		optionalProperties = new HashMap<>();
	}

	public FormBuilder setHref(String href) {
		this.href = href;
		return this;
	}

	public FormBuilder setOp(Operation... op) {
		return setOp(new ArrayList<>(Arrays.asList(op)));
	}

	@JsonSetter
	public FormBuilder setOp(List<Operation> op) {
		this.op = op;
		return this;
	}

	public FormBuilder setOp(Operation op) {
		return setOp(new ArrayList<>(Arrays.asList(op)));
	}

	public FormBuilder addOp(Operation op) {
		this.op.add(op);
		return this;
	}

	public FormBuilder setSubprotocol(String subprotocol) {
		this.subprotocol = subprotocol;
		return this;
	}

	public FormBuilder setContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public FormBuilder setOptionalProperties(Map<String, Object> optionalProperties) {
		this.optionalProperties = optionalProperties;
		return this;
	}

	public FormBuilder setOptional(String name, Object value) {
		optionalProperties.put(name, value);
		return this;
	}

	public Form build() {
		Form form = new Form();
		form.href = href;
		form.op = op;
		form.subprotocol = subprotocol;
		form.contentType = contentType;
		form.optionalProperties = optionalProperties;
		return form;
	}
}