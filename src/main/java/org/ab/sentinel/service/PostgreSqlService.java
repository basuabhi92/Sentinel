package org.ab.sentinel.service;

import berlin.yuna.typemap.model.TypeMapI;
import org.ab.sentinel.AppEvents;

import static org.ab.sentinel.jooq.Tables.APPS;
import static org.ab.sentinel.jooq.Tables.INTEGRATIONS;
import static org.ab.sentinel.jooq.tables.Users.USERS;
import static org.nanonative.nano.helper.config.ConfigRegister.registerConfig;

import org.ab.sentinel.db.DataSourceConfig;
import org.ab.sentinel.db.DataSourceFactory;
import org.ab.sentinel.db.JooqFactory;
import org.ab.sentinel.dto.AppDto;
import org.ab.sentinel.dto.UserDto;
import org.ab.sentinel.dto.integrations.AppIntegrationRequestDto;
import org.ab.sentinel.jooq.tables.records.AppsRecord;
import org.ab.sentinel.jooq.tables.records.IntegrationsRecord;
import org.ab.sentinel.jooq.tables.records.UsersRecord;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.nanonative.nano.core.model.Service;
import org.nanonative.nano.helper.event.model.Event;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;


public final class PostgreSqlService extends Service {

    public static final String CONFIG_DB_USER = registerConfig("pg_db_user", "Database user");
    public static final String CONFIG_DB_PASS = registerConfig("pg_db_pass", "Database password");
    public static final String CONFIG_DB_NAME = registerConfig("pg_db_name", "Database name");
    public static final String CONFIG_DB_HOST = registerConfig("pg_db_host", "Database host");
    public static final String CONFIG_DB_PORT = registerConfig("pg_db_port", "Database port");
    public static final String CONFIG_DB_OPTIONS = registerConfig("pg_db_opts", "Database options");
    private String dbHost;
    private Integer dbPort;
    private String dbName;
    private String dbUser;
    private String dbPass;
    private String dbOpts;
    private DataSource ds;
    private DataSourceConfig dsConfig;
    private DSLContext dsl;

    @Override
    public void start() {
        createOrUpdateDs(dbHost, dbPort, dbName, dbUser, dbPass, dbOpts);
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
        event.channel(AppEvents.ADD_USER).ifPresent(ev -> ev.respond(saveUser(ev.payload())));
        event.channel(AppEvents.FETCH_USER).ifPresent(ev -> ev.respond(fetchUser(ev.payload())));
        event.channel(AppEvents.FETCH_APPS).ifPresent(ev -> ev.respond(getApps()));
        event.channel(AppEvents.APP_INT_REQ).ifPresent(ev -> ev.respond(saveNewUserIntegration(ev.payload())));
    }

    private Map<String, AppDto> getApps() {
        final Map<String, AppsRecord> apps = dsl.selectFrom(APPS).fetchMap(APPS.NAME);
        return apps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
                var r = e.getValue();
                var meta = (r.getMetadata() == null) ? null : r.getMetadata().data();
                return new AppDto(r.getId(), r.getName(), r.getLogoUrl(), meta);
            }, (a, b) -> a, // shouldn't hit since names are unique
            LinkedHashMap::new));
    }

    private IntegrationsRecord saveNewUserIntegration(AppIntegrationRequestDto req) {
        final IntegrationsRecord res = dsl.insertInto(INTEGRATIONS).set(INTEGRATIONS.APP_ID, req.appId()).set(INTEGRATIONS.USER_ID, req.userId()).set(INTEGRATIONS.SCOPES, req.scopes().split(",")).set(INTEGRATIONS.ACCESS_TOKEN_ENC, req.accessToken().getBytes(StandardCharsets.UTF_8)).set(INTEGRATIONS.EXPIRES_AT, req.expiresAt().atOffset(ZoneOffset.UTC)).returning(INTEGRATIONS.USER_ID, INTEGRATIONS.APP_ID).fetchOne();
        return res;
    }

    private UsersRecord fetchUser(final String email) {
        final UsersRecord user = dsl.selectFrom(USERS).where(USERS.EMAIL.eq(email)).fetchOne();
        return user;
    }

    private UsersRecord saveUser(final UserDto user) {
        final UsersRecord userRecord = dsl.transactionResult(configuration -> {
            DSLContext ctx = DSL.using(configuration);
            if (ctx.fetchExists(ctx.selectOne().from(USERS).where(USERS.EMAIL.eq(user.email())))) {
                return null;
            }
            return ctx.insertInto(USERS).set(USERS.EMAIL, user.email()).set(USERS.NAME, user.name()).set(USERS.PASSWORD_HASH, user.passwordHash()).returning(USERS.ID).fetchOne();
        });
        return userRecord;
    }

    @Override
    public void configure(final TypeMapI<?> changes, final TypeMapI<?> merged) {
        this.dbName = changes.asStringOpt(CONFIG_DB_NAME).orElse(merged.asString(CONFIG_DB_NAME));
        this.dbUser = changes.asStringOpt(CONFIG_DB_USER).orElse(merged.asString(CONFIG_DB_USER));

        // On the config change event, this merged has values from application.properties and not application-<profile>.properties
        // even after doing NanoUtils.readProfiles() and using the context returned.
        this.dbPass = changes.asStringOpt(CONFIG_DB_PASS).orElse(merged.asString(CONFIG_DB_PASS));
        this.dbPort = changes.asIntOpt(CONFIG_DB_PORT).orElse(merged.asInt(CONFIG_DB_PORT));
        this.dbHost = changes.asStringOpt(CONFIG_DB_HOST).orElse(merged.asString(CONFIG_DB_HOST));
        this.dbOpts = changes.asStringOpt(CONFIG_DB_OPTIONS).orElse(merged.asString(CONFIG_DB_OPTIONS));
        if (changes.containsKey(CONFIG_DB_PORT)) {
            createOrUpdateDs(this.dbHost, changes.asInt(CONFIG_DB_PORT), this.dbName, this.dbUser, this.dbPass, this.dbOpts);
        }
    }

    private void createOrUpdateDs(String host, Integer port, String name, String user, String pass, String options) {
        DataSourceConfig newConfig = new DataSourceConfig(host, port, name, user, pass, options);
        if (null == this.dsConfig || !this.dsConfig.equals(newConfig)) {
            this.ds = DataSourceFactory.create(newConfig);
            this.dsl = JooqFactory.create(ds);
            this.dsConfig = newConfig;
        }
    }
}
