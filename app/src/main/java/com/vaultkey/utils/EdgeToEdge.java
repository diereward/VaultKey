package com.vaultkey.utils;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.vaultkey.R;

public final class EdgeToEdge {

    private EdgeToEdge() {}

    @SuppressWarnings("deprecation")
    public static void enable(Activity activity, View root) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(params);
        }

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, root);
        boolean light = activity.getResources().getBoolean(R.bool.is_light_mode);
        controller.setAppearanceLightStatusBars(light);
        controller.setAppearanceLightNavigationBars(light);
    }

    public static void applySystemBarsPadding(
        View view,
        boolean left,
        boolean top,
        boolean right,
        boolean bottom
    ) {
        int initialLeft = view.getPaddingLeft();
        int initialTop = view.getPaddingTop();
        int initialRight = view.getPaddingRight();
        int initialBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(insetTypes());
            v.setPadding(
                initialLeft + (left ? insets.left : 0),
                initialTop + (top ? insets.top : 0),
                initialRight + (right ? insets.right : 0),
                initialBottom + (bottom ? insets.bottom : 0)
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    public static int insetTypes() {
        return WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
    }
}
