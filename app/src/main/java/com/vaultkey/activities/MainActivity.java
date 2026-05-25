package com.vaultkey.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import com.vaultkey.R;
import com.vaultkey.databinding.ActivityMainBinding;
import com.vaultkey.fragments.GeneratorFragment;
import com.vaultkey.fragments.PasswordsFragment;
import com.vaultkey.fragments.SettingsFragment;
import com.vaultkey.utils.EdgeToEdge;

public class MainActivity extends BaseActivity {

    private static final String STATE_SELECTED_TAB = "selected_tab";

    private ActivityMainBinding binding;
    private int selectedTab = R.id.nav_passwords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupEdgeToEdge();

        if (savedInstanceState == null) {
            selectTab(getIntent().getIntExtra("nav_tab", R.id.nav_passwords), true);
        } else {
            selectedTab = normalizeTab(savedInstanceState.getInt(STATE_SELECTED_TAB, inferCurrentTab()));
            updateNavSelection(selectedTab);
        }

        binding.navPasswords.setOnClickListener(v -> selectTab(R.id.nav_passwords, false));
        binding.navGenerator.setOnClickListener(v -> selectTab(R.id.nav_generator, false));
        binding.navSettings.setOnClickListener(v -> selectTab(R.id.nav_settings, false));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_SELECTED_TAB, selectedTab);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int tab = intent.getIntExtra("nav_tab", -1);
        if (tab != -1) {
            selectTab(normalizeTab(tab), false);
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

    private void selectTab(int tabId, boolean force) {
        tabId = normalizeTab(tabId);
        if (!force && selectedTab == tabId) return;
        selectedTab = tabId;
        updateNavSelection(tabId);
        loadFragment(tabFragment(tabId));
    }

    private void updateNavSelection(int tabId) {
        updateNavItem(binding.navPasswordsIcon, binding.navPasswordsText, tabId == R.id.nav_passwords);
        updateNavItem(binding.navGeneratorIcon, binding.navGeneratorText, tabId == R.id.nav_generator);
        updateNavItem(binding.navSettingsIcon, binding.navSettingsText, tabId == R.id.nav_settings);
    }

    private void updateNavItem(ImageView icon, TextView text, boolean selected) {
        int color = getColor(selected ? R.color.brand : R.color.on_surface_variant);
        icon.setColorFilter(color);
        text.setTextColor(color);
        text.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void setupEdgeToEdge() {
        EdgeToEdge.enable(this, binding.getRoot());

        int baseHorizontalMargin = dp(14);
        int baseBottomMargin = dp(14);
        int extraContentGap = dp(8);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(EdgeToEdge.insetTypes());

            ViewGroup.MarginLayoutParams navParams =
                (ViewGroup.MarginLayoutParams) binding.bottomNavContainer.getLayoutParams();
            navParams.leftMargin = baseHorizontalMargin + insets.left;
            navParams.rightMargin = baseHorizontalMargin + insets.right;
            navParams.bottomMargin = baseBottomMargin + insets.bottom;
            binding.bottomNavContainer.setLayoutParams(navParams);

            binding.bottomNavContainer.post(() -> {
                int navBottomSpace = binding.bottomNavContainer.getHeight()
                    + navParams.bottomMargin
                    + extraContentGap;
                binding.fragmentContainer.setPadding(
                    insets.left,
                    insets.top,
                    insets.right,
                    navBottomSpace
                );
            });

            return windowInsets;
        });
        ViewCompat.requestApplyInsets(binding.getRoot());
    }

    public void lockAndGoToLock() {
        startActivity(new Intent(this, LockActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    private int normalizeTab(int tabId) {
        if (tabId == R.id.nav_passwords || tabId == R.id.nav_generator || tabId == R.id.nav_settings) return tabId;
        return R.id.nav_passwords;
    }

    private int inferCurrentTab() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (fragment instanceof GeneratorFragment) return R.id.nav_generator;
        if (fragment instanceof SettingsFragment) return R.id.nav_settings;
        return R.id.nav_passwords;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
