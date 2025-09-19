package org.ab.sentinel.controller;

import berlin.yuna.typemap.logic.JsonEncoder;
import berlin.yuna.typemap.model.LinkedTypeMap;
import org.ab.sentinel.AppEvents;
import org.ab.sentinel.dto.github.GithubDto;
import org.ab.sentinel.dto.github.GithubTokenValidationResultDto;
import org.ab.sentinel.util.JwtHelper;
import org.nanonative.nano.helper.event.model.Event;
import org.nanonative.nano.services.http.model.HttpObject;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.ab.sentinel.util.ResponseHelper.jsonOk;
import static org.ab.sentinel.util.ResponseHelper.missingFields;
import static org.ab.sentinel.util.ResponseHelper.options;
import static org.ab.sentinel.util.ResponseHelper.problem;
import static org.nanonative.nano.helper.NanoUtils.hasText;

public class AppController {

    public static void preflight(Event event) {
        HttpObject request = event.payload(HttpObject.class);
        if (request.isMethodOptions() && (request.pathMatch("/app/list") || request.pathMatch("/app/integration"))) {
            options(event);
        }
    }

    public static void getApps(Event event) {
        HttpObject request = event.payload(HttpObject.class);

        if (request.isMethodGet() && request.pathMatch("/app/list")) {
            event.context().sendEventR(AppEvents.APPS_LIST, () -> null).responseOpt(LinkedHashMap.class).ifPresentOrElse(apps -> {
                jsonOk(event, JsonEncoder.toJson(apps));
            },
                () -> problem(event, 500, "No Apps found"));
        }
    }

    public static void integrationRequest(final Event event) {
        final HttpObject req = event.payload(HttpObject.class);
        if (!(req.isMethodPost() && req.pathMatch("/app/integration"))) {
            return;
        }

        String auth = req.header("Authorization");

        if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            problem(event, 401, "Missing bearer token");
            return;
        }

        final String token = auth.substring(7).trim();

        if (!hasText(token) || !JwtHelper.verify(token)) {
            problem(event, 401, "Not allowed");
            return;
        }

        final LinkedTypeMap body = req.bodyAsJson().asMap();

        final String missing = missingFields(body, "app_id", "user_id");
        if (null != missing) {
            problem(event, 400, String.format("Missing required field(s): %s", missing));
            return;
        }

        final int appId = body.asInt("app_id");
        final String userId = body.asString("user_id");

        switch (appId) {
            case 1 -> handleGithubIntegration(event, body, userId, appId);
            default -> problem(event, 404, "App not available");
        }
    }

    private static void handleGithubIntegration(final Event event, final LinkedTypeMap body, final String userId, final int appId) {
        final String missing = missingFields(body, "access_token");
        if (missing != null) {
            problem(event, 400, "Missing required field(s): " + missing);
            return;
        }
        final GithubDto githubDto = new GithubDto(
            userId,
            body.asString("access_token"),
            body.asStringOpt("opts").orElse(""),
            appId
        );
        event.context()
            .sendEventR(AppEvents.GITHUB_INT_REQ, () -> githubDto)
            .responseOpt(GithubTokenValidationResultDto.class)
            .ifPresentOrElse(gh -> {
                if (!gh.ok()) {
                    problem(event, gh.statusCode(), gh.reason());
                } else {
                    jsonOk(event, Map.of("integration", "success"));
                }
            }, () -> problem(event, 502, "GitHub token validation unavailable"));
    }
}
