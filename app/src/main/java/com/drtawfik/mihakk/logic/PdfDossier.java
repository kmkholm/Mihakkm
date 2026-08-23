package com.drtawfik.mihakk.logic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.data.Journal;
import com.drtawfik.mihakk.data.Prefs;
import com.drtawfik.mihakk.data.Repo;
import com.drtawfik.mihakk.util.DateUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.Map;

/**
 * Renders the "peer review service record" — the one-page-ish evidence sheet a
 * promotion committee or an annual report actually asks for. A4 at 72 dpi.
 */
public class PdfDossier {

    private static final int PAGE_W = 595;
    private static final int PAGE_H = 842;
    private static final int MARGIN = 46;
    private static final int CONTENT_W = PAGE_W - MARGIN * 2;

    private static final int INK = 0xFF1A1D21;
    private static final int MUTED = 0xFF6B7280;
    private static final int RULE = 0xFFDDDDDD;

    private final Context ctx;
    private final Repo repo;
    private final Stats stats;
    private final boolean rtl;

    /** The accent chosen in Settings, so the printed record matches the app. */
    private final int ACCENT;
    private final int ACCENT_PALE;   // bar track
    private final int ACCENT_WASH;   // summary tiles

    private PdfDocument doc;
    private PdfDocument.Page page;
    private Canvas canvas;
    private int y;
    private int pageNo;

    public PdfDossier(Context ctx, Repo repo, Stats stats, boolean rtl, int accent) {
        this.ctx = ctx;
        this.repo = repo;
        this.stats = stats;
        this.rtl = rtl;
        this.ACCENT = accent;
        this.ACCENT_PALE = towardsWhite(accent, 0.72f);
        this.ACCENT_WASH = towardsWhite(accent, 0.93f);
    }

    /** Mixes a colour with white — a printable tint of whatever accent is set. */
    private static int towardsWhite(int color, float amount) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        r = Math.round(r + (255 - r) * amount);
        g = Math.round(g + (255 - g) * amount);
        b = Math.round(b + (255 - b) * amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public File write(File outFile, String rangeLabel) throws Exception {
        doc = new PdfDocument();
        newPage();

        header(rangeLabel);
        summaryGrid();
        yearTable();
        journalTable();
        publisherAndOutcome();
        footerNote();

        finishPage();
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            doc.writeTo(fos);
        }
        doc.close();
        return outFile;
    }

    // ------------------------------------------------------------- sections

    private void header(String rangeLabel) {
        String name = Prefs.get(ctx, Prefs.REVIEWER_NAME, "");
        String aff = Prefs.get(ctx, Prefs.AFFILIATION, "");
        String orcid = Prefs.get(ctx, Prefs.ORCID_ID, "");

        text(ctx.getString(R.string.dossier_title), 20, ACCENT, true);
        gap(2);
        if (!name.isEmpty()) text(name, 13, INK, true);
        if (!aff.isEmpty()) text(aff, 10, MUTED, false);
        if (!orcid.isEmpty()) text("ORCID: " + OrcidClient.normaliseId(orcid), 10, MUTED, false);
        text(ctx.getString(R.string.dossier_period, rangeLabel), 10, MUTED, false);
        text(ctx.getString(R.string.dossier_generated,
                DateUtil.pretty(DateUtil.today(), locale())), 9, MUTED, false);
        gap(8);
        rule();
        gap(10);
    }

    private void summaryGrid() {
        String[][] cells = {
                {ctx.getString(R.string.stat_delivered), String.valueOf(stats.done)},
                {ctx.getString(R.string.stat_verified), String.valueOf(stats.verified)},
                {ctx.getString(R.string.stat_journals), String.valueOf(stats.byJournal.size())},
                {ctx.getString(R.string.stat_acceptance), pct(stats.acceptanceRate())},
                {ctx.getString(R.string.stat_median_turnaround),
                        stats.medianTurnaround < 0 ? "—" : ctx.getString(R.string.n_days, stats.medianTurnaround)},
                {ctx.getString(R.string.stat_on_time),
                        stats.onTimeDenom == 0 ? "—" : pct(stats.onTimeRate())},
        };

        int cols = 3;
        int cellW = CONTENT_W / cols;
        int rows = (int) Math.ceil(cells.length / (double) cols);
        ensure(rows * 46 + 10);

        Paint box = new Paint(Paint.ANTI_ALIAS_FLAG);
        box.setColor(ACCENT_WASH);

        for (int i = 0; i < cells.length; i++) {
            int r = i / cols, c = i % cols;
            int left = MARGIN + c * cellW;
            int top = y + r * 46;
            canvas.drawRect(left, top, left + cellW - 6, top + 40, box);
            drawAt(cells[i][1], left + 8, top + 6, cellW - 20, 16, ACCENT, true);
            drawAt(cells[i][0], left + 8, top + 24, cellW - 20, 8, MUTED, false);
        }
        y += rows * 46 + 8;
    }

    private void yearTable() {
        if (stats.byYear.isEmpty()) return;
        sectionTitle(ctx.getString(R.string.dossier_by_year));

        int max = 1;
        for (int v : stats.byYear.values()) max = Math.max(max, v);

        Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
        java.util.List<String> years = new java.util.ArrayList<>(stats.byYear.keySet());
        java.util.Collections.sort(years, java.util.Collections.reverseOrder());

        for (String yr : years) {
            ensure(22);
            int n = stats.byYear.get(yr);
            drawAt(yr, MARGIN, y, 60, 10, INK, true);
            int barLeft = MARGIN + 62;
            int barMax = CONTENT_W - 62 - 40;
            bar.setColor(ACCENT_PALE);
            canvas.drawRect(barLeft, y + 1, barLeft + barMax, y + 11, bar);
            bar.setColor(ACCENT);
            canvas.drawRect(barLeft, y + 1, barLeft + Math.max(2, barMax * n / max), y + 11, bar);
            drawAt(String.valueOf(n), MARGIN + CONTENT_W - 36, y, 36, 10, INK, true);
            y += 18;
        }
        gap(8);
    }

    private void journalTable() {
        if (stats.byJournal.isEmpty()) return;
        sectionTitle(ctx.getString(R.string.dossier_by_journal));
        tableHead(new String[]{
                ctx.getString(R.string.col_journal),
                ctx.getString(R.string.col_publisher),
                ctx.getString(R.string.col_quartile),
                ctx.getString(R.string.col_count)});

        int[] w = {CONTENT_W * 46 / 100, CONTENT_W * 30 / 100, CONTENT_W * 12 / 100, CONTENT_W * 12 / 100};
        for (Map.Entry<String, Integer> e : stats.byJournal.entrySet()) {
            Journal j = repo.journalByName(e.getKey());
            ensure(20);
            int x = MARGIN;
            drawAt(e.getKey(), x, y, w[0] - 6, 9, INK, false);
            x += w[0];
            drawAt(j == null ? "" : j.publisher, x, y, w[1] - 6, 9, MUTED, false);
            x += w[1];
            drawAt(j == null ? "" : j.quartile, x, y, w[2] - 6, 9, MUTED, false);
            x += w[2];
            drawAt(String.valueOf(e.getValue()), x, y, w[3] - 6, 9, INK, true);
            y += 16;
        }
        gap(8);
    }

    private void publisherAndOutcome() {
        if (!stats.byPublisher.isEmpty()) {
            sectionTitle(ctx.getString(R.string.dossier_by_publisher));
            text(join(stats.byPublisher), 10, INK, false);
            gap(6);
        }
        if (!stats.byRecommendation.isEmpty()) {
            sectionTitle(ctx.getString(R.string.dossier_by_outcome));
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> e : stats.byRecommendation.entrySet()) {
                if (sb.length() > 0) sb.append("   ·   ");
                sb.append(ReportBuilder.recommendationLabel(ctx, e.getKey()))
                        .append(": ").append(e.getValue());
            }
            text(sb.toString(), 10, INK, false);
            gap(6);
        }
        if (stats.hours > 0) {
            sectionTitle(ctx.getString(R.string.dossier_effort));
            text(ctx.getString(R.string.dossier_hours, (int) Math.round(stats.hours)), 10, INK, false);
            gap(6);
        }
    }

    private void footerNote() {
        ensure(60);
        gap(6);
        rule();
        gap(6);
        text(ctx.getString(R.string.dossier_note), 8, MUTED, false);
    }

    private String join(Map<String, Integer> m) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            if (sb.length() > 0) sb.append("   ·   ");
            sb.append(e.getKey()).append(": ").append(e.getValue());
        }
        return sb.toString();
    }

    // --------------------------------------------------------- drawing bits

    private void sectionTitle(String s) {
        ensure(30);
        gap(4);
        text(s, 12, ACCENT, true);
        gap(2);
    }

    private void tableHead(String[] cols) {
        ensure(20);
        int[] w = {CONTENT_W * 46 / 100, CONTENT_W * 30 / 100, CONTENT_W * 12 / 100, CONTENT_W * 12 / 100};
        int x = MARGIN;
        for (int i = 0; i < cols.length && i < w.length; i++) {
            drawAt(cols[i], x, y, w[i] - 6, 8, MUTED, true);
            x += w[i];
        }
        y += 13;
        rule();
        y += 4;
    }

    private void text(String s, int size, int color, boolean bold) {
        if (s == null || s.isEmpty()) return;
        StaticLayout l = layout(s, CONTENT_W, size, color, bold);
        ensure(l.getHeight() + 4);
        canvas.save();
        canvas.translate(MARGIN, y);
        l.draw(canvas);
        canvas.restore();
        y += l.getHeight() + 3;
    }

    private void drawAt(String s, int x, int top, int width, int size, int color, boolean bold) {
        if (s == null || s.isEmpty()) return;
        StaticLayout l = layout(s, Math.max(20, width), size, color, bold);
        canvas.save();
        canvas.translate(x, top);
        l.draw(canvas);
        canvas.restore();
    }

    private StaticLayout layout(String s, int width, int size, int color, boolean bold) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(size);
        p.setFakeBoldText(bold);
        return StaticLayout.Builder.obtain(s, 0, s.length(), p, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(rtl ? TextDirectionHeuristics.RTL : TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .setIncludePad(false)
                .build();
    }

    private void rule() {
        Paint p = new Paint();
        p.setColor(RULE);
        p.setStrokeWidth(1);
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, p);
    }

    private void gap(int px) {
        y += px;
    }

    private void ensure(int needed) {
        if (y + needed > PAGE_H - MARGIN - 16) {
            finishPage();
            newPage();
        }
    }

    private void newPage() {
        pageNo++;
        PdfDocument.PageInfo info =
                new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create();
        page = doc.startPage(info);
        canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);
        y = MARGIN;
    }

    private void finishPage() {
        if (page == null) return;
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(MUTED);
        p.setTextSize(8);
        String foot = ctx.getString(R.string.dossier_footer, pageNo);
        canvas.drawText(foot, MARGIN, PAGE_H - MARGIN + 12, p);
        doc.finishPage(page);
        page = null;
    }

    private Locale locale() {
        return rtl ? new Locale("ar") : Locale.ENGLISH;
    }

    private String pct(double v) {
        return String.format(locale(), "%.0f%%", v);
    }
}
