package com.example.orion;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class BiometricPrompt extends AppCompatActivity {

    // BiometricPrompt instances
    androidx.biometric.BiometricPrompt biometricPrompt;
    androidx.biometric.BiometricPrompt.PromptInfo promptInfo;

    // Global values
    private int VAL1 = -1, VAL2 = -1, VAL3 = -1;
    private String INPUT;
    private int BARCODE_INDEX = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Manage error toasts
        manageBiometricErrors();

        // Define action of success or failures
        initializeBiometricPrompt();

        // Authenticate the user
        biometricPrompt.authenticate(promptInfo);
    }

    /**
     * Manages the biometric errors and shows appropriate toast messages.
     */
    private void manageBiometricErrors() {
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(getApplicationContext(), "Device Doesn't have fingerprint", Toast.LENGTH_SHORT).show();
                biometricUnlocked();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(getApplicationContext(), "Not Working", Toast.LENGTH_SHORT).show();
                biometricUnlocked();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(getApplicationContext(), "No FingerPrint Assigned", Toast.LENGTH_SHORT).show();
                biometricUnlocked();
                break;
        }
    }

    /**
     * Initializes the BiometricPrompt and its callback methods.
     */
    private void initializeBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new androidx.biometric.BiometricPrompt(this, executor, new androidx.biometric.BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                startBiometricPrompt();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull androidx.biometric.BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                biometricUnlocked();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });

        promptInfo = new androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Orion")
                .setDescription("Use Fingerprint To Log In")
                .setDeviceCredentialAllowed(true)
                .build();
    }

    /**
     * Restarts the biometric prompt if authentication fails.
     */
    private void startBiometricPrompt() {
        Intent intent = new Intent(this, BiometricPrompt.class);
        startActivity(intent);
        finish();
    }

    /**
     * Exits to the home activity with a success status.
     */
    private void biometricUnlocked() {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("lock_passed", true);
        startActivity(i);
        finish();
    }
}
