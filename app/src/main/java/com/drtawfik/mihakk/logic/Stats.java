package com.drtawfik.mihakk.logic;

import com.drtawfik.mihakk.data.Journal;
import com.drtawfik.mihakk.data.Repo;
import com.drtawfik.mihakk.data.Review;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** The numbers a promotion file asks for, computed over a set of reviews. */
public class Stats {

    public int done;                 // reports actually delivered
    public int open;                 // still in hand
    public int invitations;          // every invitation on record
    public int accepted;             // invitations taken up (incl. already delivered)
    public int declined;
    public int expired;
    public int verified;             // ORCID-verified
    public double hours;

    public int onTime;
    public int onTimeDenom;
    public int meanTurnaround = -1;
    public int medianTurnaround = -1;

    public final Map<String, Integer> byYear = new TreeMap<>();
    public final Map<String, Integer> byJournal = new LinkedHashMap<>();
    public final Map<String, Integer> byPublisher = new LinkedHashMap<>();
    public final Map<String, Integer> byQuartile = new TreeMap<>();
    public final Map<String, Integer> byRecommendation = new LinkedHashMap<>();
    public final Map<String, Integer> byStudyType = new LinkedHashMap<>();

    public double acceptanceRate() {
        int responded = accepted + declined;
        return responded == 0 ? 0 : (accepted * 100.0) / responded;
    }

    public double onTimeRate() {
        return onTimeDenom == 0 ? 0 : (onTime * 100.0) / onTimeDenom;
    }

    public static Stats compute(Repo repo, List<Review> reviews) {
        Stats s = new Stats();
        Map<String, Integer> journalCounts = new LinkedHashMap<>();
        Map<String, Integer> publisherCounts = new LinkedHashMap<>();
        List<Integer> turnarounds = new ArrayList<>();

        for (Review r : reviews) {
            s.invitations++;

            if (Review.S_DECLINED.equals(r.status)) s.declined++;
            else if (Review.S_EXPIRED.equals(r.status)) s.expired++;
            else s.accepted++;

            if (r.isOpen()) s.open++;
            if (!r.isDone()) continue;

            // --- delivered reports only from here down --------------------
            s.done++;
            if (r.verified) s.verified++;
            s.hours += r.hours;

            String y = r.tallyYear();
            if (!y.isEmpty()) bump(s.byYear, y);

            String jn = r.journalName == null ? "" : r.journalName.trim();
            if (!jn.isEmpty()) {
                bump(journalCounts, jn);
                Journal j = repo.journalByName(jn);
                if (j != null) {
                    if (!j.publisher.isEmpty()) bump(publisherCounts, j.publisher);
                    if (!j.quartile.isEmpty()) bump(s.byQuartile, j.quartile);
                }
            }
            if (r.recommendation != null && !r.recommendation.isEmpty())
                bump(s.byRecommendation, r.recommendation);
            if (r.studyType != null && !r.studyType.isEmpty())
                bump(s.byStudyType, r.studyType);

            if (!r.dueOn.isEmpty() && !r.submittedOn.isEmpty()) {
                s.onTimeDenom++;
                if (r.onTime()) s.onTime++;
            }
            int t = r.turnaroundDays();
            if (t >= 0) turnarounds.add(t);
        }

        s.byJournal.putAll(sortDesc(journalCounts));
        s.byPublisher.putAll(sortDesc(publisherCounts));

        if (!turnarounds.isEmpty()) {
            int sum = 0;
            for (int t : turnarounds) sum += t;
            s.meanTurnaround = Math.round((float) sum / turnarounds.size());
            Collections.sort(turnarounds);
            int n = turnarounds.size();
            s.medianTurnaround = (n % 2 == 1)
                    ? turnarounds.get(n / 2)
                    : Math.round((turnarounds.get(n / 2 - 1) + turnarounds.get(n / 2)) / 2f);
        }
        return s;
    }

    private static void bump(Map<String, Integer> m, String k) {
        Integer v = m.get(k);
        m.put(k, v == null ? 1 : v + 1);
    }

    private static LinkedHashMap<String, Integer> sortDesc(Map<String, Integer> in) {
        List<Map.Entry<String, Integer>> es = new ArrayList<>(in.entrySet());
        Collections.sort(es, (a, b) -> {
            int c = b.getValue().compareTo(a.getValue());
            return c != 0 ? c : a.getKey().compareToIgnoreCase(b.getKey());
        });
        LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : es) out.put(e.getKey(), e.getValue());
        return out;
    }

    /** Years present in the data, newest first — drives the year filter chips. */
    public static List<String> years(List<Review> reviews) {
        List<String> ys = new ArrayList<>();
        for (Review r : reviews) {
            String y = r.tallyYear();
            if (!y.isEmpty() && !ys.contains(y)) ys.add(y);
        }
        Collections.sort(ys, Collections.reverseOrder());
        return ys;
    }
}
