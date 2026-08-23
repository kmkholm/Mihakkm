package com.drtawfik.mihakk.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.data.Checklist;
import com.drtawfik.mihakk.data.Content;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class ChecklistPickerActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_list_shell);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.checklist_pick);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView header = findViewById(R.id.header);
        header.setText(R.string.checklist_pick_hint);
        header.setVisibility(View.VISIBLE);

        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new Adapter(Content.checklists(this)));
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        private final List<Checklist> items;

        Adapter(List<Checklist> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_two_line, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Checklist c = items.get(pos);
            boolean ar = isArabic();
            h.title.setText(c.name(ar));

            String sub = c.summary(ar);
            if (sub.isEmpty() && !c.guideline.isEmpty())
                sub = getString(R.string.checklist_source, c.guideline);
            h.subtitle.setText(sub);

            h.trailing.setText(getString(R.string.checklist_items, c.itemCount()));
            h.itemView.setOnClickListener(v -> {
                setResult(RESULT_OK, new Intent().putExtra("key", c.key));
                finish();
            });
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
