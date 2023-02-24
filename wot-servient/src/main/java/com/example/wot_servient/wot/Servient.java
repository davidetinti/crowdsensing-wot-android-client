package com.example.wot_servient.wot;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import android.content.Context;
import android.util.Log;

import com.example.wot_servient.wot.binding.ProtocolClient;
import com.example.wot_servient.wot.binding.ProtocolClientException;
import com.example.wot_servient.wot.binding.ProtocolClientFactory;
import com.example.wot_servient.wot.binding.ProtocolClientNotImplementedException;
import com.example.wot_servient.wot.binding.ProtocolServer;
import com.example.wot_servient.wot.binding.ProtocolServerException;
import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentCodecException;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.ExposedThing;
import com.example.wot_servient.wot.thing.Thing;
import com.example.wot_servient.wot.thing.action.ExposedThingAction;
import com.example.wot_servient.wot.thing.event.ExposedThingEvent;
import com.example.wot_servient.wot.thing.filter.DiscoveryMethod;
import com.example.wot_servient.wot.thing.filter.ThingFilter;
import com.example.wot_servient.wot.thing.filter.ThingQueryException;
import com.example.wot_servient.wot.thing.form.Form;
import com.example.wot_servient.wot.thing.form.FormBuilder;
import com.example.wot_servient.wot.thing.form.Operation;
import com.example.wot_servient.wot.thing.property.ExposedThingProperty;
import com.example.wot_servient.wot.thing.schema.ObjectSchema;
import com.example.wot_servient.wot.utilities.Futures;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * The Servient hosts, exposes and consumes things based on provided protocol bindings.
 * https://w3c.github.io/wot-architecture/#sec-servient-implementation<br> It reads the servers
 * contained in the configuration parameter "wot.servient.servers", starts them and thus exposes
 * Things via the protocols supported by the servers. "wot.servient.servers" should contain an array
 * of strings of fully qualified class names implementing {@link ProtocolServer}.<br> It also reads
 * the clients contained in the configuration parameter "wot.servient.client-factories" and is then
 * able to consume Things via the protocols supported by the clients.
 * "wot.servient.client-factories" should contain an array of strings of fully qualified class names
 * implementing {@link ProtocolClientFactory}.<br> The optional configuration parameter
 * "wot.servient.credentials" can contain credentials (e.g. username and password) for the different
 * things. The parameter should contain a map that uses the thing ids as key.
 */
public class Servient {

	private static final String TAG = "Servient";
	private final List<ProtocolServer> servers;
	private final Map<String, ProtocolClientFactory> clientFactories;
	private final Map<String, ExposedThing> exposedThings;
	private final Map<String, Object> credentialStore;

	/**
	 * Create a {@link Servient} with {@link Config} passed as parameter.
	 */
	public Servient(Config config, Context context) throws ServientException {
		this(new ServientConfig(config, context));
	}

	Servient(ServientConfig config) {
		this(config.getServers(), config.getClientFactories(), config.getCredentialStore(), new HashMap<>());
	}

	Servient(List<ProtocolServer> servers, Map<String, ProtocolClientFactory> clientFactories, Map<String, Object> credentialStore, Map<String, ExposedThing> exposedThings) {
		this.servers = servers;
		this.clientFactories = clientFactories;
		this.credentialStore = credentialStore;
		this.exposedThings = exposedThings;
	}

	/**
	 * Returns a list of the IP addresses of all network interfaces of the local computer. If no IP
	 * addresses can be obtained, 127.0.0.1 is returned.
	 */
	public static Set<String> getAddresses() {
		try {
			Set<String> addresses = new HashSet<>();
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			if (ifaces == null) {
				return new HashSet<>(Collections.singletonList("127.0.0.1"));
			} else {
				while (ifaces.hasMoreElements()) {
					NetworkInterface iface = ifaces.nextElement();
					if (!iface.isUp() || iface.isLoopback() || iface.isPointToPoint()) {
						continue;
					}
					Enumeration<InetAddress> ifaceAddresses = iface.getInetAddresses();
					while (ifaceAddresses.hasMoreElements()) {
						InetAddress ifaceAddress = ifaceAddresses.nextElement();
						String address = getAddressByInetAddress(ifaceAddress);
						if (address != null) {
							addresses.add(address);
						}
					}
				}
				return addresses;
			}
		} catch (SocketException e) {
			return new HashSet<>(Collections.singletonList("127.0.0.1"));
		}
	}

	private static String getAddressByInetAddress(InetAddress ifaceAddress) {
		if (ifaceAddress.isLoopbackAddress() || ifaceAddress.isLinkLocalAddress() || ifaceAddress.isMulticastAddress()) {
			return null;
		}
		if (ifaceAddress instanceof Inet4Address) {
			return ifaceAddress.getHostAddress();
		} else if (ifaceAddress instanceof Inet6Address) {
			String hostAddress = ifaceAddress.getHostAddress();
			// remove scope
			if (hostAddress == null)
				throw new AssertionError();
			int percent = hostAddress.indexOf('%');
			if (percent != -1) {
				hostAddress = hostAddress.substring(0, percent);
			}
			return "[" + hostAddress + "]";
		} else {
			return null;
		}
	}

	/**
	 * Creates a {@link Servient} with the given <code>config</code>. The servient will not start
	 * any servers and can therefore only consume things and not expose any things.
	 */
	public static Servient clientOnly(Config config, Context context) throws ServientException {
		Config clientOnlyConfig = ConfigFactory.parseString("wot.servient.servers = []")
				.withFallback(config);
		return new Servient(clientOnlyConfig, context);
	}

	/**
	 * Creates a {@link Servient} with the given <code>config</code>. The servient will not start
	 * any clients and can therefore only produce and expose things.
	 */
	public static Servient serverOnly(Config config, Context context) throws ServientException {
		Config clientOnlyConfig = ConfigFactory.parseString("wot.servient.client-factories = []")
				.withFallback(config);
		return new Servient(clientOnlyConfig, context);
	}

	/**
	 * Returns the version of the servient. If this is not possible, {@code null} is returned.
	 */
	public static String getVersion() {
		final Properties properties = new Properties();
		try {
			properties.load(Objects.requireNonNull(Servient.class.getClassLoader())
									.getResourceAsStream("project.properties"));
			return properties.getProperty("version");
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public String toString() {
		return "Servient [servers=" + getServers() + " clientFactories=" + clientFactories.values() + "]";
	}

	/**
	 * Returns a {@link List} of all {@link ProtocolServer} supported by {@link Servient}
	 */
	public List<ProtocolServer> getServers() {
		return servers;
	}

	/**
	 * Start the {@link Servient}.
	 */
	public CompletableFuture<Void> start() {
		Log.i(TAG, "Start Servient");
		CompletableFuture[] clientFutures = clientFactories.values().stream()
				.map(ProtocolClientFactory::init).toArray(CompletableFuture[]::new);
		CompletableFuture[] serverFutures = servers.stream()
				.map(protocolServer -> protocolServer.start(this))
				.toArray(CompletableFuture[]::new);
		CompletableFuture[] futures = Stream.concat(Arrays.stream(clientFutures), Arrays.stream(serverFutures))
				.toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(futures);
	}

	/**
	 * Turn off the {@link Servient}.
	 */
	public CompletableFuture<Void> shutdown() {
		Log.i(TAG, "Stop Servient");
		CompletableFuture[] clientFutures = clientFactories.values().stream()
				.map(ProtocolClientFactory::destroy).toArray(CompletableFuture[]::new);
		CompletableFuture[] serverFutures = servers.stream().map(ProtocolServer::stop)
				.toArray(CompletableFuture[]::new);
		CompletableFuture[] futures = Stream.concat(Arrays.stream(clientFutures), Arrays.stream(serverFutures))
				.toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(futures);
	}

	/**
	 * All the {@link ProtocolServer} expose the {@link Thing} that correspond to the id passed as
	 * parameter. A {@link Thing} must be added to the {@link Servient} before being exposed.
	 *
	 * @param id that correspond to a {@link ExposedThing} already added to the {@link Servient}.
	 * @return {@link CompletableFuture} {@link ExposedThing}
	 */
	public CompletableFuture<ExposedThing> expose(String id) {
		ExposedThing thing = exposedThings.get(id);
		if (servers.isEmpty()) {
			//return failedFuture(new ServientException("Servient has no servers to expose Things"));// From Java 9
			// Workaround before Java 9
			CompletableFuture<ExposedThing> toReturn = new CompletableFuture<>();
			toReturn.completeExceptionally(new ServientException("Servient has no servers to expose Things"));
			return toReturn;
		}
		if (thing == null) {
			//return failedFuture(new ServientException("Thing must be added to the servient first"));// From Java 9
			// Workaround before Java 9
			CompletableFuture<ExposedThing> toReturn = new CompletableFuture<>();
			toReturn.completeExceptionally(new ServientException("Thing must be added to the servient first"));
			return toReturn;
		}
		Log.i(TAG, "Servient exposing " + id);
		// initializing forms
		thing.setBase("");
		Map<String, ExposedThingProperty<Object>> properties = thing.getProperties();
		properties.forEach((n, p) -> p.setForms(new ArrayList<>()));
		Map<String, ExposedThingAction<Object, Object>> actions = thing.getActions();
		actions.forEach((n, a) -> a.setForms(new ArrayList<>()));
		Map<String, ExposedThingEvent<Object>> events = thing.getEvents();
		events.forEach((n, e) -> e.setForms(new ArrayList<>()));
		//TODO qua aggiungere gli identifiers alla exposedThing
		CompletableFuture[] serverFutures = getServers().stream().map(s -> s.expose(thing))
				.toArray(CompletableFuture[]::new);
		return CompletableFuture.allOf(serverFutures).thenApply(result -> thing);
	}

	/**
	 * Every {@link ProtocolServer} stop to expose the {@link Thing} corresponding to the id
	 * passed as parameter. It is not possible to interact with it afterwards.
	 *
	 * @param id that correspond to a {@link ExposedThing}
	 * @return {@link CompletableFuture} {@link ExposedThing}
	 */
	public CompletableFuture<ExposedThing> destroy(String id) {
		ExposedThing thing = exposedThings.get(id);
		if (thing == null)
			throw new AssertionError();
		if (servers.isEmpty()) {
			//return failedFuture(new ServientException("Servient has no servers to stop exposure Things"));// From Java 9
			// Workaround before Java 9
			CompletableFuture<ExposedThing> toReturn = new CompletableFuture<>();
			toReturn.completeExceptionally(new ServientException("Servient has no servers to stop exposure Things"));
			return toReturn;
		}
		Log.i(TAG, "Servient stop exposing " + thing);
		// reset forms
		thing.setForms(new ArrayList<>());
		Map<String, ExposedThingProperty<Object>> properties = thing.getProperties();
		properties.forEach((n, p) -> p.setForms(new ArrayList<>()));
		Map<String, ExposedThingAction<Object, Object>> actions = thing.getActions();
		actions.forEach((n, a) -> a.setForms(new ArrayList<>()));
		Map<String, ExposedThingEvent<Object>> events = thing.getEvents();
		events.forEach((n, e) -> e.setForms(new ArrayList<>()));
		CompletableFuture[] serverFutures = getServers().stream().map(s -> s.destroy(thing))
				.toArray(CompletableFuture[]::new);
		exposedThings.remove(id); // Test
		return CompletableFuture.allOf(serverFutures).thenApply(result -> thing);
	}

	public ArrayList<String> getTDIdentifiersForExposedThing(String exposedThingId) {
		ArrayList<String> TDIdentifiers = new ArrayList<>();
		for (ProtocolServer protocolServer : servers) {
			try {
				TDIdentifiers.add(protocolServer.getTDIdentifier(exposedThingId));
			} catch (ProtocolServerException protocolServerException) {
				Log.e(TAG, protocolServerException.getMessage());
			}
		}
		return TDIdentifiers;
	}

	/**
	 * Adds {@link Thing} to the servient. This allows the Thing to be exposed later.
	 *
	 * @param exposedThing {@link ExposedThing}
	 */
	public boolean addThing(ExposedThing exposedThing) {
		if (exposedThing.getId() == null || exposedThing.getId().isEmpty()) {
			Log.w("Servient", "Servient generating ID for " + exposedThing);
			exposedThing.setId(Thing.randomId());
		}
		ExposedThing previous = exposedThings.putIfAbsent(exposedThing.getId(), exposedThing);
		return previous == null;
	}

	/**
	 * Calls <code>url</code> and expects a Thing Description there. Returns the description as a
	 * {@link Thing}.
	 */
	public CompletableFuture<Thing> fetch(String url) throws URISyntaxException {
		return fetch(new URI(url));
	}

	/**
	 * Calls <code>url</code> and expects a Thing Description there. Returns the description as a
	 * {@link Thing}.
	 */
	public CompletableFuture<Thing> fetch(URI url) {
		Log.d("Servient", "Fetch thing from url " + url);
		String scheme = url.getScheme();
		try {
			ProtocolClient client = getClientFor(scheme);
			if (client != null) {
				Form form = new FormBuilder().setHref(url.toString()).build();
				return client.readResource(form).thenApply(content -> {
					try {
						Map map = ContentManager.contentToValue(content, new ObjectSchema());
						return Thing.fromMap(map);
					} catch (ContentCodecException e) {
						throw new CompletionException(new ServientException("Error while fetching TD: " + e));
					}
				});
			} else {
				CompletableFuture<Thing> toReturn = new CompletableFuture<>();
				toReturn.completeExceptionally(new ServientException("Unable to fetch '" + url + "'. Missing ClientFactory for scheme '" + scheme + "'"));
				return toReturn;
			}
		} catch (ProtocolClientException | IOException e) {
			CompletableFuture<Thing> toReturn = new CompletableFuture<>();
			toReturn.completeExceptionally(new ServientException("Unable to create client: " + e.getMessage()));
			return toReturn;
		}
	}

	/**
	 * Searches for the matching {@link ProtocolClient} for <code>scheme</code> (e.g. http, coap,
	 * mqtt, etc.). If no client can be found, <code>null</code> is returned.
	 */
	public ProtocolClient getClientFor(String scheme) throws ProtocolClientException {
		Log.d(TAG, "Available clients: " + clientFactories);
		ProtocolClientFactory factory = clientFactories.get(scheme);
		if (factory != null) {
			return factory.getClient();
		} else {
			Log.w(TAG, "Servient has no ClientFactory for scheme " + scheme);
			return null;
		}
	}

	/**
	 * Searches for the matching {@link ProtocolClient} for <code>scheme</code> (e.g. http, coap,
	 * mqtt, etc.). If no client can be found, <code>false</code> is returned.
	 */
	public boolean hasClientFor(String scheme) {
		return clientFactories.containsKey(scheme);
	}

	/**
	 * Calls <code>url</code> and expects a Thing Directory there. Returns a list with all found
	 * {@link Thing}.
	 */
	public CompletableFuture<Map<String, Thing>> fetchDirectory(String url) throws URISyntaxException {
		return fetchDirectory(new URI(url));
	}

	/**
	 * Calls <code>url</code> and expects a Thing Directory there. Returns a list with all found
	 * {@link Thing}.
	 */
	public CompletableFuture<Map<String, Thing>> fetchDirectory(URI url) {
		Log.d("Servient", "Fetch thing directory from url " + url);
		String scheme = url.getScheme();
		try {
			ProtocolClient client = getClientFor(scheme);
			if (client != null) {
				Form form = new FormBuilder().setHref(url.toString()).build();
				return client.readResource(form).thenApply(content -> {
					try {
						Map<String, Map> value = ContentManager.contentToValue(content, new ObjectSchema());
						Map<String, Thing> directoryThings = new HashMap<>();
						if (value != null) {
							for (Map.Entry<String, Map> entry : value.entrySet()) {
								String id = entry.getKey();
								Map map = entry.getValue();
								Thing thing = Thing.fromMap(map);
								directoryThings.put(id, thing);
							}
						}
						return directoryThings;
					} catch (ContentCodecException e2) {
						throw new CompletionException(new ServientException("Error while fetching TD directory: " + e2));
					}
				});
			} else {
				CompletableFuture<Map<String, Thing>> toReturn = new CompletableFuture<>();
				toReturn.completeExceptionally(new ServientException("Unable to fetch directory '" + url + "'. Missing ClientFactory for scheme '" + scheme + "'"));
				return toReturn;
			}
		} catch (ProtocolClientException | IOException e) {
			CompletableFuture<Map<String, Thing>> toReturn = new CompletableFuture<>();
			toReturn.completeExceptionally(new ServientException("Unable to create client: " + e.getMessage()));
			return toReturn;
		}
	}

	/**
	 * Adds <code>thing</code> to the Thing Directory <code>directory</code>.
	 */
	public CompletableFuture<Void> register(String directory, ExposedThing thing) throws URISyntaxException {
		return register(new URI(directory), thing);
	}

	/**
	 * Adds <code>thing</code> to the Thing Directory <code>directory</code>.
	 */
	private CompletableFuture<Void> register(URI directory, ExposedThing thing) {
		Log.d(TAG, "Registering TD for " + thing.getTitle() + " in directory: " + directory);
		return CompletableFuture.runAsync(() -> {
			String stringedThing = thing.toJson();
			byte[] input = stringedThing.getBytes(StandardCharsets.UTF_8);
			String href = directory.toString() + "/things/" + thing.getId();
			Request request = new Request.Builder().put(RequestBody.create(input)).url(href)
					.header("Content-Type", "application/json").build();
			try {
				resolveRequestToContent(request).get();
			} catch (ExecutionException | InterruptedException e) {
				throw new CompletionException(new ServientException("Unable to register TD: " + e.getMessage()));
			}
		});
	}

	/**
	 * Removes <code>thing</code> from Thing Directory <code>directory</code>.
	 */
	public CompletableFuture<Void> unregister(String directory, ExposedThing thing) throws URISyntaxException {
		return unregister(new URI(directory), thing);
	}

	/**
	 * Removes <code>thing</code> from Thing Directory <code>directory</code>.
	 */
	private CompletableFuture<Void> unregister(URI directory, ExposedThing thing) {
		Log.d(TAG, "Unregistering TD for " + thing.getTitle() + " in directory: " + directory);
		return CompletableFuture.runAsync(() -> {
			String href = directory.toString() + "/things/" + thing.getId();
			Request request = new Request.Builder().delete().url(href).build();
			try {
				resolveRequestToContent(request).get();
			} catch (ExecutionException | InterruptedException e) {
				throw new CompletionException(new ServientException("Unable to unregister TD: " + e.getMessage()));
			}
		});
	}

	private CompletableFuture<Content> resolveRequestToContent(Request request) {
		return supplyAsync(() -> {
			Log.d(TAG, "Sending " + request.method() + " to " + request.url());
			OkHttpClient requestClient = new OkHttpClient.Builder().build();
			try (Response response = requestClient.newCall(request).execute()) {
				return checkResponse(response);
			} catch (IOException | ProtocolClientException e) {
				throw new CompletionException(new ProtocolClientException("Error during http request: " + e.getMessage()));
			}
		});
	}

	private Content checkResponse(Response response) throws ProtocolClientException {
		int statusCode = response.code();
		if (statusCode < HttpURLConnection.HTTP_OK) {
			throw new ProtocolClientException("Received '" + statusCode + "' and cannot continue (not implemented)");
		} else if (statusCode < HttpURLConnection.HTTP_MULT_CHOICE) {
			ResponseBody responseBody = response.body();
			String type = null;
			if (responseBody != null) {
				MediaType mediaType = responseBody.contentType();
				if (mediaType != null)
					type = mediaType.toString();
			}
			try {
				byte[] body;
				if (responseBody != null)
					body = responseBody.bytes();
				else
					body = new byte[0];
				return new Content(type, body);
			} catch (IOException e) {
				throw new ProtocolClientException("Error during http request: " + e.getMessage());
			}
		} else {
			throw new ProtocolClientException("Client error " + statusCode + ": " + response.message());
		}
	}

	/**
	 * Starts a discovery process for all available Things. Not all {@link ProtocolClient}
	 * implementations support discovery. If none of the available clients support discovery, a
	 * {@link ProtocolClientNotImplementedException} will be thrown.
	 */
	public Observable<Thing> discover() throws ServientException {
		return discover(new ThingFilter(DiscoveryMethod.ANY));
	}

	/**
	 * Starts a discovery process and searches for the things defined in <code>filter</code>. Not
	 * all {@link ProtocolClient} implementations support discovery. If none of the available
	 * clients support discovery, a {@link ProtocolClientNotImplementedException} will be thrown.
	 */
	public Observable<Thing> discover(ThingFilter filter) throws ServientException {
		switch (filter.getMethod()) {
			case DIRECTORY:
				return discoverDirectory(filter);
			case LOCAL:
				return discoverLocal(filter);
			default:
				return discoverAny(filter);
		}
	}

	private @io.reactivex.rxjava3.annotations.NonNull Observable<Thing> discoverDirectory(ThingFilter filter) {
		return Futures.toObservable(fetchDirectory(filter.getUrl()).thenApply(Map::values))
				.flatMapIterable(myThings -> myThings);
	}

	private @io.reactivex.rxjava3.annotations.NonNull Observable<Thing> discoverLocal(ThingFilter filter) {
		List<Thing> myThings = getExposedThings().values().stream().map(Thing.class::cast)
				.collect(Collectors.toList());
		if (filter.getQuery() != null) {
			try {
				List<Thing> filteredThings = filter.getQuery().filter(myThings);
				return Observable.fromIterable(filteredThings);
			} catch (ThingQueryException e) {
				return Observable.error(e);
			}
		} else {
			return Observable.fromIterable(myThings);
		}
	}

	private Observable<Thing> discoverAny(ThingFilter filter) throws ServientException {
		@NonNull Observable<Thing> observable = Observable.empty();
		// try to run a discovery with every available protocol binding
		boolean leastOneClientHasImplementedDiscovery = false;
		for (ProtocolClientFactory factory : clientFactories.values()) {
			try {
				ProtocolClient client = factory.getClient();
				observable = observable.mergeWith(client.discover(filter));
				leastOneClientHasImplementedDiscovery = true;
			} catch (Exception ignored) {
			}
		}
		// fail if none of the available protocol bindings support discovery
		if (!leastOneClientHasImplementedDiscovery) {
			throw new ProtocolClientNotImplementedException("None of the available clients implements 'discovery'. Therefore discovery function is not available.");
		}
		// ensure local things are contained
		observable = observable.mergeWith(discoverLocal(filter));
		// remove things without id and duplicate things
		observable = observable.filter(thing -> thing.getId() != null && !thing.getId().isEmpty())
				.distinct(Thing::getId);
		return observable;
	}

	/**
	 * Returns all things that have been added to the servient.
	 */
	public Map<String, ExposedThing> getExposedThings() {
		return exposedThings;
	}

	public ExposedThing getExposedThing(String id) {
		return exposedThings.get(id);
	}

	/**
	 * Returns the server of type <code>server</code>. If the serving does not support this type,
	 * <code>null</code> is returned.
	 */
	public <T extends ProtocolServer> T getServer(Class<T> server) {
		Optional<T> optional = (Optional<T>) servers.stream().filter(server::isInstance)
				.findFirst();
		return optional.orElse(null);
	}

	/**
	 * Returns the security credentials (e.g. username and password) for the thing with the id
	 * <code>id</code>.<br> See also: https://www.w3.org/TR/wot-thing-description/#security-serialization-json
	 */
	public Object getCredentials(String id) {
		Log.d("Servient", "Servient looking up credentials for " + id);
		return credentialStore.get(id);
	}

	public List<String> getClientSchemes() {
		return new ArrayList<>(clientFactories.keySet());
	}

	public Integer getNumberOfActiveProtocolsServer() {
		return servers.size();
	}

	public boolean checkIfSchemeHasOverridenMethodForOperation(String scheme, Operation operation) {
		try {
			ProtocolClient protocolClient = getClientFor(scheme);
			switch (operation.toString()) {
				case "READ_PROPERTY":
					return !protocolClient.getClass().getMethod("readResource", Form.class)
							.isDefault();
				case "WRITE_PROPERTY":
					return !protocolClient.getClass()
							.getMethod("writeResource", Form.class, Content.class).isDefault();
				case "OBSERVE_PROPERTY":
					return !protocolClient.getClass().getMethod("observeResource", Form.class)
							.isDefault();
			}
		} catch (Exception ignore) {
		}
		return true;
	}
}
