package org.ab.sentinel.controller;

import berlin.yuna.typemap.model.LinkedTypeMap;
import org.ab.sentinel.AppEvents;
import org.ab.sentinel.dto.UserDto;
import org.ab.sentinel.jooq.tables.records.UsersRecord;
import org.ab.sentinel.util.JwtHelper;
import org.ab.sentinel.util.Passwords;
import org.ab.sentinel.util.Pbkdf2Passwords;
import org.nanonative.nano.helper.event.model.Event;
import org.nanonative.nano.services.http.model.ContentType;
import org.nanonative.nano.services.http.model.HttpObject;

import java.util.Map;

import static org.ab.sentinel.jooq.tables.Users.USERS;

public class UserController {

    private static final long TOKEN_TTL = 3600;
    private static final String CORS_METHODS = "GET,POST,PUT,PATCH,DELETE,OPTIONS";
    private static final Passwords passwords = new Pbkdf2Passwords(200_000, 16, 32);

    public static void preflight(Event event) {
        HttpObject request = event.payload(HttpObject.class);
        if (request.isMethodOptions() && (request.pathMatch("/auth/register") || request.pathMatch("/auth/login"))) {
            HttpObject resp = request.statusCode(200).corsResponse(event.payload(HttpObject.class).headerMap().asString("Origin"), CORS_METHODS);
            event.response(resp);
        }
    }

    public static void loginUser(Event event) {
        HttpObject request = event.payload(HttpObject.class);
        if (request.isMethodPost() && request.pathMatch("/auth/login")) {
            final LinkedTypeMap body = request.bodyAsJson().asMap();

            final String email = body.asString("email");
            final String password = body.asString("password");

            if (email == null || password == null || email.isBlank() || password.isBlank()) {
                problem(event, 400, "email and password are required");
                return;
            }

            event.context().sendEventR(AppEvents.USER_LOGIN, () -> email).responseOpt(UsersRecord.class).ifPresentOrElse(user -> {
                final String storedHash = user.get(USERS.PASSWORD_HASH);
                if (passwords.verify(password, storedHash)) {
                    final String token = JwtHelper.createToken(Map.of("sub", user.get(USERS.ID), "email", email), TOKEN_TTL);
                    jsonOk(event, Map.of("token", token));
                } else {
                    problem(event, 422, "Invalid email or password");
                }
            }, () -> problem(event, 500, "Email is not registered"));
        }
    }

    public static void registerUser(Event event) {
        HttpObject request = event.payload(HttpObject.class);
        if (request.isMethodPost() && request.pathMatch("/auth/register")) {
            event.acknowledge();
            final LinkedTypeMap body = request.bodyAsJson().asMap();

            final String email = body.asString("email");
            final String password = body.asString("password");
            final String name = body.asString("name");

            if (email == null || password == null || email.isBlank() || password.isBlank()) {
                problem(event, 400, "Email and password are required");
                return;
            }

            final String hash = passwords.hash(password);
            event.context().sendEventR(AppEvents.USER_REGISTER, () -> new UserDto(email, hash, name)).responseOpt(UsersRecord.class).ifPresentOrElse(user -> {
                final String token = JwtHelper.createToken(Map.of("sub", user.getId(), "email", email), TOKEN_TTL);
                jsonOk(event, Map.of("token", token));

            }, () -> problem(event, 422, "Email already registered"));
        }
    }

    private static void jsonOk(final Event event, final Map<String, Object> body) {
        HttpObject request = event.payload(HttpObject.class);
        HttpObject resp = request.corsResponse(request.headerMap().asString("Origin"), CORS_METHODS, null, 86400, true).statusCode(200).contentType(ContentType.APPLICATION_JSON).body(body);
        event.response(resp);
    }

    private static void problem(final Event event, final int status, final String message) {
        HttpObject request = event.payload(HttpObject.class);
        HttpObject resp = request.corsResponse(request.headerMap().asString("Origin"), CORS_METHODS).statusCode(status).contentType(ContentType.APPLICATION_PROBLEM_JSON).body(Map.of("message", message, "timestamp", System.currentTimeMillis()));
        event.response(resp);
    }
}
