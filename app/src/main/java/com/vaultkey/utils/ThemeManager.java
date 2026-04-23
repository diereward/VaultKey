package com.vaultkey.utils;

import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    public static void apply(Context ctx) {
        switch (PreferencesManager.getInstance(ctx).getTheme()) {
            case "LIGHT":  AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case "SYSTEM": AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
            default:       AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
        }
    }
}
