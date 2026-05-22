package com.uit.vitour.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * SessionManager.java — Lightweight session helper for the ViTour app.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * IMPORTANT: Firebase Auth ALREADY handles session persistence automatically.
 * FirebaseAuth.getCurrentUser() returns null when signed out, and a FirebaseUser
 * when a valid session exists — even after the app is killed and relaunched.
 *
 * This class provides:
 *   1. A SINGLE PLACE to check session state (avoids scattered FirebaseAuth calls)
 *   2. SharedPreferences cache of uid + displayName for instant offline access
 *      (so ProfileFragment can show a name before the Firestore round-trip completes)
 *   3. A clear contract for logout (signs out from both Firebase Auth AND Google)
 *
 * USAGE:
 *   SessionManager session = new SessionManager(context);
 *
 *   // On login success:
 *   session.saveSession(firebaseUser.getUid(), firebaseUser.getDisplayName());
 *
 *   // In LoginActivity.onCreate():
 *   if (session.isLoggedIn()) { navigateToMain(); }
 *
 *   // In ProfileFragment logout:
 *   session.logout(googleSignInClient);
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String PREFS = "vitour_session";
    private static final String KEY_UID          = "uid";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_EMAIL        = "email";
    private static final String KEY_PHOTO_URL    = "photo_url";
    private static final String KEY_PROVIDER     = "provider";   // "email" | "google"

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                       .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Session check
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true if a Firebase Auth session is active.
     *
     * SOURCE OF TRUTH: FirebaseAuth.getCurrentUser() — not SharedPreferences.
     * SharedPreferences is used only to cache profile data for the UI.
     */
    public boolean isLoggedIn() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean loggedIn = user != null;
        Log.d(TAG, "isLoggedIn() → " + loggedIn
                + (user != null ? " uid=" + user.getUid() : ""));
        return loggedIn;
    }

    /**
     * Returns the current FirebaseUser, or null if no session.
     * Equivalent to FirebaseAuth.getInstance().getCurrentUser().
     */
    public FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save session (call after successful login/register)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Caches user profile data locally so the UI can display it instantly
     * without waiting for a Firestore round-trip.
     *
     * @param uid         Firebase Auth UID
     * @param displayName User's name (from Google account or registration form)
     * @param email       User's email
     * @param photoUrl    Profile photo URL (null for email/password users)
     * @param provider    "email" or "google"
     */
    public void saveSession(String uid, String displayName, String email,
                            String photoUrl, String provider) {
        prefs.edit()
             .putString(KEY_UID, uid)
             .putString(KEY_DISPLAY_NAME, displayName)
             .putString(KEY_EMAIL, email)
             .putString(KEY_PHOTO_URL, photoUrl)
             .putString(KEY_PROVIDER, provider)
             .apply();
        Log.d(TAG, "Session saved — uid=" + uid + ", provider=" + provider);
    }

    /** Convenience overload for email/password login (no photo, provider="email"). */
    public void saveSession(String uid, String displayName, String email) {
        saveSession(uid, displayName, email, null, "email");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read cached session data
    // ─────────────────────────────────────────────────────────────────────────

    public String getCachedUid()         { return prefs.getString(KEY_UID, null); }
    public String getCachedDisplayName() { return prefs.getString(KEY_DISPLAY_NAME, ""); }
    public String getCachedEmail()       { return prefs.getString(KEY_EMAIL, ""); }
    public String getCachedPhotoUrl()    { return prefs.getString(KEY_PHOTO_URL, null); }
    public String getCachedProvider()    { return prefs.getString(KEY_PROVIDER, "email"); }

    /** @return true if the cached session was created via Google Sign-In. */
    public boolean isGoogleUser() {
        return "google".equals(getCachedProvider());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Logout
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Signs the user out of Firebase Auth and clears the local cache.
     * Does NOT sign out from Google — the next Google Sign-In will
     * still auto-select the same account.
     * Use this for simple logout.
     */
    public void logout() {
        FirebaseAuth.getInstance().signOut();
        clearLocalSession();
        Log.d(TAG, "Logged out — Firebase Auth session cleared");
    }

    /**
     * Full Google Sign-Out: signs out from Firebase AND revokes Google account
     * access. The next time the user taps "Continue with Google", they will
     * see the account picker again (no auto-sign-in).
     *
     * Use this when the user explicitly requests "Sign out".
     *
     * @param googleSignInClient The GoogleSignInClient created in the Activity/Fragment.
     *                           Pass null if Google Sign-In was never used.
     * @param onComplete         Runnable to execute after sign-out completes (navigate to login).
     */
    public void logoutGoogle(com.google.android.gms.auth.api.signin.GoogleSignInClient googleSignInClient,
                             Runnable onComplete) {
        FirebaseAuth.getInstance().signOut();

        if (googleSignInClient != null) {
            // revokeAccess() is optional — use signOut() for simpler UX:
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Log.d(TAG, "Google sign-out complete. success=" + task.isSuccessful());
                clearLocalSession();
                if (onComplete != null) onComplete.run();
            });
        } else {
            clearLocalSession();
            if (onComplete != null) onComplete.run();
        }
    }

    /** Clears only the SharedPreferences cache. Does NOT touch Firebase Auth. */
    private void clearLocalSession() {
        prefs.edit().clear().apply();
        Log.d(TAG, "Local session cache cleared");
    }
}
