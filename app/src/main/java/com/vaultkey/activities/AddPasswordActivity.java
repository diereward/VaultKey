package com.vaultkey.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.vaultkey.R;
import com.vaultkey.data.database.VaultDatabase;
import com.vaultkey.data.model.PasswordEntry;
import com.vaultkey.databinding.ActivityAddPasswordBinding;
import com.vaultkey.utils.AvatarHelper;
import com.vaultkey.utils.EncryptionManager;
import com.vaultkey.utils.PasswordGenerator;
import com.vaultkey.utils.PreferencesManager;
import com.vaultkey.utils.StrengthHelper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddPasswordActivity extends BaseActivity {

    private ActivityAddPasswordBinding binding;
    private long editId = -1;
    private long createdAt = System.currentTimeMillis();
    private String avatarPath = "";
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> avatarPicker = registerForActivityResult(
        new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) { avatarPath = uri.toString(); refreshAvatarPreview(); }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(R.string.title_new_password);

        binding.acCategory.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_dropdown_item_1line, getResources().getStringArray(R.array.password_categories)));
        binding.acCategory.setText(getString(R.string.category_other), false);

        binding.cardAvatar.setOnClickListener(v -> avatarPicker.launch("image/*"));
        if (binding.btnClearAvatar != null) {
            binding.btnClearAvatar.setOnClickListener(v -> {
                avatarPath = "";
                refreshAvatarPreview();
            });
        }
        refreshAvatarPreview();

        TextWatcher avatarWatcher = simpleWatcher(this::refreshAvatarPreview);
        binding.etTitle.addTextChangedListener(avatarWatcher);
        binding.etUrl.addTextChangedListener(avatarWatcher);

        String generatedPassword = getIntent().getStringExtra("generated_password");
        if (generatedPassword != null && !generatedPassword.isEmpty()) {
            binding.etPassword.setText(generatedPassword);
            updateStrength(generatedPassword);
        }

        editId = getIntent().getLongExtra("id", -1);
        if (editId != -1) {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(R.string.title_edit_password);
            String dataKey = PreferencesManager.getInstance(this).getDataEncryptionKey();
            io.execute(() -> {
                PasswordEntry entry = VaultDatabase.getInstance(this).passwordDao().getById(editId);
                if (entry == null) return;
                String plainPass = EncryptionManager.decrypt(entry.password, dataKey);
                runOnUiThread(() -> {
                    createdAt = entry.createdAt;
                    binding.etTitle.setText(entry.title);
                    binding.etUsername.setText(entry.username);
                    binding.etPassword.setText(plainPass);
                    binding.etUrl.setText(entry.url);
                    binding.etNotes.setText(entry.notes);
                    binding.acCategory.setText(entry.category == null ? getString(R.string.category_other) : entry.category, false);
                    avatarPath = entry.avatarPath == null ? "" : entry.avatarPath;
                    refreshAvatarPreview();
                });
            });
        }

        binding.etPassword.addTextChangedListener(simpleWatcher(() ->
            updateStrength(binding.etPassword.getText() == null ? "" : binding.etPassword.getText().toString())));

        binding.btnSave.setOnClickListener(v -> save());
    }

    private TextWatcher simpleWatcher(Runnable callback) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { callback.run(); }
            @Override public void afterTextChanged(Editable s) {}
        };
    }

    private void refreshAvatarPreview() {
        ImageView imageView = binding.getRoot().findViewById(R.id.ivAvatarPreview);
        TextView initialView = binding.getRoot().findViewById(R.id.tvAvatarInitial);
        if (imageView == null || initialView == null) return;

        if (binding.btnClearAvatar != null) {
            binding.btnClearAvatar.setVisibility(!avatarPath.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        String title = binding.etTitle.getText() == null ? "" : binding.etTitle.getText().toString();
        String url = binding.etUrl.getText() == null ? "" : binding.etUrl.getText().toString();
        AvatarHelper.loadPasswordAvatar(this, imageView, initialView, avatarPath, url, title);
    }

    private void updateStrength(String password) {
        if (password.isEmpty()) { binding.cardStrength.setVisibility(android.view.View.GONE); return; }
        binding.cardStrength.setVisibility(android.view.View.VISIBLE);
        PasswordGenerator.PasswordAnalysis analysis = PasswordGenerator.analyze(this, password);
        StrengthHelper.update(binding.cardStrength, analysis.strength, analysis.score, analysis.crackTime);
    }

    private void save() {
        String title = text(binding.etTitle).trim();
        String plainPassword = text(binding.etPassword);
        String url = text(binding.etUrl).trim();

        if (title.isEmpty() || plainPassword.isEmpty()) {
            Toast.makeText(this, R.string.add_password_fill_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!url.isEmpty() && !android.util.Patterns.WEB_URL.matcher(url).matches()) {
            binding.etUrl.setError(getString(R.string.add_password_invalid_url));
            return;
        }

        String dataKey = PreferencesManager.getInstance(this).getDataEncryptionKey();
        PasswordGenerator.PasswordAnalysis analysis = PasswordGenerator.analyze(this, plainPassword);

        PasswordEntry entry = new PasswordEntry();
        if (editId != -1) entry.id = editId;
        entry.title = title;
        entry.username = text(binding.etUsername);
        entry.password = EncryptionManager.encrypt(plainPassword, dataKey);
        entry.url = url;
        entry.notes = text(binding.etNotes);
        String category = text(binding.acCategory).trim();
        entry.category = category.isEmpty() ? getString(R.string.category_other) : category;
        entry.strengthScore = analysis.score;
        entry.avatarPath = avatarPath;
        entry.createdAt = editId == -1 ? System.currentTimeMillis() : createdAt;
        entry.updatedAt = System.currentTimeMillis();

        io.execute(() -> {
            VaultDatabase db = VaultDatabase.getInstance(this);
            if (editId == -1) db.passwordDao().insert(entry);
            else db.passwordDao().update(entry);
            runOnUiThread(this::finish);
        });
    }

    private String text(android.widget.EditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
}
