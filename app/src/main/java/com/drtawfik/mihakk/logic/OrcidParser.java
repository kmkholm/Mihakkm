package com.drtawfik.mihakk.logic;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads peer-review activity out of an ORCID record.
 * <p>
 * ORCID nests these differently depending on whether you exported the whole
 * record, called {@code /peer-reviews}, or called {@code /peer-review/{putCode}},
 * so rather than walking one fixed path this scans the tree for any object that
 * looks like a peer-review entry and pulls the fields out of it. That keeps the
 * import working across all three shapes and across future schema tweaks.
 */
public final class OrcidParser {

    public static class Entry {
        public String putCode = "";
        public String completionDate = "";   // ISO yyyy-MM-dd, may be partial-filled
        public String issn = "";
        public String organization = "";     // convening organisation, e.g. "IEEE"
        public String sourceName = "";       // who deposited the record
        public String subject = "";          // journal / container title when present
        public String reviewType = "";       // review | evaluation
        public String url = "";

        /**
         * Best available label for the journal column.
         * <p>
         * ORCID's {@code convening-organization} is usually the publisher — using
         * it would collapse every IEEE journal into one row — so the depositing
         * client's name is tried first, since for journal peer review that is
         * normally the venue itself. Aggregators that deposit on behalf of many
         * journals are skipped, because their name says nothing about the venue.
         */
        public String journalLabel() {
            if (!subject.isEmpty()) return subject;
            if (!sourceName.isEmpty() && !isAggregator(sourceName)) return sourceName;
            if (!organization.isEmpty()) return organization;
            if (!sourceName.isEmpty()) return sourceName;
            return issn.isEmpty() ? "" : issn;
        }
    }

    private static final String[] AGGREGATORS = {
            "crossref", "publons", "web of science", "clarivate", "orcid",
            "researcher", "scholarone", "editorial manager", "aries", "ex libris"
    };

    static boolean isAggregator(String name) {
        String n = name.toLowerCase(java.util.Locale.US);
        for (String a : AGGREGATORS) if (n.contains(a)) return true;
        return false;
    }

    private OrcidParser() {
    }

    public static List<Entry> parse(String json) throws Exception {
        List<Entry> out = new ArrayList<>();
        Object root = new org.json.JSONTokener(json).nextValue();
        walk(root, out);
        return out;
    }

    private static void walk(Object node, List<Entry> out) {
        if (node instanceof JSONArray) {
            JSONArray a = (JSONArray) node;
            for (int i = 0; i < a.length(); i++) walk(a.opt(i), out);
            return;
        }
        if (!(node instanceof JSONObject)) return;
        JSONObject o = (JSONObject) node;

        if (looksLikePeerReview(o)) {
            out.add(extract(o));
            // Peer-review objects do not nest another one inside themselves.
            return;
        }
        for (java.util.Iterator<String> it = o.keys(); it.hasNext(); ) {
            walk(o.opt(it.next()), out);
        }
    }

    private static boolean looksLikePeerReview(JSONObject o) {
        if (!o.has("put-code")) return false;
        return o.has("review-group-id") || o.has("completion-date")
                || o.has("convening-organization") || o.has("review-type");
    }

    private static Entry extract(JSONObject o) {
        Entry e = new Entry();
        e.putCode = o.opt("put-code") == null ? "" : String.valueOf(o.opt("put-code"));
        e.reviewType = o.optString("review-type", "");
        e.completionDate = date(o.optJSONObject("completion-date"));

        String groupId = o.optString("review-group-id", "");
        if (groupId.toLowerCase().startsWith("issn:")) e.issn = groupId.substring(5).trim();
        else if (!groupId.isEmpty()) e.issn = groupId;

        JSONObject org = o.optJSONObject("convening-organization");
        if (org != null) e.organization = org.optString("name", "");

        JSONObject src = o.optJSONObject("source");
        if (src != null) {
            JSONObject sn = src.optJSONObject("source-name");
            if (sn != null) e.sourceName = sn.optString("value", "");
        }

        // Full records carry the container (journal) title; summaries do not.
        JSONObject cont = o.optJSONObject("subject-container-name");
        if (cont != null) e.subject = value(cont.optJSONObject("title"), cont.optString("value", ""));
        if (e.subject.isEmpty()) {
            JSONObject sname = o.optJSONObject("subject-name");
            if (sname != null) e.subject = value(sname.optJSONObject("title"), "");
        }

        JSONObject url = o.optJSONObject("review-url");
        if (url != null) e.url = url.optString("value", "");
        return e;
    }

    private static String value(JSONObject holder, String fallback) {
        if (holder == null) return fallback;
        String v = holder.optString("value", "");
        return v.isEmpty() ? fallback : v;
    }

    /** ORCID dates are {year:{value},month:{value},day:{value}} with month/day optional. */
    private static String date(JSONObject d) {
        if (d == null) return "";
        String y = part(d, "year");
        if (y.isEmpty()) return "";
        String m = part(d, "month");
        String day = part(d, "day");
        if (m.isEmpty()) m = "01";
        if (day.isEmpty()) day = "01";
        return y + "-" + pad(m) + "-" + pad(day);
    }

    private static String part(JSONObject d, String key) {
        JSONObject p = d.optJSONObject(key);
        if (p != null) return p.optString("value", "");
        String direct = d.optString(key, "");
        return TextUtils.isEmpty(direct) || "null".equals(direct) ? "" : direct;
    }

    private static String pad(String v) {
        return v.length() == 1 ? "0" + v : v;
    }
}
