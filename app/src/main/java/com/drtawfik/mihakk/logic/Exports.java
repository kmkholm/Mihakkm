package com.drtawfik.mihakk.logic;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.drtawfik.mihakk.data.Journal;
import com.drtawfik.mihakk.data.Repo;
import com.drtawfik.mihakk.data.Review;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class Exports {

    private Exports() {
    }

    // ==================================================================== CSV

    public static String csv(Repo repo, List<Review> reviews) {
        StringBuilder sb = new StringBuilder();
        sb.append("Journal,Publisher,Quartile,ManuscriptID,Title,Round,StudyType,Status,"
                + "Recommendation,Invited,Due,Submitted,TurnaroundDays,OnTime,Hours,Verified,Source,Tags\n");
        for (Review r : reviews) {
            Journal j = repo.journalByName(r.journalName);
            int t = r.turnaroundDays();
            row(sb,
                    r.journalName,
                    j == null ? "" : j.publisher,
                    j == null ? "" : j.quartile,
                    r.manuscriptId,
                    r.title,
                    String.valueOf(r.round),
                    r.studyType,
                    r.status,
                    r.recommendation,
                    r.invitedOn,
                    r.dueOn,
                    r.submittedOn,
                    t < 0 ? "" : String.valueOf(t),
                    r.submittedOn.isEmpty() || r.dueOn.isEmpty() ? "" : (r.onTime() ? "yes" : "no"),
                    r.hours > 0 ? String.valueOf(r.hours) : "",
                    r.verified ? "yes" : "no",
                    r.source,
                    r.tags);
        }
        return sb.toString();
    }

    private static void row(StringBuilder sb, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(quote(cells[i]));
        }
        sb.append('\n');
    }

    private static String quote(String s) {
        if (s == null) return "";
        String v = s.replace("\r", " ").replace("\n", " ");
        if (v.contains(",") || v.contains("\"")) return '"' + v.replace("\"", "\"\"") + '"';
        return v;
    }

    // ==================================================================== ICS

    /** Deadlines as all-day calendar events with an alarm N days before. */
    public static String ics(List<Review> reviews) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n")
                .append("VERSION:2.0\r\n")
                .append("PRODID:-//Dr. Mohammed Tawfik//Mihakk//EN\r\n")
                .append("CALSCALE:GREGORIAN\r\n");
        for (Review r : reviews) {
            if (r.dueOn == null || r.dueOn.isEmpty()) continue;
            String d = r.dueOn.replace("-", "");
            String end = com.drtawfik.mihakk.util.DateUtil.plusDays(r.dueOn, 1).replace("-", "");
            sb.append("BEGIN:VEVENT\r\n")
                    .append("UID:mihakk-").append(r.id).append("@drtawfik\r\n")
                    .append("DTSTART;VALUE=DATE:").append(d).append("\r\n")
                    .append("DTEND;VALUE=DATE:").append(end).append("\r\n")
                    .append("SUMMARY:").append(esc("Review due: " + r.displayTitle())).append("\r\n")
                    .append("DESCRIPTION:").append(esc(descOf(r))).append("\r\n")
                    .append("BEGIN:VALARM\r\n")
                    .append("TRIGGER:-P").append(Math.max(0, r.reminderDays)).append("D\r\n")
                    .append("ACTION:DISPLAY\r\n")
                    .append("DESCRIPTION:").append(esc("Review due soon")).append("\r\n")
                    .append("END:VALARM\r\n")
                    .append("END:VEVENT\r\n");
        }
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private static String descOf(Review r) {
        StringBuilder sb = new StringBuilder();
        if (!r.journalName.isEmpty()) sb.append(r.journalName);
        if (!r.manuscriptId.isEmpty()) sb.append(sb.length() > 0 ? " — " : "").append(r.manuscriptId);
        if (r.round > 1) sb.append(" (round ").append(r.round).append(')');
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
                .replace("\r", "").replace("\n", "\\n");
    }

    // ================================================================= shared

    /** Writes into the app's own external files dir and returns a shareable uri. */
    public static Uri writeShareable(Context ctx, String dir, String filename, String content)
            throws Exception {
        File folder = new File(ctx.getExternalFilesDir(null), dir);
        if (!folder.exists() && !folder.mkdirs()) throw new Exception("cannot create " + folder);
        File f = new File(folder, filename);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            // Excel needs the BOM to read UTF-8 Arabic correctly.
            if (filename.endsWith(".csv")) w.write('﻿');
            w.write(content);
        }
        return uriFor(ctx, f);
    }

    public static Uri uriFor(Context ctx, File f) {
        return FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".files", f);
    }

    public static void share(Context ctx, Uri uri, String mime, String subject) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType(mime);
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.putExtra(Intent.EXTRA_SUBJECT, subject);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ctx.startActivity(Intent.createChooser(i, subject));
    }
}
