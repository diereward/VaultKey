package com.vaultkey.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.vaultkey.BuildConfig;
import com.vaultkey.R;
import com.vaultkey.activities.MainActivity;
import com.vaultkey.databinding.FragmentSettingsBinding;
import com.vaultkey.databinding.ViewSettingsActionRowBinding;
import com.vaultkey.utils.PreferencesManager;
import com.vaultkey.utils.ThemeManager;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {

    private static final int[] STEPS_SEC = {10, 20, 30, 60, 90, 120, 180};

    private FragmentSettingsBinding binding;
    private PreferencesManager prefs;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = PreferencesManager.getInstance(requireContext());

        binding.toolbar.getMenu().clear();
        binding.toolbar.inflateMenu(R.menu.top_bar_actions);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_lock && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).lockAndGoToLock();
                return true;
            }
            return false;
        });

        row(binding.rowChangeMaster, R.string.settings_change_master, R.string.settings_change_master_subtitle, R.drawable.ic_lock, R.color.accent_purple, R.color.accent_purple_container);
        row(binding.rowAbout, R.string.settings_about_button, R.string.settings_about_subtitle, R.drawable.ic_settings, R.color.accent_blue, R.color.accent_blue_container);

        setupTheme();
        setupSecurity();

        binding.rowAbout.getRoot().setOnClickListener(v ->
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_about_title)
                .setMessage(getString(R.string.settings_about_message, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
                .setPositiveButton(R.string.ok, null).show());
    }

    private void row(ViewSettingsActionRowBinding r, int title, int subtitle, int icon, int tint, int bg) {
        r.actionTitle.setText(title);
        r.actionSubtitle.setText(subtitle);
        r.actionIcon.setImageResource(icon);
        r.actionIcon.setColorFilter(requireContext().getColor(tint));
        r.actionIconCard.setCardBackgroundColor(requireContext().getColor(bg));
    }

    private void setupTheme() {
        String theme = prefs.getTheme();
        binding.rgTheme.check("LIGHT".equals(theme) ? R.id.rbLight : "SYSTEM".equals(theme) ? R.id.rbSystem : R.id.rbDark);
        binding.rgTheme.setOnCheckedChangeListener((group, id) -> {
            String next = id == R.id.rbLight ? "LIGHT" : id == R.id.rbSystem ? "SYSTEM" : "DARK";
            if (next.equals(prefs.getTheme())) return;
            prefs.setTheme(next);
            ThemeManager.apply(requireContext());
            if (getActivity() != null) getActivity().recreate();
        });
    }

    private void setupSecurity() {
        binding.switchBiometric.setChecked(prefs.useBiometric());
        binding.switchBiometric.setOnCheckedChangeListener((v, checked) -> prefs.setUseBiometric(checked));

        setupAutoLock();
        setupVoiceSettings();

        binding.rowChangeMaster.getRoot().setOnClickListener(v -> showChangeMasterDialog());
    }

    private void setupAutoLock() {
        int progress = findAutoLockIndex(prefs.getAutoLockTimeoutSeconds());
        binding.seekAutoLock.setMax(STEPS_SEC.length - 1);
        binding.seekAutoLock.setProgress(progress);
        binding.chipAutoLock.setText(formatAutoLockLabel(STEPS_SEC[progress]));
        binding.seekAutoLock.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean u) {
                prefs.setAutoLockTimeoutSeconds(STEPS_SEC[p]);
                binding.chipAutoLock.setText(formatAutoLockLabel(STEPS_SEC[p]));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void setupVoiceSettings() {
        float currentRate = prefs.getVoiceRate();
        binding.seekVoiceRate.setMax(10); // 0.1 to 2.0
        binding.seekVoiceRate.setProgress(rateToProgress(currentRate));
        binding.chipVoiceRate.setText(getString(R.string.settings_voice_rate_format, String.format(Locale.US, "%.1f", currentRate)));

        binding.seekVoiceRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float rate = progressToRate(progress);
                prefs.setVoiceRate(rate);
                binding.chipVoiceRate.setText(getString(R.string.settings_voice_rate_format, String.format(Locale.US, "%.1f", rate)));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private int rateToProgress(float rate) {
        // Map 0.2 -> 0, 1.0 -> 4, 2.0 -> 9 etc.
        // Let's use simpler: (rate - 0.2) / 0.2
        return Math.round((rate - 0.2f) / 0.2f);
    }

    private float progressToRate(int progress) {
        // progress 0 = 0.2x, 4 = 1.0x, 9 = 2.0x
        return 0.2f + (progress * 0.2f);
    }

    private void showChangeMasterDialog() {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_master, null, false);
        TextInputEditText etOld = content.findViewById(R.id.etOldPassword);
        TextInputEditText etNew = content.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = content.findViewById(R.id.etConfirmPassword);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_change_master_title)
            .setView(content)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String old = text(etOld).trim(), pw = text(etNew), confirm = text(etConfirm);
            if (old.isEmpty()) { toast(getString(R.string.settings_invalid_old_password)); return; }
            if (pw.length() < 6) { toast(getString(R.string.settings_invalid_new_password)); return; }
            if (!pw.equals(confirm)) { toast(getString(R.string.settings_password_mismatch)); return; }

            android.content.Context ctx = requireContext().getApplicationContext();
            io.execute(() -> {
                try {
                    if (!prefs.verifyMasterPassword(old)) throw new SecurityException("invalid");
                    prefs.upgradeLegacyIfNeeded(old);
                    prefs.changeMasterPassword(pw);
                    postUi(() -> { Toast.makeText(ctx, R.string.settings_change_master_success, Toast.LENGTH_SHORT).show(); dialog.dismiss(); });
                } catch (SecurityException e) { toast(ctx.getString(R.string.settings_invalid_old_password)); }
                catch (Exception e) { toast(ctx.getString(R.string.settings_change_master_error)); }
            });
        }));
        dialog.show();
    }

    private int findAutoLockIndex(int seconds) {
        for (int i = 0; i < STEPS_SEC.length; i++) if (STEPS_SEC[i] == seconds) return i;
        return 2;
    }

    private String formatAutoLockLabel(int seconds) {
        if (seconds < 60) return getString(R.string.settings_autolock_format_seconds, seconds);
        double mins = seconds / 60.0;
        String m = mins == Math.rint(mins) ? String.valueOf((int) mins) : String.format(Locale.getDefault(), "%.1f", mins);
        return getString(R.string.settings_autolock_format_minutes, m);
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }

    private void toast(String msg) {
        Activity a = getActivity();
        if (a != null) a.runOnUiThread(() -> Toast.makeText(a, msg, Toast.LENGTH_SHORT).show());
    }

    private void postUi(Runnable action) {
        Activity a = getActivity();
        if (a != null) a.runOnUiThread(action);
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }

    @Override
    public void onDestroy() { super.onDestroy(); io.shutdown(); }
}
