package org.ab.sentinel.controller;

import berlin.yuna.typemap.model.LinkedTypeMap;
import org.ab.sentinel.AppEvents;
import org.ab.sentinel.dto.github.GithubDto;
import org.ab.sentinel.util.JwtHelper;
import org.nanonative.nano.helper.event.model.Event;
import org.nanonative.nano.services.http.model.HttpObject;

import java.util.Map;

import static org.ab.sentinel.util.ResponseHelper.jsonOk;
import static org.ab.sentinel.util.ResponseHelper.missingFields;
import static org.ab.sentinel.util.ResponseHelper.problem;
import static org.nanonative.nano.helper.NanoUtils.hasText;
import static org.nanonative.nano.services.http.model.HttpHeaders.AUTHORIZATION;

public class AppController {

    public static void getApps(Event<HttpObject, HttpObject> event) {
        event.payloadOpt()
            .filter(HttpObject::isMethodGet)
            .filter(request -> request.pathMatch("/app/list"))
            .ifPresent(request -> {
                event.context().newEvent(AppEvents.FETCH_APPS).send().responseOpt().ifPresentOrElse(apps -> {
                        event.respond(jsonOk(event, apps));
                    },
                    () -> event.respond(problem(event, 500, "No Apps found")));
            });
    }

    public static void integrationRequest(final Event<HttpObject, HttpObject> event) {
        event.payloadOpt()
            .filter(HttpObject::isMethodPost)
            .filter(request -> request.pathMatch("/app/integration"))
            .ifPresent(request -> {
                String auth = request.header(AUTHORIZATION);
                if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    event.respond(problem(event, 401, "Missing bearer token"));
                    return;
                }
                final String token = auth.substring(7).trim();

                if (!hasText(token) || !JwtHelper.verify(token)) {
                    event.respond(problem(event, 401, "Not allowed"));
                    return;
                }
                final LinkedTypeMap body = request.bodyAsJson().asMap();
                final String missing = missingFields(body, "app_id", "user_id");
                if (null != missing) {
                    event.respond(problem(event, 400, String.format("Missing required field(s): %s", missing)));
                    return;
                }
                final int appId = body.asInt("app_id");
                final String userId = body.asString("user_id");

                switch (appId) {
                    case 1 -> handleGithubIntegration(event, body, userId, appId);
                    default -> event.respond(problem(event, 404, "App not available"));
                }
            });
    }

    private static void handleGithubIntegration(final Event<HttpObject, HttpObject> event, final LinkedTypeMap body, final String userId, final int appId) {
        final String missing = missingFields(body, "access_token");
        if (missing != null) {
            event.respond(problem(event, 400, "Missing required field(s): " + missing));
            return;
        }
        final GithubDto githubDto = new GithubDto(
            userId,
            body.asString("access_token"),
            body.asStringOpt("opts").orElse(""),
            appId
        );
        event.context()
            .newEvent(AppEvents.GITHUB_INT_REQ, () -> githubDto)
            .send()
            .responseOpt()
            .ifPresentOrElse(gh -> {
                if (!gh.ok()) {
                    event.respond(problem(event, gh.statusCode(), gh.reason()));
                } else {
                    event.respond(jsonOk(event, Map.of("integration", "success")));
                }
            }, () -> event.respond(problem(event, 502, "GitHub token validation unavailable")));
    }
}
