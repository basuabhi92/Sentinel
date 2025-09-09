package org.ab.sentinel.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class Pbkdf2Passwords implements Passwords {
    private final int iterations;
    private final int saltLen;   // bytes
    private final int keyLen;    // bytes (32 = 256-bit)
    private final SecureRandom rng = new SecureRandom();

    public Pbkdf2Passwords(int iterations, int saltLen, int keyLen) {
        this.iterations = iterations;
        this.saltLen = saltLen;
        this.keyLen = keyLen;
    }

    @Override
    public String hash(String raw) {
        byte[] salt = new byte[saltLen];
        rng.nextBytes(salt);
        byte[] dk = derive(raw.toCharArray(), salt, iterations, keyLen);
        return "pbkdf2_sha256$" + iterations + "$" + b64(salt) + "$" + b64(dk);
    }

    @Override
    public boolean verify(String raw, String stored) {
        // expected format: algo$iterations$salt$hash
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !parts[0].equals("pbkdf2_sha256")) return false;
        int iters = Integer.parseInt(parts[1]);
        byte[] salt = b64d(parts[2]);
        byte[] expected = b64d(parts[3]);
        byte[] dk = derive(raw.toCharArray(), salt, iters, expected.length);
        return constantTimeEq(dk, expected);
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations, int keyLen) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLen * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("PBKDF2 failure", e);
        }
    }

    private static String b64(byte[] b) { return Base64.getEncoder().withoutPadding().encodeToString(b); }
    private static byte[] b64d(String s) { return Base64.getDecoder().decode(s); }

    private static boolean constantTimeEq(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int r = 0;
        for (int i = 0; i < a.length; i++) r |= a[i] ^ b[i];
        return r == 0;
    }
}

