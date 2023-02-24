package com.example.wot_servient.wot.thing.form;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A form contains all the information from an endpoint for communication.<br> See also:
 * https://www.w3.org/TR/wot-thing-description/#form
 */
public class Form {

	protected String href;
	@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
	@JsonInclude(JsonInclude.Include.NON_NULL) protected List<Operation> op;
	@JsonInclude(JsonInclude.Include.NON_NULL) protected String subprotocol;
	@JsonInclude(JsonInclude.Include.NON_NULL) protected String contentType;
	protected Map<String, Object> optionalProperties = new HashMap<>();

	public String getHref() {
		return href;
	}

	@JsonIgnore
	public String getHrefScheme() {
		try {
			// remove uri variables first
			String sanitizedHref = href;
			int index = href.indexOf('{');
			if (index != -1) {
				sanitizedHref = sanitizedHref.substring(0, index);
			}
			return new URI(sanitizedHref).getScheme();
		} catch (URISyntaxException e) {
			Log.w("Form", "Form href is invalid: " + e);
			return null;
		}
	}

	public List<Operation> getOp() {
		return op;
	}

	public String getSubprotocol() {
		return subprotocol;
	}

	public String getContentType() {
		return contentType;
	}

	@JsonAnySetter
	private void setOptionalForJackson(String name, String value) {
		getOptionalProperties().put(name, value);
	}

	@JsonAnyGetter
	public Map<String, Object> getOptionalProperties() {
		return optionalProperties;
	}

	public Object getOptional(String name) {
		return optionalProperties.get(name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(href, op, subprotocol, contentType, optionalProperties);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Form)) {
			return false;
		}
		Form form = (Form) o;
		return Objects.equals(href, form.href) && Objects.equals(op, form.op) && Objects.equals(subprotocol, form.subprotocol) && Objects.equals(contentType, form.contentType) && Objects.equals(optionalProperties, form.optionalProperties);
	}

	@Override
	public String toString() {
		return "Form{" + "href='" + href + '\'' + ", op=" + op + ", subprotocol='" + subprotocol + '\'' + ", contentType='" + contentType + '\'' + ", optionalProperties=" + optionalProperties + '}';
	}
}
