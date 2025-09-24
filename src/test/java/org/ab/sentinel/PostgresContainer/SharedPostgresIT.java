package org.ab.sentinel.PostgresContainer;

import org.ab.sentinel.model.Postgres;
import org.flywaydb.core.api.output.MigrateResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.flywaydb.core.Flyway;

import java.util.concurrent.locks.ReentrantLock;

import static org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT;

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
            return new Postgres(pg.getHost(), pg.getMappedPort(POSTGRESQL_PORT), pg.getDatabaseName(), pg.getUsername(), pg.getPassword());
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
        runFlyway(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
        return new Postgres(pg.getHost(), pg.getMappedPort(POSTGRESQL_PORT), pg.getDatabaseName(), pg.getUsername(), pg.getPassword());
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
    }
}
