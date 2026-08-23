package com.drtawfik.mihakk.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.alarm.ReminderScheduler;
import com.drtawfik.mihakk.data.Prefs;
import com.drtawfik.mihakk.logic.Backup;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SettingsFragment extends Fragment {

    private MaterialSwitch lockSwitch;
    private MaterialButton remindHour;
    private MaterialButton defaultDue;

    private final ActivityResultLauncher<String[]> pickBackup =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::doRestore);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                             @Nullable Bundle state) {
        View v = inflater.inflate(R.layout.fragment_settings, parent, false);

        bindText(v.findViewById(R.id.name), Prefs.REVIEWER_NAME);
        bindText(v.findViewById(R.id.affiliation), Prefs.AFFILIATION);
        bindText(v.findViewById(R.id.orcid), Prefs.ORCID_ID);

        bindLanguage(v);
        bindTheme(v);
        bindSecurity(v);
        bindReminders(v);

        ((MaterialButton) v.findViewById(R.id.orcidSync)).setOnClickListener(x ->
                startActivity(new Intent(requireContext(), OrcidActivity.class)));
        ((MaterialButton) v.findViewById(R.id.journals)).setOnClickListener(x ->
                startActivity(new Intent(requireContext(), JournalsActivity.class)));
        ((MaterialButton) v.findViewById(R.id.about)).setOnClickListener(x ->
                startActivity(new Intent(requireContext(), AboutActivity.class)));
        ((MaterialButton) v.findViewById(R.id.backup)).setOnClickListener(x -> doBackup());
        ((MaterialButton) v.findViewById(R.id.restore)).setOnClickListener(x ->
                pickBackup.launch(new String[]{"application/json", "text/plain", "*/*"}));
        return v;
    }

    // ------------------------------------------------------------- profile

    private void bindText(TextInputEditText field, String key) {
        field.setText(Prefs.get(requireContext(), key, ""));
        field.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(Editable s) {
                Prefs.set(requireContext(), key, s.toString());
            }
        });
    }

    // ---------------------------------------------------------- appearance

    private void bindLanguage(View v) {
        MaterialButtonToggleGroup g = v.findViewById(R.id.langGroup);
        String lang = Prefs.get(requireContext(), Prefs.LANG, "");
        g.check("ar".equals(lang) ? R.id.langAr : "en".equals(lang) ? R.id.langEn : R.id.langSystem);
        g.addOnButtonCheckedListener((group, id, checked) -> {
            if (!checked) return;
            String next = id == R.id.langAr ? "ar" : id == R.id.langEn ? "en" : "";
            if (next.equals(Prefs.get(requireContext(), Prefs.LANG, ""))) return;
            Prefs.set(requireContext(), Prefs.LANG, next);
            requireActivity().recreate();
        });
    }

    private void bindTheme(View v) {
        MaterialButtonToggleGroup g = v.findViewById(R.id.themeGroup);
        String theme = Prefs.get(requireContext(), Prefs.THEME, "system");
        g.check("light".equals(theme) ? R.id.themeLight
                : "dark".equals(theme) ? R.id.themeDark : R.id.themeSystem);
        g.addOnButtonCheckedListener((group, id, checked) -> {
            if (!checked) return;
            String next = id == R.id.themeLight ? "light" : id == R.id.themeDark ? "dark" : "system";
            if (next.equals(Prefs.get(requireContext(), Prefs.THEME, "system"))) return;
            Prefs.set(requireContext(), Prefs.THEME, next);
            requireActivity().recreate();
        });
    }

    // ------------------------------------------------------------ security

    private void bindSecurity(View v) {
        lockSwitch = v.findViewById(R.id.lockSwitch);
        MaterialSwitch bio = v.findViewById(R.id.biometricSwitch);
        MaterialSwitch shots = v.findViewById(R.id.shotsSwitch);
        MaterialButton changePin = v.findViewById(R.id.changePin);

        lockSwitch.setChecked(Prefs.lockEnabled(requireContext()));
        bio.setChecked(Prefs.getBool(requireContext(), Prefs.LOCK_BIOMETRIC, false));
        shots.setChecked(Prefs.getBool(requireContext(), Prefs.BLOCK_SHOTS, false));
        changePin.setEnabled(Prefs.lockEnabled(requireContext()));
        bio.setEnabled(Prefs.lockEnabled(requireContext()));

        lockSwitch.setOnClickListener(x -> {
            boolean want = lockSwitch.isChecked();
            if (want) askNewPin(() -> {
                changePin.setEnabled(true);
                bio.setEnabled(true);
            }, () -> lockSwitch.setChecked(false));
            else confirmPinThen(() -> {
                Pin.clear(requireContext());
                changePin.setEnabled(false);
                bio.setEnabled(false);
                bio.setChecked(false);
                toast(getString(R.string.lock_removed));
            }, () -> lockSwitch.setChecked(true));
        });

        changePin.setOnClickListener(x ->
                confirmPinThen(() -> askNewPin(null, null), null));

        bio.setOnClickListener(x ->
                Prefs.setBool(requireContext(), Prefs.LOCK_BIOMETRIC, bio.isChecked()));

        shots.setOnClickListener(x -> {
            Prefs.setBool(requireContext(), Prefs.BLOCK_SHOTS, shots.isChecked());
            requireActivity().recreate();
        });
    }

    private void askNewPin(@Nullable Runnable onDone, @Nullable Runnable onCancel) {
        EditText first = pinField();
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.lock_set_pin)
                .setView(first)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, (d, w) -> {
                    if (onCancel != null) onCancel.run();
                })
                .setPositiveButton(R.string.ok, (d, w) -> {
                    String p = first.getText().toString();
                    if (p.length() < 4) {
                        toast(getString(R.string.lock_too_short));
                        if (onCancel != null) onCancel.run();
                        return;
                    }
                    EditText again = pinField();
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.lock_confirm_pin)
                            .setView(again)
                            .setCancelable(false)
                            .setNegativeButton(R.string.cancel, (d2, w2) -> {
                                if (onCancel != null) onCancel.run();
                            })
                            .setPositiveButton(R.string.ok, (d2, w2) -> {
                                if (!p.equals(again.getText().toString())) {
                                    toast(getString(R.string.lock_mismatch));
                                    if (onCancel != null) onCancel.run();
                                    return;
                                }
                                Pin.set(requireContext(), p);
                                if (onDone != null) onDone.run();
                            })
                            .show();
                })
                .show();
    }

    private void confirmPinThen(Runnable onOk, @Nullable Runnable onFail) {
        if (!Prefs.lockEnabled(requireContext())) {
            onOk.run();
            return;
        }
        EditText f = pinField();
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.lock_enter_pin)
                .setView(f)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, (d, w) -> {
                    if (onFail != null) onFail.run();
                })
                .setPositiveButton(R.string.ok, (d, w) -> {
                    if (Pin.check(requireContext(), f.getText().toString())) onOk.run();
                    else {
                        toast(getString(R.string.lock_wrong));
                        if (onFail != null) onFail.run();
                    }
                })
                .show();
    }

    private EditText pinField() {
        EditText e = new EditText(requireContext());
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        int pad = Ui.dp(requireContext(), 20);
        e.setPadding(pad, pad, pad, pad);
        return e;
    }

    // ----------------------------------------------------------- reminders

    private void bindReminders(View v) {
        remindHour = v.findViewById(R.id.remindHour);
        defaultDue = v.findViewById(R.id.defaultDue);
        updateReminderLabels();

        remindHour.setOnClickListener(x -> pickNumber(R.string.set_remind_hour, 0, 23,
                Prefs.getInt(requireContext(), Prefs.REMIND_HOUR, 8), value -> {
                    Prefs.setInt(requireContext(), Prefs.REMIND_HOUR, value);
                    ReminderScheduler.schedule(requireContext());
                    updateReminderLabels();
                }));

        defaultDue.setOnClickListener(x -> pickNumber(R.string.set_default_due, 1, 90,
                Prefs.getInt(requireContext(), Prefs.DEFAULT_DUE_DAYS, 21), value -> {
                    Prefs.setInt(requireContext(), Prefs.DEFAULT_DUE_DAYS, value);
                    updateReminderLabels();
                }));
    }

    private void updateReminderLabels() {
        int h = Prefs.getInt(requireContext(), Prefs.REMIND_HOUR, 8);
        remindHour.setText(getString(R.string.set_remind_hour) + "  ·  "
                + String.format(java.util.Locale.US, "%02d:00", h));
        defaultDue.setText(getString(R.string.set_default_due) + "  ·  "
                + Prefs.getInt(requireContext(), Prefs.DEFAULT_DUE_DAYS, 21));
    }

    private interface OnNumber {
        void picked(int value);
    }

    private void pickNumber(int titleRes, int min, int max, int current, OnNumber cb) {
        NumberPicker p = new NumberPicker(requireContext());
        p.setMinValue(min);
        p.setMaxValue(max);
        p.setValue(current);
        new AlertDialog.Builder(requireContext())
                .setTitle(titleRes)
                .setView(p)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (d, w) -> cb.picked(p.getValue()))
                .show();
    }

    // ---------------------------------------------------------------- data

    private void doBackup() {
        try {
            Uri uri = com.drtawfik.mihakk.logic.Exports.writeShareable(requireContext(), "backups",
                    "mihakk-backup-" + com.drtawfik.mihakk.util.DateUtil.today() + ".json",
                    Backup.export(requireContext()));
            com.drtawfik.mihakk.logic.Exports.share(requireContext(), uri,
                    "application/json", getString(R.string.set_backup));
            toast(getString(R.string.backup_done));
        } catch (Exception e) {
            toast(String.valueOf(e.getMessage()));
        }
    }

    private void doRestore(@Nullable Uri uri) {
        if (uri == null) return;
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
            if (in == null) throw new Exception("no stream");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            Backup.RestoreResult r = Backup.restore(requireContext(),
                    new String(bos.toByteArray(), StandardCharsets.UTF_8));
            toast(getString(R.string.restore_done, r.reviews, r.journals));
        } catch (Exception e) {
            toast(getString(R.string.restore_failed));
        }
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
    }
}
