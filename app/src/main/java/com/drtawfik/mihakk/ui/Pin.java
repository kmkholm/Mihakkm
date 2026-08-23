package com.drtawfik.mihakk.ui;

import android.content.Context;

import com.drtawfik.mihakk.data.Prefs;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * The app-lock PIN.
 * <p>
 * This is a gate on the screen, not encryption of the store: it keeps a
 * colleague who picks up the phone out of your review notes. The salted hash
 * means the PIN itself is not sitting in preferences in the clear.
 */
public final class Pin {

    private Pin() {
    }

    public static void set(Context ctx, String pin) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltB64 = android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP);
        Prefs.set(ctx, Prefs.LOCK_SALT, saltB64);
        Prefs.set(ctx, Prefs.LOCK_PIN, hash(pin, saltB64));
    }

    public static void clear(Context ctx) {
        Prefs.set(ctx, Prefs.LOCK_PIN, "");
        Prefs.set(ctx, Prefs.LOCK_SALT, "");
        Prefs.setBool(ctx, Prefs.LOCK_BIOMETRIC, false);
    }

    public static boolean check(Context ctx, String pin) {
        String stored = Prefs.get(ctx, Prefs.LOCK_PIN, "");
        if (stored.isEmpty()) return true;
        String salt = Prefs.get(ctx, Prefs.LOCK_SALT, "");
        return constantTimeEquals(stored, hash(pin, salt));
    }

    private static String hash(String pin, String saltB64) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(android.util.Base64.decode(saltB64, android.util.Base64.NO_WRAP));
            byte[] out = md.digest(pin.getBytes(StandardCharsets.UTF_8));
            // Stretch a little: a 4-digit PIN has almost no entropy on its own.
            for (int i = 0; i < 20_000; i++) {
                md.reset();
                md.update(out);
                out = md.digest();
            }
            return android.util.Base64.encodeToString(out, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
