package com.drtawfik.mihakk.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.data.Journal;
import com.drtawfik.mihakk.data.Repo;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The journal registry. Rows appear on their own as reviews are logged; filling
 * in publisher and quartile once is what lets the record PDF group by publisher
 * and by quartile later.
 */
public class JournalsActivity extends BaseActivity {

    private static final String[] QUARTILES = {"", "Q1", "Q2", "Q3", "Q4"};

    private Repo repo;
    private Adapter adapter;
    private TextView empty;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_list_shell);
        repo = new Repo(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.journals_title);
        toolbar.setNavigationOnClickListener(v -> finish());

        empty = findViewById(R.id.empty);
        empty.setText(R.string.journals_empty);

        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter();
        list.setAdapter(adapter);
        refresh();
    }

    private void refresh() {
        List<Journal> data = repo.journals();
        adapter.submit(data);
        empty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void edit(Journal j) {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_journal, null);
        EditText name = form.findViewById(R.id.name);
        EditText publisher = form.findViewById(R.id.publisher);
        EditText issn = form.findViewById(R.id.issn);
        EditText impact = form.findViewById(R.id.impact);
        EditText indexing = form.findViewById(R.id.indexing);
        MaterialAutoCompleteTextView quartile = form.findViewById(R.id.quartile);

        quartile.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, QUARTILES));

        name.setText(j.name);
        publisher.setText(j.publisher);
        issn.setText(j.issn);
        impact.setText(j.impactFactor > 0 ? String.valueOf(j.impactFactor) : "");
        indexing.setText(j.indexing);
        quartile.setText(j.quartile, false);

        new AlertDialog.Builder(this)
                .setTitle(R.string.journal_edit)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String previous = j.name;
                    j.name = name.getText().toString().trim();
                    if (j.name.isEmpty()) return;
                    // Naming an ISSN here is how an ORCID import gets readable
                    // journals, so the rename has to reach the reviews too.
                    int moved = repo.renameJournal(previous, j.name);
                    if (moved > 0) {
                        Toast.makeText(this, getString(R.string.journal_renamed, moved, j.name),
                                Toast.LENGTH_LONG).show();
                    }
                    j.publisher = publisher.getText().toString().trim();
                    j.issn = issn.getText().toString().trim();
                    j.indexing = indexing.getText().toString().trim();
                    j.quartile = quartile.getText().toString().trim();
                    try {
                        String v = impact.getText().toString().trim();
                        j.impactFactor = v.isEmpty() ? 0 : Double.parseDouble(v);
                    } catch (NumberFormatException e) {
                        j.impactFactor = 0;
                    }
                    repo.saveJournal(j);
                    refresh();
                })
                .show();
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        private final List<Journal> items = new ArrayList<>();

        void submit(List<Journal> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_two_line, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Journal j = items.get(pos);
            h.title.setText(j.name);
            h.subtitle.setText(j.subtitle());
            h.trailing.setText(getString(R.string.j_reviews, j.reviewCount));
            h.itemView.setOnClickListener(v -> edit(j));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView title, subtitle, trailing;

            VH(View v) {
                super(v);
                title = v.findViewById(R.id.title);
                subtitle = v.findViewById(R.id.subtitle);
                trailing = v.findViewById(R.id.trailing);
            }
        }
    }
}
