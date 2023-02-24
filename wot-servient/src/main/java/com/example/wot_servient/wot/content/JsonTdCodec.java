package com.example.wot_servient.wot.content;

import com.example.wot_servient.wot.thing.schema.DataSchema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

/**
 * (De)serializes data in JSON format.
 */
public class JsonTdCodec implements ContentCodec {

	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	public String getMediaType() {
		return "application/td+json";
	}

	@Override
	public <T> T bytesToValue(byte[] body, DataSchema<T> schema, Map<String, String> parameters) throws ContentCodecException {
		try {
			return getMapper().readValue(body, schema.getClassType());
		} catch (IOException e) {
			throw new ContentCodecException("Failed to decode " + getMediaType() + ": " + e.toString());
		}
	}

	@Override
	public byte[] valueToBytes(Object value, Map<String, String> parameters) throws ContentCodecException {
		try {
			return getMapper().writeValueAsBytes(value);
		} catch (JsonProcessingException e) {
			throw new ContentCodecException("Failed to encode " + getMediaType() + ": " + e.toString());
		}
	}

	ObjectMapper getMapper() {
		return mapper;
	}
}
