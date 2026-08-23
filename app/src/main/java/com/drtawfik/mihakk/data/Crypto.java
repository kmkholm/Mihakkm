package com.drtawfik.mihakk.data;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Field-level encryption for the confidential columns.
 * <p>
 * The key lives in the AndroidKeyStore, so it is held by the platform (hardware
 * backed where the device offers it) and never sits in the app's own files. That
 * protects the store against anyone reading the database off the device — an adb
 * pull, a backup extraction, a lost phone — without asking the reviewer to type
 * anything. It deliberately does <em>not</em> require user authentication to use:
 * the daily deadline check runs while the screen is locked, and a reviewer who
 * wants the data sealed behind a credential turns on the app lock as well.
 * <p>
 * Values are tagged with a version sentinel, so a row that predates encryption is
 * recognised and returned as it is. That makes {@link #decrypt} safe to run over
 * mixed data and makes the migration idempotent.
 */
public final class Crypto {

    private static final String TAG = "MihakkCrypto";
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String ALIAS = "mihakk_field_key";
    private static final String TRANSFORM = "AES/GCM/NoPadding";

    /**
     * Marks a value this class produced. It opens with SOH, which cannot occur in
     * typed text — written as an escape rather than as a literal control byte so it
     * stays visible in the source and survives a re-encode.
     */
    private static final String TAG_V1 = "\u0001M1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private static volatile boolean unavailable;

    private Crypto() {
    }

    public static String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) return plain;
        if (isEncrypted(plain)) return plain;
        SecretKey key = key();
        if (key == null) return plain;
        try {
            Cipher c = Cipher.getInstance(TRANSFORM);
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = c.getIV();
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return TAG_V1 + Base64.encodeToString(out, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "encrypt failed", e);
            return plain;
        }
    }

    public static String decrypt(String stored) {
        if (stored == null || stored.isEmpty()) return stored;
        if (!isEncrypted(stored)) return stored;      // written before encryption existed
        SecretKey key = key();
        if (key == null) return "";
        try {
            byte[] raw = Base64.decode(stored.substring(TAG_V1.length()), Base64.NO_WRAP);
            if (raw.length <= IV_BYTES) return "";
            Cipher c = Cipher.getInstance(TRANSFORM);
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, raw, 0, IV_BYTES));
            byte[] plain = c.doFinal(raw, IV_BYTES, raw.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // A key that no longer opens the data is worth surfacing as empty rather
            // than as garbage: the row is still there, and its plaintext columns read.
            Log.e(TAG, "decrypt failed", e);
            return "";
        }
    }

    public static boolean isEncrypted(String v) {
        return v != null && v.startsWith(TAG_V1);
    }

    /** True when the platform gave us a usable key, i.e. rows really are encrypted. */
    public static boolean isActive() {
        return key() != null;
    }

    private static SecretKey key() {
        if (unavailable) return null;
        try {
            KeyStore ks = KeyStore.getInstance(KEYSTORE);
            ks.load(null);
            KeyStore.Entry entry = ks.getEntry(ALIAS, null);
            if (entry instanceof KeyStore.SecretKeyEntry) {
                return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
            }
            return generate();
        } catch (Exception e) {
            Log.e(TAG, "keystore unavailable", e);
            unavailable = true;
            return null;
        }
    }

    private static SecretKey generate() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        kg.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build());
        return kg.generateKey();
    }
}
