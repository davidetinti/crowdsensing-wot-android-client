package com.example.wot_servient.coap.client;

import android.util.Log;

import com.example.wot_servient.wot.binding.ProtocolClient;
import com.example.wot_servient.wot.binding.ProtocolClientException;
import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentCodecException;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.form.Form;
import com.example.wot_servient.wot.thing.schema.StringSchema;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Function;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Allows consuming Things via CoAP.
 */
@SuppressWarnings("squid:S1192")
public class CoapProtocolClient implements ProtocolClient {

	private static final String TAG = "CoapProtocolClient";
	private final Function<String, CoapClient> clientCreator;

	public CoapProtocolClient(ExecutorService executor, ScheduledThreadPoolExecutor secondaryExecutor) {
		this.clientCreator = url -> new CoapClient(url)
				//                .setExecutor(executor)
				.setExecutors(executor, secondaryExecutor, true).setTimeout(10 * 1000L);
	}

	CoapProtocolClient(Function<String, CoapClient> clientCreator) {
		this.clientCreator = clientCreator;
	}

	@Override
	public CompletableFuture<Content> readResource(Form form) {
		CompletableFuture<Content> future = new CompletableFuture<>();
		String url = form.getHref();
		CoapClient client = clientCreator.apply(url);
		Request request = generateRequest(form, CoAP.Code.GET);
		Log.d(TAG, "CoapClient sending " + request.getCode() + " to " + url);
		client.advanced(new FutureCoapHandler(future), request);
		return future;
	}

	@Override
	public CompletableFuture<Content> writeResource(Form form, Content content) {
		CompletableFuture<Content> future = new CompletableFuture<>();
		String url = form.getHref();
		CoapClient client = clientCreator.apply(url);
		Request request = generateRequest(form, CoAP.Code.PUT);
		Log.d(TAG, "CoapClient sending " + request.getCode() + " to " + url);
		if (content != null) {
			request.setPayload(content.getBody());
		}
		client.advanced(new FutureCoapHandler(future), request);
		return future;
	}

	@Override
	public CompletableFuture<Content> invokeResource(Form form, Content content) {
		CompletableFuture<Content> future = new CompletableFuture<>();
		String url = form.getHref();
		CoapClient client = clientCreator.apply(url);
		Request request = generateRequest(form, CoAP.Code.POST);
		Log.d(TAG, "CoapClient sending " + request.getCode() + " to " + url);
		if (content != null) {
			request.setPayload(content.getBody());
		}
		client.advanced(new FutureCoapHandler(future), request);
		return future;
	}

	@Override
	public Observable<Content> observeResource(Form form) {
		String url = form.getHref();
		//        return Observable.using(
		//                () -> clientCreator.apply(url),
		//                client -> Observable.create(source -> {
		//                    // Californium does not offer any method to wait until the observation is established...
		//                    // This causes new values not being recognized directly after observation creation.
		//                    // The client must wait "some" time before it can be sure that the observation is active.
		//                    Log.d(TAG, "CoapClient subscribe to " + url);
		//                    client.observe(new CoapHandler() {
		//                        @Override
		//                        public void onLoad(CoapResponse response) {
		//                            String type = MediaTypeRegistry.toString(response.getOptions().getContentFormat());
		//                            byte[] body = response.getPayload();
		//                            Content output = new Content(type, body);
		//                            if (response.isSuccess()) {
		//                                Log.d(TAG,"Next data received for subscription " + url);
		//                                source.onNext(output);
		//                            }
		//                            else {
		//                                try {
		//                                    String error = ContentManager.contentToValue(output, new StringSchema());
		//                                    source.onError(new ProtocolClientException(error));
		//                                    Log.d(TAG, "Error received for subscription " + url + ": " + error);
		//                                }
		//                                catch (ContentCodecException e) {
		//                                    source.onError(new ProtocolClientException(e));
		//                                    Log.d(TAG, "Error received for subscription " + url + ": " + e.getMessage());
		//                                }
		//                            }
		//                        }
		//
		//                        @Override
		//                        public void onError() {
		//                            source.onError(new ProtocolClientException("Error received for subscription '" + url + "'"));
		//                            Log.d(TAG, "Error received for subscription " + url);
		//                        }
		//                    });
		//                }),
		//                CoapClient::shutdown
		//        );
		return Observable.<Content>create(source -> {
			CoapClient client = clientCreator.apply(url);
			Log.d(TAG, "CoapClient subscribe to " + url);
			client.observe(new CoapHandler() {
				@Override
				public void onLoad(CoapResponse response) {
					String type = MediaTypeRegistry.toString(response.getOptions()
																	 .getContentFormat());
					byte[] body = response.getPayload();
					Content output = new Content(type, body);
					if (response.isSuccess()) {
						Log.d(TAG, "Next data received for subscription " + url);
						source.onNext(output);
					} else {
						try {
							String error = ContentManager.contentToValue(output, new StringSchema());
							source.onError(new ProtocolClientException(error));
							Log.d(TAG, "Error received for subscription " + url + ": " + error);
						} catch (ContentCodecException e) {
							source.onError(new ProtocolClientException(e));
							Log.d(TAG, "Error received for subscription " + url + ": " + e.getMessage());
						}
					}
				}

				@Override
				public void onError() {
					source.onError(new ProtocolClientException("Error received for subscription '" + url + "'"));
					Log.d(TAG, "Error received for subscription " + url);
				}
			});
		}).subscribeOn(Schedulers.io());
	}

	private Request generateRequest(Form form, CoAP.Code code) {
		return generateRequest(form, code, false);
	}

	private Request generateRequest(Form form, CoAP.Code code, boolean observable) {
		Request request = new Request(code);
		if (form.getContentType() != null) {
			int mediaType = MediaTypeRegistry.parse(form.getContentType());
			if (mediaType != -1) {
				request.getOptions().setContentFormat(mediaType);
			}
		}
		if (observable) {
			request.setObserve();
		}
		return request;
	}
}
