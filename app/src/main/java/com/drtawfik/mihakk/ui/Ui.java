package com.drtawfik.mihakk.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.drtawfik.mihakk.R;

/** Small view helpers so the screens can stay about behaviour rather than plumbing. */
public final class Ui {

    private Ui() {
    }

    public static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static int themeColor(Context c, int attr) {
        TypedArray a = c.obtainStyledAttributes(new int[]{attr});
        try {
            return a.getColor(0, 0);
        } finally {
            a.recycle();
        }
    }

    /** A rounded count chip for the Today strip. */
    public static TextView chip(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(12);
        t.setBackgroundResource(R.drawable.bg_pill);
        t.setTextColor(themeColor(c, com.google.android.material.R.attr.colorOnSecondaryContainer));
        t.setPadding(dp(c, 12), dp(c, 6), dp(c, 12), dp(c, 6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(c, 8));
        t.setLayoutParams(lp);
        return t;
    }

    /** A "42 / Reports delivered" tile used on the record screen. */
    public static View statTile(Context c, String value, String label) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.bg_stat);
        box.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        box.setGravity(Gravity.CENTER_VERTICAL);

        TextView v = new TextView(c);
        v.setText(value);
        v.setTextSize(22);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setTextColor(themeColor(c, com.google.android.material.R.attr.colorPrimary));
        box.addView(v);

        TextView l = new TextView(c);
        l.setText(label);
        l.setTextSize(12);
        l.setTextColor(themeColor(c, com.google.android.material.R.attr.colorOnSurfaceVariant));
        box.addView(l);
        return box;
    }

    /** Two tiles side by side. */
    public static View tileRow(Context c, View a, View b) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 8);
        row.setLayoutParams(lp);

        LinearLayout.LayoutParams half =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        half.setMarginEnd(dp(c, 8));
        a.setLayoutParams(half);
        row.addView(a);

        if (b != null) {
            LinearLayout.LayoutParams half2 =
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            b.setLayoutParams(half2);
            row.addView(b);
        } else {
            View spacer = new View(c);
            spacer.setLayoutParams(
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(spacer);
        }
        return row;
    }

    public static TextView sectionHeader(Context c, String text) {
        TextView t = new TextView(c, null, 0, R.style.SectionHeader);
        t.setText(text);
        t.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return t;
    }

    /** A "label ......... value" row with a proportional bar behind it. */
    public static View barRow(Context c, String label, int value, int max) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(c, 5), 0, dp(c, 5));

        TextView l = new TextView(c);
        l.setText(label);
        l.setTextSize(13);
        l.setMaxLines(1);
        l.setEllipsize(android.text.TextUtils.TruncateAt.END);
        l.setTextColor(themeColor(c, com.google.android.material.R.attr.colorOnSurface));
        l.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 3f));
        row.addView(l);

        View bar = new View(c);
        bar.setBackgroundColor(themeColor(c, com.google.android.material.R.attr.colorPrimary));
        float weight = max <= 0 ? 0f : Math.max(0.04f, (value * 2f) / max);
        LinearLayout.LayoutParams bp =
                new LinearLayout.LayoutParams(0, dp(c, 8), weight);
        bp.setMarginEnd(dp(c, 8));
        bp.setMarginStart(dp(c, 8));
        bar.setLayoutParams(bp);
        row.addView(bar);

        View pad = new View(c);
        pad.setLayoutParams(new LinearLayout.LayoutParams(0, dp(c, 8),
                Math.max(0.01f, 2f - (max <= 0 ? 0f : Math.max(0.04f, (value * 2f) / max)))));
        row.addView(pad);

        TextView v = new TextView(c);
        v.setText(String.valueOf(value));
        v.setTextSize(13);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setTextColor(themeColor(c, com.google.android.material.R.attr.colorPrimary));
        row.addView(v);
        return row;
    }
}
