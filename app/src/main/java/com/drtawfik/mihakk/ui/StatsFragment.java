package com.drtawfik.mihakk.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.data.Repo;
import com.drtawfik.mihakk.data.Review;
import com.drtawfik.mihakk.logic.Exports;
import com.drtawfik.mihakk.logic.PdfDossier;
import com.drtawfik.mihakk.logic.ReportBuilder;
import com.drtawfik.mihakk.logic.Stats;
import com.drtawfik.mihakk.util.DateUtil;
import com.drtawfik.mihakk.util.LocaleUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatsFragment extends Fragment {

    private ChipGroup years;
    private LinearLayout tiles;
    private LinearLayout breakdowns;
    private TextView empty;

    private String yearFilter = "";
    private List<Review> allReviews = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                             @Nullable Bundle state) {
        View v = inflater.inflate(R.layout.fragment_stats, parent, false);
        years = v.findViewById(R.id.years);
        tiles = v.findViewById(R.id.tiles);
        breakdowns = v.findViewById(R.id.breakdowns);
        empty = v.findViewById(R.id.empty);

        ((MaterialButton) v.findViewById(R.id.exportPdf)).setOnClickListener(x -> exportPdf());
        ((MaterialButton) v.findViewById(R.id.exportCsv)).setOnClickListener(x -> exportCsv());
        ((MaterialButton) v.findViewById(R.id.exportIcs)).setOnClickListener(x -> exportIcs());
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        allReviews = new Repo(requireContext()).all();
        buildYearChips();
        refresh();
    }

    private void buildYearChips() {
        years.removeAllViews();
        List<String> ys = Stats.years(allReviews);

        Chip all = new Chip(requireContext());
        all.setCheckable(true);
        all.setText(R.string.year_all);
        all.setChecked(yearFilter.isEmpty());
        all.setOnClickListener(v -> {
            yearFilter = "";
            refresh();
        });
        years.addView(all);

        for (String y : ys) {
            Chip c = new Chip(requireContext());
            c.setCheckable(true);
            c.setText(y);
            c.setChecked(y.equals(yearFilter));
            c.setOnClickListener(v -> {
                yearFilter = y;
                refresh();
            });
            years.addView(c);
        }
    }

    private List<Review> filtered() {
        if (yearFilter.isEmpty()) return allReviews;
        List<Review> out = new ArrayList<>();
        for (Review r : allReviews) if (yearFilter.equals(r.tallyYear())) out.add(r);
        return out;
    }

    private Stats currentStats() {
        return Stats.compute(new Repo(requireContext()), filtered());
    }

    private void refresh() {
        Stats s = currentStats();
        tiles.removeAllViews();
        breakdowns.removeAllViews();

        empty.setVisibility(allReviews.isEmpty() ? View.VISIBLE : View.GONE);
        if (allReviews.isEmpty()) return;

        tiles.addView(Ui.tileRow(requireContext(),
                Ui.statTile(requireContext(), String.valueOf(s.done),
                        getString(R.string.stat_delivered)),
                Ui.statTile(requireContext(), String.valueOf(s.verified),
                        getString(R.string.stat_verified))));

        tiles.addView(Ui.tileRow(requireContext(),
                Ui.statTile(requireContext(), String.valueOf(s.byJournal.size()),
                        getString(R.string.stat_journals)),
                Ui.statTile(requireContext(), String.valueOf(s.open),
                        getString(R.string.stat_open))));

        tiles.addView(Ui.tileRow(requireContext(),
                Ui.statTile(requireContext(),
                        s.medianTurnaround < 0 ? "—" : String.valueOf(s.medianTurnaround),
                        getString(R.string.stat_median_turnaround)),
                Ui.statTile(requireContext(),
                        s.onTimeDenom == 0 ? "—" : Math.round(s.onTimeRate()) + "%",
                        getString(R.string.stat_on_time))));

        tiles.addView(Ui.tileRow(requireContext(),
                Ui.statTile(requireContext(),
                        s.accepted + s.declined == 0 ? "—" : Math.round(s.acceptanceRate()) + "%",
                        getString(R.string.stat_acceptance)),
                s.hours > 0 ? Ui.statTile(requireContext(),
                        String.valueOf(Math.round(s.hours)), getString(R.string.stat_hours)) : null));

        addBreakdown(getString(R.string.by_year), s.byYear, true);
        addBreakdown(getString(R.string.by_journal), s.byJournal, false);
        addBreakdown(getString(R.string.by_publisher), s.byPublisher, false);
        addRecommendations(s);
    }

    private void addBreakdown(String title, Map<String, Integer> data, boolean reverseKeys) {
        if (data.isEmpty()) return;
        breakdowns.addView(Ui.sectionHeader(requireContext(), title));

        int max = 1;
        for (int v : data.values()) max = Math.max(max, v);

        List<String> keys = new ArrayList<>(data.keySet());
        if (reverseKeys) java.util.Collections.sort(keys, java.util.Collections.reverseOrder());

        int shown = 0;
        for (String k : keys) {
            breakdowns.addView(Ui.barRow(requireContext(), k, data.get(k), max));
            if (++shown >= 12) break;
        }
    }

    private void addRecommendations(Stats s) {
        if (s.byRecommendation.isEmpty()) return;
        breakdowns.addView(Ui.sectionHeader(requireContext(), getString(R.string.by_outcome)));
        int max = 1;
        for (int v : s.byRecommendation.values()) max = Math.max(max, v);
        for (Map.Entry<String, Integer> e : s.byRecommendation.entrySet()) {
            breakdowns.addView(Ui.barRow(requireContext(),
                    ReportBuilder.recommendationLabel(requireContext(), e.getKey()),
                    e.getValue(), max));
        }
    }

    // ------------------------------------------------------------- exports

    private String rangeLabel() {
        return yearFilter.isEmpty() ? getString(R.string.year_all) : yearFilter;
    }

    /** The label is localised and may contain spaces; a filename should not. */
    private String rangeSlug() {
        return yearFilter.isEmpty() ? "all-years" : yearFilter;
    }

    private void exportPdf() {
        try {
            File dir = new File(requireContext().getExternalFilesDir(null), "reports");
            if (!dir.exists() && !dir.mkdirs()) throw new Exception("mkdir failed");
            File out = new File(dir, "mihakk-record-" + rangeSlug() + ".pdf");

            new PdfDossier(requireContext(), new Repo(requireContext()), currentStats(),
                    LocaleUtil.isArabic(requireContext())).write(out, rangeLabel());

            Exports.share(requireContext(), Exports.uriFor(requireContext(), out),
                    "application/pdf", getString(R.string.dossier_title));
        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

    private void exportCsv() {
        try {
            Repo repo = new Repo(requireContext());
            Uri uri = Exports.writeShareable(requireContext(), "exports",
                    "mihakk-reviews-" + DateUtil.today() + ".csv",
                    Exports.csv(repo, filtered()));
            Exports.share(requireContext(), uri, "text/csv", getString(R.string.export_csv));
        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

    private void exportIcs() {
        try {
            Uri uri = Exports.writeShareable(requireContext(), "exports",
                    "mihakk-deadlines.ics",
                    Exports.ics(new Repo(requireContext()).withDeadlines()));
            Exports.share(requireContext(), uri, "text/calendar", getString(R.string.export_ics));
        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), String.valueOf(msg), Toast.LENGTH_LONG).show();
    }
}
