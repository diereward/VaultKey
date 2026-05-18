package com.vaultkey.utils;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.util.Locale;

public class AvatarHelper {

    public static void loadPasswordAvatar(Context ctx, ImageView ivAvatar, TextView tvInitial,
                                          String avatarPath, String url, String title) {
        if (avatarPath != null && !avatarPath.isEmpty()) {
            showImage(ctx, ivAvatar, tvInitial, avatarPath);
            return;
        }

        if (url != null && !url.isEmpty()) {
            String faviconUrl = buildFaviconUrl(url);
            if (faviconUrl != null) {
                hide(tvInitial);
                ivAvatar.setVisibility(View.VISIBLE);
                Glide.with(ctx).load(faviconUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(0)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e,
                                Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                boolean isFirstResource) {
                            ivAvatar.setVisibility(View.GONE);
                            show(tvInitial, title);
                            return true;
                        }
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(ivAvatar);
                return;
            }
        }

        ivAvatar.setVisibility(View.GONE);
        show(tvInitial, title);
    }

    private static void showImage(Context ctx, ImageView iv, TextView tv, String path) {
        hide(tv);
        iv.setVisibility(View.VISIBLE);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(ctx).load(Uri.parse(path)).diskCacheStrategy(DiskCacheStrategy.NONE).into(iv);
    }

    private static void hide(TextView tv) { if (tv != null) tv.setVisibility(View.GONE); }

    private static void show(TextView tv, String label) {
        if (tv == null) return;
        tv.setVisibility(View.VISIBLE);
        tv.setText(label != null && !label.isEmpty() ? label.substring(0, 1).toUpperCase(Locale.getDefault()) : "+");
    }

    private static String buildFaviconUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            String domain = url.trim().replaceFirst("^https?://", "");
            int slash = domain.indexOf('/');
            if (slash != -1) domain = domain.substring(0, slash);
            if (domain.startsWith("www.")) domain = domain.substring(4);
            if (domain.isEmpty() || !domain.contains(".")) return null;
            Uri.parse("https://" + domain);
            return "https://icons.duckduckgo.com/ip3/" + domain + ".ico";
        } catch (Exception e) { return null; }
    }
}
