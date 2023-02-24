package com.example.wot_servient.http.server.route;

import android.util.Log;

import com.example.wot_servient.wot.content.ContentManager;

import org.eclipse.jetty.http.HttpStatus;

import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Abstract route for exposing Things. Inherited from all other routes.
 */
abstract class AbstractRoute implements Route {
    String getOrDefaultRequestContentType(Request request) {
        if (request.contentType() != null) {
            return request.contentType();
        }
        else {
            return ContentManager.DEFAULT;
        }
    }

    String unsupportedMediaTypeResponse(Response response, String requestContentType) {
        if (!ContentManager.isSupportedMediaType(requestContentType)) {
            Log.w("AbstractRoute", "Unsupported media type: " + requestContentType);
            response.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE_415);
            return "Unsupported Media Type (supported: " + String.join(", ", ContentManager.getSupportedMediaTypes()) + ")";
        }
        else {
            return null;
        }
    }

    void logRequest(Request request) {
            Log.d("AbstractRoute", "Handle " + request.requestMethod() +" to " + request.url());
            if (request.queryString() != null && !request.queryString().isEmpty()) {
                Log.d("AbstractRoute", "Request parameters: " + request.queryString());
            }
    }
}
