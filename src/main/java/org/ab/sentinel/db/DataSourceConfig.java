package org.ab.sentinel.db;

public record DataSourceConfig(String host, Integer port, String name, String user, String pass, String options) {}
