package com.drtawfik.mihakk.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * All dates in the store are plain ISO {@code yyyy-MM-dd} strings: they sort
 * lexicographically, group by year with a substring, and never shift under a
 * timezone change — which matters for a deadline tracker.
 */
public final class DateUtil {

    private DateUtil() {
    }

    private static SimpleDateFormat iso() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(TimeZone.getDefault());
        return f;
    }

    public static String today() {
        return iso().format(new Date());
    }

    public static String fromMillis(long millis) {
        return iso().format(new Date(millis));
    }

    /** Midnight local time on the given ISO date, or 0 when unparseable. */
    public static long toMillis(String isoDate) {
        if (isoDate == null || isoDate.length() < 10) return 0L;
        try {
            Calendar c = Calendar.getInstance();
            c.setTime(iso().parse(isoDate));
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTimeInMillis();
        } catch (Exception e) {
            return 0L;
        }
    }

    /** {@code to - from} in whole days. Positive when {@code to} is later. */
    public static int daysBetween(String from, String to) {
        long a = toMillis(from), b = toMillis(to);
        if (a == 0 || b == 0) return 0;
        return (int) Math.round((b - a) / 86_400_000.0);
    }

    public static String plusDays(String isoDate, int days) {
        long m = toMillis(isoDate);
        if (m == 0) return "";
        return fromMillis(m + days * 86_400_000L);
    }

    public static String isoYear(String isoDate) {
        return isoDate != null && isoDate.length() >= 4 ? isoDate.substring(0, 4) : "";
    }

    public static String thisYear() {
        return today().substring(0, 4);
    }

    /** Locale-aware medium date for display; falls back to the raw string. */
    public static String pretty(String isoDate, Locale locale) {
        long m = toMillis(isoDate);
        if (m == 0) return isoDate == null ? "" : isoDate;
        return new SimpleDateFormat("d MMM yyyy", locale).format(new Date(m));
    }
}
