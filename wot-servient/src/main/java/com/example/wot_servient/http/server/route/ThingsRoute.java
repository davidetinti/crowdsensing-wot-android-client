package com.example.wot_servient.http.server.route;

import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.ExposedThing;

import java.util.Map;

import spark.Request;
import spark.Response;

/**
 * Endpoint for listing all Things from the {@link com.example.wot_servient.wot.Servient}.
 */
public class ThingsRoute extends AbstractRoute {
    private final Map<String, ExposedThing> things;

    public ThingsRoute(Map<String, ExposedThing> things) {
        this.things = things;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        logRequest(request);

        String requestContentType = getOrDefaultRequestContentType(request);

        String unsupportedMediaTypeResponse = unsupportedMediaTypeResponse(response, requestContentType);
        if (unsupportedMediaTypeResponse != null) {
            return unsupportedMediaTypeResponse;
        }

        Content content = ContentManager.valueToContent(things, requestContentType);
        response.type(requestContentType);
        return content;
    }
}
