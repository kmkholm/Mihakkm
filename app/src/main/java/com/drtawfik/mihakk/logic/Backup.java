package com.drtawfik.mihakk.logic;

import android.content.Context;

import com.drtawfik.mihakk.data.Journal;
import com.drtawfik.mihakk.data.Prefs;
import com.drtawfik.mihakk.data.Repo;
import com.drtawfik.mihakk.data.Review;
import com.drtawfik.mihakk.data.Template;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Plain-JSON export of everything the app holds, so the record outlives the app.
 * Credentials and the app-lock PIN are deliberately left out of the file.
 */
public final class Backup {

    public static final int FORMAT = 1;

    private Backup() {
    }

    public static String export(Context ctx) throws Exception {
        Repo repo = new Repo(ctx);
        JSONObject root = new JSONObject();
        root.put("format", FORMAT);
        root.put("app", "Mihakk");
        root.put("exported_at", System.currentTimeMillis());
        root.put("reviewer_name", Prefs.get(ctx, Prefs.REVIEWER_NAME, ""));
        root.put("orcid_id", Prefs.get(ctx, Prefs.ORCID_ID, ""));

        JSONArray revs = new JSONArray();
        for (Review r : repo.all()) revs.put(toJson(r));
        root.put("reviews", revs);

        JSONArray js = new JSONArray();
        for (Journal j : repo.journals()) {
            JSONObject o = new JSONObject();
            o.put("name", j.name);
            o.put("publisher", j.publisher);
            o.put("issn", j.issn);
            o.put("quartile", j.quartile);
            o.put("impact_factor", j.impactFactor);
            o.put("indexing", j.indexing);
            o.put("notes", j.notes);
            js.put(o);
        }
        root.put("journals", js);

        JSONArray ts = new JSONArray();
        for (Template t : repo.userTemplates()) {
            JSONObject o = new JSONObject();
            o.put("key", t.key);
            o.put("name", t.nameEn);
            o.put("lang", t.lang);
            o.put("scope", t.scope);
            o.put("recommendation", t.recommendation);
            o.put("body", t.body);
            ts.put(o);
        }
        root.put("templates", ts);
        return root.toString(2);
    }

    public static class RestoreResult {
        public int reviews;
        public int journals;
        public int templates;
    }

    /** Merges into the current store; ORCID rows dedupe on put-code. */
    public static RestoreResult restore(Context ctx, String json) throws Exception {
        JSONObject root = new JSONObject(OrcidParser.clean(json));
        Repo repo = new Repo(ctx);
        RestoreResult res = new RestoreResult();

        JSONArray js = root.optJSONArray("journals");
        if (js != null) {
            for (int i = 0; i < js.length(); i++) {
                JSONObject o = js.optJSONObject(i);
                if (o == null) continue;
                Journal j = new Journal();
                j.name = o.optString("name");
                if (j.name.isEmpty()) continue;
                Journal exist = repo.journalByName(j.name);
                if (exist != null) j.id = exist.id;
                j.publisher = o.optString("publisher");
                j.issn = o.optString("issn");
                j.quartile = o.optString("quartile");
                j.impactFactor = o.optDouble("impact_factor", 0);
                j.indexing = o.optString("indexing");
                j.notes = o.optString("notes");
                repo.saveJournal(j);
                res.journals++;
            }
        }

        JSONArray revs = root.optJSONArray("reviews");
        if (revs != null) {
            java.util.Map<String, Long> byPut = repo.orcidIndex();
            for (int i = 0; i < revs.length(); i++) {
                JSONObject o = revs.optJSONObject(i);
                if (o == null) continue;
                Review r = fromJson(o);
                if (!r.orcidPutCode.isEmpty() && byPut.containsKey(r.orcidPutCode)) {
                    r.id = byPut.get(r.orcidPutCode);
                }
                repo.save(r);
                res.reviews++;
            }
        }

        JSONArray ts = root.optJSONArray("templates");
        if (ts != null) {
            for (int i = 0; i < ts.length(); i++) {
                JSONObject o = ts.optJSONObject(i);
                if (o == null) continue;
                repo.saveUserTemplate(o.optString("key"), o.optString("name"), o.optString("lang"),
                        o.optString("scope"), o.optString("recommendation"), o.optString("body"));
                res.templates++;
            }
        }
        return res;
    }

    private static JSONObject toJson(Review r) throws Exception {
        JSONObject o = new JSONObject();
        o.put("journal_name", r.journalName);
        o.put("manuscript_id", r.manuscriptId);
        o.put("title", r.title);
        o.put("authors", r.authors);
        o.put("editor", r.editor);
        o.put("round", r.round);
        o.put("study_type", r.studyType);
        o.put("status", r.status);
        o.put("recommendation", r.recommendation);
        o.put("invited_on", r.invitedOn);
        o.put("responded_on", r.respondedOn);
        o.put("due_on", r.dueOn);
        o.put("submitted_on", r.submittedOn);
        o.put("hours", r.hours);
        o.put("checklist_key", r.checklistKey);
        o.put("checklist_state", r.checklistState);
        o.put("report_text", r.reportText);
        o.put("editor_notes", r.editorNotes);
        o.put("notes", r.notes);
        o.put("tags", r.tags);
        o.put("orcid_put_code", r.orcidPutCode);
        o.put("verified", r.verified);
        o.put("source", r.source);
        o.put("reminder_days", r.reminderDays);
        return o;
    }

    private static Review fromJson(JSONObject o) {
        Review r = new Review();
        r.journalName = o.optString("journal_name");
        r.manuscriptId = o.optString("manuscript_id");
        r.title = o.optString("title");
        r.authors = o.optString("authors");
        r.editor = o.optString("editor");
        r.round = o.optInt("round", 1);
        r.studyType = o.optString("study_type");
        r.status = o.optString("status", Review.S_INVITED);
        r.recommendation = o.optString("recommendation");
        r.invitedOn = o.optString("invited_on");
        r.respondedOn = o.optString("responded_on");
        r.dueOn = o.optString("due_on");
        r.submittedOn = o.optString("submitted_on");
        r.hours = o.optDouble("hours", 0);
        r.checklistKey = o.optString("checklist_key");
        r.checklistState = o.optString("checklist_state");
        r.reportText = o.optString("report_text");
        r.editorNotes = o.optString("editor_notes");
        r.notes = o.optString("notes");
        r.tags = o.optString("tags");
        r.orcidPutCode = o.optString("orcid_put_code");
        r.verified = o.optBoolean("verified", false);
        r.source = o.optString("source", "manual");
        r.reminderDays = o.optInt("reminder_days", 3);
        return r;
    }
}
