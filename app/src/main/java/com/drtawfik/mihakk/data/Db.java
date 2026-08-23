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
    public static final int VERSION = 2;

    /**
     * The columns that carry confidential manuscript material. These are stored
     * encrypted; everything else — dates, status, journal, counts — stays in
     * plaintext so the deadline check, sorting and the statistics can all run in
     * SQL without opening the key.
     */
    public static final String[] SECRET_COLS = {
            "title", "authors", "editor", "manuscript_id",
            "report_text", "editor_notes", "notes", "checklist_state", "tags"
    };

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
        if (oldV < 2) encryptExistingRows(db);
    }

    /**
     * v1 stored the confidential columns in the clear. Re-write them through
     * {@link Crypto}; values already tagged are skipped, so an interrupted
     * migration can simply run again.
     */
    private void encryptExistingRows(SQLiteDatabase db) {
        db.beginTransaction();
        try (android.database.Cursor c = db.rawQuery(
                "SELECT _id, " + String.join(", ", SECRET_COLS) + " FROM " + T_REVIEWS, null)) {
            while (c.moveToNext()) {
                android.content.ContentValues v = new android.content.ContentValues();
                boolean any = false;
                for (String col : SECRET_COLS) {
                    int i = c.getColumnIndex(col);
                    if (i < 0 || c.isNull(i)) continue;
                    String plain = c.getString(i);
                    if (plain == null || plain.isEmpty() || Crypto.isEncrypted(plain)) continue;
                    v.put(col, Crypto.encrypt(plain));
                    any = true;
                }
                if (any) {
                    db.update(T_REVIEWS, v, "_id=?",
                            new String[]{String.valueOf(c.getLong(0))});
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            android.util.Log.e("MihakkDb", "encryption migration failed", e);
        } finally {
            db.endTransaction();
        }
    }
}
