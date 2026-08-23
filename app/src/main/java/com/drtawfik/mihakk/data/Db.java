package com.drtawfik.mihakk.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Local store. Everything stays in this file on the device — manuscript titles,
 * author names and draft reports are confidential material and are never uploaded.
 */
public class Db extends SQLiteOpenHelper {

    public static final String NAME = "mihakk.db";
    public static final int VERSION = 1;

    public static final String T_REVIEWS = "reviews";
    public static final String T_JOURNALS = "journals";
    public static final String T_TEMPLATES = "templates";

    private static Db instance;

    public static synchronized Db get(Context ctx) {
        if (instance == null) instance = new Db(ctx.getApplicationContext());
        return instance;
    }

    private Db(Context ctx) {
        super(ctx, NAME, null, VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_JOURNALS + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT NOT NULL UNIQUE COLLATE NOCASE,"
                + "publisher TEXT,"
                + "issn TEXT,"
                + "quartile TEXT,"          // Q1..Q4 or empty
                + "impact_factor REAL,"
                + "indexing TEXT,"          // SCIE / Scopus / ESCI ...
                + "notes TEXT)");

        db.execSQL("CREATE TABLE " + T_REVIEWS + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "journal_id INTEGER,"
                + "journal_name TEXT,"      // kept denormalised so ORCID rows work standalone
                + "manuscript_id TEXT,"
                + "title TEXT,"
                + "authors TEXT,"
                + "editor TEXT,"
                + "round INTEGER DEFAULT 1,"
                + "parent_id INTEGER DEFAULT 0,"
                + "study_type TEXT,"
                + "status TEXT NOT NULL DEFAULT 'invited',"
                + "recommendation TEXT,"
                + "invited_on TEXT,"        // ISO yyyy-MM-dd
                + "responded_on TEXT,"
                + "due_on TEXT,"
                + "submitted_on TEXT,"
                + "hours REAL DEFAULT 0,"
                + "checklist_key TEXT,"
                + "checklist_state TEXT,"   // JSON: {itemId:{v:ok|concern|fail|na, note:...}}
                + "report_text TEXT,"
                + "editor_notes TEXT,"
                + "notes TEXT,"
                + "tags TEXT,"
                + "orcid_put_code TEXT,"
                + "verified INTEGER DEFAULT 0,"
                + "source TEXT DEFAULT 'manual',"   // manual | orcid | csv
                + "reminder_days INTEGER DEFAULT 3,"
                + "created_at INTEGER,"
                + "updated_at INTEGER)");

        db.execSQL("CREATE INDEX idx_rev_status ON " + T_REVIEWS + "(status)");
        db.execSQL("CREATE INDEX idx_rev_due ON " + T_REVIEWS + "(due_on)");
        db.execSQL("CREATE INDEX idx_rev_sub ON " + T_REVIEWS + "(submitted_on)");
        db.execSQL("CREATE UNIQUE INDEX idx_rev_putcode ON " + T_REVIEWS + "(orcid_put_code)"
                + " WHERE orcid_put_code IS NOT NULL AND orcid_put_code <> ''");

        // User-authored templates. Built-in ones live in assets and are not copied here.
        db.execSQL("CREATE TABLE " + T_TEMPLATES + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "key TEXT UNIQUE,"
                + "name TEXT,"
                + "lang TEXT,"
                + "scope TEXT,"
                + "recommendation TEXT,"
                + "body TEXT,"
                + "updated_at INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        // v1 is the first release; future migrations get their own branches here.
    }
}
