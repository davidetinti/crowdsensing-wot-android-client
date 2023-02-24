package com.example.wot_servient.wot.thing;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a JSON-LD context.
 */
@JsonDeserialize(using = ContextDeserializer.class)
@JsonSerialize(using = ContextSerializer.class)
public class ThingContext {

	private final Map<String, String> urls = new HashMap<>();

	public ThingContext() {
	}

	public ThingContext(String url) {
		addContext(url);
	}

	public ThingContext(String prefix, String url) {
		addContext(prefix, url);
	}

	public ThingContext addContext(String url) {
		return addContext(null, url);
	}

	public ThingContext addContext(String prefix, String url) {
		urls.put(prefix, url);
		return this;
	}

	@Override
	public int hashCode() {
		return getUrls().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ThingContext)) {
			return false;
		}
		return getUrls().equals(((ThingContext) obj).getUrls());
	}

	@Override
	public String toString() {
		return "Context{" + "urls=" + urls + '}';
	}

	private Map<String, String> getUrls() {
		return urls;
	}

	public String getDefaultUrl() {
		return getUrl(null);
	}

	public String getUrl(String prefix) {
		return urls.get(prefix);
	}

	public Map<String, String> getPrefixedUrls() {
		return urls.entrySet().stream().filter(e -> e.getKey() != null)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
}
