package com.drtawfik.mihakk.logic;

import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Live pull from the ORCID public API.
 * <p>
 * Public data on pub.orcid.org can be read anonymously — no token, no client
 * registration. So the fetch is tried bare first, and the client-credentials
 * flow is only a fallback for the day ORCID decides to require one. Asking for
 * a client id and secret up front was what produced a 401 for people who had
 * never needed credentials in the first place.
 */
public final class OrcidClient {

    private static final String TOKEN_URL = "https://orcid.org/oauth/token";
    private static final String PUB_BASE = "https://pub.orcid.org/v3.0/";

    public static class ApiException extends Exception {
        public final int code;
        /** ORCID's own explanation, when it sent one. */
        public final String detail;

        ApiException(int code, String body) {
            super(describe(code, body));
            this.code = code;
            this.detail = describe(code, body);
        }

        public boolean isAuth() {
            return code == 401 || code == 403;
        }
    }

    /** Pulls the human-readable part out of an ORCID error body. */
    private static String describe(int code, String body) {
        String fallback = "HTTP " + code;
        if (body == null || body.trim().isEmpty()) return fallback;
        try {
            org.json.JSONObject o = new org.json.JSONObject(body);
            for (String key : new String[]{
                    "error_description", "developer-message", "user-message", "error"}) {
                String v = o.optString(key, "");
                if (!v.isEmpty()) return fallback + " — " + v;
            }
        } catch (Exception ignored) {
            // not JSON; fall through
        }
        String trimmed = body.trim();
        return trimmed.length() > 160 ? fallback : fallback + " — " + trimmed;
    }

    private OrcidClient() {
    }

    /** Client-credentials token with {@code /read-public} scope. */
    public static String token(String clientId, String clientSecret) throws Exception {
        String form = "client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret)
                + "&grant_type=client_credentials"
                + "&scope=" + enc("/read-public");

        HttpURLConnection c = (HttpURLConnection) new URL(TOKEN_URL).openConnection();
        try {
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(20000);
            c.setReadTimeout(30000);
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            try (OutputStream os = c.getOutputStream()) {
                os.write(form.getBytes(StandardCharsets.UTF_8));
            }
            int code = c.getResponseCode();
            String body = read(code >= 400 ? c.getErrorStream() : c.getInputStream());
            if (code >= 400) throw new ApiException(code, body);

            String token = new org.json.JSONObject(body).optString("access_token", "");
            // A 200 with no token would otherwise surface later as an unexplained
            // 401 on the next call; fail here, where the cause is visible.
            if (token.isEmpty()) throw new ApiException(code, body);
            return token;
        } finally {
            c.disconnect();
        }
    }

    /** Raw JSON of the peer-review activity for an ORCID iD. */
    public static String peerReviews(String orcidId, String accessToken) throws Exception {
        String id = normaliseId(orcidId);
        if (id.isEmpty()) throw new ApiException(0, "empty ORCID iD");

        HttpURLConnection c = (HttpURLConnection) new URL(PUB_BASE + id + "/peer-reviews").openConnection();
        try {
            c.setRequestMethod("GET");
            c.setConnectTimeout(20000);
            c.setReadTimeout(30000);
            c.setRequestProperty("Accept", "application/json");
            if (!TextUtils.isEmpty(accessToken))
                c.setRequestProperty("Authorization", "Bearer " + accessToken);
            int code = c.getResponseCode();
            String body = read(code >= 400 ? c.getErrorStream() : c.getInputStream());
            if (code >= 400) throw new ApiException(code, body);
            return body;
        } finally {
            c.disconnect();
        }
    }

    /** Accepts a bare iD or a full orcid.org URL and returns {@code 0000-0000-0000-0000}. */
    public static String normaliseId(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        int i = s.lastIndexOf('/');
        if (i >= 0) s = s.substring(i + 1);
        return s.trim();
    }

    private static String enc(String s) throws Exception {
        return URLEncoder.encode(s == null ? "" : s, "UTF-8");
    }

    private static String read(InputStream in) throws Exception {
        if (in == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        in.close();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }
}
