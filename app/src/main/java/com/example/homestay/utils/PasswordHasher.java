package com.example.homestay.utils;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHasher {
    private static final int BCRYPT_ROUNDS = 12;
    private PasswordHasher() {}
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }
    public static boolean verify(String plainPassword, String hashedPassword) {
        try { return BCrypt.checkpw(plainPassword, hashedPassword); }
        catch (Exception ignored) { return false; }
    }
    public static boolean isValidHash(String hash) {
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }
}
