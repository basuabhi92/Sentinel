package org.ab.sentinel.service.integrations;

import berlin.yuna.typemap.model.TypeMapI;
import org.ab.sentinel.AppEvents;
import org.ab.sentinel.dto.github.GithubDto;
import org.ab.sentinel.dto.github.GithubTokenValidationResultDto;
import org.ab.sentinel.dto.integrations.AppIntegrationRequestDto;
import org.ab.sentinel.jooq.tables.records.IntegrationsRecord;
import org.nanonative.nano.core.model.Service;
import org.nanonative.nano.helper.event.model.Event;
import org.nanonative.nano.services.http.model.HttpMethod;
import org.nanonative.nano.services.http.model.HttpObject;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static org.nanonative.nano.services.http.HttpClient.EVENT_SEND_HTTP;

public class GithubIntegrationService extends Service {

    // TODO: Create pagination service
    private final String GITHUB_URI = "https://api.github.com/notifications?per_page=1";

    @Override
    public void start() {
        context.info(() -> "[{}] started", name());
    }

    @Override
    public void stop() {

    }

    @Override
    public Object onFailure(final Event event) {
        return null;
    }

    @Override
    public void onEvent(final Event<?, ?> event) {
        event.channel(AppEvents.GITHUB_INT_REQ).ifPresent(ev -> ev.respond(githubIntReq(ev.payload())));
    }

    private GithubTokenValidationResultDto githubIntReq(GithubDto githubDto) {

        Map<String, String> headers = Map.of("Authorization", String.format("Bearer %s", githubDto.accessToken()), "Accept", "application/vnd.github+json", "X-GitHub-Api-Version", "2022-11-28");

        HttpObject ghReq = new HttpObject().path(GITHUB_URI).methodType(HttpMethod.GET).headerMap(headers).timeout(10000);

        // Remote call to Github will fail without Https
        final HttpObject response = this.context.newEvent(EVENT_SEND_HTTP).payload(() -> ghReq).send().response();

        String scopes = response.headerMap().asString("X-OAuth-Scopes");
        String sso = response.headerMap().asString("X-GitHub-SSO");
        String lastModified = response.headerMap().asString("Last-Modified");
        Integer xPollInterval = response.headerMap().asInt("X-Poll-Interval");
        String expHdr = response.headerMap().asStringOpt("GitHub-Authentication-Token-Expiration").orElse("github-authentication-token-expiration");
        Instant expiresAt = parseExpiry(expHdr);
        long daysRemaining = expiresAt == null ? -1 : Duration.between(Instant.now(), expiresAt).toDays();

        if (response.statusCode() == 200) {
            var res = new GithubTokenValidationResultDto(true, response.statusCode(), "", lastModified, xPollInterval, expiresAt, daysRemaining);
            var req = new AppIntegrationRequestDto(UUID.fromString(githubDto.userId()), githubDto.appId(), githubDto.accessToken(), scopes, expiresAt);
            return context.newEvent(AppEvents.APP_INT_REQ, () -> req).send().responseOpt(IntegrationsRecord.class).map(rec -> res).orElseGet(() -> new GithubTokenValidationResultDto(false, 409, "Db update failed", lastModified, xPollInterval, expiresAt, daysRemaining));
        } else if (response.statusCode() >= 400) {
            return new GithubTokenValidationResultDto(false, response.statusCode(), "invalid token", lastModified, xPollInterval, expiresAt, daysRemaining);
        } else {
            boolean hasNotifScope = scopes != null && (scopes.contains("notifications") || scopes.contains("repo"));
            if (!hasNotifScope) {
                return new GithubTokenValidationResultDto(false, response.statusCode(), "missing scope", lastModified, xPollInterval, expiresAt, daysRemaining);
            }
            if (expiresAt != null && expiresAt.isBefore(Instant.now().plus(Duration.ofDays(30)))) {
                return new GithubTokenValidationResultDto(false, 409, "expires in less than 30 days", lastModified, xPollInterval, expiresAt, daysRemaining);
            }
            return new GithubTokenValidationResultDto(false, response.statusCode(), "unknow status", lastModified, xPollInterval, expiresAt, daysRemaining);
        }
    }

    @Override
    public void configure(final TypeMapI<?> typeMapI, final TypeMapI<?> typeMapI1) {

    }

    private static Instant parseExpiry(String s) {
        if (s == null || s.isBlank()) return null;

        DateTimeFormatter fmt;
        if (s.endsWith("UTC")) {
            fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);
        } else {
            fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xx").withZone(ZoneOffset.UTC);
        }
        return Instant.from(fmt.parse(s));
    }
}
