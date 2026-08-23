package com.drtawfik.mihakk.data;

import org.json.JSONObject;

/** A review-report skeleton from {@code assets/templates.json} or written by the user. */
public class Template {

    public static final String SCOPE_AUTHORS = "to_authors";
    public static final String SCOPE_EDITOR = "to_editor";
    public static final String SCOPE_DECLINE = "decline";

    public String key = "";
    public String nameEn = "";
    public String nameAr = "";
    public String lang = "en";
    public String scope = SCOPE_AUTHORS;
    public String recommendation = "";
    public String descEn = "";
    public String descAr = "";
    public String body = "";
    public boolean builtin = true;

    public String name(boolean ar) {
        return ar && !nameAr.isEmpty() ? nameAr : nameEn;
    }

    public String desc(boolean ar) {
        return ar && !descAr.isEmpty() ? descAr : descEn;
    }

    static Template parse(JSONObject o) {
        Template t = new Template();
        t.key = o.optString("key");
        t.nameEn = o.optString("name_en");
        t.nameAr = o.optString("name_ar");
        t.lang = o.optString("lang", "en");
        t.scope = o.optString("scope", SCOPE_AUTHORS);
        t.recommendation = o.optString("recommendation", "");
        t.descEn = o.optString("description_en");
        t.descAr = o.optString("description_ar");
        t.body = o.optString("body");
        return t;
    }
}
