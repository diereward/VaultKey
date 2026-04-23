package com.vaultkey.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.security.SecureRandom;

public class PreferencesManager {

    static final String PREFS_NAME = "vault_prefs";
    private static final String KEY_MASTER_HASH_LEGACY = "master_hash";
    private static final String KEY_MASTER_VERIFIER_SECURE = "master_verifier_secure";
    private static final String KEY_MASTER_VERIFIER_PLAIN = "master_verifier_plain";
    private static final String KEY_DATA_KEY_SECURE = "data_key_secure";
    private static final String KEY_DATA_KEY_PLAIN = "data_key_plain";
    private static final String KEY_THEME = "theme";
    private static final String KEY_BIOMETRIC = "biometric";
    private static final String KEY_AUTO_LOCK = "auto_lock_sec";

    private static volatile PreferencesManager instance;
    private final SharedPreferences prefs;

    private PreferencesManager(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static PreferencesManager getInstance(Context ctx) {
        if (instance == null) {
            synchronized (PreferencesManager.class) {
                if (instance == null) instance = new PreferencesManager(ctx);
            }
        }
        return instance;
    }

    public boolean hasMasterPassword() {
        return !getMasterVerifier().isEmpty() || prefs.contains(KEY_MASTER_HASH_LEGACY);
    }

    public void createMasterPassword(String password) {
        storeMasterVerifier(MasterPasswordAuth.createVerifier(password));
        if (getDataEncryptionKey().isEmpty()) storeDataEncryptionKey(generateDataEncryptionKey());
        prefs.edit().remove(KEY_MASTER_HASH_LEGACY).apply();
    }

    public boolean verifyMasterPassword(String password) {
        String verifier = getMasterVerifier();
        if (!verifier.isEmpty()) return MasterPasswordAuth.verify(password, verifier);
        String legacy = prefs.getString(KEY_MASTER_HASH_LEGACY, "");
        return !legacy.isEmpty() && EncryptionManager.verifyPassword(password, legacy);
    }

    public void changeMasterPassword(String newPassword) {
        storeMasterVerifier(MasterPasswordAuth.createVerifier(newPassword));
    }

    public String getDataEncryptionKey() {
        String key = readSecret(KEY_DATA_KEY_SECURE, KEY_DATA_KEY_PLAIN);
        return !key.isEmpty() ? key : prefs.getString(KEY_MASTER_HASH_LEGACY, "");
    }

    public void upgradeLegacyIfNeeded(String plainPassword) {
        String legacyHash = prefs.getString(KEY_MASTER_HASH_LEGACY, "");
        if (legacyHash == null || legacyHash.isEmpty()) return;
        if (!EncryptionManager.verifyPassword(plainPassword, legacyHash)) return;
        if (getMasterVerifier().isEmpty()) storeMasterVerifier(MasterPasswordAuth.createVerifier(plainPassword));
        if (readSecret(KEY_DATA_KEY_SECURE, KEY_DATA_KEY_PLAIN).isEmpty()) storeDataEncryptionKey(legacyHash);
        prefs.edit().remove(KEY_MASTER_HASH_LEGACY).apply();
    }

    public String  getTheme()                 { return prefs.getString(KEY_THEME, "DARK"); }
    public void    setTheme(String t)         { prefs.edit().putString(KEY_THEME, t).apply(); }
    public boolean useBiometric()             { return prefs.getBoolean(KEY_BIOMETRIC, false); }
    public void    setUseBiometric(boolean v) { prefs.edit().putBoolean(KEY_BIOMETRIC, v).apply(); }
    public int     getAutoLockTimeoutSeconds(){ return prefs.getInt(KEY_AUTO_LOCK, 30); }
    public void    setAutoLockTimeoutSeconds(int s) { prefs.edit().putInt(KEY_AUTO_LOCK, s).apply(); }

    private String getMasterVerifier() {
        return readSecret(KEY_MASTER_VERIFIER_SECURE, KEY_MASTER_VERIFIER_PLAIN);
    }

    private void storeMasterVerifier(String verifier) {
        storeSecret(verifier, KEY_MASTER_VERIFIER_SECURE, KEY_MASTER_VERIFIER_PLAIN);
    }

    private void storeDataEncryptionKey(String dataKey) {
        storeSecret(dataKey, KEY_DATA_KEY_SECURE, KEY_DATA_KEY_PLAIN);
    }

    private void storeSecret(String plainText, String secureKey, String plainKey) {
        try {
            prefs.edit().putString(secureKey, KeystoreSecretBox.encrypt(plainText)).remove(plainKey).apply();
        } catch (Exception e) {
            throw new SecurityException("AndroidKeyStore unavailable - cannot store secret safely", e);
        }
    }

    private String readSecret(String secureKey, String plainKey) {
        String encrypted = prefs.getString(secureKey, "");
        if (encrypted != null && !encrypted.isEmpty()) {
            try { return KeystoreSecretBox.decrypt(encrypted); } catch (Exception ignored) {}
        }
        String plain = prefs.getString(plainKey, "");
        return plain == null ? "" : plain;
    }

    private String generateDataEncryptionKey() {
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        return Base64.encodeToString(raw, Base64.NO_WRAP);
    }
}
