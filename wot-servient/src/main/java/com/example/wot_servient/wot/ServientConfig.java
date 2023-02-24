package com.example.wot_servient.wot;

import android.content.Context;
import android.util.Log;

import com.example.wot_servient.wot.binding.ProtocolClientFactory;
import com.example.wot_servient.wot.binding.ProtocolServer;
import com.example.wot_servient.wot.utilities.Pair;
import com.typesafe.config.Config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServientConfig {

	private static final String CONFIG_SERVERS = "wot.servient.servers";
	private static final String CONFIG_CLIENT_FACTORIES = "wot.servient.client-factories";
	private static final String CONFIG_CREDENTIALS = "wot.servient.credentials";
	private final List<ProtocolServer> servers;
	private final Map<String, ProtocolClientFactory> clientFactories;
	private final Map<String, Object> credentialStore;

	ServientConfig(List<ProtocolServer> servers, Map<String, ProtocolClientFactory> clientFactories, Map<String, Object> credentialStore) {
		this.servers = servers;
		this.clientFactories = clientFactories;
		this.credentialStore = credentialStore;
	}

	public ServientConfig(Config config, Context context) throws ServientConfigException {
		List<String> requiredServers = config.getStringList(CONFIG_SERVERS);
		servers = new ArrayList<>();
		for (String serverName : requiredServers) {
			ProtocolServer server = initializeServer(config, serverName, context);
			servers.add(server);
		}
		List<String> requiredFactories = config.getStringList(CONFIG_CLIENT_FACTORIES);
		clientFactories = new HashMap<>();
		for (String factoryName : requiredFactories) {
			Pair<String, ProtocolClientFactory> pair = initializeClientFactory(config, factoryName, context);
			clientFactories.put(pair.first(), pair.second());
		}
		credentialStore = new HashMap<>();
		if (config.hasPath(CONFIG_CREDENTIALS)) {
			addCredentials(config.getObject(CONFIG_CREDENTIALS).unwrapped());
		}
	}

	private static ProtocolServer initializeServer(Config config, String serverName, Context context) throws ServientConfigException {
		try {
			Class<ProtocolServer> serverClass = (Class<ProtocolServer>) Class.forName(serverName);
			Constructor<ProtocolServer> constructor;
			ProtocolServer server;
			constructor = serverClass.getConstructor(Config.class, Context.class);
			server = constructor.newInstance(config, context);
			return server;
		} catch (ClassCastException | ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new ServientConfigException(e);
		}
	}

	private static Pair<String, ProtocolClientFactory> initializeClientFactory(Config config, String factoryName, Context context) throws ServientConfigException {
		try {
			Class<ProtocolClientFactory> factoryClass = (Class<ProtocolClientFactory>) Class.forName(factoryName);
			Constructor<ProtocolClientFactory> constructor;
			ProtocolClientFactory factory;
			constructor = factoryClass.getConstructor(Config.class, Context.class);
			factory = constructor.newInstance(config, context);
			return new Pair<>(factory.getScheme(), factory);
		} catch (ClassCastException | ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new ServientConfigException(e);
		}
	}

	/**
	 * Stores security credentials (e.g. username and password) for the thing with the id id
	 * <code>id</code>.<br> See also: https://www.w3.org/TR/wot-thing-description/#security-serialization-json
	 *
	 * @param credentials
	 */
	private void addCredentials(Map<String, Object> credentials) {
		Log.d("ServientConfig", "Servient storing credentials for " + credentials.keySet());
		credentialStore.putAll(credentials);
	}

	public List<ProtocolServer> getServers() {
		return servers;
	}

	public Map<String, ProtocolClientFactory> getClientFactories() {
		return clientFactories;
	}

	public Map<String, Object> getCredentialStore() {
		return credentialStore;
	}

	@Override
	public String toString() {
		return "ServientConfig{" + "servers=" + servers + ", clientFactories=" + clientFactories + ", credentialStore=" + credentialStore + '}';
	}
}
