package com.drtawfik.mihakk.data;

import android.database.Cursor;

public class Journal {
    public long id;
    public String name = "";
    public String publisher = "";
    public String issn = "";
    public String quartile = "";
    public double impactFactor;
    public String indexing = "";
    public String notes = "";

    /** Filled by the aggregate query on the journals screen; not a stored column. */
    public int reviewCount;

    public static Journal from(Cursor c) {
        Journal j = new Journal();
        j.id = c.getLong(c.getColumnIndexOrThrow("_id"));
        j.name = s(c, "name");
        j.publisher = s(c, "publisher");
        j.issn = s(c, "issn");
        j.quartile = s(c, "quartile");
        int i = c.getColumnIndex("impact_factor");
        j.impactFactor = (i >= 0 && !c.isNull(i)) ? c.getDouble(i) : 0;
        j.indexing = s(c, "indexing");
        j.notes = s(c, "notes");
        int rc = c.getColumnIndex("review_count");
        if (rc >= 0 && !c.isNull(rc)) j.reviewCount = c.getInt(rc);
        return j;
    }

    private static String s(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        if (i < 0 || c.isNull(i)) return "";
        return c.getString(i);
    }

    public String subtitle() {
        StringBuilder sb = new StringBuilder();
        if (!publisher.isEmpty()) sb.append(publisher);
        if (!quartile.isEmpty()) sb.append(sb.length() > 0 ? " · " : "").append(quartile);
        if (impactFactor > 0) sb.append(sb.length() > 0 ? " · IF " : "IF ").append(impactFactor);
        return sb.toString();
    }
}
