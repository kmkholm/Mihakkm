package com.drtawfik.mihakk.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.data.Prefs;
import com.drtawfik.mihakk.logic.OrcidClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_about);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView version = findViewById(R.id.version);
        try {
            String name = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            version.setText(getString(R.string.about_version, name));
        } catch (Exception e) {
            version.setText("");
        }

        ((MaterialButton) findViewById(R.id.mailBtn)).setOnClickListener(v -> sendMail());
        findViewById(R.id.email).setOnClickListener(v -> sendMail());

        showOrcid();
    }

    /** Shows the reviewer's own ORCID iD once it has been entered in Settings. */
    private void showOrcid() {
        String id = OrcidClient.normaliseId(Prefs.get(this, Prefs.ORCID_ID, ""));
        if (id.isEmpty()) return;

        TextView row = findViewById(R.id.orcid);
        MaterialButton btn = findViewById(R.id.orcidBtn);
        row.setText("ORCID: " + id);
        row.setVisibility(View.VISIBLE);
        btn.setVisibility(View.VISIBLE);

        View.OnClickListener open = v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://orcid.org/" + id)));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "https://orcid.org/" + id, Toast.LENGTH_LONG).show();
            }
        };
        btn.setOnClickListener(open);
        row.setOnClickListener(open);
    }

    private void sendMail() {
        String address = getString(R.string.about_email);
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + address));
        i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.about_email_subject));
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            // Nothing to send with — leave the address on screen instead of failing.
            Toast.makeText(this, getString(R.string.about_no_mail_app) + "\n" + address,
                    Toast.LENGTH_LONG).show();
        }
    }
}
