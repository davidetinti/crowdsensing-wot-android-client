package com.example.wot_servient.http.server.route;

import android.util.Log;

import com.example.wot_servient.wot.Servient;
import com.example.wot_servient.wot.content.Content;
import com.example.wot_servient.wot.content.ContentManager;
import com.example.wot_servient.wot.thing.ExposedThing;
import com.example.wot_servient.wot.thing.event.ExposedThingEvent;

import org.eclipse.jetty.http.HttpStatus;

import java.util.Map;

import spark.Request;
import spark.Response;

/**
 * Endpoint for interaction with a {@link com.example.wot_servient.wot.thing.event.ThingEvent}.
 */
public class SubscribeEventRoute extends AbstractInteractionRoute {
    public SubscribeEventRoute(Servient servient, String securityScheme,
                               Map<String, ExposedThing> things) {
        super(servient, securityScheme, things);
    }

    @Override
    protected Object handleInteraction(Request request,
                                       Response response,
                                       String requestContentType,
                                       String name,
                                       ExposedThing thing) {
        ExposedThingEvent<Object> event = thing.getEvent(name);
        if (event != null) {
            Content content = event.observer()
                    .map(optional -> ContentManager.valueToContent(optional.orElse(null), requestContentType))
                    .firstElement().blockingGet();

            if (content != null) {
                Log.w("SubscribeEventRoute", "Next data received for Event "+ name +": " + content);
                response.type(content.getType());
                return content;
            }
            else {
                response.status(HttpStatus.SERVICE_UNAVAILABLE_503);
                return "";
            }
        }
        else {
            response.status(HttpStatus.NOT_FOUND_404);
            return "Event not found";
        }
    }
}
