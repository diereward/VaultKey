package com.vaultkey;

import android.app.Application;
import com.vaultkey.utils.AutoLockManager;
import com.vaultkey.utils.ThemeManager;

public class VaultKeyApp extends Application {

    private static VaultKeyApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        ThemeManager.apply(this);
        AutoLockManager.getInstance().init(this);
    }

    public static VaultKeyApp getInstance() { return instance; }
}
