package org.ab.sentinel;

import org.ab.sentinel.dto.UserDto;
import org.ab.sentinel.dto.github.GithubDto;
import org.ab.sentinel.dto.github.GithubTokenValidationResultDto;
import org.ab.sentinel.dto.integrations.AppIntegrationRequestDto;
import org.ab.sentinel.jooq.tables.records.IntegrationsRecord;
import org.ab.sentinel.jooq.tables.records.UsersRecord;
import org.nanonative.nano.helper.event.model.Channel;

import java.util.Map;

import static org.nanonative.nano.helper.event.model.Channel.registerChannelId;

public final class AppEvents {
    private AppEvents() {}

    // App events
    public static final Channel<UserDto, UsersRecord> ADD_USER = registerChannelId("ADD_USER", UserDto.class, UsersRecord.class);
    public static final Channel<String, UsersRecord> FETCH_USER = registerChannelId("USER_LOGIN", String.class, UsersRecord.class);
    public static final Channel<Void, Map> FETCH_APPS = registerChannelId("FETCH_APPS", Void.class, Map.class);
    public static final Channel<AppIntegrationRequestDto, IntegrationsRecord> APP_INT_REQ = registerChannelId("APP_INT_REQ", AppIntegrationRequestDto.class, IntegrationsRecord.class);
    public static final Channel<GithubDto, GithubTokenValidationResultDto> GITHUB_INT_REQ = registerChannelId("GITHUB_INT_REQ", GithubDto.class, GithubTokenValidationResultDto.class);
    // public static final int NOTIF_INGESTED = registerChannelId("NOTIF_INGESTED");
}
