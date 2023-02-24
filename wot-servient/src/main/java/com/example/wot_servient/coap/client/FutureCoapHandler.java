package com.example.wot_servient.coap.client;

import android.util.Log;

import com.example.wot_servient.wot.binding.ProtocolClientException;
import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentCodecException;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.schema.StringSchema;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import java.util.concurrent.CompletableFuture;

public class FutureCoapHandler implements CoapHandler {

	private static final String TAG = "FutureCoapHandler";
	private final CompletableFuture future;

	FutureCoapHandler(CompletableFuture future) {
		this.future = future;
	}

	@Override
	public void onLoad(CoapResponse response) {
		Log.d(TAG, "Response received: " + response.getCode());
		String type = MediaTypeRegistry.toString(response.getOptions().getContentFormat());
		byte[] body = response.getPayload();
		Content output = new Content(type, body);
		if (response.isSuccess()) {
			future.complete(output);
		} else {
			try {
				String error = ContentManager.contentToValue(output, new StringSchema());
				future.completeExceptionally(new ProtocolClientException("Request was not successful: " + response + " (" + error + ")"));
			} catch (ContentCodecException e) {
				future.completeExceptionally(new ProtocolClientException("Request was not successful: " + response + " (" + e.getMessage() + ")"));
			}
		}
	}

	@Override
	public void onError() {
		future.completeExceptionally(new ProtocolClientException("request timeouts or has been rejected by the server"));
	}
}