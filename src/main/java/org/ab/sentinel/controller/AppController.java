package org.ab.sentinel.controller;

import berlin.yuna.typemap.model.LinkedTypeMap;
import org.ab.sentinel.AppEvents;
import org.ab.sentinel.dto.GithubDto;
import org.ab.sentinel.util.JwtHelper;
import org.nanonative.nano.helper.event.model.Event;
import org.nanonative.nano.services.http.model.HttpObject;

import java.util.Map;

import static org.ab.sentinel.util.ResponseHelper.jsonOk;
import static org.ab.sentinel.util.ResponseHelper.options;
import static org.ab.sentinel.util.ResponseHelper.problem;

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
            event.context().sendEventR(AppEvents.APP_LIST, () -> null).responseOpt(Map.class).ifPresentOrElse(apps -> {
                jsonOk(event, apps);
            }, () -> problem(event, 500, "No Apps found"));
        }
    }

    public static void integrationRequest(Event event) {
        HttpObject request = event.payload(HttpObject.class);
        if (request.isMethodPost() && request.pathMatch("/app/integration")) {
            final LinkedTypeMap body = request.bodyAsJson().asMap();

            final String token = body.asString("token");

            if (!JwtHelper.verify(token)) {
                problem(event, 500, "Not allowed");
                return;
            }

            final Integer appId = body.asInt("app_id");
            final String userId = body.asString("user_id");

            switch(appId) {
                case 1: GithubDto githubDto = new GithubDto(userId, body.asString("access_token"), body.asString("opts"));
                    event.context().sendEventR(AppEvents.GITHUB_INT_REQ, () -> githubDto).responseOpt(Boolean.class).ifPresentOrElse(gh -> {
                        if (!gh) {
                            problem(event, 422, "Invalid credentials");
                        } else {
                            jsonOk(event, Map.of("status", "success"));
                        }

                }, () -> problem(event, 500, "App not available"));
                default: problem(event, 500, "App not available");
            }
        }
    }
}
