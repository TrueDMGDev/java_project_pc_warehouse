package com.pcwarehouse.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {

    private static final String FORMAT_PREFIX = "pbkdf2_sha256";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS);
        return FORMAT_PREFIX + "$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verify(String password, String storedPassword) {
        if (password == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (!isHash(storedPassword)) {
            return Objects.equals(password, storedPassword);
        }

        String[] parts = storedPassword.split("\\$");
        if (parts.length != 4) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] actualHash = pbkdf2(password, salt, iterations);
            return constantTimeEquals(expectedHash, actualHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static boolean needsUpgrade(String storedPassword) {
        return !isHash(storedPassword);
    }

    public static boolean isHash(String storedPassword) {
        return storedPassword != null && storedPassword.startsWith(FORMAT_PREFIX + "$");
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("Password hashing is not available on this JVM.", exception);
        }
    }

    private static boolean constantTimeEquals(byte[] expected, byte[] actual) {
        if (expected.length != actual.length) {
            return false;
        }

        int difference = 0;
        for (int index = 0; index < expected.length; index++) {
            difference |= expected[index] ^ actual[index];
        }
        return difference == 0;
    }
}
