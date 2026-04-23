package com.vaultkey.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vaultkey.R;
import com.vaultkey.data.database.VaultDatabase;
import com.vaultkey.data.model.PasswordEntry;
import com.vaultkey.utils.AvatarHelper;
import com.vaultkey.utils.EncryptionManager;
import com.vaultkey.utils.PreferencesManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.VH> {

    public interface OnEditListener { void onEdit(PasswordEntry entry); }

    private List<PasswordEntry> allItems = new ArrayList<>();
    private List<PasswordEntry> filtered = new ArrayList<>();
    private final Context ctx;
    private final OnEditListener editListener;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private String query = "";

    public PasswordAdapter(Context ctx, OnEditListener editListener) {
        this.ctx = ctx;
        this.editListener = editListener;
    }

    public void submitList(List<PasswordEntry> list) {
        allItems = new ArrayList<>(list);
        applyFilter();
    }

    public void filter(String q) {
        query = q == null ? "" : q.toLowerCase(Locale.ROOT);
        applyFilter();
    }



    private void applyFilter() {
        List<PasswordEntry> oldList = new ArrayList<>(filtered);

        List<PasswordEntry> source;
        if (query.isEmpty()) {
            source = new ArrayList<>(allItems);
        } else {
            source = new ArrayList<>();
            for (PasswordEntry e : allItems) {
                String t = e.title == null ? "" : e.title.toLowerCase(Locale.ROOT);
                String u = e.username == null ? "" : e.username.toLowerCase(Locale.ROOT);
                String r = e.url == null ? "" : e.url.toLowerCase(Locale.ROOT);
                if (t.contains(query) || u.contains(query) || r.contains(query)) source.add(e);
            }
        }



        filtered = source;

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return oldList.size(); }
            @Override public int getNewListSize() { return filtered.size(); }
            @Override public boolean areItemsTheSame(int oldPos, int newPos) { return oldList.get(oldPos).id == filtered.get(newPos).id; }
            @Override public boolean areContentsTheSame(int oldPos, int newPos) { return oldList.get(oldPos).updatedAt == filtered.get(newPos).updatedAt; }
        });
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(ctx).inflate(R.layout.item_password, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PasswordEntry e = filtered.get(position);

        h.expandedRow.setVisibility(View.GONE);
        h.isShowing = false;
        h.tvPasswordValue.setTag(null);
        h.tvPasswordValue.setText("");
        h.btnTogglePassword.setImageResource(R.drawable.ic_visibility);

        h.tvTitle.setText(e.title);
        AvatarHelper.loadPasswordAvatar(ctx, h.ivAvatar, h.tvInitial, e.avatarPath, e.url, e.title);
        h.tvUsername.setText(e.username);
        if (h.tvLoginValue != null) h.tvLoginValue.setText(e.username);

        int dotColor;
        if (e.strengthScore >= 3) dotColor = ctx.getColor(R.color.success);
        else if (e.strengthScore >= 2) dotColor = ctx.getColor(R.color.warning);
        else dotColor = ctx.getColor(R.color.danger);
        h.strengthDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dotColor));

        h.rowMain.setOnClickListener(v -> {
            boolean wasGone = h.expandedRow.getVisibility() == View.GONE;
            h.expandedRow.setVisibility(wasGone ? View.VISIBLE : View.GONE);
            if (wasGone) {
                h.tvPasswordValue.setText("•••••");
                h.isShowing = false;
                h.btnTogglePassword.setImageResource(R.drawable.ic_visibility);
                io.execute(() -> {
                    String plain = decryptPassword(e);
                    v.post(() -> {
                        h.tvPasswordValue.setTag(plain);
                        h.tvPasswordValue.setText(maskPassword(plain));
                    });
                });
            }
        });

        h.btnTogglePassword.setOnClickListener(v -> {
            String plain = h.tvPasswordValue.getTag() != null ? (String) h.tvPasswordValue.getTag() : null;
            if (plain == null) {
                io.execute(() -> {
                    String p = decryptPassword(e);
                    v.post(() -> {
                        h.tvPasswordValue.setTag(p);
                        h.isShowing = true;
                        h.tvPasswordValue.setText(p);
                        h.btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
                    });
                });
            } else {
                h.isShowing = !h.isShowing;
                h.tvPasswordValue.setText(h.isShowing ? plain : maskPassword(plain));
                h.btnTogglePassword.setImageResource(h.isShowing ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
            }
        });

        h.btnCopy.setOnClickListener(v -> {
            String cached = h.tvPasswordValue.getTag() != null ? (String) h.tvPasswordValue.getTag() : null;
            if (cached != null) {
                clip("password", cached);
                Toast.makeText(ctx, R.string.password_copied, Toast.LENGTH_SHORT).show();
            } else {
                io.execute(() -> {
                    String p = decryptPassword(e);
                    v.post(() -> {
                        clip("password", p);
                        Toast.makeText(ctx, R.string.password_copied, Toast.LENGTH_SHORT).show();
                    });
                });
            }
        });

        if (h.btnCopyLogin != null) {
            h.btnCopyLogin.setOnClickListener(v -> {
                clip("login", e.username);
                Toast.makeText(ctx, R.string.login_copied, Toast.LENGTH_SHORT).show();
            });
        }

        h.btnEdit.setOnClickListener(v -> editListener.onEdit(e));
        h.btnDelete.setOnClickListener(v ->
            new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.password_delete_title)
                .setMessage(ctx.getString(R.string.password_delete_message, e.title))
                .setPositiveButton(R.string.delete, (d, w) ->
                    io.execute(() -> VaultDatabase.getInstance(ctx).passwordDao().deleteById(e.id)))
                .setNegativeButton(R.string.cancel, null)
                .show());
    }

    @Override
    public int getItemCount() { return filtered.size(); }

    private String decryptPassword(PasswordEntry e) {
        return EncryptionManager.decrypt(e.password, PreferencesManager.getInstance(ctx).getDataEncryptionKey());
    }

    private void clip(String label, String text) {
        ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, text));
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
                if (text.equals(cm.getPrimaryClip().getItemAt(0).getText().toString())) {
                    cm.setPrimaryClip(ClipData.newPlainText("", ""));
                }
            }
        }, 30_000);
    }

    private String maskPassword(String plain) {
        return "•".repeat(Math.min(plain == null ? 0 : plain.length(), 20));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvInitial, tvTitle, tvUsername, tvPasswordValue, tvLoginValue;
        ImageView ivAvatar;
        View strengthDot, rowMain, expandedRow;
        ImageButton btnEdit, btnDelete, btnTogglePassword, btnCopy, btnCopyLogin;
        boolean isShowing = false;

        VH(View v) {
            super(v);
            tvInitial = v.findViewById(R.id.tvInitial);
            ivAvatar = v.findViewById(R.id.ivAvatar);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvUsername = v.findViewById(R.id.tvUsername);
            tvPasswordValue = v.findViewById(R.id.tvPasswordValue);
            strengthDot = v.findViewById(R.id.strengthDot);
            rowMain = v.findViewById(R.id.rowMain);
            expandedRow = v.findViewById(R.id.expandedRow);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
            btnTogglePassword = v.findViewById(R.id.btnTogglePassword);
            btnCopy = v.findViewById(R.id.btnCopy);
            btnCopyLogin = v.findViewById(R.id.btnCopyLogin);
            tvLoginValue = v.findViewById(R.id.tvLoginValue);
        }
    }
}
