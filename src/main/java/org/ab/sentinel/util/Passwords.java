package org.ab.sentinel.util;

public interface Passwords {
    String hash(String raw);
    boolean verify(String raw, String stored);
}
