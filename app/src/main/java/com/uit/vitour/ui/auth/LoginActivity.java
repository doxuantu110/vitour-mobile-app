package com.uit.vitour.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.uit.vitour.BuildConfig;
import com.uit.vitour.MainActivity;
import com.uit.vitour.R;
import com.uit.vitour.databinding.ActivityLoginBinding;
import com.uit.vitour.ui.test.FirebaseTestActivity;
import com.uit.vitour.utils.SessionManager;
import com.uit.vitour.viewmodel.AuthViewModel;

/**
 * LoginActivity.java — Entry point of the app.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * SUPPORTED SIGN-IN METHODS:
 *   1. Email + Password
 *   2. Google Sign-In (NEW)
 *
 * MVVM PATTERN:
 *   View   = LoginActivity  (observes LiveData, updates UI, handles intents)
 *   VM     = AuthViewModel  (holds auth state, survives rotation)
 *   Model  = AuthRepository (Firebase Auth + Firestore calls)
 *
 * GOOGLE SIGN-IN FLOW (step by step):
 *   1. User taps "Continue with Google"
 *   2. googleSignInClient.signInIntent launched via ActivityResultLauncher
 *   3. Google shows account picker UI
 *   4. User selects/confirms their Google account
 *   5. onGoogleSignInResult() receives the result
 *   6. GoogleSignInAccount.getIdToken() extracts the OAuth ID token
 *   7. authViewModel.signInWithGoogle(idToken) called
 *   8. Repository exchanges token for Firebase credential → signs in
 *   9. Observer receives SUCCESS → navigateToMain()
 *
 * SESSION PERSISTENCE:
 *   Firebase Auth persists sessions automatically on device.
 *   LoginActivity.onCreate() checks FirebaseAuth.getCurrentUser() — if
 *   non-null, the user is already signed in and we skip directly to MainActivity.
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;
    private SessionManager sessionManager;

    // Google Sign-In client — configured in setupGoogleSignIn()
    private GoogleSignInClient googleSignInClient;

    // Modern replacement for startActivityForResult() — lifecycle-safe
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::onGoogleSignInResult
            );

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel    = new ViewModelProvider(this).get(AuthViewModel.class);
        sessionManager   = new SessionManager(this);

        // ── Session persistence: skip login if already signed in ──────────
        // Firebase Auth restores the session automatically on app relaunch.
        // No need for manual token management.
        if (authViewModel.getCurrentUser() != null) {
            Log.d(TAG, "User already signed in → skipping login screen");
            navigateToMain();
            return;
        }

        setupGoogleSignIn();
        setupClickListeners();
    }

    // ── Google Sign-In Setup ──────────────────────────────────────────────────

    /**
     * Configures the GoogleSignInClient.
     *
     * REQUIRED: requestIdToken(webClientId) — tells Google to return an ID token
     * for the specified server audience (your Firebase project's web client).
     *
     * The Web Client ID must match the one in Firebase Console:
     *   Project Settings → Your apps → Web client (auto created by Google Service)
     */
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                // ── REQUIRED: request an ID token to pass to Firebase Auth ──
                // getString(R.string.default_web_client_id) reads the value from
                // strings.xml. Replace that placeholder with your actual Web Client ID.
                .requestIdToken(getString(R.string.default_web_client_id))
                // ── Request the user's email address ───────────────────────
                .requestEmail()
                // ── Request the user's profile (displayName, photoUrl) ─────
                .requestProfile()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
        Log.d(TAG, "Google Sign-In configured with webClientId: "
                + getString(R.string.default_web_client_id));
    }

    // ── Click Listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {

        // ── Email/Password Login ──────────────────────────────────────────
        binding.btnLogin.setOnClickListener(v -> {
            String email    = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (!validateEmailPassword(email, password)) return;

            authViewModel.login(email, password).observe(this, resource -> {
                switch (resource.status) {
                    case LOADING:
                        showLoading(true);
                        break;
                    case SUCCESS:
                        showLoading(false);
                        if (resource.data != null) {
                            sessionManager.saveSession(
                                    resource.data.getUid(),
                                    resource.data.getDisplayName() != null
                                            ? resource.data.getDisplayName() : "",
                                    resource.data.getEmail() != null
                                            ? resource.data.getEmail() : "");
                        }
                        navigateToMain();
                        break;
                    case ERROR:
                        showLoading(false);
                        showError(resource.message);
                        break;
                }
            });
        });

        // ── Google Sign-In ────────────────────────────────────────────────
        binding.btnGoogleSignIn.setOnClickListener(v -> {
            Log.d(TAG, "Google Sign-In button clicked — launching sign-in intent");
            showLoading(true);
            // Launch Google account picker
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        // ── Navigate to Register ──────────────────────────────────────────
        binding.btnGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // ── Debug: Firebase Test ──────────────────────────────────────────
        if (BuildConfig.DEBUG) {
            binding.btnFirebaseTest.setVisibility(View.VISIBLE);
            binding.btnFirebaseTest.setOnClickListener(v ->
                    startActivity(new Intent(this, FirebaseTestActivity.class)));
        }
    }

    // ── Google Sign-In Result ─────────────────────────────────────────────────

    /**
     * Called by ActivityResultLauncher after the user interacts with Google's
     * account picker UI.
     *
     * SUCCESS path:
     *   GoogleSignInAccount → getIdToken() → authViewModel.signInWithGoogle(idToken)
     *   → Firebase Auth → SUCCESS → saveSession() → navigateToMain()
     *
     * FAILURE paths:
     *   ApiException code 12501 → User cancelled (pressed Back) — silent failure
     *   ApiException code 10    → Developer error: SHA-1 not registered, wrong web client ID
     *   ApiException code 7     → Network error
     */
    private void onGoogleSignInResult(ActivityResult activityResult) {
        showLoading(false);

        if (activityResult.getData() == null) {
            Log.w(TAG, "onGoogleSignInResult: data is null (user likely cancelled)");
            return;
        }

        Task<GoogleSignInAccount> task =
                GoogleSignIn.getSignedInAccountFromIntent(activityResult.getData());

        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);

            String idToken = account.getIdToken();
            if (idToken == null) {
                Log.e(TAG, "Google Sign-In succeeded but idToken is null. "
                        + "Check that requestIdToken() in GoogleSignInOptions has the correct "
                        + "Web Client ID from Firebase Console.");
                showError("Google Sign-In failed: no ID token received.\n"
                        + "Check that your Web Client ID is set correctly.");
                return;
            }

            Log.d(TAG, "Google Sign-In succeeded — account: "
                    + account.getEmail() + ", displayName: " + account.getDisplayName());
            Log.d(TAG, "DEBUG_GOOGLE_AUTH — ID Token retrieved successfully (Length: " + idToken.length() + "): " + idToken.substring(0, Math.min(idToken.length(), 20)) + "...");

            showLoading(true);

            // Exchange the Google ID token for a Firebase credential and sign in
            authViewModel.firebaseAuthWithGoogle(idToken).observe(this, resource -> {
                switch (resource.status) {
                    case LOADING:
                        showLoading(true);
                        break;
                    case SUCCESS:
                        showLoading(false);
                        if (resource.data != null) {
                            sessionManager.saveSession(
                                    resource.data.getUid(),
                                    account.getDisplayName() != null ? account.getDisplayName() : "",
                                    account.getEmail() != null ? account.getEmail() : "",
                                    account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null,
                                    "google");
                        }
                        Log.d(TAG, "Google Sign-In → Firebase SUCCESS. Navigating to main.");
                        navigateToMain();
                        break;
                    case ERROR:
                        showLoading(false);
                        Log.e(TAG, "Firebase sign-in with Google credential FAILED: "
                                + resource.message);
                        showError(resource.message);
                        break;
                }
            });

        } catch (ApiException e) {
            showLoading(false);
            String errorMsg = getGoogleSignInErrorMessage(e.getStatusCode());
            Log.e(TAG, "DEBUG_GOOGLE_AUTH — ApiException caught! Status code: " + e.getStatusCode());
            Log.e(TAG, "DEBUG_GOOGLE_AUTH — Error Message: " + errorMsg);
            if (e.getStatusCode() == 10) {
                Log.e(TAG, "DEBUG_GOOGLE_AUTH — [CRITICAL] SHA-1 fingerprint is likely missing in Firebase Console or your Web Client ID is incorrect. Check strings.xml and google-services.json.");
            }
            showError(errorMsg);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean validateEmailPassword(String email, String password) {
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
        binding.btnGoogleSignIn.setEnabled(!loading);
    }

    private void showError(String message) {
        String errorMsg = message != null ? message : "Sign in failed. Please try again.";
        Snackbar.make(binding.getRoot(), errorMsg, Snackbar.LENGTH_LONG).show();
        android.widget.Toast.makeText(this, errorMsg, android.widget.Toast.LENGTH_LONG).show();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        // Clear the back stack so the user can't go back to login with Back button
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Maps Google Sign-In API error codes to user-readable messages.
     *
     * KEY CODES:
     *   10     → DEVELOPER_ERROR — most common. Causes:
     *            a) SHA-1 fingerprint not registered in Firebase Console
     *            b) Wrong Web Client ID in strings.xml
     *            c) Package name mismatch in google-services.json
     *   12501  → SIGN_IN_CANCELLED — user pressed Back
     *   12500  → SIGN_IN_FAILED — general failure
     *   7      → NETWORK_ERROR — no internet
     *
     * HOW TO FIX DEVELOPER_ERROR (code 10):
     *   1. Run: ./gradlew signingReport  (in terminal)
     *   2. Copy the SHA-1 fingerprint from the debug section
     *   3. Firebase Console → Project Settings → Your apps → Add fingerprint
     *   4. Download the new google-services.json
     */
    private String getGoogleSignInErrorMessage(int statusCode) {
        switch (statusCode) {
            case 10:
                return "Google Sign-In configuration error.\n"
                        + "Fix: Register your SHA-1 fingerprint in Firebase Console.\n"
                        + "Run: ./gradlew signingReport  to get your SHA-1.";
            case 12501:
                return null; // User cancelled — no error to show
            case 12500:
                return "Google Sign-In failed. Please try again.";
            case 7:
                return "Network error. Check your internet connection.";
            default:
                return "Google Sign-In error (code " + statusCode + "). Please try again.";
        }
    }
}
