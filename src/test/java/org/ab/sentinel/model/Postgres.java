package org.ab.sentinel.model;

public record Postgres(String dbHost, Integer dbPort, String dbName, String dbUser, String dbPass) {}
