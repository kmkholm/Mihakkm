# مِحَكّ · Mihakk

**منضدة المُحكِّم — The reviewer's desk.**

An offline Android app for academics who referee papers. It tracks review invitations
and their deadlines, walks you through an appraisal checklist matched to the study
design, turns that checklist into a draft report, imports your verified peer-review
record from ORCID, and prints the annual figures a promotion file asks for.

Built by Dr. Mohammed Tawfik. Package `com.drtawfik.mihakk`.

---

## Why it is offline

A manuscript sent to you for review is confidential material entrusted to you by a
journal. Its title, its authors and your draft report do not belong on somebody
else's server. Everything Mihakk stores stays in one SQLite file inside the app's
private directory; cloud backup and device-to-device transfer are switched off in
`data_extraction_rules.xml`.

The app declares `INTERNET` for exactly one optional feature — pulling your public
peer-review activity from ORCID — and ORCID's peer-review records are anonymous by
design: they carry the date and the venue, never the manuscript. The default import
path is a file you download from orcid.org yourself, and works with the radio off.

---

## What it does

**Today.** Open work sorted by urgency: overdue first, then due within a week, then
invitations still waiting on your answer. A daily alarm posts one summary
notification rather than a stream of them.

**Quick add.** Paste the invitation email and the parser pulls out journal,
manuscript ID, title, handling editor and deadline — including relative deadlines
("within 14 days"). The app is also registered for `ACTION_SEND`, so you can share
the email straight from your mail app into Mihakk. Everything found is shown for
confirmation before anything is saved; nothing is guessed silently.

**Checklists.** Eight appraisal frames — general, machine learning, security and
privacy, engineering simulation, systematic review, observational, randomised trial,
survey — 104 items in all, each in Arabic and English, each weighted critical / major
/ minor. These are original reviewer-facing questions written for this app, informed
by the public reporting guidelines named on each list (CONSORT, PRISMA, STROBE,
TRIPOD+AI, CHERRIES, COPE). No guideline text is reproduced.

**Reports.** Eighteen templates — full major-revision reports, minor revision,
accept, reject, reject-and-resubmit, second-round responses, ML-specific and
survey-specific reports, confidential notes to the editor (including a carefully
worded integrity-concern note), and three ways to decline. Thirteen in English, five
in Arabic.

The checklist and the report are the same piece of work: items you marked as a
concern become the numbered major points, minor-weighted ones become the minor
points, and anything you marked as fine *with a note* becomes a listed strength. You
answer the appraisal once.

**ORCID.** Import your record file or fetch over the API with a public-API client.
Rows dedupe on ORCID's put-code, so re-importing updates rather than duplicates, and
anything you typed yourself is never overwritten — the importer only fills blanks.
The journal registry populates itself from the import, carrying the ISSN and the
publisher across, so the per-publisher and per-quartile breakdowns work after you add
the quartile once per journal.

**Service record.** Reviews delivered, ORCID-verified count, journals served,
invitation acceptance rate, median turnaround, on-time delivery, hours logged, and
breakdowns by year, journal, publisher and recommendation — filterable by year and
exportable as a print-ready A4 PDF, a CSV, or an `.ics` of your deadlines.

**Housekeeping.** Arabic and English with full RTL, light and dark themes, optional
PIN lock with biometric unlock, optional screenshot blocking, and a plain-JSON backup
so the record outlives the app.

---

## Build

Same toolchain as the author's other Android projects: Java + XML views,
AGP 9.0.1 / Gradle 9.1.0, `minSdk 26`, `targetSdk 34`, `compileSdk 36`.

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` points at the SDK and is not committed.

## Layout

```
app/src/main/java/com/drtawfik/mihakk/
  data/     Db, Repo, Review, Journal, Checklist, Template, Answers, Content, Prefs
  logic/    InviteParser, ReportBuilder, Stats, OrcidParser, OrcidClient,
            OrcidImporter, Exports, Backup, PdfDossier
  ui/       LockActivity, QuickAddActivity, ReviewEditActivity, ChecklistActivity,
            ReportActivity, OrcidActivity, JournalsActivity, StatsFragment, …
  alarm/    ReminderScheduler, ReminderReceiver, BootReceiver
app/src/main/assets/   checklists.json, templates.json   (source of truth in seed/)
```

The checklist and template content lives in `seed/` and is copied into `assets/`.
Editing the seed files and rebuilding is how you change or extend the built-in
content; user-written templates go in the database instead.

## Notes on the security model

The PIN gate is a gate on the screen, not encryption of the store. It is salted and
stretched so the PIN is not sitting in preferences in the clear, and it keeps a
colleague who picks up your phone out of your review notes — but a determined
attacker with the unlocked device and developer access can read the database. If
you need more than that, the honest answer today is device encryption plus a screen
lock; encrypting the store itself is the obvious next step.

## Verified on an emulator (API 34), 2026-08-23

Quick add parsed a ScholarOne-style invitation and filled five fields including a
relative deadline; the ML checklist produced a draft Arabic report with strengths and
numbered major points; a 435-record ORCID file imported in one pass and re-imported
with zero duplicates; the service record reported 435 delivered across 9 journals and
5 publishers with the per-year distribution intact; the PDF rendered as a single
clean A4 page.

Two bugs were found and fixed during that pass: the importer was labelling reviews
with ORCID's convening organisation, which collapsed all nine journals into five
publishers; and the auto-generated Arabic comments read badly because the checklist
questions were being forced into statements.
