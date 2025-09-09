package org.ab.sentinel.model;

public record RegisterResult(boolean ok, String userId, String reason) {
    public boolean ok() { return ok; }
    public String userId() { return userId; }
    public String reason() { return reason; }
}
