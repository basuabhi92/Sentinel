package org.ab.sentinel;

import org.ab.sentinel.controller.AppController;
import org.ab.sentinel.service.PostgreSqlService;
import org.ab.sentinel.controller.UserController;
import org.ab.sentinel.service.integrations.GithubIntegrationService;
import org.nanonative.nano.core.Nano;
import org.nanonative.nano.services.http.HttpServer;

import java.util.Map;

import static org.nanonative.nano.services.http.HttpServer.CONFIG_SERVICE_HTTP_PORT;
import static org.nanonative.nano.services.http.HttpServer.EVENT_HTTP_REQUEST;
import static org.nanonative.nano.services.logging.LogService.CONFIG_LOG_FORMATTER;
import static org.nanonative.nano.services.logging.LogService.CONFIG_LOG_LEVEL;
import static org.nanonative.nano.services.logging.model.LogLevel.DEBUG;

public class App {
    public static void main(String[] args) {
        final Nano nano = new Nano(Map.of(
            CONFIG_LOG_LEVEL, DEBUG,
            CONFIG_LOG_FORMATTER, "console",
            CONFIG_SERVICE_HTTP_PORT, "8080"
        ), new HttpServer(), new PostgreSqlService(), new GithubIntegrationService());


        nano.context(UserController.class)
            .subscribeEvent(EVENT_HTTP_REQUEST, UserController::preflight)
            .subscribeEvent(EVENT_HTTP_REQUEST, UserController::registerUser)
            .subscribeEvent(EVENT_HTTP_REQUEST, UserController::loginUser);

        nano.context(AppController.class)
            .subscribeEvent(EVENT_HTTP_REQUEST, AppController::preflight)
            .subscribeEvent(EVENT_HTTP_REQUEST, AppController::getApps)
            .subscribeEvent(EVENT_HTTP_REQUEST, AppController::integrationRequest);
    }
}
