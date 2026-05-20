package com.uit.vitour.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.uit.vitour.BuildConfig;
import com.uit.vitour.MainActivity;
import com.uit.vitour.databinding.ActivityLoginBinding;
import com.uit.vitour.ui.test.FirebaseTestActivity;
import com.uit.vitour.viewmodel.AuthViewModel;

/**
 * LoginActivity.java — Entry point of the app.
 *
 * MVVM pattern:
 * - View   = This Activity (observes LiveData, updates UI)
 * - ViewModel = AuthViewModel (holds state, delegates to Repository)
 * - Model  = AuthRepository (Firebase Auth calls)
 *
 * This Activity handles only UI concerns:
 * - Show / hide progress bar
 * - Display error messages via Snackbar
 * - Navigate on success
 *
 * Naming convention: Activity classes end in "Activity".
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Skip login if user is already signed in
        if (authViewModel.getCurrentUser() != null) {
            navigateToMain();
            return;
        }

        setupClickListeners();
    }

    private void setupClickListeners() {
        // ── Login button ──────────────────────────────────────────────────
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (!validateInputs(email, password)) return;

            // Observe login result
            authViewModel.login(email, password).observe(this, resource -> {
                switch (resource.status) {
                    case LOADING:
                        showLoading(true);
                        break;
                    case SUCCESS:
                        showLoading(false);
                        navigateToMain();
                        break;
                    case ERROR:
                        showLoading(false);
                        showError(resource.message);
                        break;
                }
            });
        });

        // ── Go to Register ────────────────────────────────────────────────
        binding.btnGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // ── Debug: Firebase Test ──────────────────────────────────────────
        // Only show this button in debug builds — hidden in release automatically.
        // Launches FirebaseTestActivity to verify Auth / Firestore / Storage.
        if (BuildConfig.DEBUG) {
            binding.btnFirebaseTest.setVisibility(View.VISIBLE);
            binding.btnFirebaseTest.setOnClickListener(v ->
                    startActivity(new Intent(this, FirebaseTestActivity.class)));
        }
    }

    private boolean validateInputs(String email, String password) {
        boolean valid = true;
        if (email.isEmpty()) {
            binding.tilEmail.setError("Email is required");
            valid = false;
        } else {
            binding.tilEmail.setError(null);
        }
        if (password.isEmpty()) {
            binding.tilPassword.setError("Password is required");
            valid = false;
        } else {
            binding.tilPassword.setError(null);
        }
        return valid;
    }

    private void showLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
    }

    private void showError(String message) {
        Snackbar.make(binding.getRoot(), message != null ? message : "Login failed",
                Snackbar.LENGTH_LONG).show();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
