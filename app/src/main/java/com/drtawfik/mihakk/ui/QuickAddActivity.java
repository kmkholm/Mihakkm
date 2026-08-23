package com.drtawfik.mihakk.ui;

import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.alarm.ReminderScheduler;
import com.drtawfik.mihakk.data.Prefs;
import com.drtawfik.mihakk.data.Repo;
import com.drtawfik.mihakk.data.Review;
import com.drtawfik.mihakk.logic.InviteParser;
import com.drtawfik.mihakk.util.DateUtil;
import com.drtawfik.mihakk.util.LocaleUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * The fast way in: paste the invitation email, confirm four fields, done.
 * <p>
 * A reviewer already has the facts in an email — retyping them into a
 * fifteen-field form is the friction that stops people keeping a record at all.
 */
public class QuickAddActivity extends BaseActivity {

    private Repo repo;

    private TextInputEditText source, title, manuscriptId;
    private MaterialAutoCompleteTextView journal;
    private MaterialButton dueBtn;
    private TextView found;

    private String dueOn = "";
    private String editor = "";
    private String authors = "";
    private boolean userTouchedDue;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_quick_add);
        repo = new Repo(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        source = findViewById(R.id.source);
        journal = findViewById(R.id.journal);
        title = findViewById(R.id.title);
        manuscriptId = findViewById(R.id.manuscriptId);
        dueBtn = findViewById(R.id.dueOn);
        found = findViewById(R.id.found);

        journal.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, journalSuggestions()));

        dueOn = DateUtil.plusDays(DateUtil.today(),
                Prefs.getInt(this, Prefs.DEFAULT_DUE_DAYS, 21));
        paintDue();

        ((MaterialButton) findViewById(R.id.paste)).setOnClickListener(v -> pasteFromClipboard());
        ((MaterialButton) findViewById(R.id.save)).setOnClickListener(v -> save(false));
        ((MaterialButton) findViewById(R.id.more)).setOnClickListener(v -> save(true));
        dueBtn.setOnClickListener(v -> pickDue());

        source.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(Editable s) {
                if (s.length() > 30) parse(s.toString());
            }
        });

        // Opened from a share sheet? Take the shared text straight away.
        String shared = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        if (shared != null && !shared.trim().isEmpty()) source.setText(shared);
    }

    private List<String> journalSuggestions() {
        List<String> names = new ArrayList<>(repo.journalNames());
        for (String s : InviteParser.seedJournalNames()) if (!names.contains(s)) names.add(s);
        return names;
    }

    private void pasteFromClipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) {
            toast(getString(R.string.quick_add_clipboard_empty));
            return;
        }
        ClipData clip = cm.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            toast(getString(R.string.quick_add_clipboard_empty));
            return;
        }
        CharSequence text = clip.getItemAt(0).coerceToText(this);
        if (text == null || text.length() == 0) {
            toast(getString(R.string.quick_add_clipboard_empty));
            return;
        }
        source.setText(text.toString());
    }

    private void parse(String raw) {
        InviteParser.Parsed p = InviteParser.parse(raw, journalSuggestions());
        if (p.isEmpty() && p.dueOn.isEmpty()) {
            found.setVisibility(android.view.View.GONE);
            return;
        }
        if (!p.journal.isEmpty()) journal.setText(p.journal, false);
        if (!p.title.isEmpty()) title.setText(p.title);
        if (!p.manuscriptId.isEmpty()) manuscriptId.setText(p.manuscriptId);
        if (!p.dueOn.isEmpty() && !userTouchedDue) {
            dueOn = p.dueOn;
            paintDue();
        }
        editor = p.editor;
        authors = p.authors;

        found.setText(getResources().getQuantityString(
                R.plurals.quick_add_found, p.foundCount(), p.foundCount()));
        found.setVisibility(android.view.View.VISIBLE);
    }

    private void pickDue() {
        Calendar cal = Calendar.getInstance();
        long ms = DateUtil.toMillis(dueOn);
        if (ms > 0) cal.setTimeInMillis(ms);
        DatePickerDialog dlg = new DatePickerDialog(this, (v, y, m, d) -> {
            userTouchedDue = true;
            dueOn = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, d);
            paintDue();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dlg.setButton(DatePickerDialog.BUTTON_NEUTRAL, getString(R.string.clear), (d, w) -> {
            userTouchedDue = true;
            dueOn = "";
            paintDue();
        });
        dlg.show();
    }

    private void paintDue() {
        String v = dueOn.isEmpty() ? "—" : DateUtil.pretty(dueOn, LocaleUtil.currentLocale(this));
        dueBtn.setText(getString(R.string.f_due_on) + "  ·  " + v);
    }

    private void save(boolean openDetails) {
        Review r = new Review();
        r.journalName = text(journal);
        r.title = text(title);
        r.manuscriptId = text(manuscriptId);
        r.editor = editor;
        r.authors = authors;
        r.notes = "";
        r.status = Review.S_INVITED;
        r.invitedOn = DateUtil.today();
        r.dueOn = dueOn;
        r.reminderDays = 3;

        if (r.journalName.isEmpty() && r.title.isEmpty() && r.manuscriptId.isEmpty()) {
            toast(getString(R.string.need_journal));
            return;
        }

        long id = repo.save(r);
        ReminderScheduler.schedule(this);

        if (openDetails) {
            Intent i = new Intent(this, ReviewEditActivity.class);
            i.putExtra(ReviewEditActivity.EXTRA_ID, id);
            startActivity(i);
        } else {
            toast(getString(R.string.saved));
        }
        finish();
    }

    private String text(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
