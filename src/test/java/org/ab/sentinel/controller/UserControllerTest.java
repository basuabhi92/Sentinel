package org.ab.sentinel.controller;

import org.ab.sentinel.PostgresContainer.SharedPostgresIT;
import org.ab.sentinel.model.Postgres;
import org.ab.sentinel.service.PostgreSqlService;
import org.ab.sentinel.service.integrations.GithubIntegrationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.nanonative.nano.core.Nano;
import org.nanonative.nano.services.http.HttpClient;
import org.nanonative.nano.services.http.HttpServer;
import org.nanonative.nano.services.http.model.HttpMethod;
import org.nanonative.nano.services.http.model.HttpObject;

import java.util.Map;

import static org.ab.sentinel.service.PostgreSqlService.CONFIG_DB_HOST;
import static org.ab.sentinel.service.PostgreSqlService.CONFIG_DB_PORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.nanonative.nano.core.model.Context.EVENT_CONFIG_CHANGE;
import static org.nanonative.nano.services.http.HttpServer.EVENT_HTTP_REQUEST;

class UserControllerTest {

    protected static String serverUrl = "http://localhost:";
    private static Postgres dbProp;

    @BeforeAll
    public static void initializeDB() {
        dbProp = SharedPostgresIT.startDatabase();
    }


    @Test
    void registerUser() {
        final Nano nano = new Nano(Map.of("app_profiles", "dev"), new HttpServer(), new PostgreSqlService(), new GithubIntegrationService(), new HttpClient());

        nano.context().newEvent(EVENT_CONFIG_CHANGE, () -> Map.of(
            CONFIG_DB_HOST, dbProp.dbHost(),
            CONFIG_DB_PORT, dbProp.dbPort()
        )).send();

        nano.context(UserControllerTest.class)
            .subscribeEvent(EVENT_HTTP_REQUEST, UserController::registerUser);

        final HttpObject result = new HttpObject()
            .methodType(HttpMethod.POST)
            .body(Map.of("email", "aa@berlin.iosk", "password", "abc","name","aj"))
            .path(serverUrl + nano.service(HttpServer.class).port() + "/auth/register")
            .send(nano.context(UserControllerTest.class));

        assertThat(result).isNotNull();
        assertThat(result.bodyAsString()).contains("token");
        assertThat(nano.stop(UserControllerTest.class).waitForStop().isReady()).isFalse();
    }

    @Test
    void getApps() {
        final Nano nano = new Nano(Map.of("app_profiles", "dev"), new HttpServer(), new PostgreSqlService(), new GithubIntegrationService(), new HttpClient());

        nano.context(UserControllerTest.class).newEvent(EVENT_CONFIG_CHANGE, () -> Map.of(
            CONFIG_DB_HOST, dbProp.dbHost(),
            CONFIG_DB_PORT, dbProp.dbPort()
        )).send();

        nano.context(AppController.class)
            .subscribeEvent(EVENT_HTTP_REQUEST, AppController::getApps);

        final HttpObject result = new HttpObject()
            .methodType(HttpMethod.GET)
            .path(serverUrl + nano.service(HttpServer.class).port() + "/app/list")
            .send(nano.context(UserControllerTest.class));

        assertThat(result).isNotNull();
        assertThat(result.bodyAsString()).contains("GitHub");
        assertThat(nano.stop(UserControllerTest.class).waitForStop().isReady()).isFalse();
    }
}
