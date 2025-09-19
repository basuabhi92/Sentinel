package org.ab.sentinel.controller;

import berlin.yuna.typemap.model.LinkedTypeMap;
import org.ab.sentinel.AppEvents;
import org.ab.sentinel.dto.UserDto;
import org.ab.sentinel.jooq.tables.records.UsersRecord;
import org.ab.sentinel.util.JwtHelper;
import org.ab.sentinel.util.Passwords;
import org.ab.sentinel.util.Pbkdf2Passwords;
import org.nanonative.nano.helper.event.model.Event;
import org.nanonative.nano.services.http.model.HttpObject;

import java.util.Map;

import static org.ab.sentinel.jooq.tables.Users.USERS;
import static org.ab.sentinel.util.ResponseHelper.jsonOk;
import static org.ab.sentinel.util.ResponseHelper.problem;

public class UserController {

    private static final long TOKEN_TTL = 3600;
    private static final Passwords passwords = new Pbkdf2Passwords(200_000, 16, 32);

    public static void loginUser(Event<HttpObject, HttpObject> event) {
        event.payloadOpt()
            .filter(HttpObject::isMethodPost)
            .filter(request -> request.pathMatch("/auth/login"))
            .ifPresent(request -> {
                final LinkedTypeMap body = request.bodyAsJson().asMap();
                final String email = body.asString("email");
                final String password = body.asString("password");

                if (email == null || password == null || email.isBlank() || password.isBlank()) {
                    event.respond(problem(event, 400, "email and password are required"));
                }
                event.context().newEvent(AppEvents.FETCH_USER, () -> email).responseOpt(UsersRecord.class).ifPresentOrElse(user -> {
                    final String storedHash = user.get(USERS.PASSWORD_HASH);
                    if (passwords.verify(password, storedHash)) {
                        final String token = JwtHelper.createToken(Map.of("sub", user.get(USERS.ID), "email", email), TOKEN_TTL);
                        event.respond(jsonOk(event, Map.of("token", token)));
                    } else {
                        event.respond(problem(event, 422, "Invalid email or password"));
                    }
                }, () -> event.respond(problem(event, 500, "Email is not registered")));

            });
    }

    public static void registerUser(Event<HttpObject, HttpObject> event) {
        event.payloadOpt()
            .filter(HttpObject::isMethodPost)
            .filter(request -> request.pathMatch("/auth/register"))
            .ifPresent(request -> {
                final LinkedTypeMap body = request.bodyAsJson().asMap();

                final String email = body.asString("email");
                final String password = body.asString("password");
                final String name = body.asString("name");

                if (email == null || password == null || email.isBlank() || password.isBlank()) {
                    event.respond(problem(event, 400, "Email and password are required"));
                    return;
                }

                final String hash = passwords.hash(password);
                event.context().newEvent(AppEvents.ADD_USER, () -> new UserDto(email, hash, name)).responseOpt(UsersRecord.class).ifPresentOrElse(user -> {
                    final String token = JwtHelper.createToken(Map.of("sub", user.getId(), "email", email), TOKEN_TTL);
                    event.respond(jsonOk(event, Map.of("token", token)));

                }, () -> event.respond(problem(event, 422, "Email already registered")));
            });
    }
}
