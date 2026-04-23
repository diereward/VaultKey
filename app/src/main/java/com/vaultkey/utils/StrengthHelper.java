package com.vaultkey.utils;

import android.content.Context;
import android.view.View;
import com.google.android.material.chip.Chip;
import com.vaultkey.R;

public final class StrengthHelper {

    private StrengthHelper() {}

    public static void update(View container, String strength, int score, String crackTime) {
        Context ctx = container.getContext();
        int color = color(ctx, strength);

        Chip chip = container.findViewById(R.id.chipStrengthLabel);
        if (chip != null) {
            String text = ctx.getString(label(strength));
            if (crackTime != null && !crackTime.isEmpty()) text += " • " + crackTime;
            chip.setText(text);
            chip.setTextColor(color);
            chip.setChipBackgroundColorResource(android.R.color.transparent);
        }

        int filled = score == 0 ? 0 : score + 1;
        int[] ids = {R.id.bar1, R.id.bar2, R.id.bar3, R.id.bar4, R.id.bar5};
        int empty = ctx.getColor(R.color.outline_variant);
        for (int i = 0; i < ids.length; i++) {
            View bar = container.findViewById(ids[i]);
            if (bar != null) bar.setBackgroundColor(i < filled ? color : empty);
        }
    }

    public static void update(View container, String strength, int score) {
        update(container, strength, score, "");
    }

    private static int color(Context ctx, String strength) {
        switch (strength) {
            case "VERY_STRONG": return ctx.getColor(R.color.brand);
            case "STRONG":      return ctx.getColor(R.color.success);
            case "FAIR":        return ctx.getColor(R.color.warning);
            default:            return ctx.getColor(R.color.danger);
        }
    }

    private static int label(String strength) {
        switch (strength) {
            case "VERY_STRONG": return R.string.strength_excellent;
            case "STRONG":      return R.string.strength_strong;
            case "FAIR":        return R.string.strength_fair;
            case "WEAK":        return R.string.strength_weak;
            default:            return R.string.strength_very_weak;
        }
    }
}
