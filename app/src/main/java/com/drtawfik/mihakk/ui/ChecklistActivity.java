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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.data.Answers;
import com.drtawfik.mihakk.data.Checklist;
import com.drtawfik.mihakk.data.Content;
import com.drtawfik.mihakk.data.Repo;
import com.drtawfik.mihakk.data.Review;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Working through the appraisal, item by item. Answers persist on every change
 * rather than on a save button — a reviewer reads a manuscript over days and
 * should never lose a note to a killed process.
 */
public class ChecklistActivity extends BaseActivity {

    private Repo repo;
    private Review review;
    private Checklist checklist;
    private Answers answers;

    private TextView progress;
    private final List<Object> rows = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_checklist);

        repo = new Repo(this);
        review = repo.byId(getIntent().getLongExtra(ReviewEditActivity.EXTRA_ID, 0));
        if (review == null) {
            finish();
            return;
        }
        checklist = Content.checklist(this, review.checklistKey);
        if (checklist == null) {
            finish();
            return;
        }
        answers = Answers.parse(review.checklistState);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(checklist.name(isArabic()));
        toolbar.setSubtitle(checklist.guideline);
        toolbar.setNavigationOnClickListener(v -> finish());

        progress = findViewById(R.id.progress);

        for (Checklist.Section s : checklist.sections) {
            rows.add(s);
            rows.addAll(s.items);
        }

        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new Adapter());

        ((MaterialButton) findViewById(R.id.toReport)).setOnClickListener(v -> {
            persist();
            Intent i = new Intent(this, ReportActivity.class);
            i.putExtra(ReviewEditActivity.EXTRA_ID, review.id);
            i.putExtra(ReportActivity.EXTRA_AUTOBUILD, true);
            startActivity(i);
        });

        paintProgress();
    }

    @Override
    protected void onPause() {
        super.onPause();
        persist();
    }

    private void persist() {
        review.checklistState = answers.toJson();
        if (review.studyType.isEmpty()) review.studyType = checklist.studyType;
        repo.save(review);
    }

    private void paintProgress() {
        progress.setText(getString(R.string.checklist_progress,
                answers.answeredCount(), checklist.itemCount(), answers.problemCount()));
    }

    // ----------------------------------------------------------- adapter

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int T_HEADER = 0;
        private static final int T_ITEM = 1;

        @Override
        public int getItemViewType(int position) {
            return rows.get(position) instanceof Checklist.Section ? T_HEADER : T_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int type) {
            LayoutInflater inf = LayoutInflater.from(p.getContext());
            return type == T_HEADER
                    ? new HeaderVH(inf.inflate(R.layout.item_check_header, p, false))
                    : new ItemVH(inf.inflate(R.layout.item_check, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
            Object row = rows.get(pos);
            if (h instanceof HeaderVH) {
                ((HeaderVH) h).text.setText(((Checklist.Section) row).title(isArabic()));
            } else {
                ((ItemVH) h).bind((Checklist.Item) row);
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }

    private static class HeaderVH extends RecyclerView.ViewHolder {
        final TextView text;

        HeaderVH(View v) {
            super(v);
            text = (TextView) v;
        }
    }

    private class ItemVH extends RecyclerView.ViewHolder {
        final TextView weight, text, hint;
        final MaterialButtonToggleGroup verdict;
        final TextInputLayout noteBox;
        final TextInputEditText note;
        TextWatcher watcher;

        ItemVH(View v) {
            super(v);
            weight = v.findViewById(R.id.weight);
            text = v.findViewById(R.id.text);
            hint = v.findViewById(R.id.hint);
            verdict = v.findViewById(R.id.verdict);
            noteBox = v.findViewById(R.id.noteBox);
            note = v.findViewById(R.id.note);
        }

        void bind(Checklist.Item item) {
            boolean ar = isArabic();
            text.setText(item.text(ar));

            String h = item.hint(ar);
            hint.setText(h);
            hint.setVisibility(h.isEmpty() ? View.GONE : View.VISIBLE);
            weight.setText(weightLabel(item.weight));

            Answers.Entry e = answers.get(item.id);

            // Detach listeners before restoring state or recycling fires them.
            verdict.clearOnButtonCheckedListeners();
            if (watcher != null) note.removeTextChangedListener(watcher);

            verdict.clearChecked();
            int checkedId = idForVerdict(e.verdict);
            if (checkedId != 0) verdict.check(checkedId);

            note.setText(e.note);
            noteBox.setVisibility(showNote(e) ? View.VISIBLE : View.GONE);

            verdict.addOnButtonCheckedListener((g, id, checked) -> {
                if (!checked) return;
                answers.setVerdict(item.id, verdictForId(id));
                Answers.Entry cur = answers.get(item.id);
                noteBox.setVisibility(showNote(cur) ? View.VISIBLE : View.GONE);
                paintProgress();
            });

            watcher = new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int a, int b, int c) {
                }

                public void onTextChanged(CharSequence s, int a, int b, int c) {
                }

                public void afterTextChanged(Editable s) {
                    answers.setNote(item.id, s.toString());
                }
            };
            note.addTextChangedListener(watcher);
        }

        private boolean showNote(Answers.Entry e) {
            return e.isProblem() || !e.note.isEmpty() || Answers.V_OK.equals(e.verdict);
        }
    }

    private String weightLabel(String weight) {
        if (Checklist.W_CRITICAL.equals(weight)) return getString(R.string.w_critical);
        if (Checklist.W_MINOR.equals(weight)) return getString(R.string.w_minor);
        return getString(R.string.w_major);
    }

    private int idForVerdict(String v) {
        if (Answers.V_OK.equals(v)) return R.id.vOk;
        if (Answers.V_CONCERN.equals(v)) return R.id.vConcern;
        if (Answers.V_FAIL.equals(v)) return R.id.vFail;
        if (Answers.V_NA.equals(v)) return R.id.vNa;
        return 0;
    }

    private String verdictForId(int id) {
        if (id == R.id.vOk) return Answers.V_OK;
        if (id == R.id.vConcern) return Answers.V_CONCERN;
        if (id == R.id.vFail) return Answers.V_FAIL;
        if (id == R.id.vNa) return Answers.V_NA;
        return "";
    }
}
