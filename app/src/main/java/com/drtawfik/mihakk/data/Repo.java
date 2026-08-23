package com.drtawfik.mihakk.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.drtawfik.mihakk.util.DateUtil;

import java.util.ArrayList;
import java.util.List;

public final class Repo {

    private final SQLiteDatabase db;

    public Repo(Context ctx) {
        this.db = Db.get(ctx).getWritableDatabase();
    }

    // ================================================================ reviews

    public long save(Review r) {
        ContentValues v = new ContentValues();
        v.put("journal_id", r.journalId);
        v.put("journal_name", r.journalName);
        v.put("manuscript_id", r.manuscriptId);
        v.put("title", r.title);
        v.put("authors", r.authors);
        v.put("editor", r.editor);
        v.put("round", r.round);
        v.put("parent_id", r.parentId);
        v.put("study_type", r.studyType);
        v.put("status", r.status);
        v.put("recommendation", r.recommendation);
        v.put("invited_on", r.invitedOn);
        v.put("responded_on", r.respondedOn);
        v.put("due_on", r.dueOn);
        v.put("submitted_on", r.submittedOn);
        v.put("hours", r.hours);
        v.put("checklist_key", r.checklistKey);
        v.put("checklist_state", r.checklistState);
        v.put("report_text", r.reportText);
        v.put("editor_notes", r.editorNotes);
        v.put("notes", r.notes);
        v.put("tags", r.tags);
        v.put("orcid_put_code", TextUtils.isEmpty(r.orcidPutCode) ? null : r.orcidPutCode);
        v.put("verified", r.verified ? 1 : 0);
        v.put("source", r.source);
        v.put("reminder_days", r.reminderDays);
        v.put("updated_at", System.currentTimeMillis());

        if (r.id > 0) {
            db.update(Db.T_REVIEWS, v, "_id=?", new String[]{String.valueOf(r.id)});
        } else {
            v.put("created_at", System.currentTimeMillis());
            r.id = db.insert(Db.T_REVIEWS, null, v);
        }
        if (!TextUtils.isEmpty(r.journalName)) touchJournal(r.journalName);
        return r.id;
    }

    public void delete(long id) {
        db.delete(Db.T_REVIEWS, "_id=?", new String[]{String.valueOf(id)});
    }

    public Review byId(long id) {
        try (Cursor c = db.query(Db.T_REVIEWS, null, "_id=?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? Review.from(c) : null;
        }
    }

    /**
     * Open work, ordered so that the most urgent thing sits at the top:
     * overdue first, then nearest deadline, then undated.
     */
    public List<Review> openWork() {
        return raw("SELECT * FROM " + Db.T_REVIEWS + " WHERE " + Review.OPEN_SQL
                + " ORDER BY (due_on IS NULL OR due_on='') ASC, due_on ASC, _id DESC", null);
    }

    public List<Review> search(String statusFilter, String text, String year) {
        StringBuilder w = new StringBuilder("1=1");
        List<String> args = new ArrayList<>();

        if (!TextUtils.isEmpty(statusFilter)) {
            if ("open".equals(statusFilter)) w.append(" AND ").append(Review.OPEN_SQL);
            else if ("done".equals(statusFilter)) w.append(" AND ").append(Review.DONE_SQL);
            else {
                w.append(" AND status=?");
                args.add(statusFilter);
            }
        }
        if (!TextUtils.isEmpty(text)) {
            w.append(" AND (title LIKE ? OR journal_name LIKE ? OR manuscript_id LIKE ?"
                    + " OR authors LIKE ? OR editor LIKE ? OR tags LIKE ?)");
            String like = "%" + text + "%";
            for (int i = 0; i < 6; i++) args.add(like);
        }
        if (!TextUtils.isEmpty(year)) {
            w.append(" AND (substr(COALESCE(NULLIF(submitted_on,''),invited_on),1,4)=?)");
            args.add(year);
        }
        return raw("SELECT * FROM " + Db.T_REVIEWS + " WHERE " + w
                        + " ORDER BY COALESCE(NULLIF(submitted_on,''), NULLIF(due_on,''), invited_on) DESC, _id DESC",
                args.toArray(new String[0]));
    }

    /** Every open review with a deadline, for the reminder scheduler. */
    public List<Review> withDeadlines() {
        return raw("SELECT * FROM " + Db.T_REVIEWS + " WHERE " + Review.OPEN_SQL
                + " AND due_on IS NOT NULL AND due_on<>'' ORDER BY due_on ASC", null);
    }

    public List<Review> all() {
        return raw("SELECT * FROM " + Db.T_REVIEWS + " ORDER BY _id ASC", null);
    }

    private List<Review> raw(String sql, String[] args) {
        List<Review> out = new ArrayList<>();
        try (Cursor c = db.rawQuery(sql, args)) {
            while (c.moveToNext()) out.add(Review.from(c));
        }
        return out;
    }

    // -------------------------------------------------- dashboard buckets

    public static class Board {
        public final List<Review> overdue = new ArrayList<>();
        public final List<Review> dueSoon = new ArrayList<>();   // within 7 days
        public final List<Review> later = new ArrayList<>();
        public final List<Review> undecided = new ArrayList<>(); // invited, not answered
        public int openCount;
    }

    public Board board() {
        Board b = new Board();
        for (Review r : openWork()) {
            b.openCount++;
            if (Review.S_INVITED.equals(r.status)) {
                b.undecided.add(r);
                continue;
            }
            int d = r.daysLeft();
            if (d == Integer.MAX_VALUE) b.later.add(r);
            else if (d < 0) b.overdue.add(r);
            else if (d <= 7) b.dueSoon.add(r);
            else b.later.add(r);
        }
        return b;
    }

    // =============================================================== journals

    /** Creates the journal row on first sight so the registry fills itself in. */
    public long touchJournal(String name) {
        if (TextUtils.isEmpty(name)) return 0;
        try (Cursor c = db.query(Db.T_JOURNALS, new String[]{"_id"}, "name=? COLLATE NOCASE",
                new String[]{name}, null, null, null)) {
            if (c.moveToFirst()) return c.getLong(0);
        }
        ContentValues v = new ContentValues();
        v.put("name", name);
        return db.insert(Db.T_JOURNALS, null, v);
    }

    public long saveJournal(Journal j) {
        ContentValues v = new ContentValues();
        v.put("name", j.name);
        v.put("publisher", j.publisher);
        v.put("issn", j.issn);
        v.put("quartile", j.quartile);
        v.put("impact_factor", j.impactFactor);
        v.put("indexing", j.indexing);
        v.put("notes", j.notes);
        if (j.id > 0) {
            db.update(Db.T_JOURNALS, v, "_id=?", new String[]{String.valueOf(j.id)});
        } else {
            j.id = db.insertWithOnConflict(Db.T_JOURNALS, null, v, SQLiteDatabase.CONFLICT_IGNORE);
        }
        return j.id;
    }

    public void deleteJournal(long id) {
        db.delete(Db.T_JOURNALS, "_id=?", new String[]{String.valueOf(id)});
    }

    public List<Journal> journals() {
        List<Journal> out = new ArrayList<>();
        String sql = "SELECT j.*, (SELECT COUNT(*) FROM " + Db.T_REVIEWS + " r"
                + " WHERE r.journal_name = j.name COLLATE NOCASE) AS review_count"
                + " FROM " + Db.T_JOURNALS + " j ORDER BY review_count DESC, j.name ASC";
        try (Cursor c = db.rawQuery(sql, null)) {
            while (c.moveToNext()) out.add(Journal.from(c));
        }
        return out;
    }

    public Journal journalByName(String name) {
        if (TextUtils.isEmpty(name)) return null;
        try (Cursor c = db.query(Db.T_JOURNALS, null, "name=? COLLATE NOCASE",
                new String[]{name}, null, null, null)) {
            return c.moveToFirst() ? Journal.from(c) : null;
        }
    }

    public List<String> journalNames() {
        List<String> out = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT name FROM " + Db.T_JOURNALS + " ORDER BY name", null)) {
            while (c.moveToNext()) out.add(c.getString(0));
        }
        return out;
    }

    // ============================================================== templates

    public void saveUserTemplate(String key, String name, String lang, String scope,
                                 String recommendation, String body) {
        ContentValues v = new ContentValues();
        v.put("key", key);
        v.put("name", name);
        v.put("lang", lang);
        v.put("scope", scope);
        v.put("recommendation", recommendation);
        v.put("body", body);
        v.put("updated_at", System.currentTimeMillis());
        db.insertWithOnConflict(Db.T_TEMPLATES, null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void deleteUserTemplate(String key) {
        db.delete(Db.T_TEMPLATES, "key=?", new String[]{key});
    }

    public List<Template> userTemplates() {
        List<Template> out = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT * FROM " + Db.T_TEMPLATES + " ORDER BY name", null)) {
            while (c.moveToNext()) {
                Template t = new Template();
                t.key = c.getString(c.getColumnIndexOrThrow("key"));
                t.nameEn = t.nameAr = c.getString(c.getColumnIndexOrThrow("name"));
                t.lang = c.getString(c.getColumnIndexOrThrow("lang"));
                t.scope = c.getString(c.getColumnIndexOrThrow("scope"));
                t.recommendation = c.getString(c.getColumnIndexOrThrow("recommendation"));
                t.body = c.getString(c.getColumnIndexOrThrow("body"));
                t.builtin = false;
                out.add(t);
            }
        }
        return out;
    }

    // ================================================================= counts

    public int count(String where) {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + Db.T_REVIEWS
                + (where == null ? "" : " WHERE " + where), null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public int countDoneThisYear() {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + Db.T_REVIEWS + " WHERE "
                        + Review.DONE_SQL + " AND substr(COALESCE(NULLIF(submitted_on,''),invited_on),1,4)=?",
                new String[]{DateUtil.thisYear()})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public int countVerified() {
        return count("verified=1");
    }

    /** put_code -> row id, so an ORCID re-import updates instead of duplicating. */
    public java.util.Map<String, Long> orcidIndex() {
        java.util.Map<String, Long> m = new java.util.HashMap<>();
        try (Cursor c = db.rawQuery("SELECT _id, orcid_put_code FROM " + Db.T_REVIEWS
                + " WHERE orcid_put_code IS NOT NULL AND orcid_put_code<>''", null)) {
            while (c.moveToNext()) m.put(c.getString(1), c.getLong(0));
        }
        return m;
    }

    public SQLiteDatabase raw() {
        return db;
    }
}
