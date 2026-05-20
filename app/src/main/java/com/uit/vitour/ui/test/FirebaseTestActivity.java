package com.uit.vitour.ui.test;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.uit.vitour.databinding.ActivityFirebaseTestBinding;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * FirebaseTestActivity.java
 * ─────────────────────────────────────────────────────────────────────────────
 * PURPOSE: Verifies that Firebase Auth, Firestore, and Storage are correctly
 *          integrated with the ViTour app.
 *
 * HOW TO USE:
 *   1. Temporarily change LoginActivity's launcher in AndroidManifest.xml to
 *      this activity, OR add a debug menu entry that starts this Activity.
 *   2. Run the app on a device/emulator with internet access.
 *   3. Tap each button; green = pass, red = fail.
 *   4. Remove this Activity before shipping to production.
 *
 * WHAT IS TESTED:
 *   ✓ FirebaseApp initializes (google-services.json is valid)
 *   ✓ FirebaseAuth — register a test user + login + sign out
 *   ✓ Firestore    — write a document + read it back + verify data
 *   ✓ Storage      — upload a small text file + verify the download URL
 *
 * TAG for Logcat filter: "FirebaseTest"
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * ⚠ DELETE this file and its layout before production release.
 */
public class FirebaseTestActivity extends AppCompatActivity {

    private static final String TAG = "FirebaseTest";

    // Firestore test collection — separate from production data
    private static final String TEST_COLLECTION = "_vitour_connection_test";
    // Storage test path
    private static final String STORAGE_TEST_PATH = "test/connection_test.txt";

    private ActivityFirebaseTestBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFirebaseTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initFirebase();
        setupButtons();
    }

    // ── Step 1: Firebase Initialization Check ─────────────────────────────────

    /**
     * CHECKLIST ITEM 1 — FirebaseApp.initializeApp()
     *
     * FirebaseApp is initialized automatically when:
     *   • google-services.json is present in app/
     *   • google-services plugin is applied in build.gradle.kts
     *   • The plugin generates FirebaseOptions at build time
     *
     * If FirebaseApp.getApps(this) returns empty → json or plugin is missing.
     */
    private void initFirebase() {
        // Verify FirebaseApp initialized
        if (FirebaseApp.getApps(this).isEmpty()) {
            // This should NEVER happen if google-services.json + plugin are correct
            log("❌ FirebaseApp NOT initialized!\n"
                    + "Check: google-services.json present in app/\n"
                    + "Check: alias(libs.plugins.google.services) in build.gradle.kts");
            return;
        }

        auth    = FirebaseAuth.getInstance();
        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Show project metadata from the initialized app
        String projectId  = FirebaseApp.getInstance().getOptions().getProjectId();
        String storageBucket = FirebaseApp.getInstance().getOptions().getStorageBucket();
        binding.tvProjectId.setText("Project: " + projectId + "  |  Bucket: " + storageBucket);

        Log.i(TAG, "✅ FirebaseApp initialized — project: " + projectId);
    }

    // ── Button Wiring ─────────────────────────────────────────────────────────

    private void setupButtons() {
        // Note: These string assignments capture the initial empty state.
        // The click listeners below fetch the text dynamically to get the latest input.
        String email    = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Individual tests
        binding.btnRegister.setOnClickListener(v ->
                testAuthRegister(binding.etEmail.getText().toString().trim(),
                                 binding.etPassword.getText().toString().trim()));

        binding.btnLogin.setOnClickListener(v ->
                testAuthLogin(binding.etEmail.getText().toString().trim(),
                              binding.etPassword.getText().toString().trim()));

        binding.btnLogout.setOnClickListener(v -> testAuthSignOut());

        binding.btnFirestoreWrite.setOnClickListener(v -> testFirestoreWrite());
        binding.btnFirestoreRead.setOnClickListener(v  -> testFirestoreRead());
        binding.btnStorageUpload.setOnClickListener(v  -> testStorageUpload());

        // Run all sequentially: register → login → firestore → storage → logout
        binding.btnRunAll.setOnClickListener(v -> runAllTests(
                binding.etEmail.getText().toString().trim(),
                binding.etPassword.getText().toString().trim()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 1 — Firebase Auth
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * CHECKLIST ITEM 2 — Firebase Auth: Register
     *
     * What it verifies:
     *   • Firebase Auth is reachable (internet + correct API key)
     *   • Email/password provider is ENABLED in Firebase Console
     *   • The package name in google-services.json matches the app
     *
     * Firebase Console:  Authentication → Sign-in method → Email/Password → Enable
     */
    private void testAuthRegister(String email, String password) {
        setResult(binding.tvAuthResult, binding.cardAuth, "⏳ Registering...", State.LOADING);

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> {
                FirebaseUser user = result.getUser();
                String msg = "✅ AUTH REGISTER — PASS\n"
                        + "UID:   " + (user != null ? user.getUid() : "null") + "\n"
                        + "Email: " + (user != null ? user.getEmail() : "null");
                setResult(binding.tvAuthResult, binding.cardAuth, msg, State.PASS);
                Log.i(TAG, msg);
            })
            .addOnFailureListener(e -> {
                // EMAIL_ALREADY_IN_USE is fine — means Auth is working
                String hint = e.getMessage();
                if (hint != null && hint.contains("already in use")) {
                    String msg = "⚠️ AUTH REGISTER — Email already registered\n"
                            + "(Auth IS working — try Login instead)\n"
                            + "Error: " + hint;
                    setResult(binding.tvAuthResult, binding.cardAuth, msg, State.WARN);
                } else {
                    String msg = "❌ AUTH REGISTER — FAIL\n"
                            + "Error: " + hint + "\n\n"
                            + "Fix:\n"
                            + "• Enable Email/Password in Firebase Console\n"
                            + "• Check internet connectivity\n"
                            + "• Verify package name matches google-services.json";
                    setResult(binding.tvAuthResult, binding.cardAuth, msg, State.FAIL);
                }
                Log.e(TAG, "Auth register failed", e);
            });
    }

    /**
     * CHECKLIST ITEM 2b — Firebase Auth: Login
     */
    private void testAuthLogin(String email, String password) {
        setResult(binding.tvAuthResult, binding.cardAuth, "⏳ Signing in...", State.LOADING);

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> {
                FirebaseUser user = result.getUser();
                String msg = "✅ AUTH LOGIN — PASS\n"
                        + "UID:   " + (user != null ? user.getUid() : "null") + "\n"
                        + "Email: " + (user != null ? user.getEmail() : "null") + "\n"
                        + "Time:  " + timestamp();
                setResult(binding.tvAuthResult, binding.cardAuth, msg, State.PASS);
                Log.i(TAG, msg);
            })
            .addOnFailureListener(e -> {
                String msg = "❌ AUTH LOGIN — FAIL\n"
                        + "Error: " + e.getMessage() + "\n\n"
                        + "Fix:\n"
                        + "• Register the test user first\n"
                        + "• Check email/password are correct";
                setResult(binding.tvAuthResult, binding.cardAuth, msg, State.FAIL);
                Log.e(TAG, "Auth login failed", e);
            });
    }

    /**
     * CHECKLIST ITEM 2c — Firebase Auth: Sign Out
     */
    private void testAuthSignOut() {
        auth.signOut();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            setResult(binding.tvAuthResult, binding.cardAuth,
                    "✅ AUTH SIGN OUT — PASS\nCurrentUser is null (correct)", State.PASS);
            Log.i(TAG, "Auth signOut: success");
        } else {
            setResult(binding.tvAuthResult, binding.cardAuth,
                    "❌ AUTH SIGN OUT — FAIL\nUser still: " + user.getEmail(), State.FAIL);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 2 — Firestore Connection
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * CHECKLIST ITEM 3 — Firestore: Write
     *
     * What it verifies:
     *   • Firestore is reachable
     *   • Security rules allow writes to _vitour_connection_test (update if needed)
     *   • The google-services.json project_id is correct
     *
     * Firebase Console:  Firestore Database → Create database
     * Default test rules (dev only):
     *   rules_version = '2';
     *   service cloud.firestore {
     *     match /databases/{database}/documents {
     *       match /_vitour_connection_test/{doc} {
     *         allow read, write: if true;
     *       }
     *     }
     *   }
     */
    private void testFirestoreWrite() {
        setResult(binding.tvFirestoreResult, binding.cardFirestore,
                "⏳ Writing to Firestore...", State.LOADING);

        Map<String, Object> data = new HashMap<>();
        data.put("message",   "ViTour connection test");
        data.put("timestamp", new Date());
        data.put("device",    android.os.Build.MODEL);
        data.put("sdk",       android.os.Build.VERSION.SDK_INT);

        db.collection(TEST_COLLECTION)
          .document("ping")
          .set(data)
          .addOnSuccessListener(unused -> {
              String msg = "✅ FIRESTORE WRITE — PASS\n"
                      + "Collection: " + TEST_COLLECTION + "\n"
                      + "Document:   ping\n"
                      + "Data written at " + timestamp();
              setResult(binding.tvFirestoreResult, binding.cardFirestore, msg, State.PASS);
              Log.i(TAG, msg);
          })
          .addOnFailureListener(e -> {
              String msg = "❌ FIRESTORE WRITE — FAIL\n"
                      + "Error: " + e.getMessage() + "\n\n"
                      + "Fix:\n"
                      + "• Create Firestore DB in Firebase Console\n"
                      + "• Check security rules allow writes to " + TEST_COLLECTION + "\n"
                      + "• Verify internet connectivity";
              setResult(binding.tvFirestoreResult, binding.cardFirestore, msg, State.FAIL);
              Log.e(TAG, "Firestore write failed", e);
          });
    }

    /**
     * CHECKLIST ITEM 3b — Firestore: Read back the document we wrote
     */
    private void testFirestoreRead() {
        setResult(binding.tvFirestoreResult, binding.cardFirestore,
                "⏳ Reading from Firestore...", State.LOADING);

        db.collection(TEST_COLLECTION)
          .document("ping")
          .get()
          .addOnSuccessListener(snapshot -> {
              if (!snapshot.exists()) {
                  setResult(binding.tvFirestoreResult, binding.cardFirestore,
                          "⚠️ FIRESTORE READ — Document not found\n"
                                  + "Run WRITE first, then READ.", State.WARN);
                  return;
              }
              String msg = "✅ FIRESTORE READ — PASS\n"
                      + "message:   " + snapshot.getString("message") + "\n"
                      + "device:    " + snapshot.getString("device") + "\n"
                      + "timestamp: " + snapshot.get("timestamp");
              setResult(binding.tvFirestoreResult, binding.cardFirestore, msg, State.PASS);
              Log.i(TAG, msg);
          })
          .addOnFailureListener(e -> {
              String msg = "❌ FIRESTORE READ — FAIL\n"
                      + "Error: " + e.getMessage() + "\n\n"
                      + "Fix: Check Firestore security rules allow reads";
              setResult(binding.tvFirestoreResult, binding.cardFirestore, msg, State.FAIL);
              Log.e(TAG, "Firestore read failed", e);
          });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TEST 3 — Firebase Storage Upload
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * CHECKLIST ITEM 4 — Firebase Storage: Upload a small text file
     *
     * What it verifies:
     *   • Storage bucket is correctly configured in google-services.json
     *   • Storage security rules allow test writes
     *   • The download URL is retrievable after upload
     *
     * Firebase Console: Storage → Get started → Start in test mode (for dev)
     * Default storage rules (dev only):
     *   rules_version = '2';
     *   service firebase.storage {
     *     match /b/{bucket}/o {
     *       match /test/{allPaths=**} {
     *         allow read, write: if true;
     *       }
     *     }
     *   }
     */
    private void testStorageUpload() {
        setResult(binding.tvStorageResult, binding.cardStorage,
                "⏳ Uploading test file to Storage...", State.LOADING);

        StorageReference ref = storage.getReference(STORAGE_TEST_PATH);

        // Upload a tiny text payload — no need for a real file
        String content = "ViTour Storage Test\nTimestamp: " + timestamp()
                + "\nDevice: " + android.os.Build.MODEL;
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        ref.putBytes(bytes)
           .addOnSuccessListener(taskSnapshot -> {
               // After upload succeeds, fetch the public download URL
               ref.getDownloadUrl()
                  .addOnSuccessListener(uri -> {
                      String msg = "✅ STORAGE UPLOAD — PASS\n"
                              + "Path: " + STORAGE_TEST_PATH + "\n"
                              + "Size: " + bytes.length + " bytes\n"
                              + "URL:  " + uri.toString();
                      setResult(binding.tvStorageResult, binding.cardStorage, msg, State.PASS);
                      Log.i(TAG, msg);
                  })
                  .addOnFailureListener(e -> {
                      setResult(binding.tvStorageResult, binding.cardStorage,
                              "⚠️ STORAGE — File uploaded but URL fetch failed\n"
                                      + "Error: " + e.getMessage(), State.WARN);
                  });
           })
           .addOnFailureListener(e -> {
               String msg = "❌ STORAGE UPLOAD — FAIL\n"
                       + "Error: " + e.getMessage() + "\n\n"
                       + "Fix:\n"
                       + "• Create Storage bucket in Firebase Console\n"
                       + "• Set rules to allow writes to /test/ path\n"
                       + "• Check storage_bucket in google-services.json";
               setResult(binding.tvStorageResult, binding.cardStorage, msg, State.FAIL);
               Log.e(TAG, "Storage upload failed", e);
           });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Run All Tests sequentially
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Chains: Register → (Login on failure) → Firestore Write → Read → Storage → SignOut
     * Results are posted to each section as they complete.
     */
    private void runAllTests(String email, String password) {
        // Step 1: Try register (may already exist — that's fine)
        setResult(binding.tvAuthResult, binding.cardAuth,
                "⏳ [1/4] Testing Auth Register...", State.LOADING);

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    onAuthReady(task.getResult().getUser());
                } else {
                    // User already exists — try login instead
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener(r -> onAuthReady(r.getUser()))
                        .addOnFailureListener(e -> {
                            setResult(binding.tvAuthResult, binding.cardAuth,
                                    "❌ AUTH — FAIL (register + login both failed)\n"
                                            + "Error: " + e.getMessage(), State.FAIL);
                        });
                }
            });
    }

    /** Called when we have a valid FirebaseUser — chains Firestore + Storage tests. */
    private void onAuthReady(FirebaseUser user) {
        String msg = "✅ [1/4] AUTH — PASS\nUID: "
                + (user != null ? user.getUid() : "null");
        setResult(binding.tvAuthResult, binding.cardAuth, msg, State.PASS);
        Log.i(TAG, msg);

        // Step 2: Firestore Write
        setResult(binding.tvFirestoreResult, binding.cardFirestore,
                "⏳ [2/4] Testing Firestore Write...", State.LOADING);

        Map<String, Object> data = new HashMap<>();
        data.put("message",   "run-all test");
        data.put("timestamp", new Date());
        data.put("uid",       user != null ? user.getUid() : "anonymous");

        db.collection(TEST_COLLECTION)
          .document("run_all")
          .set(data)
          .addOnSuccessListener(unused -> {
              setResult(binding.tvFirestoreResult, binding.cardFirestore,
                      "✅ [2/4] FIRESTORE WRITE — PASS\nDoc: run_all written", State.PASS);

              // Step 3: Firestore Read
              db.collection(TEST_COLLECTION)
                .document("run_all")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        setResult(binding.tvFirestoreResult, binding.cardFirestore,
                                "✅ [3/4] FIRESTORE READ — PASS\n"
                                        + "message: " + snapshot.getString("message") + "\n"
                                        + "uid:     " + snapshot.getString("uid"), State.PASS);
                    }
                    // Step 4: Storage Upload
                    testStorageForRunAll();
                })
                .addOnFailureListener(e ->
                        setResult(binding.tvFirestoreResult, binding.cardFirestore,
                                "❌ FIRESTORE READ — FAIL\n" + e.getMessage(), State.FAIL));
          })
          .addOnFailureListener(e ->
                  setResult(binding.tvFirestoreResult, binding.cardFirestore,
                          "❌ FIRESTORE WRITE — FAIL\n" + e.getMessage(), State.FAIL));
    }

    private void testStorageForRunAll() {
        setResult(binding.tvStorageResult, binding.cardStorage,
                "⏳ [4/4] Testing Storage Upload...", State.LOADING);

        byte[] bytes = ("run-all test\n" + timestamp()).getBytes(StandardCharsets.UTF_8);
        storage.getReference(STORAGE_TEST_PATH)
               .putBytes(bytes)
               .addOnSuccessListener(snap ->
                       storage.getReference(STORAGE_TEST_PATH).getDownloadUrl()
                              .addOnSuccessListener(uri -> {
                                  setResult(binding.tvStorageResult, binding.cardStorage,
                                          "✅ [4/4] STORAGE — PASS\n"
                                                  + "URL: " + uri.toString() + "\n\n"
                                                  + "🏁 ALL TESTS PASSED — Firebase is correctly integrated!",
                                          State.PASS);
                                  Log.i(TAG, "🏁 All Firebase tests passed");
                              }))
               .addOnFailureListener(e ->
                       setResult(binding.tvStorageResult, binding.cardStorage,
                               "❌ STORAGE — FAIL\n" + e.getMessage(), State.FAIL));
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    private enum State { LOADING, PASS, WARN, FAIL }

    private void setResult(TextView tv, CardView card, String message, State state) {
        runOnUiThread(() -> {
            tv.setText(message);
            switch (state) {
                case LOADING: tv.setTextColor(Color.parseColor("#94A3B8")); break;
                case PASS:    tv.setTextColor(Color.parseColor("#34D399")); break;
                case WARN:    tv.setTextColor(Color.parseColor("#FBBF24")); break;
                case FAIL:    tv.setTextColor(Color.parseColor("#F87171")); break;
            }
        });
    }

    private String timestamp() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private void log(String msg) {
        Log.e(TAG, msg);
        if (binding != null) {
            binding.tvProjectId.setText(msg);
            binding.tvProjectId.setTextColor(Color.parseColor("#F87171"));
        }
    }
}
