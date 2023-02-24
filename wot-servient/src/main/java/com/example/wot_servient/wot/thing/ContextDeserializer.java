package com.example.wot_servient.wot.thing;

import android.util.Log;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Deserializes the individual context or the list of contexts of a {@link Thing} from JSON. Is used
 * by Jackson
 */
class ContextDeserializer extends JsonDeserializer {

	@Override
	public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		JsonToken t = p.currentToken();
		if (t == JsonToken.VALUE_STRING) {
			return new ThingContext(p.getValueAsString());
		} else if (t == JsonToken.START_ARRAY) {
			ThingContext context = new ThingContext();
			ArrayNode arrayNode = p.getCodec().readTree(p);
			Iterator<JsonNode> arrayElements = arrayNode.elements();
			while (arrayElements.hasNext()) {
				JsonNode arrayElement = arrayElements.next();
				if (arrayElement instanceof TextNode) {
					context.addContext(arrayElement.asText());
				} else if (arrayElement instanceof ObjectNode) {
					Iterator<Map.Entry<String, JsonNode>> objectEntries = arrayElement.fields();
					while (objectEntries.hasNext()) {
						Map.Entry<String, JsonNode> objectEntry = objectEntries.next();
						String prefix = objectEntry.getKey();
						String url = objectEntry.getValue().asText();
						context.addContext(prefix, url);
					}
				}
			}
			return context;
		} else {
			Log.w("ContextDeserializer", "Unable to deserialize Context of type " + t);
			return null;
		}
	}
}
