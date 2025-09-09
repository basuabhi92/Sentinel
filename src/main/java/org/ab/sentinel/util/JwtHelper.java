package org.ab.sentinel.util;

import berlin.yuna.typemap.logic.JsonEncoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class JwtHelper {
    private final byte[] secret; // e.g. from env: System.getenv("JWT_SECRET")

    public JwtHelper(String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    // Create a JWT with HS256. `claims` should at least include "sub"
    public String createToken(Map<String, Object> claims, long ttlSeconds) {
        long now = Instant.now().getEpochSecond();
        long exp = now + ttlSeconds;

        String headerJson  = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = JsonEncoder.toJson(merge(claims, Map.of("iat", now, "exp", exp)));

        String header  = b64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = b64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;

        String signature = b64Url(hmacSha256(signingInput.getBytes(StandardCharsets.UTF_8)));
        return signingInput + "." + signature;
    }

    // Basic verification: signature + exp
    public boolean verify(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) return false;

        String signingInput = parts[0] + "." + parts[1];
        String expectedSig = b64Url(hmacSha256(signingInput.getBytes(StandardCharsets.UTF_8)));
        if (!constantTimeEq(parts[2], expectedSig)) return false;

        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        long exp = extractLong(payloadJson, "exp");
        return Instant.now().getEpochSecond() < exp;
    }

    // ---- helpers (tiny/no-reflection json ops) ----

    private byte[] hmacSha256(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private String b64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean constantTimeEq(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }

    private static Map<String, Object> merge(Map<String, Object> a, Map<String, Object> b) {
        var m = new HashMap<String, Object>(a);
        m.putAll(b); return m;
    }

    // tiny extractor for numeric "exp" field
    private static long extractLong(String json, String field) {
        String needle = "\"" + field + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return 0L;
        i += needle.length();
        int j = i;
        while (j < json.length() && "0123456789".indexOf(json.charAt(j)) >= 0) j++;
        return Long.parseLong(json.substring(i, j));
    }
}

