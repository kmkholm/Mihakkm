package com.drtawfik.mihakk.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.data.Repo;
import com.drtawfik.mihakk.data.Review;
import com.drtawfik.mihakk.ui.LockActivity;
import com.drtawfik.mihakk.util.LocaleUtil;

import java.util.ArrayList;
import java.util.List;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_DAILY = "com.drtawfik.mihakk.DAILY_CHECK";
    private static final String CHANNEL = "deadlines";
    private static final int NOTIF_ID = 7701;

    @Override
    public void onReceive(Context rawCtx, Intent intent) {
        // Notification text is user-visible, so honour the in-app language.
        Context ctx = LocaleUtil.wrap(rawCtx);

        List<String> overdue = new ArrayList<>();
        List<String> soon = new ArrayList<>();

        try {
            for (Review r : new Repo(ctx).withDeadlines()) {
                int d = r.daysLeft();
                if (d == Integer.MAX_VALUE) continue;
                if (d < 0) overdue.add(ctx.getString(R.string.notif_overdue_line,
                        r.displayTitle(), -d));
                else if (d <= Math.max(0, r.reminderDays))
                    soon.add(ctx.getString(R.string.notif_soon_line, r.displayTitle(), d));
            }
        } catch (Exception ignored) {
        }

        if (!overdue.isEmpty() || !soon.isEmpty()) notify(ctx, rawCtx, overdue, soon);

        // Chain tomorrow's check.
        ReminderScheduler.schedule(rawCtx);
    }

    private void notify(Context ctx, Context rawCtx, List<String> overdue, List<String> soon) {
        NotificationManager nm =
                (NotificationManager) rawCtx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL,
                    ctx.getString(R.string.channel_deadlines), NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription(ctx.getString(R.string.channel_deadlines_desc));
            nm.createNotificationChannel(ch);
        }

        String title = !overdue.isEmpty()
                ? ctx.getResources().getQuantityString(R.plurals.notif_overdue_title,
                overdue.size(), overdue.size())
                : ctx.getResources().getQuantityString(R.plurals.notif_soon_title,
                soon.size(), soon.size());

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        for (String s : overdue) style.addLine(s);
        for (String s : soon) style.addLine(s);

        PendingIntent pi = PendingIntent.getActivity(rawCtx, 0,
                new Intent(rawCtx, LockActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(rawCtx, CHANNEL)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(overdue.isEmpty()
                        ? (soon.isEmpty() ? "" : soon.get(0)) : overdue.get(0))
                .setStyle(style)
                .setAutoCancel(true)
                .setContentIntent(pi);

        try {
            NotificationManagerCompat.from(rawCtx).notify(NOTIF_ID, b.build());
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS not granted — nothing to do.
        }
    }
}
