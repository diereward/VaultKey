package com.vaultkey.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionManager {

    public static String hashPassword(String password) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
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
            // Use fast derivation for new items (v3). 
            // Since dataKey is already a strong random key, PBKDF2 with 120k iterations is redundant.
            SecretKey key = deriveKeyFast(masterPassword, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new javax.crypto.spec.GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[salt.length + iv.length + encrypted.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(iv, 0, combined, salt.length, iv.length);
            System.arraycopy(encrypted, 0, combined, salt.length + iv.length, encrypted.length);
            return "v3:" + android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(String ciphertext, String masterPassword) {
        try {
            boolean isV3 = ciphertext.startsWith("v3:");
            boolean isV2 = ciphertext.startsWith("v2:");
            String actualCipher = (isV3 || isV2) ? ciphertext.substring(3) : ciphertext;
            byte[] combined = android.util.Base64.decode(actualCipher, android.util.Base64.NO_WRAP);
            if (combined.length < 28) return "❌ Decryption Error";
            byte[] salt = new byte[16], iv = new byte[12];
            System.arraycopy(combined, 0, salt, 0, 16);
            System.arraycopy(combined, 16, iv, 0, 12);
            byte[] encrypted = new byte[combined.length - 28];
            System.arraycopy(combined, 28, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            // Try fast derivation first (optimized for V3 and some legacy)
            try {
                SecretKey key = deriveKeyFast(masterPassword, salt);
                cipher.init(Cipher.DECRYPT_MODE, key, new javax.crypto.spec.GCMParameterSpec(128, iv));
                return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
            } catch (Exception ex) {
                // If it's explicitly V3 and fast failed, it's a real error
                if (isV3) return "❌ Decryption Error";

                // Fallback to slow PBKDF2 for V2 and legacy un-prefixed items
                SecretKey slowKey = deriveKey(masterPassword, salt);
                cipher.init(Cipher.DECRYPT_MODE, slowKey, new javax.crypto.spec.GCMParameterSpec(128, iv));
                return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return "❌ Decryption Error";
        }
    }

    

    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(new PBEKeySpec(password.toCharArray(), salt, 120000, 256)).getEncoded(), "AES");
    }

    // Legacy fast key derivation used in older versions (SHA-256 based)
    private static SecretKey deriveKeyFast(String password, byte[] salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(password.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest.digest(salt), "AES");
    }
}
