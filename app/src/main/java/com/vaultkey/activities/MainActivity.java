package com.vaultkey.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import com.vaultkey.R;
import com.vaultkey.databinding.ActivityMainBinding;
import com.vaultkey.fragments.GeneratorFragment;
import com.vaultkey.fragments.PasswordsFragment;
import com.vaultkey.fragments.SettingsFragment;

public class MainActivity extends BaseActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            int tab = normalizeTab(getIntent().getIntExtra("nav_tab", R.id.nav_passwords));
            binding.bottomNav.setSelectedItemId(tab);
            loadFragment(tabFragment(tab));
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_passwords) { loadFragment(new PasswordsFragment()); return true; }
            if (id == R.id.nav_generator) { loadFragment(new GeneratorFragment()); return true; }
            if (id == R.id.nav_settings)  { loadFragment(new SettingsFragment());  return true; }
            return false;
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int tab = intent.getIntExtra("nav_tab", -1);
        if (tab != -1) {
            tab = normalizeTab(tab);
            binding.bottomNav.setSelectedItemId(tab);
            loadFragment(tabFragment(tab));
        }
    }

    private Fragment tabFragment(int tabId) {
        if (tabId == R.id.nav_generator) return new GeneratorFragment();
        if (tabId == R.id.nav_settings)  return new SettingsFragment();
        return new PasswordsFragment();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragmentContainer, fragment)
            .commit();
    }

    public void lockAndGoToLock() {
        startActivity(new Intent(this, LockActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    private int normalizeTab(int tabId) {
        if (tabId == R.id.nav_passwords || tabId == R.id.nav_generator || tabId == R.id.nav_settings) return tabId;
        return R.id.nav_passwords;
    }
}
