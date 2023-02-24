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
 * Endpoint for writing values to a {@link com.example.wot_servient.wot.thing.property.ThingProperty}.
 */
public class WritePropertyRoute extends AbstractInteractionRoute {
    public WritePropertyRoute(Servient servient, String securityScheme,
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
            if (!property.isReadOnly()) {
                return writeProperty(request, response, requestContentType, property);
            }
            else {
                response.status(HttpStatus.BAD_REQUEST_400);
                return "Property readOnly";
            }
        }
        else {
            response.status(HttpStatus.NOT_FOUND_404);
            return "Property not found";
        }
    }

    private Object writeProperty(Request request,
                                 Response response,
                                 String requestContentType,
                                 ExposedThingProperty<Object> property) {
        try {
            Content content = new Content(requestContentType, request.bodyAsBytes());
            Object input = ContentManager.contentToValue(content, property);

            Object output = property.write(input).get();
            if (output != null) {
                response.status(HttpStatus.OK_200);
                return output;
            }
            else {
                response.status(HttpStatus.NO_CONTENT_204);
                return "";
            }
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
}
