package com.drtawfik.mihakk;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.drtawfik.mihakk.ui.BaseActivity;
import com.drtawfik.mihakk.ui.QuickAddActivity;
import com.drtawfik.mihakk.ui.ReviewsFragment;
import com.drtawfik.mihakk.ui.SettingsFragment;
import com.drtawfik.mihakk.ui.StatsFragment;
import com.drtawfik.mihakk.ui.TodayFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends BaseActivity {

    private FloatingActionButton fab;
    private int current = R.id.nav_today;

    private final ActivityResultLauncher<String> notifPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            });

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v ->
                startActivity(new Intent(this, QuickAddActivity.class)));

        BottomNavigationView nav = findViewById(R.id.nav);
        nav.setOnItemSelectedListener(item -> {
            show(item.getItemId());
            return true;
        });

        if (state == null) show(R.id.nav_today);
        else current = state.getInt("tab", R.id.nav_today);

        askForNotifications();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", current);
    }

    private void show(int id) {
        current = id;
        Fragment f;
        if (id == R.id.nav_reviews) f = new ReviewsFragment();
        else if (id == R.id.nav_stats) f = new StatsFragment();
        else if (id == R.id.nav_settings) f = new SettingsFragment();
        else f = new TodayFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.host, f)
                .commit();

        // Only the two work screens can meaningfully add a review.
        fab.setVisibility(id == R.id.nav_today || id == R.id.nav_reviews
                ? View.VISIBLE : View.GONE);
    }

    private void askForNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}
