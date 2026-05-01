package com.vaultkey.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.vaultkey.R;
import com.vaultkey.databinding.ActivityLockBinding;
import com.vaultkey.utils.AutoLockManager;
import com.vaultkey.utils.PreferencesManager;
import com.vaultkey.utils.ThemeManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LockActivity extends AppCompatActivity {

    private static final String TAG = "LockActivity";
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MS = 30_000;

    private ActivityLockBinding binding;
    private PreferencesManager prefs;
    private BiometricPrompt biometricPrompt;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private int failedAttempts = 0;
    private long lockoutUntil = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        binding = ActivityLockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = PreferencesManager.getInstance(this);
        android.content.SharedPreferences sp = getSharedPreferences("vault_prefs", android.content.Context.MODE_PRIVATE);
        failedAttempts = sp.getInt("failed_attempts", 0);
        lockoutUntil = sp.getLong("lockout_until", 0);
        boolean hasPassword = prefs.hasMasterPassword();

        if (!hasPassword) {
            binding.tvSubtitle.setText(R.string.lock_subtitle_create);
            binding.btnUnlock.setText(R.string.lock_button_create_vault);
            setBiometricUiVisible(false);
        } else {
            binding.tvSubtitle.setText(R.string.lock_subtitle_enter);
            setupBiometric();
        }

        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0 || before > 0) {
                    binding.cardError.setVisibility(View.GONE);
                    binding.tilPassword.setError(null);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnUnlock.setOnClickListener(v -> handleUnlock());
    }

    private void setupBiometric() {
        if (!prefs.useBiometric()) { setBiometricUiVisible(false); return; }

        BiometricManager bm = BiometricManager.from(this);
        if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
            setBiometricUiVisible(false);
            return;
        }

        biometricPrompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    failedAttempts = 0;
                    getSharedPreferences("vault_prefs", android.content.Context.MODE_PRIVATE).edit().putInt("failed_attempts", 0).putLong("lockout_until", 0).apply();
                    startMain();
                }
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    Log.d(TAG, "Biometric error " + errorCode + ": " + errString);
                    if (errorCode == BiometricPrompt.ERROR_LOCKOUT || errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                        showError(getString(R.string.lock_error_too_many_attempts));
                    }
                    setBiometricUiVisible(true);
                }
                @Override
                public void onAuthenticationFailed() {
                    showError(getString(R.string.lock_error_fingerprint_failed));
                }
            });

        setBiometricUiVisible(true);
        binding.btnBiometric.setOnClickListener(v -> showBiometricPrompt());
        binding.btnBiometric.postDelayed(this::showBiometricPrompt, 300);
    }

    private void showBiometricPrompt() {
        if (biometricPrompt == null) return;
        if (BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) return;

        biometricPrompt.authenticate(new BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_name))
            .setSubtitle(getString(R.string.lock_biometric_prompt_subtitle))
            .setNegativeButtonText(getString(R.string.lock_biometric_negative))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build());
    }

    private void handleUnlock() {
        if (System.currentTimeMillis() < lockoutUntil) {
            long secsLeft = (lockoutUntil - System.currentTimeMillis()) / 1000 + 1;
            showError(getString(R.string.lock_error_too_many_attempts) + " (" + secsLeft + "s)");
            return;
        }

        String pw = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";
        if (pw.isEmpty()) { binding.tilPassword.setError(getString(R.string.lock_error_enter_password)); return; }
        binding.btnUnlock.setEnabled(false);
        boolean hasPassword = prefs.hasMasterPassword();

        io.execute(() -> {
            try {
                if (!hasPassword) {
                    if (pw.length() < 6) {
                        runOnUiThread(() -> { showError(getString(R.string.lock_error_short_password)); binding.btnUnlock.setEnabled(true); });
                        return;
                    }
                    prefs.createMasterPassword(pw);
                    runOnUiThread(this::startMain);
                } else {
                    boolean ok = prefs.verifyMasterPassword(pw);
                    runOnUiThread(() -> {
                        if (ok) {
                            failedAttempts = 0;
                            getSharedPreferences("vault_prefs", android.content.Context.MODE_PRIVATE).edit().putInt("failed_attempts", 0).putLong("lockout_until", 0).apply();
                            prefs.upgradeLegacyIfNeeded(pw);
                            startMain();
                        } else {
                            failedAttempts++;
                            if (failedAttempts >= MAX_ATTEMPTS) {
                                lockoutUntil = System.currentTimeMillis() + LOCKOUT_MS;
                                failedAttempts = 0;
                            }
                            getSharedPreferences("vault_prefs", android.content.Context.MODE_PRIVATE).edit()
                                .putInt("failed_attempts", failedAttempts)
                                .putLong("lockout_until", lockoutUntil)
                                .apply();
                            binding.etPassword.setText("");
                            showError(getString(R.string.lock_error_wrong_password));
                            binding.btnUnlock.setEnabled(true);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Auth error", e);
                runOnUiThread(() -> { showError(getString(R.string.lock_error_prefix, e.getMessage())); binding.btnUnlock.setEnabled(true); });
            }
        });
    }

    private void showError(String msg) {
        binding.tvError.setText(msg);
        binding.cardError.setVisibility(View.VISIBLE);
    }

    private void setBiometricUiVisible(boolean visible) {
        int state = visible ? View.VISIBLE : View.GONE;
        binding.btnBiometric.setVisibility(state);
        binding.rowBiometricDivider.setVisibility(state);
    }

    private void startMain() {
        AutoLockManager.getInstance().cancelTimer();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        AutoLockManager.getInstance().resetTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
}
