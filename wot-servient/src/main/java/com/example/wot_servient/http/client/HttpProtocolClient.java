package com.example.wot_servient.http.client;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import android.util.Log;

import com.example.wot_servient.wot.binding.ProtocolClient;
import com.example.wot_servient.wot.binding.ProtocolClientException;
import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.thing.form.Form;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Allows consuming Things via HTTP.
 */
public class HttpProtocolClient implements ProtocolClient {

	private static final String TAG = "HttpProtocolClient";
	private static final String HTTP_METHOD_NAME = "htv:methodName";
	private static final Duration LONG_POLLING_TIMEOUT = Duration.ofMinutes(60);
	private static OkHttpClient requestClient;

	public HttpProtocolClient() {
		if (requestClient == null)
			requestClient = new OkHttpClient.Builder().build();
	}

	@Override
	public CompletableFuture<Content> readResource(Form form) {
		Request request = generateRequest(form, "GET", null);
		return resolveRequestToContent(request);
	}

	@Override
	public CompletableFuture<Content> writeResource(Form form, Content content) {
		Request request = generateRequest(form, "PUT", content);
		return resolveRequestToContent(request);
	}

	@Override
	public CompletableFuture<Content> invokeResource(Form form, Content content) {
		Request request = generateRequest(form, "POST", content);
		return resolveRequestToContent(request);
	}

	@Override
	public Observable<Content> observeResource(Form form) {
		return Observable.<Content>create(emitter -> {
			while (!emitter.isDisposed()) {
				OkHttpClient client = requestClient.newBuilder()
						.connectTimeout(LONG_POLLING_TIMEOUT).build();
				Request request = generateRequest(form, "GET", null);
				try (Response response = client.newCall(request).execute()) {
					Content content = checkResponse(response);
					emitter.onNext(content);
				} catch (ProtocolClientException ignored) {
				}
			}
		}).subscribeOn(Schedulers.io());
	}

	private Request generateRequest(Form form, String defaultMethod, Content content) {
		String href = form.getHref();
		String method = defaultMethod;
		if (form.getOptional(HTTP_METHOD_NAME) != null) {
			method = (String) form.getOptional(HTTP_METHOD_NAME);
		}
		Request.Builder builder = new Request.Builder();
		builder.get();
		builder.url(href);
		if (content != null) {
			builder.header("Content-Type", content.getType());
			MediaType type = MediaType.get(content.getType());
			byte[] body = content.getBody();
			builder.method(method, RequestBody.create(type, body));
		}
		return builder.build();
	}

	private CompletableFuture<Content> resolveRequestToContent(Request request) {
		return supplyAsync(() -> {
			Log.d(TAG, "Sending " + request.method() + " to " + request.url());
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
}
