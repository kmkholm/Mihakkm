package com.drtawfik.mihakk.ui;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.data.Prefs;

/**
 * The accent the whole app is drawn in.
 * <p>
 * Each entry is a full theme rather than a single tinted colour, so light and
 * dark each get a hue picked for their own background instead of one colour
 * washed out on white and glaring on black.
 */
public enum Accent {

    GOLD("gold", R.style.Theme_MihakkGold, R.color.a_gold, R.string.accent_gold),
    INDIGO("indigo", R.style.Theme_MihakkIndigo, R.color.a_indigo, R.string.accent_indigo),
    EMERALD("emerald", R.style.Theme_MihakkEmerald, R.color.a_emerald, R.string.accent_emerald),
    STEEL("steel", R.style.Theme_MihakkSteel, R.color.a_steel, R.string.accent_steel),
    MAROON("maroon", R.style.Theme_MihakkMaroon, R.color.a_maroon, R.string.accent_maroon),
    VIOLET("violet", R.style.Theme_MihakkViolet, R.color.a_violet, R.string.accent_violet),
    GRAPHITE("graphite", R.style.Theme_MihakkGraphite, R.color.a_graphite, R.string.accent_graphite);

    public final String key;
    public final int themeRes;
    public final int swatchRes;
    public final int labelRes;

    Accent(String key, int themeRes, int swatchRes, int labelRes) {
        this.key = key;
        this.themeRes = themeRes;
        this.swatchRes = swatchRes;
        this.labelRes = labelRes;
    }

    public static Accent current(Context ctx) {
        String want = Prefs.get(ctx, Prefs.ACCENT, GOLD.key);
        for (Accent a : values()) if (a.key.equals(want)) return a;
        return GOLD;
    }

    public static void set(Context ctx, Accent a) {
        Prefs.set(ctx, Prefs.ACCENT, a.key);
    }

    public int swatchColor(Context ctx) {
        return ContextCompat.getColor(ctx, swatchRes);
    }
}
