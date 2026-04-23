package com.vaultkey.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import com.vaultkey.activities.LockActivity;

public class AutoLockManager {

    private static volatile AutoLockManager instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable lockRunnable;
    private Context appContext;
    private long lastInteractionTime = System.currentTimeMillis();

    private AutoLockManager() {}

    public static AutoLockManager getInstance() {
        if (instance == null) {
            synchronized (AutoLockManager.class) {
                if (instance == null) instance = new AutoLockManager();
            }
        }
        return instance;
    }

    public void init(Context ctx) { this.appContext = ctx.getApplicationContext(); }

    public void onAppForeground() {
        if (appContext == null) return;
        int seconds = PreferencesManager.getInstance(appContext).getAutoLockTimeoutSeconds();
        if (seconds > 0 && System.currentTimeMillis() - lastInteractionTime > seconds * 1000L) {
            lock();
        } else {
            resetTimer();
        }
    }

    public void resetTimer() {
        lastInteractionTime = System.currentTimeMillis();
        if (appContext == null) return;
        cancelTimer();
        int seconds = PreferencesManager.getInstance(appContext).getAutoLockTimeoutSeconds();
        if (seconds <= 0) return;
        lockRunnable = this::lock;
        handler.postDelayed(lockRunnable, seconds * 1000L);
    }

    public void cancelTimer() {
        if (lockRunnable != null) { handler.removeCallbacks(lockRunnable); lockRunnable = null; }
    }

    private void lock() {
        cancelTimer();
        lastInteractionTime = System.currentTimeMillis();
        if (appContext == null) return;
        appContext.startActivity(new Intent(appContext, LockActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
}
