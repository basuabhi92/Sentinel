package org.ab.sentinel.service;

import berlin.yuna.typemap.model.TypeMap;
import berlin.yuna.typemap.model.TypeMapI;
import org.ab.sentinel.AppEvents;
import org.ab.sentinel.model.LoginRequest;
import org.ab.sentinel.model.LoginResult;
import org.ab.sentinel.model.RegisterRequest;
import org.ab.sentinel.model.RegisterResult;
import org.ab.sentinel.util.JwtHelper;
import org.nanonative.nano.core.model.Service;
import org.nanonative.nano.helper.event.model.Event;
import org.nanonative.nano.services.http.model.ContentType;
import org.nanonative.nano.services.http.model.HttpObject;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class UserService extends Service {

    private static final long TOKEN_TTL = 3600;
    private static final String CORS_METHODS = "GET,POST,PUT,PATCH,DELETE,OPTIONS";
    private static final boolean CORS_ALLOW_CREDENTIALS = false;

    private JwtHelper jwtHelper;

    @Override
    public String name() {return "UserService";}

    @Override
    public void start() {
        jwtHelper = new JwtHelper("SENTINEL_SECRET");
        context.info(() -> "[" + name() + "] started ");
    }

    @Override
    public void stop() {

    }

    @Override
    public Object onFailure(final Event event) {
        // Log the failure event
        context.error(() -> "Event failure: " + event);
        return null;
    }


    @Override
    public void onEvent(final Event event) {

        event.payloadOpt(HttpObject.class).ifPresent(request -> {

            if (request.isMethodOptions()
                && (request.pathMatch("/auth/register") || request.pathMatch("/auth/login"))) {
                event.response(handleCorsPreflight(request));
                return;
            }

            if (request.isMethodPost() && request.pathMatch("/auth/register")) {
                event.acknowledge();
                registerUser(request, event);
            }

            if (request.isMethodPost() && request.pathMatch("/auth/login")) {
                event.acknowledge();
                loginUser(request, event);
            }
        });
    }

    private void loginUser(HttpObject request, Event event) {
        try {
            final String json = new String(request.body(), StandardCharsets.UTF_8);
            final TypeMap body = new TypeMap(json);

            final String email    = body.asString("email");
            final String password = body.asString("password");

            if (email == null || password == null || email.isBlank() || password.isBlank()) {
                problem(request, event, 400, "email and password are required");
                return;
            }

            // Ask Postgres service to validate user login (wait for reply)
            context.sendEventR(AppEvents.USER_LOGIN, () -> new LoginRequest(email, password))
                .responseOpt(LoginResult.class)
                .ifPresentOrElse(result -> {
                    if (result.ok()) {
                        final String token = jwtHelper.createToken(
                            Map.of("sub", result.userId(),
                                "email", email),
                            TOKEN_TTL
                        );

                        jsonOk(request, event, new TypeMap().putR("token", token));
                    } else {
                        problem(request, event, 422,
                            result.reason() == null ? "Login invalid" : result.reason());
                    }
                }, () -> problem(request, event, 500, "Internal Server Error"));

        } catch (Exception ex) {
            context.error(() -> name() + " login error: " + ex);
            problem(request, event, 500, "Internal Server Error");
        }
    }

    private void registerUser(HttpObject request, Event event) {
        try {
            final String json = new String(request.body(), StandardCharsets.UTF_8);
            final TypeMap body = new TypeMap(json);

            final String email    = body.asString("email");
            final String password = body.asString("password");
            final String name     = body.asString("name");

            // Basic payload checks
            if (email == null || password == null || email.isBlank() || password.isBlank()) {
                problem(request, event, 400, "email and password are required");
                return;
            }

            // Ask Postgres service to register user (wait for reply)
            context.sendEventR(AppEvents.USER_REGISTER, () -> new RegisterRequest(email, password, name))
                .responseOpt(RegisterResult.class)
                .ifPresentOrElse(result -> {
                    if (result.ok()) {
                        final String token = jwtHelper.createToken(
                            Map.of("sub", result.userId(),
                                "email", email),
                            TOKEN_TTL
                        );

                        jsonOk(request, event, new TypeMap().putR("token", token));
                    } else {
                        problem(request, event, 422,
                            result.reason() == null ? "Registration invalid" : result.reason());
                    }
                }, () -> problem(request, event, 500, "Internal Server Error"));

        } catch (Exception ex) {
            context.error(() -> name() + " register error: " + ex);
            problem(request, event, 500, "Internal Server Error");
        }
    }

    private void jsonOk(final HttpObject req, final Event event, final TypeMap body) {
        HttpObject resp = new HttpObject()
            .statusCode(200)
            .contentType(ContentType.APPLICATION_JSON)
            .bodyT(body);
        applyCors(resp, req);
        event.response(resp);
    }

    private void problem(final HttpObject req, final Event event, final int status, final String message) {
        HttpObject resp = new HttpObject()
            .statusCode(200)
            .contentType(ContentType.APPLICATION_JSON)
            .bodyT(new TypeMap()
                .putR("message", message)
                .putR("timestamp", System.currentTimeMillis()));
        applyCors(resp, req);
        event.response(resp);
    }

    @Override
    public void configure(final TypeMapI<?> typeMapI, final TypeMapI<?> typeMapI1) {

    }

    private HttpObject handleCorsPreflight(final HttpObject request) {
        final String origin     = safeHeader(request, "Origin");
        final String reqHeaders = safeHeader(request, "Access-Control-Request-Headers");

        HttpObject resp = new HttpObject()
            .statusCode(204)
            .header("Access-Control-Allow-Origin", origin == null || origin.isBlank() ? "*" : origin)
            .header("Vary", "Origin, Access-Control-Request-Headers, Access-Control-Request-Method")
            .header("Access-Control-Allow-Methods", CORS_METHODS)
            .header("Access-Control-Allow-Headers",
                (reqHeaders == null || reqHeaders.isBlank()) ? "Content-Type,Authorization" : reqHeaders)
            .header("Access-Control-Max-Age", "86400");

        return resp;
    }

    private void applyCors(final HttpObject resp, final HttpObject req) {
        String origin = safeHeader(req, "Origin");
        String allow  = (origin == null || origin.isBlank()) ? "*" : origin;

        resp.header("Access-Control-Allow-Origin", allow)
            .header("Vary", "Origin")
            .header("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
    }

    private String safeHeader(final HttpObject req, final String name) {
        try { return req.header(name); } catch (Exception ignore) { return null; }
    }
}
