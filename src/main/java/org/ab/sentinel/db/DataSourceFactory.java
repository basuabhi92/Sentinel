package org.ab.sentinel.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;


public final class DataSourceFactory {
    public static DataSource create() {
        var cfg = new HikariConfig();

        var defaultUrl = String.format(
            "jdbc:postgresql://%s:%d/%s?sslmode=%s",
            env("DB_HOST", "127.0.0.1"),
            Integer.parseInt(env("DB_PORT", "5432")),
            env("DB_NAME", "nanodb"),
            env("DB_SSLMODE", env("APP_ENV", "local").equalsIgnoreCase("local") ? "disable" : "require")
        );
        cfg.setJdbcUrl(env("DB_URL", defaultUrl));

        cfg.setUsername(env("DB_USER", "nanouser"));
        cfg.setPassword(env("DB_PASS", "CHANGEME"));

        // Small pools play nicer with micro VMs
        cfg.setMaximumPoolSize(Integer.parseInt(env("DB_POOL_MAX", "5")));
        cfg.setMinimumIdle(Integer.parseInt(env("DB_POOL_MIN", "1")));
        cfg.setConnectionTimeout(Long.parseLong(env("DB_CONN_TIMEOUT_MS", "20000")));
        cfg.setValidationTimeout(5000);
        cfg.setIdleTimeout(Long.parseLong(env("DB_IDLE_TIMEOUT_MS", "600000")));   // 10m
        cfg.setMaxLifetime(Long.parseLong(env("DB_MAX_LIFETIME_MS", "1740000"))); // 29m
        cfg.setKeepaliveTime(Long.parseLong(env("DB_KEEPALIVE_MS", "300000")));   // 5m

        cfg.setConnectionTestQuery("SELECT 1");
        cfg.addDataSourceProperty("reWriteBatchedInserts", "true");

        return new HikariDataSource(cfg);
    }

    private static String env(String k, String dfault) {
        var v = System.getenv(k);
        return (v == null || v.isBlank()) ? dfault : v;
    }
}

