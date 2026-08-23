package com.drtawfik.mihakk.util;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import com.drtawfik.mihakk.data.Prefs;

import java.util.Locale;

/**
 * In-app language override.
 * <p>
 * Anything that formats a user-visible string must be built from a context that
 * has been through {@link #wrap} — a bare application context keeps the system
 * language and produces a half-translated screen.
 */
public final class LocaleUtil {

    private LocaleUtil() {
    }

    public static Context wrap(Context base) {
        String lang = Prefs.get(base, Prefs.LANG, "");
        if (lang == null || lang.isEmpty()) return base;

        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration cfg = new Configuration(base.getResources().getConfiguration());
        cfg.setLocale(locale);
        cfg.setLayoutDirection(locale);
        return base.createConfigurationContext(cfg);
    }

    public static boolean isArabic(Context ctx) {
        String lang = Prefs.get(ctx, Prefs.LANG, "");
        if (lang != null && !lang.isEmpty()) return "ar".equals(lang);
        return "ar".equals(currentLocale(ctx).getLanguage());
    }

    public static Locale currentLocale(Context ctx) {
        Configuration cfg = ctx.getResources().getConfiguration();
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                ? cfg.getLocales().get(0) : cfg.locale;
    }

    public static void applyTheme(Context ctx) {
        String theme = Prefs.get(ctx, Prefs.THEME, "system");
        switch (theme == null ? "system" : theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
