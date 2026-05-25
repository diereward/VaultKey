package com.vaultkey.activities;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.vaultkey.utils.AutoLockManager;
import com.vaultkey.utils.ThemeManager;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AutoLockManager.getInstance().onAppForeground();
    }

    @Override
    protected void onPause() {
        super.onPause();
        AutoLockManager.getInstance().cancelTimer();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        AutoLockManager.getInstance().resetTimer();
    }
}
