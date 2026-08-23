package com.drtawfik.mihakk.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.data.Prefs;
import com.drtawfik.mihakk.logic.OrcidClient;
import com.drtawfik.mihakk.logic.OrcidImporter;
import com.drtawfik.mihakk.util.DateUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class OrcidActivity extends BaseActivity {

    private TextInputEditText orcidId, clientId, clientSecret;
    private TextView result, lastSync;

    private final ActivityResultLauncher<String[]> pickFile =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::importFile);

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_orcid);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        orcidId = findViewById(R.id.orcidId);
        clientId = findViewById(R.id.clientId);
        clientSecret = findViewById(R.id.clientSecret);
        result = findViewById(R.id.result);
        lastSync = findViewById(R.id.lastSync);

        orcidId.setText(Prefs.get(this, Prefs.ORCID_ID, ""));
        clientId.setText(Prefs.get(this, Prefs.ORCID_CLIENT_ID, ""));
        clientSecret.setText(Prefs.get(this, Prefs.ORCID_CLIENT_SECRET, ""));
        paintLastSync();

        ((MaterialButton) findViewById(R.id.pickFile)).setOnClickListener(v ->
                pickFile.launch(new String[]{"application/json", "text/plain", "*/*"}));

        ((MaterialButton) findViewById(R.id.openSite)).setOnClickListener(v -> {
            String id = OrcidClient.normaliseId(text(orcidId));
            Uri u = Uri.parse("https://orcid.org/" + (id.isEmpty() ? "my-orcid" : id));
            startActivity(new Intent(Intent.ACTION_VIEW, u));
        });

        ((MaterialButton) findViewById(R.id.fetch)).setOnClickListener(v -> fetch());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Prefs.set(this, Prefs.ORCID_ID, text(orcidId));
        Prefs.set(this, Prefs.ORCID_CLIENT_ID, text(clientId));
        Prefs.set(this, Prefs.ORCID_CLIENT_SECRET, text(clientSecret));
    }

    private void paintLastSync() {
        String v = Prefs.get(this, Prefs.ORCID_LAST_SYNC, "");
        lastSync.setText(getString(R.string.orcid_last_sync,
                v.isEmpty() ? getString(R.string.orcid_never) : v));
    }

    // ------------------------------------------------------- file import

    private void importFile(@Nullable Uri uri) {
        if (uri == null) return;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new Exception("no stream");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            apply(new String(bos.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            result.setText(getString(R.string.orcid_failed, String.valueOf(e.getMessage())));
        }
    }

    // ------------------------------------------------------ network fetch

    private void fetch() {
        String id = text(orcidId);
        if (id.isEmpty()) {
            toast(getString(R.string.orcid_id_hint));
            return;
        }
        String cid = text(clientId), secret = text(clientSecret);
        if (cid.isEmpty() || secret.isEmpty()) {
            result.setText(R.string.orcid_need_creds);
            return;
        }
        Prefs.set(this, Prefs.ORCID_ID, id);
        Prefs.set(this, Prefs.ORCID_CLIENT_ID, cid);
        Prefs.set(this, Prefs.ORCID_CLIENT_SECRET, secret);

        result.setText(R.string.orcid_working);
        Handler main = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String token = OrcidClient.token(cid, secret);
                String json = OrcidClient.peerReviews(id, token);
                main.post(() -> apply(json));
            } catch (Exception e) {
                String msg = e instanceof OrcidClient.ApiException
                        ? "HTTP " + ((OrcidClient.ApiException) e).code
                        : String.valueOf(e.getMessage());
                main.post(() -> result.setText(getString(R.string.orcid_failed, msg)));
            }
        });
    }

    private void apply(String json) {
        try {
            OrcidImporter.Result r = OrcidImporter.importJson(this, json);
            if (r.seen == 0) {
                result.setText(R.string.orcid_nothing);
                return;
            }
            result.setText(getString(R.string.orcid_result, r.added, r.updated, r.seen));
            Prefs.set(this, Prefs.ORCID_LAST_SYNC, DateUtil.today());
            paintLastSync();
        } catch (Exception e) {
            result.setText(getString(R.string.orcid_failed, String.valueOf(e.getMessage())));
        }
    }

    private String text(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
