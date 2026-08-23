package com.drtawfik.mihakk.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.drtawfik.mihakk.R;
import com.google.android.material.appbar.MaterialToolbar;

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
    }
}
