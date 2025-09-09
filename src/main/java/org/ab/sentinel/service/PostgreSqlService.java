package org.ab.sentinel.service;

import berlin.yuna.typemap.model.TypeMapI;
import org.ab.sentinel.AppEvents;
import static org.ab.sentinel.jooq.tables.Users.USERS;

import org.ab.sentinel.db.DataSourceFactory;
import org.ab.sentinel.db.JooqFactory;
import org.ab.sentinel.jooq.tables.records.UsersRecord;
import org.ab.sentinel.model.LoginRequest;
import org.ab.sentinel.model.LoginResult;
import org.ab.sentinel.model.RegisterRequest;
import org.ab.sentinel.model.RegisterResult;
import org.ab.sentinel.util.Passwords;
import org.ab.sentinel.util.Pbkdf2Passwords;
import org.jooq.DSLContext;
import org.nanonative.nano.core.model.Service;
import org.nanonative.nano.helper.event.model.Event;

import javax.sql.DataSource;


public final class PostgreSqlService extends Service {
    private DSLContext dsl;
    private final Passwords passwords = new Pbkdf2Passwords(200_000, 16, 32);

    @Override public String name() { return "PostgreSqlService"; }

    @Override
    public void start() {
        context.info(() -> "[" + name() + "] started ");
        DataSource ds = DataSourceFactory.create();
        context.info(() -> "[" + name() + "] started ");
        this.dsl = JooqFactory.create(ds);
        context.info(() -> "[" + name() + "] started ");
    }

    @Override
    public void stop() {

    }

    @Override
    public Object onFailure(final Event event) {
        context.error(() -> "Event failure: " + event);
        return null;
    }

    @Override
    public void onEvent(final Event event) {
        event.ifPresentAck(AppEvents.USER_REGISTER, RegisterRequest.class, this::registerUser);
        event.ifPresentAck(AppEvents.USER_LOGIN, LoginRequest.class, this::loginUser);
    }

    private LoginResult loginUser(final LoginRequest payload) {
        try {
            final String email = payload.email();
            final String rawPw = payload.password();

            final org.jooq.Record rec = dsl.select(USERS.ID, USERS.PASSWORD_HASH)
                .from(USERS)
                .where(USERS.EMAIL.eq(email))
                .fetchOne();

            if (rec == null) {
                return new LoginResult(false, null, "Invalid email or password");
            }

            final String storedHash = rec.get(USERS.PASSWORD_HASH);
            final boolean ok = passwords.verify(rawPw, storedHash);
            if (!ok) {
                return new LoginResult(false, null, "Invalid email or password");
            }

            final String userId = String.valueOf(rec.get(USERS.ID));
            return new LoginResult(true, userId, null);

        } catch (Exception e) {
            return new LoginResult(false, null, "DB error: " + e.getMessage());
        }
    }

    private RegisterResult registerUser(RegisterRequest payload) {
        try {
            final String email = payload.email();
            final String name  = payload.name();
            final String rawPw = payload.password();

            final boolean exists = dsl.fetchExists(
                dsl.selectOne().from(USERS).where(USERS.EMAIL.eq(email))
            );
            if (exists) {
                return new RegisterResult(false, null, "Email already registered");
            }

            // hash & insert
            final String hash = passwords.hash(rawPw);
            final UsersRecord inserted = dsl.insertInto(USERS,
                    USERS.EMAIL, USERS.NAME, USERS.PASSWORD_HASH)
                .values(email, name, hash)
                .returning(USERS.ID)
                .fetchOne();

            final String userId = inserted.get(USERS.ID).toString();

            return new RegisterResult(true, userId, null);

        } catch (Exception e) {
            return new RegisterResult(false, null, "DB error: " + e);
        }
    }

    @Override
    public void configure(final TypeMapI<?> typeMapI, final TypeMapI<?> typeMapI1) {

    }
}
