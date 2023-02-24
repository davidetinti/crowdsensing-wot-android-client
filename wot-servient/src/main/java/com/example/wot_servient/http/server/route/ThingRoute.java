package com.example.wot_servient.http.server.route;

import com.example.wot_servient.wot.Servient;
import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentCodecException;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.ExposedThing;

import org.eclipse.jetty.http.HttpStatus;

import java.util.Map;

import spark.Request;
import spark.Response;

/**
 * Endpoint for displaying a Thing Description.
 */
public class ThingRoute extends AbstractInteractionRoute {
    public ThingRoute(Servient servient, String securityScheme,
                      Map<String, ExposedThing> things) {
        super(servient, securityScheme, things);
    }

    @Override
    protected Object handleInteraction(Request request,
                                       Response response,
                                       String requestContentType,
                                       String name,
                                       ExposedThing thing) {
        try {
            Content content = ContentManager.valueToContent(thing, requestContentType);
            response.type(content.getType());
            return content;
        }
        catch (ContentCodecException e) {
            response.status(HttpStatus.SERVICE_UNAVAILABLE_503);
            return e;
        }
    }
}
