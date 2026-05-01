package com.vaultkey.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.vaultkey.R;
import com.vaultkey.activities.AddPasswordActivity;
import com.vaultkey.activities.MainActivity;
import com.vaultkey.adapters.PasswordAdapter;
import com.vaultkey.data.database.VaultDatabase;
import com.vaultkey.data.model.PasswordEntry;
import com.vaultkey.utils.AutoLockManager;
import java.util.ArrayList;
import java.util.List;

public class PasswordsFragment extends Fragment {

    private PasswordAdapter adapter;
    private List<PasswordEntry> allPasswords = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_passwords, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.top_bar_actions);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_lock && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).lockAndGoToLock();
                return true;
            }
            return false;
        });

        RecyclerView rv = view.findViewById(R.id.rvPasswords);
        View empty = view.findViewById(R.id.emptyState);
        adapter = new PasswordAdapter(requireContext(), entry -> {
            Intent intent = new Intent(requireContext(), AddPasswordActivity.class);
            intent.putExtra("id", entry.id);
            startActivity(intent);
        });
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        VaultDatabase.getInstance(requireContext()).passwordDao().getAll()
            .observe(getViewLifecycleOwner(), list -> {
                allPasswords = list == null ? new ArrayList<>() : list;
                adapter.submitList(allPasswords);
                adapter.filter(((EditText) view.findViewById(R.id.etSearch)).getText().toString());
                empty.setVisibility(allPasswords.isEmpty() ? View.VISIBLE : View.GONE);
            });

        view.findViewById(R.id.fabAdd).setOnClickListener(v ->
            startActivity(new Intent(requireContext(), AddPasswordActivity.class)));

        EditText et = view.findViewById(R.id.etSearch);
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { adapter.filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        AutoLockManager.getInstance().resetTimer();
    }

    @Override
    public void onDestroy() {
        if (adapter != null) adapter.release();
        super.onDestroy();
    }
}
