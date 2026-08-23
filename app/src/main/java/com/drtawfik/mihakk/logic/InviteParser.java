package com.drtawfik.mihakk.logic;

import com.drtawfik.mihakk.util.DateUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls the useful fields out of a pasted review-invitation email.
 * <p>
 * Editorial systems (ScholarOne, Editorial Manager, MDPI, IEEE) all send
 * label-shaped mail — "Manuscript ID: …", "Title: …", "within 14 days" — so the
 * parser looks for labels first and only falls back to shape-matching. Nothing
 * here needs to be perfect: whatever it finds is shown for confirmation before
 * anything is saved.
 */
public final class InviteParser {

    public static class Parsed {
        public String journal = "";
        public String manuscriptId = "";
        public String title = "";
        public String authors = "";
        public String editor = "";
        public String dueOn = "";

        public boolean isEmpty() {
            return journal.isEmpty() && manuscriptId.isEmpty() && title.isEmpty();
        }

        public int foundCount() {
            int n = 0;
            if (!journal.isEmpty()) n++;
            if (!manuscriptId.isEmpty()) n++;
            if (!title.isEmpty()) n++;
            if (!dueOn.isEmpty()) n++;
            if (!editor.isEmpty()) n++;
            return n;
        }
    }

    // Labels seen across the major editorial systems, most specific first.
    private static final String[] ID_LABELS = {
            "manuscript id", "manuscript number", "manuscript no", "ms id", "ms. no",
            "submission id", "paper id", "article id", "manuscript reference",
    };
    private static final String[] TITLE_LABELS = {
            "manuscript title", "article title", "paper title", "title",
    };
    private static final String[] JOURNAL_LABELS = {
            "journal name", "journal", "publication",
    };
    private static final String[] EDITOR_LABELS = {
            "handling editor", "associate editor", "editor-in-chief", "guest editor", "editor",
    };
    private static final String[] AUTHOR_LABELS = {
            "authors", "author(s)", "corresponding author", "author",
    };
    private static final String[] DUE_LABELS = {
            "due date", "review due", "deadline", "due by", "response due", "reviews are due",
    };

    /** Editorial-system manuscript IDs: PREFIX-D-24-01234, Access-2026-41927, sensors-2938471. */
    private static final Pattern ID_SHAPE = Pattern.compile(
            "\\b([A-Za-z][A-Za-z&._-]{1,24}-(?:[A-Z]-)?\\d{2,4}-\\d{3,6})\\b");
    private static final Pattern ID_SHAPE_LOOSE = Pattern.compile(
            "\\b([A-Za-z]{3,20}-\\d{5,9})\\b");

    private static final Pattern QUOTED = Pattern.compile("[\"“«]([^\"”»]{15,300})[\"”»]");
    private static final Pattern ENTITLED = Pattern.compile(
            "(?:entitled|titled)\\s*[:\\-]?\\s*[\"“«]?([^\"”»\\n]{15,300})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern WITHIN_DAYS = Pattern.compile(
            "within\\s+(\\d{1,3})\\s*(?:calendar\\s*)?days", Pattern.CASE_INSENSITIVE);

    private InviteParser() {
    }

    public static Parsed parse(String raw, List<String> knownJournals) {
        Parsed p = new Parsed();
        if (raw == null || raw.trim().isEmpty()) return p;

        String text = raw.replace("\r", "");
        String[] lines = text.split("\n");

        p.manuscriptId = label(lines, ID_LABELS);
        p.title = label(lines, TITLE_LABELS);
        p.journal = label(lines, JOURNAL_LABELS);
        p.editor = label(lines, EDITOR_LABELS);
        p.authors = label(lines, AUTHOR_LABELS);

        String dueRaw = label(lines, DUE_LABELS);
        if (!dueRaw.isEmpty()) p.dueOn = parseDate(dueRaw);

        if (p.manuscriptId.isEmpty()) p.manuscriptId = shapeMatch(text);
        if (p.title.isEmpty()) p.title = quotedTitle(text);
        if (p.journal.isEmpty()) p.journal = knownJournal(text, knownJournals);
        if (p.dueOn.isEmpty()) p.dueOn = relativeDue(text);
        if (p.dueOn.isEmpty()) p.dueOn = anyDate(text);

        p.title = tidyTitle(p.title);
        p.editor = tidyPerson(p.editor);
        p.manuscriptId = p.manuscriptId.trim();
        p.journal = tidyJournal(p.journal);
        return p;
    }

    // ------------------------------------------------------------- labels

    /** Value after "Label:" — on the same line, or the next non-empty one. */
    private static String label(String[] lines, String[] labels) {
        for (String want : labels) {
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                int colon = line.indexOf(':');
                if (colon <= 0) continue;
                String head = line.substring(0, colon).trim().toLowerCase(Locale.US);
                head = head.replaceAll("^[\\*\\-\\u2022\\s]+", "");
                if (!head.equals(want)) continue;

                String value = line.substring(colon + 1).trim();
                if (value.isEmpty() && i + 1 < lines.length) value = lines[i + 1].trim();
                if (!value.isEmpty() && value.length() < 400) return value;
            }
        }
        return "";
    }

    // -------------------------------------------------------------- shapes

    private static String shapeMatch(String text) {
        Matcher m = ID_SHAPE.matcher(text);
        if (m.find()) return m.group(1);
        m = ID_SHAPE_LOOSE.matcher(text);
        if (m.find()) return m.group(1);
        return "";
    }

    private static String quotedTitle(String text) {
        Matcher m = ENTITLED.matcher(text);
        if (m.find()) return m.group(1);
        m = QUOTED.matcher(text);
        if (m.find()) return m.group(1);
        return "";
    }

    /** A journal already in the registry beats any guess from the prose. */
    private static String knownJournal(String text, List<String> known) {
        if (known == null) return "";
        String hay = text.toLowerCase(Locale.US);
        String best = "";
        for (String j : known) {
            if (j == null || j.trim().length() < 4) continue;
            if (hay.contains(j.toLowerCase(Locale.US)) && j.length() > best.length()) best = j;
        }
        return best;
    }

    // --------------------------------------------------------------- dates

    private static String relativeDue(String text) {
        Matcher m = WITHIN_DAYS.matcher(text);
        if (!m.find()) return "";
        try {
            return DateUtil.plusDays(DateUtil.today(), Integer.parseInt(m.group(1)));
        } catch (Exception e) {
            return "";
        }
    }

    private static final String[] MONTHS = {
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"
    };

    private static final Pattern ISO = Pattern.compile("\\b(20\\d{2})-(\\d{1,2})-(\\d{1,2})\\b");
    private static final Pattern DMY = Pattern.compile(
            "\\b(\\d{1,2})[\\s/.-]+([A-Za-z]{3,9}|\\d{1,2})[\\s/.,-]+(20\\d{2})\\b");
    private static final Pattern MDY = Pattern.compile(
            "\\b([A-Za-z]{3,9})\\s+(\\d{1,2}),?\\s+(20\\d{2})\\b");

    private static String parseDate(String s) {
        Matcher m = ISO.matcher(s);
        if (m.find()) return iso(m.group(1), m.group(2), m.group(3));

        m = MDY.matcher(s);
        if (m.find()) {
            int mo = monthOf(m.group(1));
            if (mo > 0) return iso(m.group(3), String.valueOf(mo), m.group(2));
        }
        m = DMY.matcher(s);
        if (m.find()) {
            String mid = m.group(2);
            int mo = mid.matches("\\d+") ? Integer.parseInt(mid) : monthOf(mid);
            if (mo > 0 && mo <= 12) return iso(m.group(3), String.valueOf(mo), m.group(1));
        }
        return "";
    }

    /** Any future-looking date in the body, used only when nothing was labelled. */
    private static String anyDate(String text) {
        String found = parseDate(text);
        if (found.isEmpty()) return "";
        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, -1);
        return DateUtil.toMillis(found) >= c.getTimeInMillis() ? found : "";
    }

    private static int monthOf(String name) {
        String n = name.toLowerCase(Locale.US);
        for (int i = 0; i < MONTHS.length; i++) {
            if (MONTHS[i].startsWith(n) || n.startsWith(MONTHS[i].substring(0, 3))) return i + 1;
        }
        return 0;
    }

    private static String iso(String y, String m, String d) {
        try {
            return String.format(Locale.US, "%04d-%02d-%02d",
                    Integer.parseInt(y), Integer.parseInt(m), Integer.parseInt(d));
        } catch (Exception e) {
            return "";
        }
    }

    // -------------------------------------------------------------- tidying

    private static String tidyTitle(String t) {
        String s = t.trim();
        s = s.replaceAll("^[\"“«]|[\"”»]$", "").trim();
        s = s.replaceAll("\\s{2,}", " ");
        // Editorial mail often runs "<title>" straight into "by <authors>".
        s = s.replaceAll("(?i)\\s+by\\s+[A-Z][^,]{2,40}(,.*)?$", "");
        return s;
    }

    private static String tidyPerson(String e) {
        String s = e.trim().replaceAll("[,;].*$", "");
        s = s.replaceAll("(?i)\\s*\\((editor|associate editor|editor-in-chief)\\)\\s*$", "");
        return s.length() > 80 ? "" : s;
    }

    private static String tidyJournal(String j) {
        String s = j.trim().replaceAll("\\s{2,}", " ");
        s = s.replaceAll("(?i)^the\\s+", "");
        return s.length() > 120 ? "" : s;
    }

    /** Journal names worth matching even before the registry has any rows. */
    public static List<String> seedJournalNames() {
        List<String> l = new ArrayList<>();
        l.add("IEEE Access");
        l.add("IEEE Internet of Things Journal");
        l.add("IEEE Transactions on Industrial Informatics");
        l.add("Scientific Reports");
        l.add("Expert Systems with Applications");
        l.add("Computers & Security");
        l.add("Journal of Network and Computer Applications");
        l.add("Applied Sciences");
        l.add("Sensors");
        l.add("Electronics");
        l.add("Mathematics");
        l.add("PLOS ONE");
        l.add("Neural Computing and Applications");
        l.add("Computers in Biology and Medicine");
        l.add("Future Generation Computer Systems");
        return l;
    }
}
