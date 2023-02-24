package com.example.wot_servient.http.server.route;

import com.example.wot_servient.wot.Servient;
import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentCodecException;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.ExposedThing;
import com.example.wot_servient.wot.thing.property.ExposedThingProperty;

import org.eclipse.jetty.http.HttpStatus;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import spark.Request;
import spark.Response;

/**
 * Endpoint for reading values from a {@link com.example.wot_servient.wot.thing.property.ThingProperty}.
 */
public class ReadPropertyRoute extends AbstractInteractionRoute {
    public ReadPropertyRoute(Servient servient, String securityScheme,
                             Map<String, ExposedThing> things) {
        super(servient, securityScheme, things);
    }

    @Override
    protected Object handleInteraction(Request request,
                                       Response response,
                                       String requestContentType,
                                       String name,
                                       ExposedThing thing) {
        ExposedThingProperty<Object> property = thing.getProperty(name);
        if (property != null) {
            if (!property.isWriteOnly()) {

                try {
                    Object value = property.read().get();

                    Content content = ContentManager.valueToContent(value, requestContentType);
                    response.type(content.getType());
                    return content;
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                catch (ContentCodecException | ExecutionException e) {
                    response.status(HttpStatus.SERVICE_UNAVAILABLE_503);
                    return e;
                }
            }
            else {
                response.status(HttpStatus.BAD_REQUEST_400);
                return "Property writeOnly";
            }
        }
        else {
            response.status(HttpStatus.NOT_FOUND_404);
            return "Property not found";
        }
    }
}
