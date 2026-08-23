package com.drtawfik.mihakk.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.drtawfik.mihakk.data.Prefs;
import com.drtawfik.mihakk.util.LocaleUtil;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
    }

    @Override
    protected void onCreate(@Nullable Bundle state) {
        LocaleUtil.applyTheme(this);
        // Must land before the window is created, or the accent only takes effect
        // on the next launch.
        setTheme(Accent.current(this).themeRes);
        super.onCreate(state);
        // Off by default so the screen can be captured; a reviewer who wants the
        // recents thumbnail blanked turns it on in Settings.
        if (Prefs.getBool(this, Prefs.BLOCK_SHOTS, false)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    protected boolean isArabic() {
        return LocaleUtil.isArabic(this);
    }
}
