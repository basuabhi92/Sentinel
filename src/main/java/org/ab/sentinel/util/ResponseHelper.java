package org.ab.sentinel.util;

import berlin.yuna.typemap.model.LinkedTypeMap;
import org.nanonative.nano.helper.event.model.Event;
import org.nanonative.nano.services.http.model.ContentType;
import org.nanonative.nano.services.http.model.HttpObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.nanonative.nano.helper.NanoUtils.hasText;

public class ResponseHelper {

    public static HttpObject jsonOk(final Event<HttpObject, HttpObject> event, final Map<String, ?> body) {
        HttpObject resp = event.payload().createCorsResponse().statusCode(200).contentType(ContentType.APPLICATION_JSON).body(body);
        return resp;
    }

    public static HttpObject problem(final Event<HttpObject, HttpObject> event, final int status, final String message) {
        HttpObject resp = event.payload().createCorsResponse().statusCode(status).contentType(ContentType.APPLICATION_PROBLEM_JSON).body(Map.of("message", message, "timestamp", System.currentTimeMillis()));
        return resp;
    }

    public static String missingFields(final LinkedTypeMap body, final String... keys) {
        final List<String> missing = Arrays.stream(keys)
            .filter(k -> !hasText(body.asString(k)))
            .toList();
        return missing.isEmpty() ? null : String.join(", ", missing);
    }
}
