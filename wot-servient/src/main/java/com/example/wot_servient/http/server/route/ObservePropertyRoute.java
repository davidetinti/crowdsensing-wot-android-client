package com.example.wot_servient.http.server.route;

import android.util.Log;

import com.example.wot_servient.wot.Servient;
import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.ExposedThing;
import com.example.wot_servient.wot.thing.property.ExposedThingProperty;

import org.eclipse.jetty.http.HttpStatus;

import java.util.Map;

import spark.Request;
import spark.Response;

/**
 * Endpoint for subscribing to value changes for a {@link com.example.wot_servient.wot.thing.property.ThingProperty}.
 */
public class ObservePropertyRoute extends AbstractInteractionRoute {

    public ObservePropertyRoute(Servient servient, String securityScheme,
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
            if (!property.isWriteOnly() && property.isObservable()) {
                Content content = property.observer()
                        .map(optional -> ContentManager.valueToContent(optional.orElse(null), requestContentType))
                        .firstElement().blockingGet();

                if (content != null) {
                    Log.w("ObservePropertyRoute", "Next data received for Event " + name + ": " + content);
                    response.type(content.getType());
                    return content;
                }
                else {
                    response.status(HttpStatus.SERVICE_UNAVAILABLE_503);
                    return "";
                }
            }
            else {
                response.status(HttpStatus.BAD_REQUEST_400);
                return "Property writeOnly/not observable";
            }
        }
        else {
            response.status(HttpStatus.NOT_FOUND_404);
            return "Property not found";
        }
    }
}
