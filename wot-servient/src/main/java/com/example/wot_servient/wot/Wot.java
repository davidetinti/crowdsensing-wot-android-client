package com.example.wot_servient.wot;

import com.example.wot_servient.wot.binding.ProtocolServer;
import com.example.wot_servient.wot.binding.ProtocolServerException;
import com.example.wot_servient.wot.thing.ConsumedThing;
import com.example.wot_servient.wot.thing.ExposedThing;
import com.example.wot_servient.wot.thing.Thing;
import com.example.wot_servient.wot.thing.filter.ThingFilter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import io.reactivex.rxjava3.core.Observable;

/**
 * Fornisce i metodi per consumare, esporre, fare il fetch e fare il discover delle things.
 * https://w3c.github.io/wot-scripting-api/#the-wot-api-object
 */
public interface Wot {
	/**
	 * @param directory {@link String}
	 * @param thing     {@link ExposedThing}
	 * @return {@link CompletableFuture} {@link Void}
	 */
	CompletableFuture<Void> unregister(String directory, ExposedThing thing) throws URISyntaxException;

	/**
	 * @param directory {@link String}
	 * @param thing     {@link ExposedThing}
	 * @return {@link CompletableFuture} {@link Void}
	 */
	CompletableFuture<Void> register(String directory, ExposedThing thing) throws URISyntaxException;

	/**
	 * Avvia il discovery che ritornerà le things che rispettano il <code>filter</code> passato come parametro
	 *
	 * @param filter {@link ThingFilter}
	 * @return {@link Observable} {@link Thing}
	 */
	Observable<Thing> discover(ThingFilter filter) throws WotException;

	/**
	 * Avvia il discovery che ritornerà tutte le Things disponibili.
	 *
	 * @return {@link Observable} {@link Thing}
	 */
	Observable<Thing> discover() throws WotException;

	/**
	 * Prende in input una {@link Thing} e ritorna una {@link ExposedThing}.
	 *
	 * @param thing {@link Thing}
	 * @return {@link ExposedThing}
	 * @throws WotException If thing with same id is already exposed
	 */
	ExposedThing produce(Thing thing) throws WotException;

	/**
	 * Prende in input una {@link Thing} e ritorna una {@link ConsumedThing}.
	 * Il risultato può essere usato epr interagire con essa.
	 *
	 * @param thing {@link Thing}
	 * @return {@link ConsumedThing}
	 */
	ConsumedThing consume(Thing thing);

	/**
	 * Prende in input una {@link String} che contiene una <a href="https://www.w3.org/TR/wot-thing-description/">Thing Description</a>
	 * e ritorna una {@link ConsumedThing}. Il risultato può essere usato epr interagire con essa.
	 *
	 * @param thingDescription <a href="https://www.w3.org/TR/wot-thing-description/">Thing Description</a>
	 * @return {@link ConsumedThing}
	 */
	ConsumedThing consume(String thingDescription);

	/**
	 * Prende in input un {@link URI} (es. "http://..." o "coap://...") che rimanda ad una
	 * <a href="https://www.w3.org/TR/wot-thing-description/">Thing Description</a> e ritorna la
	 * {@link Thing} corrispondente.
	 *
	 * @param url {@link URI} (es. "http://..." o "coap://...")
	 * @return {@link CompletableFuture} {@link Thing}
	 */
	CompletableFuture<Thing> fetch(URI url);

	/**
	 * Prende in input una {@link String} che contine un {@link java.net.URL} (es. "http://..." o "coap://...")
	 * che rimanda ad una <a href="https://www.w3.org/TR/wot-thing-description/">Thing Description</a>
	 * e ritorna la {@link Thing} corrispondente.
	 *
	 * @param url {@link String} che contine un {@link java.net.URL} (es. "http://..." o "coap://...")
	 * @return {@link Thing}
	 */
	CompletableFuture<Thing> fetch(String url) throws URISyntaxException;

	/**
	 * Spegne il {@link Servient} e smette di esporre tutte le things
	 *
	 * @return {@link CompletableFuture} {@link Void}
	 */
	CompletableFuture<Void> destroy();

	/**
	 * Prende in input una {@link String} che contiene l'id di una {@link ExposedThing} e smette di esporla
	 *
	 * @return {@link CompletableFuture} {@link ExposedThing}
	 */
	CompletableFuture<ExposedThing> destroyExposedThingById(String exposedThingId);

	/**
	 * Ritorna il numero dei {@link ProtocolServer} attualmente attivi.
	 *
	 * @return {@link Integer} dei {@link ProtocolServer} attualmente attivi.
	 */
	Integer getNumberOfActiveProtocolsServer();

	/**
	 * Prende in input l'id di una {@link ExposedThing} e ritorna la {@link ExposedThing} già esposta.
	 *
	 * @param exposedThingId - L'id di una {@link ExposedThing}
	 * @return {@link ExposedThing}
	 */
	ExposedThing getExistingExposedThing(String exposedThingId);

	/**
	 * Prende in input l'id di una {@link ExposedThing} e ritorna un'ArrayList di {@link URI} della {@link ExposedThing}
	 *
	 * @param exposedThingId - L'id di una {@link ExposedThing}
	 * @return Un'{@link ArrayList} di {@link URI}
	 */
	ArrayList<String> getTDIdentifiersForExposedThing(String exposedThingId) throws ProtocolServerException;
}
