package org.ab.sentinel.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.util.Optional;


public final class DataSourceFactory {

    public static DataSource create(DataSourceConfig cfg) {
        return Optional.ofNullable(cfg.options())
            .map(opt -> create(String.format("jdbc:postgresql://%s:%d/%s?%s", cfg.host(), cfg.port(), cfg.name(), cfg.options()), cfg.user(), cfg.pass()))
            .orElseGet(() -> create(String.format("jdbc:postgresql://%s:%d/%s", cfg.host(), cfg.port(), cfg.name()), cfg.user(), cfg.pass()));
    }

    private static DataSource create(String dbUrl, String dbUser, String dbPass) {
        var cfg = new HikariConfig();
        cfg.setJdbcUrl(dbUrl);
        cfg.setUsername(dbUser);
        cfg.setPassword(dbPass);

        cfg.setMaximumPoolSize(5);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(20000);
        cfg.setValidationTimeout(5000);
        cfg.setIdleTimeout(300000);     // 5min
        cfg.setMaxLifetime(1740000);    // 29min
        cfg.setKeepaliveTime(300000);   // 5min

        cfg.setConnectionTestQuery("SELECT 1");
        cfg.addDataSourceProperty("reWriteBatchedInserts", "true");

        return new HikariDataSource(cfg);
    }
}

