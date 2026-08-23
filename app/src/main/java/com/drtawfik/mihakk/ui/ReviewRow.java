package com.drtawfik.mihakk.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.data.Review;
import com.drtawfik.mihakk.logic.ReportBuilder;
import com.drtawfik.mihakk.util.DateUtil;
import com.drtawfik.mihakk.util.LocaleUtil;

/**
 * One review card. Shared by the Today screen (which inflates rows into a
 * LinearLayout) and the Reviews list (which recycles them), so a card looks the
 * same wherever it appears.
 */
public final class ReviewRow {

    private ReviewRow() {
    }

    public static View inflate(Context ctx, ViewGroup parent) {
        return LayoutInflater.from(ctx).inflate(R.layout.item_review, parent, false);
    }

    public static void bind(View v, Review r) {
        Context ctx = v.getContext();
        TextView title = v.findViewById(R.id.title);
        TextView subtitle = v.findViewById(R.id.subtitle);
        TextView status = v.findViewById(R.id.status);
        TextView due = v.findViewById(R.id.due);
        View accent = v.findViewById(R.id.accent);

        title.setText(r.displayTitle());
        subtitle.setText(subtitleOf(ctx, r));
        status.setText(ReportBuilder.statusLabel(ctx, r.status));

        int colour = ContextCompat.getColor(ctx, R.color.ink_muted);
        String dueText = "";

        if (r.isDone()) {
            dueText = r.submittedOn.isEmpty() ? ""
                    : DateUtil.pretty(r.submittedOn, LocaleUtil.currentLocale(ctx));
            colour = ContextCompat.getColor(ctx, R.color.ok);
        } else if (r.dueOn.isEmpty()) {
            dueText = ctx.getString(R.string.no_deadline);
        } else {
            int d = r.daysLeft();
            if (d < 0) {
                dueText = ctx.getString(R.string.days_overdue, -d);
                colour = ContextCompat.getColor(ctx, R.color.danger);
            } else if (d == 0) {
                dueText = ctx.getString(R.string.due_today);
                colour = ContextCompat.getColor(ctx, R.color.warn);
            } else {
                dueText = ctx.getString(R.string.days_left, d);
                if (d <= 7) colour = ContextCompat.getColor(ctx, R.color.warn);
            }
        }
        due.setText(dueText);
        due.setTextColor(colour);
        accent.setBackgroundColor(colour);
    }

    private static String subtitleOf(Context ctx, Review r) {
        StringBuilder sb = new StringBuilder();
        if (!r.journalName.isEmpty()) sb.append(r.journalName);
        if (!r.manuscriptId.isEmpty() && !r.manuscriptId.equals(r.displayTitle()))
            sb.append(sb.length() > 0 ? " · " : "").append(r.manuscriptId);
        if (r.round > 1)
            sb.append(sb.length() > 0 ? " · " : "").append(ctx.getString(R.string.round_n, r.round));
        if (r.verified)
            sb.append(sb.length() > 0 ? " · " : "").append(ctx.getString(R.string.verified_badge));
        return sb.toString();
    }
}
