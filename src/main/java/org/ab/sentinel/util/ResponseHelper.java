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

    private static final String CORS_METHODS = "GET,POST,PUT,PATCH,DELETE,OPTIONS";

    public static void jsonOk(final Event event, final Map<String, Object> body) {
        HttpObject request = event.payload(HttpObject.class);
        HttpObject resp = request.corsResponse(request.headerMap().asString("Origin"), CORS_METHODS, null, 86400, true).statusCode(200).contentType(ContentType.APPLICATION_JSON).body(body);
        event.response(resp);
    }

    public static void problem(final Event event, final int status, final String message) {
        HttpObject request = event.payload(HttpObject.class);
        HttpObject resp = request.corsResponse(request.headerMap().asString("Origin"), CORS_METHODS).statusCode(status).contentType(ContentType.APPLICATION_PROBLEM_JSON).body(Map.of("message", message, "timestamp", System.currentTimeMillis()));
        event.response(resp);
    }

    public static void options(final Event event) {
        HttpObject resp = event.payload(HttpObject.class).statusCode(200).corsResponse(event.payload(HttpObject.class).headerMap().asString("Origin"), CORS_METHODS);
        event.response(resp);
    }

    public static String missingFields(final LinkedTypeMap body, final String... keys) {
        final List<String> missing = Arrays.stream(keys)
            .filter(k -> !hasText(body.asString(k)))
            .toList();
        return missing.isEmpty() ? null : String.join(", ", missing);
    }
}
