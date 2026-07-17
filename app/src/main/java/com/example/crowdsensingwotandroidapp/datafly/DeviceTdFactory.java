package com.example.crowdsensingwotandroidapp.datafly;

import android.os.Build;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Genera la Thing Description del device dalle capability rilevate.
 *
 * Stessa struttura delle TD della tesi (id "smartphone_<uuid>", title col nome
 * del device) con in piu' le annotazioni del registry che rendono il device
 * "matchabile" dal Master (extractCapabilities in @datafly/specs):
 *   "@type": ["<semanticType>"], "dfy:spec": "<key>@<version>".
 *
 * Le form delle property puntano a un href segnaposto: l'esposizione reale del
 * servient (lettura push dal Master) si riaggancia in M7 col canale protetto.
 */
public final class DeviceTdFactory {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private DeviceTdFactory() {
	}

	public static ObjectNode build(String deviceId,
			List<SensorDiscovery.DiscoveredCapability> capabilities) {
		ObjectNode td = MAPPER.createObjectNode();
		ArrayNode context = td.putArray("@context");
		context.add("https://www.w3.org/2022/wot/td/v1.1");
		td.put("id", deviceId);
		td.put("title", Build.MANUFACTURER + " " + Build.MODEL);
		td.putArray("@type").add("dfy:WorkerDevice");

		ObjectNode securityDefinitions = td.putObject("securityDefinitions");
		securityDefinitions.putObject("nosec_sc").put("scheme", "nosec");
		td.putArray("security").add("nosec_sc");

		ObjectNode properties = td.putObject("properties");
		for (SensorDiscovery.DiscoveredCapability capability : capabilities) {
			ObjectNode property = properties.putObject(capability.affordanceName);
			property.putArray("@type").add(capability.semanticType);
			property.put("dfy:spec", capability.specRef());
			property.put("type", "object");
			ObjectNode form = property.putArray("forms").addObject();
			form.put("href", "urn:dfy:device:" + deviceId + ":" + capability.affordanceName);
			form.putArray("op").add("readproperty");
		}
		return td;
	}
}
