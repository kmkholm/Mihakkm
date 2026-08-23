package com.drtawfik.mihakk.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.data.Content;
import com.drtawfik.mihakk.data.Prefs;
import com.drtawfik.mihakk.data.Repo;
import com.drtawfik.mihakk.data.Review;
import com.drtawfik.mihakk.data.Template;
import com.drtawfik.mihakk.logic.ReportBuilder;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ReportActivity extends BaseActivity {

    public static final String EXTRA_AUTOBUILD = "autobuild";

    private static final String[] SCOPES = {
            Template.SCOPE_AUTHORS, Template.SCOPE_EDITOR, Template.SCOPE_DECLINE
    };

    private Repo repo;
    private Review review;

    private ChipGroup scopes;
    private MaterialAutoCompleteTextView templatePicker;
    private TextInputEditText body;

    private String activeScope = Template.SCOPE_AUTHORS;
    private List<Template> shown = new ArrayList<>();
    private Template selected;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_report);

        repo = new Repo(this);
        review = repo.byId(getIntent().getLongExtra(ReviewEditActivity.EXTRA_ID, 0));
        if (review == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitle(review.displayTitle());
        toolbar.setNavigationOnClickListener(v -> saveAndFinish());

        scopes = findViewById(R.id.scopes);
        templatePicker = findViewById(R.id.template);
        body = findViewById(R.id.body);

        buildScopeChips();
        reloadTemplates();

        body.setText(review.reportText);
        if (TextUtils.isEmpty(review.reportText)) body.setHint(R.string.report_empty_hint);

        ((MaterialButton) findViewById(R.id.generate)).setOnClickListener(v -> generate(false));
        ((MaterialButton) findViewById(R.id.copy)).setOnClickListener(v -> copy());
        ((MaterialButton) findViewById(R.id.share)).setOnClickListener(v -> share());

        if (getIntent().getBooleanExtra(EXTRA_AUTOBUILD, false)
                && TextUtils.isEmpty(review.reportText)) {
            generate(true);
        }
    }

    private void buildScopeChips() {
        scopes.removeAllViews();
        for (String s : SCOPES) {
            Chip c = new Chip(this);
            c.setCheckable(true);
            c.setText(scopeLabel(s));
            c.setChecked(s.equals(activeScope));
            c.setOnClickListener(v -> {
                activeScope = s;
                reloadTemplates();
            });
            scopes.addView(c);
        }
    }

    private String scopeLabel(String s) {
        if (Template.SCOPE_EDITOR.equals(s)) return getString(R.string.report_scope_editor);
        if (Template.SCOPE_DECLINE.equals(s)) return getString(R.string.report_scope_decline);
        return getString(R.string.report_scope_authors);
    }

    private void reloadTemplates() {
        shown = Content.templatesFor(this, activeScope, isArabic());
        List<String> labels = new ArrayList<>();
        for (Template t : shown) labels.add(t.name(isArabic()));

        templatePicker.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, labels));
        templatePicker.setOnItemClickListener((p, v, pos, id) -> {
            selected = shown.get(pos);
            Prefs.set(this, Prefs.LAST_TEMPLATE, selected.key);
        });

        selected = pickDefault();
        if (selected != null) templatePicker.setText(selected.name(isArabic()), false);
        else templatePicker.setText("", false);
    }

    /**
     * Prefer the template the reviewer used last, then one matching the
     * recommendation already recorded, then simply the first in the list.
     */
    private Template pickDefault() {
        if (shown.isEmpty()) return null;
        String last = Prefs.get(this, Prefs.LAST_TEMPLATE, "");
        for (Template t : shown) if (t.key.equals(last)) return t;
        if (!TextUtils.isEmpty(review.recommendation))
            for (Template t : shown)
                if (review.recommendation.equals(t.recommendation)) return t;
        return shown.get(0);
    }

    private void generate(boolean silent) {
        if (selected == null) {
            toast(getString(R.string.report_empty_hint));
            return;
        }
        String current = body.getText() == null ? "" : body.getText().toString();
        if (!silent && !current.trim().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.report_overwrite)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, (d, w) -> writeGenerated())
                    .show();
            return;
        }
        writeGenerated();
    }

    private void writeGenerated() {
        body.setText(ReportBuilder.build(this, selected, review));
        if (TextUtils.isEmpty(review.recommendation)
                && !TextUtils.isEmpty(selected.recommendation)
                && !"none".equals(selected.recommendation)) {
            review.recommendation = selected.recommendation;
        }
    }

    private void persist() {
        review.reportText = body.getText() == null ? "" : body.getText().toString();
        repo.save(review);
    }

    private void copy() {
        persist();
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("review", review.reportText));
            toast(getString(R.string.report_copied));
        }
    }

    private void share() {
        persist();
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, review.displayTitle());
        i.putExtra(Intent.EXTRA_TEXT, review.reportText);
        startActivity(Intent.createChooser(i, getString(R.string.report_share)));
    }

    private void saveAndFinish() {
        persist();
        finish();
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
