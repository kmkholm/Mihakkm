package com.drtawfik.mihakk.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.data.Repo;
import com.drtawfik.mihakk.data.Review;
import com.drtawfik.mihakk.logic.ReportBuilder;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class ReviewsFragment extends Fragment {

    private static final String[] FILTERS = {
            "", "open", "done",
            Review.S_INVITED, Review.S_IN_PROGRESS, Review.S_SUBMITTED, Review.S_DECLINED
    };

    /**
     * Text search decrypts every row to match against it, so running on each
     * keystroke made the field drop characters on a large record. Coalesce.
     */
    private static final long SEARCH_DELAY_MS = 250;

    private ReviewAdapter adapter;
    private TextInputEditText search;
    private ChipGroup filters;
    private TextView empty;
    private String activeFilter = "";

    private final android.os.Handler debounce =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable doSearch = this::refresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                             @Nullable Bundle state) {
        View v = inflater.inflate(R.layout.fragment_reviews, parent, false);
        search = v.findViewById(R.id.search);
        filters = v.findViewById(R.id.filters);
        empty = v.findViewById(R.id.empty);

        RecyclerView list = v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReviewAdapter(this::open);
        list.setAdapter(adapter);

        buildFilterChips();
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
                debounce.removeCallbacks(doSearch);
                debounce.postDelayed(doSearch, SEARCH_DELAY_MS);
            }

            public void afterTextChanged(Editable s) {
            }
        });
        return v;
    }

    private void buildFilterChips() {
        filters.removeAllViews();
        for (int i = 0; i < FILTERS.length; i++) {
            String code = FILTERS[i];
            Chip c = new Chip(requireContext());
            c.setCheckable(true);
            c.setText(labelFor(code));
            c.setChecked(i == 0);
            c.setOnClickListener(v -> {
                activeFilter = code;
                refresh();
            });
            filters.addView(c);
        }
    }

    private String labelFor(String code) {
        if (code.isEmpty()) return getString(R.string.filter_all);
        if ("open".equals(code)) return getString(R.string.filter_open);
        if ("done".equals(code)) return getString(R.string.filter_done);
        return ReportBuilder.statusLabel(requireContext(), code);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onDestroyView() {
        debounce.removeCallbacks(doSearch);
        super.onDestroyView();
    }

    private void refresh() {
        if (adapter == null) return;
        String text = search.getText() == null ? "" : search.getText().toString().trim();
        List<Review> data = new Repo(requireContext()).search(activeFilter, text, null);
        adapter.submit(data);
        empty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void open(Review r) {
        Intent i = new Intent(requireContext(), ReviewEditActivity.class);
        i.putExtra(ReviewEditActivity.EXTRA_ID, r.id);
        startActivity(i);
    }
}
