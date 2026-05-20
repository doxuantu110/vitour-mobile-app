package com.uit.vitour.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.uit.vitour.MainActivity;
import com.uit.vitour.databinding.ActivityRegisterBinding;
import com.uit.vitour.viewmodel.AuthViewModel;

/**
 * RegisterActivity.java — New account registration.
 *
 * Same MVVM pattern as LoginActivity.
 * On success, navigates directly to MainActivity (user is signed in).
 */
public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupClickListeners();
    }

    private void setupClickListeners() {
        // ── Register button ───────────────────────────────────────────────
        binding.btnRegister.setOnClickListener(v -> {
            String fullName = binding.etFullName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (!validateInputs(fullName, email, password)) return;

            authViewModel.register(fullName, email, password).observe(this, resource -> {
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

        // ── Back to Login ─────────────────────────────────────────────────
        binding.btnGoLogin.setOnClickListener(v -> finish());
    }

    private boolean validateInputs(String fullName, String email, String password) {
        boolean valid = true;
        if (fullName.isEmpty()) {
            binding.tilFullName.setError("Full name is required");
            valid = false;
        } else {
            binding.tilFullName.setError(null);
        }
        if (email.isEmpty()) {
            binding.tilEmail.setError("Email is required");
            valid = false;
        } else {
            binding.tilEmail.setError(null);
        }
        if (password.length() < 6) {
            binding.tilPassword.setError("Password must be at least 6 characters");
            valid = false;
        } else {
            binding.tilPassword.setError(null);
        }
        return valid;
    }

    private void showLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!loading);
    }

    private void showError(String message) {
        Snackbar.make(binding.getRoot(), message != null ? message : "Registration failed",
                Snackbar.LENGTH_LONG).show();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
