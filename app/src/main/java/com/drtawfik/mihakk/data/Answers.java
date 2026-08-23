package com.drtawfik.mihakk.data;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** The reviewer's verdict on each checklist item, serialised into {@code reviews.checklist_state}. */
public class Answers {

    public static final String V_OK = "ok";
    public static final String V_CONCERN = "concern";
    public static final String V_FAIL = "fail";
    public static final String V_NA = "na";

    public static class Entry {
        public String verdict = "";
        public String note = "";

        public boolean isProblem() {
            return V_CONCERN.equals(verdict) || V_FAIL.equals(verdict);
        }

        public boolean isAnswered() {
            return verdict != null && !verdict.isEmpty();
        }
    }

    private final Map<String, Entry> map = new LinkedHashMap<>();

    public static Answers parse(String json) {
        Answers a = new Answers();
        if (json == null || json.trim().isEmpty()) return a;
        try {
            JSONObject o = new JSONObject(json);
            for (Iterator<String> it = o.keys(); it.hasNext(); ) {
                String k = it.next();
                JSONObject e = o.optJSONObject(k);
                if (e == null) continue;
                Entry en = new Entry();
                en.verdict = e.optString("v", "");
                en.note = e.optString("note", "");
                a.map.put(k, en);
            }
        } catch (Exception ignored) {
        }
        return a;
    }

    public String toJson() {
        JSONObject o = new JSONObject();
        try {
            for (Map.Entry<String, Entry> e : map.entrySet()) {
                Entry v = e.getValue();
                if (!v.isAnswered() && v.note.isEmpty()) continue;
                JSONObject j = new JSONObject();
                j.put("v", v.verdict);
                j.put("note", v.note);
                o.put(e.getKey(), j);
            }
        } catch (Exception ignored) {
        }
        return o.toString();
    }

    public Entry get(String id) {
        Entry e = map.get(id);
        if (e == null) {
            e = new Entry();
            map.put(id, e);
        }
        return e;
    }

    public boolean has(String id) {
        Entry e = map.get(id);
        return e != null && e.isAnswered();
    }

    public void setVerdict(String id, String verdict) {
        get(id).verdict = verdict;
    }

    public void setNote(String id, String note) {
        get(id).note = note == null ? "" : note;
    }

    public int answeredCount() {
        int n = 0;
        for (Entry e : map.values()) if (e.isAnswered()) n++;
        return n;
    }

    public int problemCount() {
        int n = 0;
        for (Entry e : map.values()) if (e.isProblem()) n++;
        return n;
    }
}
