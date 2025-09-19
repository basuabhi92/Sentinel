package org.ab.sentinel.PostgresContainer;

import org.ab.sentinel.model.Postgres;
import org.flywaydb.core.api.output.MigrateResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.flywaydb.core.Flyway;

import java.util.concurrent.locks.ReentrantLock;

public final class SharedPostgresIT {

    private SharedPostgresIT() {}

    private static final DockerImageName IMAGE = DockerImageName.parse("postgres:16-alpine");
    private static final String TEST_DB = "nanodb";
    private static final String TEST_USER = "nanouser";
    private static final String TEST_PASS = "nanopass";

    private static volatile PostgreSQLContainer<?> pg;
    private static final ReentrantLock lock = new ReentrantLock();

    public static Postgres startDatabase() {
        if (null != pg && pg.isRunning()) {
            return new Postgres(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        }
        lock.lock();
        if (null == pg || !pg.isRunning()) {
            pg = new PostgreSQLContainer<>(IMAGE)
                .withDatabaseName(TEST_DB)
                .withUsername(TEST_USER)
                .withPassword(TEST_PASS)
                .withReuse(false);
            pg.start();
        }
        lock.unlock();
        Postgres dbProp = new Postgres(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        runFlyway(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        return dbProp;
    }

    private static void runFlyway(String jdbcUrl, String user, String pass) {

        MigrateResult result = Flyway.configure()
            .dataSource(jdbcUrl, user, pass)
            .locations("classpath:db/migration")
            .schemas("public")
            .baselineOnMigrate(true)
            .cleanDisabled(true)
            .outOfOrder(false)
            .validateOnMigrate(true)
            .sqlMigrationPrefix("V_")
            .sqlMigrationSeparator("__")
            .sqlMigrationSuffixes(".sql")
            .validateMigrationNaming(true)
            .load()
            .migrate();

        System.out.println("Flyway applied " + result.migrationsExecuted + " migrations");
    }
}
