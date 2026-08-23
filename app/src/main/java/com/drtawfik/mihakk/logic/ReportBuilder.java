package com.drtawfik.mihakk.logic;

import android.content.Context;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.data.Answers;
import com.drtawfik.mihakk.data.Checklist;
import com.drtawfik.mihakk.data.Content;
import com.drtawfik.mihakk.data.Prefs;
import com.drtawfik.mihakk.data.Review;
import com.drtawfik.mihakk.data.Template;
import com.drtawfik.mihakk.util.DateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns a template plus a filled-in checklist into a draft report.
 * <p>
 * This is the loop the whole app exists for: answering the checklist is what
 * produces the numbered major/minor comments, so the appraisal work is not
 * done twice.
 */
public final class ReportBuilder {

    private ReportBuilder() {
    }

    public static String build(Context ctx, Template t, Review r) {
        if (t == null) return "";
        boolean ar = "ar".equals(t.lang);
        String body = t.body == null ? "" : t.body;

        Checklist cl = Content.checklist(ctx, r.checklistKey);
        Answers ans = Answers.parse(r.checklistState);

        Points p = collect(cl, ans, ar);

        body = sub(body, "manuscript_title", or(r.title, cue(ar, "manuscript title")));
        body = sub(body, "manuscript_id", or(r.manuscriptId, cue(ar, "ID")));
        body = sub(body, "journal", or(r.journalName, cue(ar, "journal")));
        body = sub(body, "authors", or(r.authors, cue(ar, "authors")));
        body = sub(body, "editor", or(r.editor, ar ? "رئيس التحرير" : "Editor"));
        body = sub(body, "date", DateUtil.pretty(DateUtil.today(), ar ? new Locale("ar") : Locale.ENGLISH));
        body = sub(body, "reviewer_name", or(Prefs.reviewerName(ctx), cue(ar, "your name")));
        body = sub(body, "round", String.valueOf(Math.max(1, r.round)));
        body = sub(body, "recommendation", recommendationLabel(ctx, r.recommendation));
        body = sub(body, "summary", cue(ar, ar ? "لخّص إسهام البحث في ٣-٥ أسطر" : "summarise the contribution in 3-5 lines"));
        body = sub(body, "strengths", p.strengths.isEmpty()
                ? cue(ar, ar ? "اذكر نقاط القوة" : "list the strengths")
                : numbered(p.strengths));
        body = sub(body, "major_points", p.major.isEmpty()
                ? cue(ar, ar ? "أدرج الملاحظات الجوهرية" : "list the major comments")
                : numbered(p.major));
        body = sub(body, "minor_points", p.minor.isEmpty()
                ? cue(ar, ar ? "أدرج الملاحظات الطفيفة" : "list the minor comments")
                : numbered(p.minor));
        body = sub(body, "references_note", cue(ar, ar
                ? "ملاحظة على حداثة المراجع وكفايتها"
                : "note on reference currency and coverage"));
        return body;
    }

    // ---------------------------------------------------------------- points

    private static class Points {
        final List<String> major = new ArrayList<>();
        final List<String> minor = new ArrayList<>();
        final List<String> strengths = new ArrayList<>();
    }

    private static Points collect(Checklist cl, Answers ans, boolean ar) {
        Points p = new Points();
        if (cl == null) return p;
        for (Checklist.Section s : cl.sections) {
            for (Checklist.Item it : s.items) {
                Answers.Entry e = ans.get(it.id);
                if (!e.isAnswered() && e.note.isEmpty()) continue;

                if (e.isProblem()) {
                    String text = !e.note.isEmpty() ? e.note : asConcern(it.text(ar), ar);
                    String line = "(" + s.title(ar) + ") " + text;
                    if (it.isMajor() || Answers.V_FAIL.equals(e.verdict)) p.major.add(line);
                    else p.minor.add(line);
                } else if (Answers.V_OK.equals(e.verdict) && !e.note.isEmpty()) {
                    p.strengths.add(e.note);
                }
            }
        }
        return p;
    }

    /**
     * Checklist items are questions put to the reviewer. With no note to use,
     * keep the question — asking the authors to clarify reads far better than a
     * machine-made statement, and in Arabic it avoids mangling the interrogative.
     */
    private static String asConcern(String question, boolean ar) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) return "";
        if (!q.endsWith("?") && !q.endsWith("؟")) q = q + (ar ? "؟" : "?");
        return (ar ? "يُرجى توضيح ما يلي: " : "Please clarify: ") + q;
    }

    private static String numbered(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append("\n\n");
            sb.append(i + 1).append(". ").append(items.get(i));
        }
        return sb.toString();
    }

    // ----------------------------------------------------------------- utils

    private static String sub(String body, String name, String value) {
        return body.replace("{{" + name + "}}", value == null ? "" : value);
    }

    private static String or(String v, String fallback) {
        return v == null || v.trim().isEmpty() ? fallback : v;
    }

    private static String cue(boolean ar, String what) {
        return "[" + what + "]";
    }

    public static String recommendationLabel(Context ctx, String code) {
        if (code == null) return "";
        switch (code) {
            case Review.R_ACCEPT:
                return ctx.getString(R.string.rec_accept);
            case Review.R_MINOR:
                return ctx.getString(R.string.rec_minor);
            case Review.R_MAJOR:
                return ctx.getString(R.string.rec_major);
            case Review.R_REJECT:
                return ctx.getString(R.string.rec_reject);
            case Review.R_RESUBMIT:
                return ctx.getString(R.string.rec_resubmit);
            default:
                return "";
        }
    }

    public static String statusLabel(Context ctx, String code) {
        if (code == null) return "";
        switch (code) {
            case Review.S_INVITED:
                return ctx.getString(R.string.st_invited);
            case Review.S_ACCEPTED:
                return ctx.getString(R.string.st_accepted);
            case Review.S_IN_PROGRESS:
                return ctx.getString(R.string.st_in_progress);
            case Review.S_SUBMITTED:
                return ctx.getString(R.string.st_submitted);
            case Review.S_COMPLETED:
                return ctx.getString(R.string.st_completed);
            case Review.S_DECLINED:
                return ctx.getString(R.string.st_declined);
            case Review.S_EXPIRED:
                return ctx.getString(R.string.st_expired);
            default:
                return code;
        }
    }
}
