package com.drtawfik.mihakk.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.alarm.ReminderScheduler;
import com.drtawfik.mihakk.data.Checklist;
import com.drtawfik.mihakk.data.Content;
import com.drtawfik.mihakk.data.Prefs;
import com.drtawfik.mihakk.data.Repo;
import com.drtawfik.mihakk.data.Review;
import com.drtawfik.mihakk.logic.ReportBuilder;
import com.drtawfik.mihakk.util.DateUtil;
import com.drtawfik.mihakk.util.LocaleUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.List;

public class ReviewEditActivity extends BaseActivity {

    public static final String EXTRA_ID = "review_id";
    public static final int REQ_CHECKLIST = 41;

    private Repo repo;
    private Review review;

    private MaterialAutoCompleteTextView journal, status, recommendation;
    private TextInputEditText title, manuscriptId, round, authors, editor, hours, tags, notes, editorNotes;
    private MaterialButton invitedOn, dueOn, submittedOn, checklistBtn, reportBtn, moreToggle;
    private android.view.View advanced;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_review_edit);
        repo = new Repo(this);

        long id = getIntent().getLongExtra(EXTRA_ID, 0);
        review = id > 0 ? repo.byId(id) : null;
        if (review == null) {
            review = new Review();
            review.invitedOn = DateUtil.today();
            review.dueOn = DateUtil.plusDays(review.invitedOn,
                    Prefs.getInt(this, Prefs.DEFAULT_DUE_DAYS, 21));
            review.reminderDays = 3;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(id > 0 ? R.string.edit_title_edit : R.string.edit_title_new);
        toolbar.setNavigationOnClickListener(v -> saveAndFinish());
        toolbar.inflateMenu(R.menu.edit);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_save) {
                saveAndFinish();
                return true;
            }
            if (item.getItemId() == R.id.action_delete) {
                confirmDelete();
                return true;
            }
            return false;
        });

        wire();
        fill();
    }

    private void wire() {
        journal = findViewById(R.id.journal);
        title = findViewById(R.id.title);
        manuscriptId = findViewById(R.id.manuscriptId);
        round = findViewById(R.id.round);
        authors = findViewById(R.id.authors);
        editor = findViewById(R.id.editor);
        hours = findViewById(R.id.hours);
        tags = findViewById(R.id.tags);
        notes = findViewById(R.id.notes);
        editorNotes = findViewById(R.id.editorNotes);
        status = findViewById(R.id.status);
        recommendation = findViewById(R.id.recommendation);
        invitedOn = findViewById(R.id.invitedOn);
        dueOn = findViewById(R.id.dueOn);
        submittedOn = findViewById(R.id.submittedOn);
        checklistBtn = findViewById(R.id.checklist);
        reportBtn = findViewById(R.id.report);
        moreToggle = findViewById(R.id.moreToggle);
        advanced = findViewById(R.id.advanced);

        moreToggle.setOnClickListener(v ->
                setAdvancedVisible(advanced.getVisibility() != android.view.View.VISIBLE));

        List<String> names = repo.journalNames();
        journal.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, names));

        status.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                labels(Review.STATUSES, true)));
        status.setOnItemClickListener((p, v, pos, id) -> {
            review.status = Review.STATUSES[pos];
            if (review.isDone() && review.submittedOn.isEmpty()) {
                review.submittedOn = DateUtil.today();
                paintDates();
            }
            if (Review.S_DECLINED.equals(review.status) && review.respondedOn.isEmpty())
                review.respondedOn = DateUtil.today();
        });

        recommendation.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                labels(Review.RECOMMENDATIONS, false)));
        recommendation.setOnItemClickListener((p, v, pos, id) ->
                review.recommendation = Review.RECOMMENDATIONS[pos]);

        invitedOn.setOnClickListener(v -> pickDate(review.invitedOn, d -> {
            review.invitedOn = d;
            if (review.dueOn.isEmpty())
                review.dueOn = DateUtil.plusDays(d, Prefs.getInt(this, Prefs.DEFAULT_DUE_DAYS, 21));
            paintDates();
        }));
        dueOn.setOnClickListener(v -> pickDate(review.dueOn, d -> {
            review.dueOn = d;
            paintDates();
        }));
        submittedOn.setOnClickListener(v -> pickDate(review.submittedOn, d -> {
            review.submittedOn = d;
            if (!review.isDone()) review.status = Review.S_SUBMITTED;
            paintStatus();
            paintDates();
        }));

        findViewById(R.id.addCalendar).setOnClickListener(v -> addToCalendar());
        findViewById(R.id.nextRound).setOnClickListener(v -> startNextRound());

        checklistBtn.setOnClickListener(v -> openChecklist());
        reportBtn.setOnClickListener(v -> openReport());
    }

    private String[] labels(String[] codes, boolean isStatus) {
        String[] out = new String[codes.length];
        for (int i = 0; i < codes.length; i++) {
            out[i] = isStatus ? ReportBuilder.statusLabel(this, codes[i])
                    : ReportBuilder.recommendationLabel(this, codes[i]);
        }
        return out;
    }

    private void fill() {
        journal.setText(review.journalName, false);
        title.setText(review.title);
        manuscriptId.setText(review.manuscriptId);
        round.setText(String.valueOf(Math.max(1, review.round)));
        authors.setText(review.authors);
        editor.setText(review.editor);
        hours.setText(review.hours > 0 ? String.valueOf(review.hours) : "");
        tags.setText(review.tags);
        notes.setText(review.notes);
        editorNotes.setText(review.editorNotes);
        paintStatus();
        paintDates();
        paintChecklist();
        // Nothing hidden should ever be silently holding data the reviewer typed.
        setAdvancedVisible(hasAdvancedContent());
    }

    private boolean hasAdvancedContent() {
        return review.round > 1 || review.hours > 0
                || !review.editor.isEmpty() || !review.authors.isEmpty()
                || !review.tags.isEmpty() || !review.notes.isEmpty()
                || !review.editorNotes.isEmpty();
    }

    private void setAdvancedVisible(boolean visible) {
        advanced.setVisibility(visible ? android.view.View.VISIBLE : android.view.View.GONE);
        moreToggle.setText(visible ? R.string.edit_show_less : R.string.edit_show_more);
    }

    private void paintStatus() {
        status.setText(ReportBuilder.statusLabel(this, review.status), false);
        recommendation.setText(ReportBuilder.recommendationLabel(this, review.recommendation), false);
    }

    private void paintDates() {
        invitedOn.setText(dateLabel(R.string.f_invited_on, review.invitedOn));
        dueOn.setText(dateLabel(R.string.f_due_on, review.dueOn));
        submittedOn.setText(dateLabel(R.string.f_submitted_on, review.submittedOn));
    }

    private String dateLabel(int labelRes, String iso) {
        String value = iso == null || iso.isEmpty() ? "—"
                : DateUtil.pretty(iso, LocaleUtil.currentLocale(this));
        return getString(labelRes) + "  ·  " + value;
    }

    private void paintChecklist() {
        Checklist c = Content.checklist(this, review.checklistKey);
        checklistBtn.setText(c == null ? getString(R.string.checklist_none) : c.name(isArabic()));
    }

    private interface OnDate {
        void picked(String iso);
    }

    private void pickDate(String current, OnDate cb) {
        Calendar cal = Calendar.getInstance();
        long millis = DateUtil.toMillis(current);
        if (millis > 0) cal.setTimeInMillis(millis);

        DatePickerDialog dlg = new DatePickerDialog(this, (view, y, m, d) ->
                cb.picked(String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, d)),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dlg.setButton(DatePickerDialog.BUTTON_NEUTRAL, getString(R.string.clear),
                (d, w) -> cb.picked(""));
        dlg.show();
    }

    // ------------------------------------------------------------- actions

    private void openChecklist() {
        if (!collect()) return;
        review.id = repo.save(review);
        if (TextUtils.isEmpty(review.checklistKey)) {
            Intent i = new Intent(this, ChecklistPickerActivity.class);
            i.putExtra(EXTRA_ID, review.id);
            startActivityForResult(i, REQ_CHECKLIST);
        } else {
            Intent i = new Intent(this, ChecklistActivity.class);
            i.putExtra(EXTRA_ID, review.id);
            startActivity(i);
        }
    }

    private void openReport() {
        if (!collect()) return;
        review.id = repo.save(review);
        Intent i = new Intent(this, ReportActivity.class);
        i.putExtra(EXTRA_ID, review.id);
        startActivity(i);
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_CHECKLIST && res == RESULT_OK && data != null) {
            review.checklistKey = data.getStringExtra("key");
            repo.save(review);
            paintChecklist();
            Intent i = new Intent(this, ChecklistActivity.class);
            i.putExtra(EXTRA_ID, review.id);
            startActivity(i);
        }
    }

    /** Reloads from storage so a checklist answered in another screen is not lost. */
    @Override
    protected void onResume() {
        super.onResume();
        if (review.id > 0) {
            Review fresh = repo.byId(review.id);
            if (fresh != null) {
                review.checklistState = fresh.checklistState;
                review.checklistKey = fresh.checklistKey;
                review.reportText = fresh.reportText;
                paintChecklist();
            }
        }
    }

    private void startNextRound() {
        if (!collect()) return;
        review.id = repo.save(review);

        Review next = new Review();
        next.journalName = review.journalName;
        next.manuscriptId = review.manuscriptId;
        next.title = review.title;
        next.authors = review.authors;
        next.editor = review.editor;
        next.studyType = review.studyType;
        next.checklistKey = review.checklistKey;
        next.round = Math.max(1, review.round) + 1;
        next.parentId = review.id;
        next.status = Review.S_ACCEPTED;
        next.invitedOn = DateUtil.today();
        next.dueOn = DateUtil.plusDays(next.invitedOn, Prefs.getInt(this, Prefs.DEFAULT_DUE_DAYS, 21));
        next.reminderDays = review.reminderDays;
        long id = repo.save(next);

        Intent i = new Intent(this, ReviewEditActivity.class);
        i.putExtra(EXTRA_ID, id);
        startActivity(i);
        finish();
    }

    private void addToCalendar() {
        if (review.dueOn.isEmpty()) {
            toast(getString(R.string.no_deadline));
            return;
        }
        long start = DateUtil.toMillis(review.dueOn);
        Intent i = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 86_400_000L)
                .putExtra(CalendarContract.Events.TITLE,
                        getString(R.string.f_due_on) + ": " + review.displayTitle())
                .putExtra(CalendarContract.Events.DESCRIPTION, review.journalName);
        if (i.resolveActivity(getPackageManager()) != null) startActivity(i);
        else toast(getString(R.string.action_add_calendar));
    }

    private void confirmDelete() {
        if (review.id == 0) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    repo.delete(review.id);
                    toast(getString(R.string.deleted));
                    finish();
                })
                .show();
    }

    // -------------------------------------------------------------- saving

    /** Pulls the form into the model. False when something required is missing. */
    private boolean collect() {
        review.journalName = text(journal);
        review.title = text(title);
        review.manuscriptId = text(manuscriptId);
        review.authors = text(authors);
        review.editor = text(editor);
        review.tags = text(tags);
        review.notes = text(notes);
        review.editorNotes = text(editorNotes);

        String rd = text(round);
        review.round = rd.isEmpty() ? 1 : Math.max(1, safeInt(rd));
        String h = text(hours);
        review.hours = h.isEmpty() ? 0 : safeDouble(h);

        if (review.journalName.isEmpty() && review.title.isEmpty()
                && review.manuscriptId.isEmpty()) {
            toast(getString(R.string.need_journal));
            return false;
        }
        return true;
    }

    private void saveAndFinish() {
        if (!collect()) return;
        repo.save(review);
        ReminderScheduler.schedule(this);
        toast(getString(R.string.saved));
        finish();
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
    }

    private String text(android.widget.EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private int safeInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 1;
        }
    }

    private double safeDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
