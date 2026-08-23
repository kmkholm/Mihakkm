package com.drtawfik.mihakk.data;

import android.database.Cursor;

import com.drtawfik.mihakk.util.DateUtil;

public class Review {

    // ---- status values -------------------------------------------------
    public static final String S_INVITED = "invited";
    public static final String S_ACCEPTED = "accepted";
    public static final String S_IN_PROGRESS = "in_progress";
    public static final String S_SUBMITTED = "submitted";
    public static final String S_COMPLETED = "completed";
    public static final String S_DECLINED = "declined";
    public static final String S_EXPIRED = "expired";

    public static final String[] STATUSES = {
            S_INVITED, S_ACCEPTED, S_IN_PROGRESS, S_SUBMITTED, S_COMPLETED, S_DECLINED, S_EXPIRED
    };

    /** Statuses that count as "work delivered" for the promotion dossier. */
    public static final String DONE_SQL = "status IN ('submitted','completed')";
    /** Statuses that still need something from the reviewer. */
    public static final String OPEN_SQL = "status IN ('invited','accepted','in_progress')";

    // ---- recommendation values ----------------------------------------
    public static final String R_ACCEPT = "accept";
    public static final String R_MINOR = "minor_revision";
    public static final String R_MAJOR = "major_revision";
    public static final String R_REJECT = "reject";
    public static final String R_RESUBMIT = "resubmit";

    public static final String[] RECOMMENDATIONS = {
            R_ACCEPT, R_MINOR, R_MAJOR, R_REJECT, R_RESUBMIT
    };

    public long id;
    public long journalId;
    public String journalName = "";
    public String manuscriptId = "";
    public String title = "";
    public String authors = "";
    public String editor = "";
    public int round = 1;
    public long parentId;
    public String studyType = "";
    public String status = S_INVITED;
    public String recommendation = "";
    public String invitedOn = "";
    public String respondedOn = "";
    public String dueOn = "";
    public String submittedOn = "";
    public double hours;
    public String checklistKey = "";
    public String checklistState = "";
    public String reportText = "";
    public String editorNotes = "";
    public String notes = "";
    public String tags = "";
    public String orcidPutCode = "";
    public boolean verified;
    public String source = "manual";
    public int reminderDays = 3;
    public long createdAt;
    public long updatedAt;

    public static Review from(Cursor c) {
        Review r = new Review();
        r.id = c.getLong(c.getColumnIndexOrThrow("_id"));
        r.journalId = c.getLong(c.getColumnIndexOrThrow("journal_id"));
        r.journalName = str(c, "journal_name");
        r.manuscriptId = str(c, "manuscript_id");
        r.title = str(c, "title");
        r.authors = str(c, "authors");
        r.editor = str(c, "editor");
        r.round = c.getInt(c.getColumnIndexOrThrow("round"));
        r.parentId = c.getLong(c.getColumnIndexOrThrow("parent_id"));
        r.studyType = str(c, "study_type");
        r.status = str(c, "status");
        r.recommendation = str(c, "recommendation");
        r.invitedOn = str(c, "invited_on");
        r.respondedOn = str(c, "responded_on");
        r.dueOn = str(c, "due_on");
        r.submittedOn = str(c, "submitted_on");
        r.hours = c.getDouble(c.getColumnIndexOrThrow("hours"));
        r.checklistKey = str(c, "checklist_key");
        r.checklistState = str(c, "checklist_state");
        r.reportText = str(c, "report_text");
        r.editorNotes = str(c, "editor_notes");
        r.notes = str(c, "notes");
        r.tags = str(c, "tags");
        r.orcidPutCode = str(c, "orcid_put_code");
        r.verified = c.getInt(c.getColumnIndexOrThrow("verified")) != 0;
        r.source = str(c, "source");
        r.reminderDays = c.getInt(c.getColumnIndexOrThrow("reminder_days"));
        r.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
        r.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
        return r;
    }

    private static String str(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        if (i < 0 || c.isNull(i)) return "";
        String v = c.getString(i);
        if (v == null) return "";
        // Confidential columns come back tagged; everything else passes through.
        return Crypto.isEncrypted(v) ? Crypto.decrypt(v) : v;
    }

    public boolean isOpen() {
        return S_INVITED.equals(status) || S_ACCEPTED.equals(status) || S_IN_PROGRESS.equals(status);
    }

    public boolean isDone() {
        return S_SUBMITTED.equals(status) || S_COMPLETED.equals(status);
    }

    /** Days until the deadline; negative when overdue. Integer.MAX_VALUE when no due date. */
    public int daysLeft() {
        if (dueOn == null || dueOn.isEmpty()) return Integer.MAX_VALUE;
        return DateUtil.daysBetween(DateUtil.today(), dueOn);
    }

    /** Days from invitation to report submission, or -1 when not computable. */
    public int turnaroundDays() {
        if (invitedOn.isEmpty() || submittedOn.isEmpty()) return -1;
        int d = DateUtil.daysBetween(invitedOn, submittedOn);
        return d < 0 ? -1 : d;
    }

    public boolean onTime() {
        if (submittedOn.isEmpty() || dueOn.isEmpty()) return false;
        return DateUtil.daysBetween(submittedOn, dueOn) >= 0;
    }

    /** Year used for the annual tally: submission year, falling back to invitation year. */
    public String tallyYear() {
        String d = !submittedOn.isEmpty() ? submittedOn : invitedOn;
        return d.length() >= 4 ? d.substring(0, 4) : "";
    }

    public String displayTitle() {
        if (!title.isEmpty()) return title;
        if (!manuscriptId.isEmpty()) return manuscriptId;
        if (!journalName.isEmpty()) return journalName;
        return "—";
    }
}
