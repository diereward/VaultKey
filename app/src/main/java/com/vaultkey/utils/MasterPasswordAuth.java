package com.vaultkey.utils;

import android.util.Base64;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class MasterPasswordAuth {

    private static final int SALT_SIZE = 16;
    private static final int ITERATIONS = 120000;

    private MasterPasswordAuth() {}

    public static String createVerifier(String password) {
        try {
            byte[] salt = new byte[SALT_SIZE];
            new SecureRandom().nextBytes(salt);
            byte[] hash = pbkdf2(password, salt, ITERATIONS);
            return "v2$" + ITERATIONS + "$" + Base64.encodeToString(salt, Base64.NO_WRAP)
                + "$" + Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static boolean verify(String password, String verifier) {
        if (verifier == null || verifier.isEmpty()) return false;
        if (!verifier.startsWith("v2$")) return EncryptionManager.verifyPassword(password, verifier);
        try {
            String[] parts = verifier.split("\\$");
            if (parts.length != 4) return false;
            byte[] salt = Base64.decode(parts[2], Base64.NO_WRAP);
            byte[] expected = Base64.decode(parts[3], Base64.NO_WRAP);
            return MessageDigest.isEqual(expected, pbkdf2(password, salt, Integer.parseInt(parts[1])));
        } catch (Exception e) { return false; }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) throws Exception {
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(new PBEKeySpec(password.toCharArray(), salt, iterations, 256)).getEncoded();
    }
}
