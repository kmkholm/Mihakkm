package com.drtawfik.mihakk.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {

    private static final String FILE = "mihakk_prefs";

    public static final String REVIEWER_NAME = "reviewer_name";
    public static final String REVIEWER_EMAIL = "reviewer_email";
    public static final String AFFILIATION = "affiliation";
    public static final String ORCID_ID = "orcid_id";
    public static final String ORCID_CLIENT_ID = "orcid_client_id";
    public static final String ORCID_CLIENT_SECRET = "orcid_client_secret";
    public static final String ORCID_LAST_SYNC = "orcid_last_sync";
    public static final String LANG = "lang";                 // "" = follow system, "ar", "en"
    public static final String THEME = "theme";               // system | light | dark
    public static final String ACCENT = "accent";             // see ui.Accent
    public static final String LOCK_PIN = "lock_pin";         // salted hash, empty = off
    public static final String LOCK_SALT = "lock_salt";
    public static final String LOCK_BIOMETRIC = "lock_biometric";
    public static final String BLOCK_SHOTS = "block_shots";
    public static final String REMIND_HOUR = "remind_hour";   // 0-23
    public static final String DEFAULT_DUE_DAYS = "default_due_days";
    public static final String LAST_TEMPLATE = "last_template";

    private Prefs() {
    }

    public static SharedPreferences of(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String get(Context c, String key, String def) {
        return of(c).getString(key, def);
    }

    public static void set(Context c, String key, String value) {
        of(c).edit().putString(key, value).apply();
    }

    public static int getInt(Context c, String key, int def) {
        return of(c).getInt(key, def);
    }

    public static void setInt(Context c, String key, int value) {
        of(c).edit().putInt(key, value).apply();
    }

    public static boolean getBool(Context c, String key, boolean def) {
        return of(c).getBoolean(key, def);
    }

    public static void setBool(Context c, String key, boolean value) {
        of(c).edit().putBoolean(key, value).apply();
    }

    public static boolean lockEnabled(Context c) {
        return !get(c, LOCK_PIN, "").isEmpty();
    }

    public static String reviewerName(Context c) {
        return get(c, REVIEWER_NAME, "");
    }
}
