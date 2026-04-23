package com.vaultkey.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.vaultkey.R;
import com.vaultkey.activities.AddPasswordActivity;
import com.vaultkey.activities.MainActivity;
import com.vaultkey.databinding.FragmentGeneratorBinding;
import com.vaultkey.databinding.ViewToggleRowBinding;
import com.vaultkey.utils.AutoLockManager;
import com.vaultkey.utils.PasswordGenerator;
import com.vaultkey.utils.StrengthHelper;

public class GeneratorFragment extends Fragment {

    private FragmentGeneratorBinding binding;
    private MaterialSwitch swLower, swUpper, swDigits, swSymbols;
    private String generatedPassword = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGeneratorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.toolbar.getMenu().clear();
        binding.toolbar.inflateMenu(R.menu.top_bar_actions);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_lock && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).lockAndGoToLock();
                return true;
            }
            return false;
        });

        swLower = setupSwitch(binding.toggleLower, R.string.generator_toggle_lower);
        swUpper = setupSwitch(binding.toggleUpper, R.string.generator_toggle_upper);
        swDigits = setupSwitch(binding.toggleDigits, R.string.generator_toggle_digits);
        swSymbols = setupSwitch(binding.toggleSymbols, R.string.generator_toggle_symbols);
        swLower.setChecked(true);
        swUpper.setChecked(true);
        swDigits.setChecked(true);
        swSymbols.setChecked(true);

        binding.seekLength.setMin(6);
        binding.seekLength.setMax(32);
        binding.seekLength.setProgress(16);
        binding.chipLength.setText(getString(R.string.generator_length_chars, 16));

        binding.seekLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                binding.chipLength.setText(getString(R.string.generator_length_chars, progress));
                if (fromUser) { syncPresetChips(progress); generate(); }
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });

        setPreset(binding.chip8, 8);
        setPreset(binding.chip12, 12);
        setPreset(binding.chip16, 16);
        setPreset(binding.chip24, 24);
        setPreset(binding.chip32, 32);
        syncPresetChips(16);

        binding.btnRefresh.setOnClickListener(v -> generate());
        binding.btnCopy.setOnClickListener(v -> copyGenerated(binding.btnCopy));
        binding.btnSaveAsPassword.setOnClickListener(v -> {
            if (generatedPassword.isEmpty()) return;
            Intent intent = new Intent(requireContext(), AddPasswordActivity.class);
            intent.putExtra("generated_password", generatedPassword);
            startActivity(intent);
        });

        generate();
    }

    private MaterialSwitch setupSwitch(ViewToggleRowBinding row, int labelRes) {
        row.tvToggleLabel.setText(labelRes);
        return row.switchToggle;
    }

    private void setPreset(Chip chip, int length) {
        chip.setOnClickListener(v -> {
            binding.seekLength.setProgress(length);
            binding.chipLength.setText(getString(R.string.generator_length_chars, length));
            syncPresetChips(length);
            generate();
        });
    }

    private void syncPresetChips(int progress) {
        binding.chip8.setChecked(progress == 8);
        binding.chip12.setChecked(progress == 12);
        binding.chip16.setChecked(progress == 16);
        binding.chip24.setChecked(progress == 24);
        binding.chip32.setChecked(progress == 32);
    }

    private void generate() {
        int len = binding.seekLength.getProgress();
        generatedPassword = PasswordGenerator.generate(len, swLower.isChecked(), swUpper.isChecked(),
            swDigits.isChecked(), swSymbols.isChecked());
        binding.tvGeneratedPassword.setText(generatedPassword);
        PasswordGenerator.PasswordAnalysis a = PasswordGenerator.analyze(requireContext(), generatedPassword);
        StrengthHelper.update(binding.strengthContainer, a.strength, a.score, a.crackTime);
    }

    private void copyGenerated(MaterialButton btn) {
        if (generatedPassword.isEmpty()) return;
        ((ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE))
            .setPrimaryClip(ClipData.newPlainText("password", generatedPassword));
        Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show();
        btn.setText(R.string.copied);
        btn.postDelayed(() -> { if (isAdded()) btn.setText(R.string.generator_copy); }, 1500);
    }

    @Override
    public void onResume() {
        super.onResume();
        AutoLockManager.getInstance().resetTimer();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
