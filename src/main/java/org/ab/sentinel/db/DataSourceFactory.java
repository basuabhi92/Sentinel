package org.ab.sentinel.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.util.Optional;


public final class DataSourceFactory {

    public static DataSource create(String dbHost, Integer dbPort, String dbName, String dbUser, String dbPass, String options) {
        return Optional.ofNullable(options)
            .map(opt -> create(String.format("jdbc:postgresql://%s:%d/%s?%s", dbHost, dbPort, dbName, opt), dbUser, dbPass))
            .orElseGet(() -> create(String.format("jdbc:postgresql://%s:%d/%s", dbHost, dbPort, dbName), dbUser, dbPass));
    }

    public static DataSource create(String dbUrl, String dbUser, String dbPass) {
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

