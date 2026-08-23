package com.drtawfik.mihakk.data;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The built-in expert content — appraisal checklists and report templates —
 * read once from assets and kept in memory. Read-only; user edits live in the DB.
 */
public final class Content {

    private static final String TAG = "MihakkContent";

    private static List<Checklist> checklists;
    private static List<Template> templates;

    private Content() {
    }

    public static synchronized List<Checklist> checklists(Context ctx) {
        if (checklists == null) {
            checklists = new ArrayList<>();
            JSONObject root = readJson(ctx, "checklists.json");
            if (root != null) {
                JSONArray arr = root.optJSONArray("checklists");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o != null) checklists.add(Checklist.parse(o));
                    }
                }
            }
        }
        return checklists;
    }

    public static Checklist checklist(Context ctx, String key) {
        if (key == null || key.isEmpty()) return null;
        for (Checklist c : checklists(ctx)) if (key.equals(c.key)) return c;
        return null;
    }

    /** Built-in templates plus anything the user saved, built-ins first. */
    public static synchronized List<Template> templates(Context ctx) {
        if (templates == null) {
            templates = new ArrayList<>();
            JSONObject root = readJson(ctx, "templates.json");
            if (root != null) {
                JSONArray arr = root.optJSONArray("templates");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o != null) templates.add(Template.parse(o));
                    }
                }
            }
        }
        List<Template> all = new ArrayList<>(templates);
        all.addAll(new Repo(ctx).userTemplates());
        return all;
    }

    public static Template template(Context ctx, String key) {
        if (key == null || key.isEmpty()) return null;
        for (Template t : templates(ctx)) if (key.equals(t.key)) return t;
        return null;
    }

    /** Templates filtered by scope, ordered so the UI language comes first. */
    public static List<Template> templatesFor(Context ctx, String scope, final boolean arabicFirst) {
        List<Template> out = new ArrayList<>();
        for (Template t : templates(ctx)) if (scope == null || scope.equals(t.scope)) out.add(t);
        Collections.sort(out, (a, b) -> {
            int ra = rank(a, arabicFirst), rb = rank(b, arabicFirst);
            return ra != rb ? Integer.compare(ra, rb) : a.key.compareTo(b.key);
        });
        return out;
    }

    private static int rank(Template t, boolean arabicFirst) {
        boolean isAr = "ar".equals(t.lang);
        return (isAr == arabicFirst) ? 0 : 1;
    }

    private static JSONObject readJson(Context ctx, String asset) {
        try (InputStream in = ctx.getAssets().open(asset)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return new JSONObject(new String(bos.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e(TAG, "cannot read asset " + asset, e);
            return null;
        }
    }
}
