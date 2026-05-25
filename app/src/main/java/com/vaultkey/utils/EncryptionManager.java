package com.vaultkey.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionManager {

    public static class DecryptionException extends Exception {
        public DecryptionException(String message) {
            super(message);
        }

        public DecryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static String hashPassword(String password) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verifyPassword(String input, String stored) {
        return hashPassword(input).equals(stored);
    }

    public static String encrypt(String plaintext, String masterPassword) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            SecretKey key = deriveKey(masterPassword, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new javax.crypto.spec.GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[salt.length + iv.length + encrypted.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(iv, 0, combined, salt.length, iv.length);
            System.arraycopy(encrypted, 0, combined, salt.length + iv.length, encrypted.length);
            return "v4:" + android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(String ciphertext, String masterPassword) throws DecryptionException {
        try {
            boolean isV4 = ciphertext.startsWith("v4:");
            boolean isV3 = ciphertext.startsWith("v3:");
            boolean isV2 = ciphertext.startsWith("v2:");
            String actualCipher = (isV4 || isV3 || isV2) ? ciphertext.substring(3) : ciphertext;
            byte[] combined = android.util.Base64.decode(actualCipher, android.util.Base64.NO_WRAP);
            if (combined.length < 28) throw new DecryptionException("Ciphertext is too short");
            byte[] salt = new byte[16], iv = new byte[12];
            System.arraycopy(combined, 0, salt, 0, 16);
            System.arraycopy(combined, 16, iv, 0, 12);
            byte[] encrypted = new byte[combined.length - 28];
            System.arraycopy(combined, 28, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            if (isV4 || isV2 || (!isV3 && !ciphertext.contains(":"))) {
                SecretKey slowKey = deriveKey(masterPassword, salt);
                cipher.init(Cipher.DECRYPT_MODE, slowKey, new javax.crypto.spec.GCMParameterSpec(128, iv));
                return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
            } else if (isV3) {
                SecretKey key = deriveKeyFast(masterPassword, salt);
                cipher.init(Cipher.DECRYPT_MODE, key, new javax.crypto.spec.GCMParameterSpec(128, iv));
                return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
            }
            throw new DecryptionException("Unsupported ciphertext format");
        } catch (Exception e) {
            if (e instanceof DecryptionException) throw (DecryptionException) e;
            throw new DecryptionException("Decryption failed", e);
        }
    }

    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(new PBEKeySpec(password.toCharArray(), salt, 120000, 256)).getEncoded(), "AES");
    }

    private static SecretKey deriveKeyFast(String password, byte[] salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(password.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest.digest(salt), "AES");
    }
}
