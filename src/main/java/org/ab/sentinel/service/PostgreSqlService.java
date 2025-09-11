package org.ab.sentinel.service;

import berlin.yuna.typemap.model.TypeMapI;
import org.ab.sentinel.AppEvents;
import static org.ab.sentinel.jooq.tables.Users.USERS;
import static org.nanonative.nano.helper.config.ConfigRegister.registerConfig;

import org.ab.sentinel.db.DataSourceFactory;
import org.ab.sentinel.db.JooqFactory;
import org.ab.sentinel.dto.UserDto;
import org.ab.sentinel.jooq.tables.records.UsersRecord;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.nanonative.nano.core.model.Service;
import org.nanonative.nano.helper.event.model.Event;

import javax.sql.DataSource;


public final class PostgreSqlService extends Service {

    public static final String CONFIG_DB_URL = registerConfig("db_url", "Database url");
    public static final String CONFIG_DB_USER = registerConfig("db_user", "Database user");
    public static final String CONFIG_DB_PASS = registerConfig("db_pass", "Database password");
    private final String localDbUrl = "jdbc:postgresql://127.0.0.1:5432/nanodb?sslmode=disable";
    private final String localDbUser = "nanouser";
    private final String localDbPass = "CHANGEME";
    private String dbUrl;
    private String dbUser;
    private String dbPass;
    private DSLContext dsl;

    @Override public String name() { return "PostgreSqlService"; }

    @Override
    public void start() {
        DataSource ds = DataSourceFactory.create(dbUrl, dbUser, dbPass);
        this.dsl = JooqFactory.create(ds);
        context.info(() -> "[" + name() + "] started ");
    }

    @Override
    public void stop() {

    }

    @Override
    public Object onFailure(final Event event) {
        return null;
    }

    @Override
    public void onEvent(final Event event) {
        event.ifPresentAck(AppEvents.USER_REGISTER, UserDto.class, this::saveUser);
        event.ifPresentAck(AppEvents.USER_LOGIN, String.class, this::fetchUser);
    }

    private UsersRecord fetchUser(final String email) {
        final UsersRecord user = dsl
            .selectFrom(USERS)
            .where(USERS.EMAIL.eq(email))
            .fetchOne();
        return user;
    }

    private UsersRecord saveUser(final UserDto user) {

        final UsersRecord userRecord = dsl.transactionResult(configuration -> {
            DSLContext ctx = DSL.using(configuration);
            if (ctx.fetchExists(ctx.selectOne()
                .from(USERS)
                .where(USERS.EMAIL.eq(user.email())))) {
                return null;
            }
            return ctx.insertInto(USERS)
                .set(USERS.EMAIL, user.email())
                .set(USERS.NAME, user.name())
                .set(USERS.PASSWORD_HASH, user.passwordHash())
                .returning(USERS.ID)
                .fetchOne();
        });
        return userRecord;
    }

    @Override
    public void configure(final TypeMapI<?> configs, final TypeMapI<?> merged) {
        this.dbUrl  = merged.asStringOpt(CONFIG_DB_URL).orElseGet(() -> localDbUrl);
        this.dbUser  = merged.asStringOpt(CONFIG_DB_USER).orElseGet(() -> localDbUser);
        this.dbPass  = merged.asStringOpt(CONFIG_DB_PASS).orElseGet(() -> localDbPass);
    }
}
