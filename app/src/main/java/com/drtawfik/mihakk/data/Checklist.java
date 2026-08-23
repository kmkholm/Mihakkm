package com.drtawfik.mihakk.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** An appraisal checklist for one study type, loaded from {@code assets/checklists.json}. */
public class Checklist {

    public static final String W_CRITICAL = "critical";
    public static final String W_MAJOR = "major";
    public static final String W_MINOR = "minor";

    public String key = "";
    public String nameEn = "";
    public String nameAr = "";
    public String studyType = "";
    public String guideline = "";
    public String sourceUrl = "";
    public String summaryEn = "";
    public String summaryAr = "";
    public final List<Section> sections = new ArrayList<>();

    public static class Section {
        public String titleEn = "";
        public String titleAr = "";
        public final List<Item> items = new ArrayList<>();

        public String title(boolean ar) {
            return ar && !titleAr.isEmpty() ? titleAr : titleEn;
        }
    }

    public static class Item {
        public String id = "";
        public String textEn = "";
        public String textAr = "";
        public String hintEn = "";
        public String hintAr = "";
        public String weight = W_MAJOR;

        public String text(boolean ar) {
            return ar && !textAr.isEmpty() ? textAr : textEn;
        }

        public String hint(boolean ar) {
            return ar && !hintAr.isEmpty() ? hintAr : hintEn;
        }

        public boolean isMajor() {
            return W_CRITICAL.equals(weight) || W_MAJOR.equals(weight);
        }
    }

    public String name(boolean ar) {
        return ar && !nameAr.isEmpty() ? nameAr : nameEn;
    }

    public String summary(boolean ar) {
        return ar && !summaryAr.isEmpty() ? summaryAr : summaryEn;
    }

    public int itemCount() {
        int n = 0;
        for (Section s : sections) n += s.items.size();
        return n;
    }

    public Item item(String id) {
        for (Section s : sections)
            for (Item i : s.items)
                if (i.id.equals(id)) return i;
        return null;
    }

    static Checklist parse(JSONObject o) {
        Checklist c = new Checklist();
        c.key = o.optString("key");
        c.nameEn = o.optString("name_en");
        c.nameAr = o.optString("name_ar");
        c.studyType = o.optString("study_type");
        c.guideline = o.optString("guideline");
        c.sourceUrl = o.optString("source_url");
        c.summaryEn = o.optString("summary_en");
        c.summaryAr = o.optString("summary_ar");

        JSONArray secs = o.optJSONArray("sections");
        if (secs == null) return c;
        for (int i = 0; i < secs.length(); i++) {
            JSONObject so = secs.optJSONObject(i);
            if (so == null) continue;
            Section s = new Section();
            s.titleEn = so.optString("title_en");
            s.titleAr = so.optString("title_ar");
            JSONArray items = so.optJSONArray("items");
            if (items != null) {
                for (int j = 0; j < items.length(); j++) {
                    JSONObject io = items.optJSONObject(j);
                    if (io == null) continue;
                    Item it = new Item();
                    it.id = io.optString("id", c.key + "-" + i + "-" + j);
                    it.textEn = io.optString("text_en");
                    it.textAr = io.optString("text_ar");
                    it.hintEn = io.optString("hint_en");
                    it.hintAr = io.optString("hint_ar");
                    it.weight = io.optString("weight", W_MAJOR);
                    s.items.add(it);
                }
            }
            c.sections.add(s);
        }
        return c;
    }
}
