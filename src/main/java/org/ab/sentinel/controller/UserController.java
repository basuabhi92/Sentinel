package org.ab.sentinel.controller;

import berlin.yuna.typemap.model.TypeMap;
import org.ab.sentinel.AppEvents;
import org.ab.sentinel.model.LoginRequest;
import org.ab.sentinel.model.LoginResult;
import org.ab.sentinel.model.RegisterRequest;
import org.ab.sentinel.model.RegisterResult;
import org.ab.sentinel.util.JwtHelper;
import org.nanonative.nano.helper.event.model.Event;
import org.nanonative.nano.services.http.model.ContentType;
import org.nanonative.nano.services.http.model.HttpObject;

import java.util.Map;

public class UserController {

    private static final long TOKEN_TTL = 3600;
    private static final String CORS_METHODS = "GET,POST,PUT,PATCH,DELETE,OPTIONS";

    public static void preflight(Event event) {
        HttpObject request = event.payload(HttpObject.class);
        if (request.isMethodOptions()
            && (request.pathMatch("/auth/register") || request.pathMatch("/auth/login"))) {
            HttpObject resp = new HttpObject()
                .statusCode(200)
                .header("Access-Control-Allow-Headers", "Content-Type,Authorization")
                .corsResponse("*",  CORS_METHODS, "Content-Type,Authorization", 86400);
            event.response(resp);
        }
    }

    public static void loginUser(Event event) {
        HttpObject request = event.payload(HttpObject.class);
        if (request.isMethodPost() && request.pathMatch("/auth/login")) {
            final TypeMap body = new TypeMap(request.bodyAsJson().asMap());

            final String email = body.asString("email");
            final String password = body.asString("password");

            if (email == null || password == null || email.isBlank() || password.isBlank()) {
                problem(event, 400, "email and password are required");
                return;
            }

            // Ask Postgres service to validate user login (wait for reply)
            event.context().sendEventR(AppEvents.USER_LOGIN, () -> new LoginRequest(email, password))
                .responseOpt(LoginResult.class)
                .ifPresentOrElse(result -> {
                    if (result.ok()) {
                        final String token = JwtHelper.createToken(
                            Map.of("sub", result.userId(),
                                "email", email),
                            TOKEN_TTL
                        );

                        jsonOk(event, new TypeMap().putR("token", token));
                    } else {
                        problem(event, 422,
                            result.reason() == null ? "Login invalid" : result.reason());
                    }
                }, () -> problem(event, 500, "Internal Server Error"));
        }
    }

    public static void registerUser(Event event) {
        HttpObject request = event.payload(HttpObject.class);
        if (request.isMethodPost() && request.pathMatch("/auth/register")) {
            event.acknowledge();
            final TypeMap body = new TypeMap(request.bodyAsJson().asMap());

            final String email = body.asString("email");
            final String password = body.asString("password");
            final String name = body.asString("name");

            // Basic payload checks
            if (email == null || password == null || email.isBlank() || password.isBlank()) {
                problem(event, 400, "email and password are required");
                return;
            }

            // Ask Postgres service to register user (wait for reply)
            event.context().sendEventR(AppEvents.USER_REGISTER, () -> new RegisterRequest(email, password, name))
                .responseOpt(RegisterResult.class)
                .ifPresentOrElse(result -> {
                    if (result.ok()) {
                        final String token = JwtHelper.createToken(
                            Map.of("sub", result.userId(),
                                "email", email),
                            TOKEN_TTL
                        );

                        jsonOk(event, new TypeMap().putR("token", token));
                    } else {
                        problem(event, 422,
                            result.reason() == null ? "Registration invalid" : result.reason());
                    }
                }, () -> problem(event, 500, "Internal Server Error"));
        }
    }

    private static void jsonOk(final Event event, final TypeMap body) {
        HttpObject resp = new HttpObject()
            .statusCode(200)
            .contentType(ContentType.APPLICATION_JSON)
            .bodyT(body)
            .corsResponse(safeHeader(event.payload(HttpObject.class), "Origin"),  CORS_METHODS, "Content-Type, Authorization", 86400, true);
        event.response(resp);
    }

    private static void problem(final Event event, final int status, final String message) {
        HttpObject resp = new HttpObject()
            .statusCode(status)
            .contentType(ContentType.APPLICATION_JSON)
            .bodyT(new TypeMap()
                .putR("message", message)
                .putR("timestamp", System.currentTimeMillis()))
             .header("Access-Control-Allow-Origin", "allow")
            .corsResponse("*",  CORS_METHODS, "Content-Type, Authorization", 86400);
        event.response(resp);
    }

    private static String safeHeader(final HttpObject req, final String name) {
        try { return req.header(name); } catch (Exception ignore) { return null; }
    }
}
