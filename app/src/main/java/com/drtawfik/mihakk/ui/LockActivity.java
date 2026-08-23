package com.drtawfik.mihakk.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.drtawfik.mihakk.MainActivity;
import com.drtawfik.mihakk.R;
import com.drtawfik.mihakk.alarm.ReminderScheduler;
import com.drtawfik.mihakk.data.Prefs;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LockActivity extends BaseActivity {

    private TextInputEditText pin;
    private TextView error;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);

        if (!Prefs.lockEnabled(this)) {
            proceed();
            return;
        }

        setContentView(R.layout.activity_lock);
        pin = findViewById(R.id.pin);
        error = findViewById(R.id.error);
        MaterialButton unlock = findViewById(R.id.unlock);
        MaterialButton useBio = findViewById(R.id.useBiometric);

        unlock.setOnClickListener(v -> attempt());
        pin.setOnEditorActionListener((v, id, ev) -> {
            attempt();
            return true;
        });

        if (Prefs.getBool(this, Prefs.LOCK_BIOMETRIC, false) && biometricAvailable()) {
            useBio.setVisibility(View.VISIBLE);
            useBio.setOnClickListener(v -> promptBiometric());
            promptBiometric();
        }
    }

    private void attempt() {
        String entered = pin.getText() == null ? "" : pin.getText().toString();
        if (Pin.check(this, entered)) {
            proceed();
        } else {
            error.setText(R.string.lock_wrong);
            error.setVisibility(View.VISIBLE);
            pin.setText("");
        }
    }

    private boolean biometricAvailable() {
        return BiometricManager.from(this).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void promptBiometric() {
        BiometricPrompt prompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        proceed();
                    }
                });
        prompt.authenticate(new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.lock_biometric_prompt))
                .setNegativeButtonText(getString(R.string.lock_use_pin))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build());
    }

    private void proceed() {
        ReminderScheduler.schedule(this);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
