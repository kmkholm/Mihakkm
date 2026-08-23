package com.drtawfik.mihakk.logic;

import android.content.Context;

import com.drtawfik.mihakk.data.Journal;
import com.drtawfik.mihakk.data.Repo;
import com.drtawfik.mihakk.data.Review;

import java.util.List;
import java.util.Map;

/**
 * Folds ORCID activity into the local store.
 * <p>
 * ORCID peer-review records are deliberately anonymous — no manuscript title,
 * no authors — so an imported row is a <em>verified tally entry</em>: it proves
 * the review happened, when, and for whom. Anything you typed yourself is never
 * overwritten by a re-import; the importer only fills blanks and sets the
 * verified flag.
 */
public final class OrcidImporter {

    public static class Result {
        public int added;
        public int updated;
        public int seen;

        public boolean isEmpty() {
            return added == 0 && updated == 0;
        }
    }

    private OrcidImporter() {
    }

    public static Result importJson(Context ctx, String json) throws Exception {
        List<OrcidParser.Entry> entries = OrcidParser.parse(json);
        Repo repo = new Repo(ctx);
        Map<String, Long> existing = repo.orcidIndex();
        List<Journal> known = repo.journals();

        Result res = new Result();
        res.seen = entries.size();

        for (OrcidParser.Entry e : entries) {
            if (e.putCode.isEmpty()) continue;

            String label = resolveJournal(e, known);
            registerJournal(repo, known, label, e);
            Long id = existing.get(e.putCode);

            if (id != null) {
                Review r = repo.byId(id);
                if (r == null) continue;
                boolean dirty = false;
                if (!r.verified) {
                    r.verified = true;
                    dirty = true;
                }
                if (r.submittedOn.isEmpty() && !e.completionDate.isEmpty()) {
                    r.submittedOn = e.completionDate;
                    dirty = true;
                }
                if (r.journalName.isEmpty() && !label.isEmpty()) {
                    r.journalName = label;
                    dirty = true;
                }
                if (!r.isDone()) {
                    r.status = Review.S_COMPLETED;
                    dirty = true;
                }
                if (dirty) {
                    repo.save(r);
                    res.updated++;
                }
            } else {
                Review r = new Review();
                r.orcidPutCode = e.putCode;
                r.verified = true;
                r.source = "orcid";
                r.status = Review.S_COMPLETED;
                r.submittedOn = e.completionDate;
                r.journalName = label;
                r.notes = buildNote(e);
                repo.save(r);
                existing.put(e.putCode, r.id);
                res.added++;
            }
        }
        return res;
    }

    /**
     * Fills the journal registry as a side effect of importing, carrying over the
     * ISSN and the convening organisation as the publisher. That is what makes the
     * per-publisher and per-quartile breakdowns work later: the reviewer only has
     * to add the quartile once per journal, not once per review.
     */
    private static void registerJournal(Repo repo, List<Journal> known,
                                        String label, OrcidParser.Entry e) {
        if (label == null || label.trim().isEmpty()) return;

        Journal existing = null;
        for (Journal j : known) if (j.name.equalsIgnoreCase(label)) existing = j;

        if (existing == null) {
            Journal j = new Journal();
            j.name = label;
            j.issn = e.issn;
            j.publisher = e.organization;
            repo.saveJournal(j);
            known.add(j);
            return;
        }
        // Never overwrite what the reviewer typed; only fill the blanks.
        boolean dirty = false;
        if (existing.issn.isEmpty() && !e.issn.isEmpty()) {
            existing.issn = e.issn;
            dirty = true;
        }
        if (existing.publisher.isEmpty() && !e.organization.isEmpty()) {
            existing.publisher = e.organization;
            dirty = true;
        }
        if (dirty) repo.saveJournal(existing);
    }

    /** Prefer a journal already in the registry with this ISSN over ORCID's own label. */
    private static String resolveJournal(OrcidParser.Entry e, List<Journal> known) {
        if (!e.issn.isEmpty()) {
            String want = e.issn.replace("-", "").toLowerCase();
            for (Journal j : known) {
                if (j.issn == null || j.issn.isEmpty()) continue;
                if (j.issn.replace("-", "").toLowerCase().contains(want)) return j.name;
            }
        }
        return e.journalLabel();
    }

    private static String buildNote(OrcidParser.Entry e) {
        StringBuilder sb = new StringBuilder();
        if (!e.issn.isEmpty()) sb.append("ISSN ").append(e.issn);
        if (!e.organization.isEmpty())
            sb.append(sb.length() > 0 ? " · " : "").append(e.organization);
        if (!e.sourceName.isEmpty() && !e.sourceName.equals(e.organization))
            sb.append(sb.length() > 0 ? " · " : "").append(e.sourceName);
        return sb.toString();
    }
}
