package com.drtawfik.mihakk.data;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * ISSN to journal title, read once from {@code assets/issn.json}.
 * <p>
 * ORCID peer-review records name the platform that deposited them — Clarivate,
 * Elsevier Editorial, Springer Nature — not the journal. Without this lookup a
 * whole career of reviewing collapses into three publisher names. The ISSN in
 * {@code review-group-id} is the only field that identifies the venue, so it is
 * what the import resolves against.
 */
public final class IssnDirectory {

    public static class Entry {
        public final String title;
        public final String publisher;

        Entry(String title, String publisher) {
            this.title = title;
            this.publisher = publisher;
        }
    }

    private static Map<String, Entry> map;

    private IssnDirectory() {
    }

    public static synchronized Entry lookup(Context ctx, String issn) {
        if (issn == null || issn.trim().isEmpty()) return null;
        load(ctx);
        return map.get(normalise(issn));
    }

    /** ISSNs are written with and without the hyphen, and the check digit may be X. */
    private static String normalise(String issn) {
        return issn.replace("-", "").replace(" ", "").toUpperCase(Locale.US);
    }

    private static void load(Context ctx) {
        if (map != null) return;
        map = new HashMap<>();
        try (InputStream in = ctx.getAssets().open("issn.json")) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            JSONObject root = new JSONObject(new String(bos.toByteArray(), StandardCharsets.UTF_8));
            JSONObject journals = root.optJSONObject("journals");
            if (journals == null) return;
            for (Iterator<String> it = journals.keys(); it.hasNext(); ) {
                String key = it.next();
                JSONObject o = journals.optJSONObject(key);
                if (o == null) continue;
                map.put(normalise(key),
                        new Entry(o.optString("title", ""), o.optString("publisher", "")));
            }
        } catch (Exception e) {
            Log.e("MihakkIssn", "cannot read issn.json", e);
        }
    }
}
