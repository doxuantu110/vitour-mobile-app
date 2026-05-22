package com.uit.vitour.model;

/**
 * User.java — Profile data stored in Firestore under /users/{uid}.
 *
 * Firebase Auth handles credentials (email/password or Google OAuth).
 * This model holds the extra profile info we store and sync ourselves.
 *
 * Firestore document ID = Firebase Auth UID.
 *
 * SIGN-IN METHODS SUPPORTED:
 *   • Email/Password — fullName set at registration
 *   • Google Sign-In — displayName + photoUrl auto-populated from Google account
 *
 * FIELD NOTES:
 *   photoUrl  → For Google users: populated from GoogleSignInAccount.getPhotoUrl()
 *               For email users: null (user can upload manually via Firebase Storage)
 *   provider  → "email" | "google" — lets the ProfileFragment adjust UI
 *               (e.g. hide "Change Password" for Google users)
 */
public class User {

    private String uid;
    private String fullName;
    private String email;
    private String photoUrl;     // Firebase Storage URL (email) or Google photo URL
    private String provider;     // "email" or "google"

    // ── Required no-arg constructor for Firestore deserialization ─────────
    public User() {}

    /** Constructor for email/password registration. */
    public User(String uid, String fullName, String email) {
        this.uid      = uid;
        this.fullName = fullName;
        this.email    = email;
        this.provider = "email";
    }

    /** Constructor for Google Sign-In — includes photoUrl and provider. */
    public User(String uid, String fullName, String email, String photoUrl, String provider) {
        this.uid      = uid;
        this.fullName = fullName;
        this.email    = email;
        this.photoUrl = photoUrl;
        this.provider = provider;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public String getUid()                   { return uid; }
    public void   setUid(String uid)         { this.uid = uid; }

    public String getFullName()              { return fullName; }
    public void   setFullName(String v)      { this.fullName = v; }

    public String getEmail()                 { return email; }
    public void   setEmail(String v)         { this.email = v; }

    public String getPhotoUrl()              { return photoUrl; }
    public void   setPhotoUrl(String v)      { this.photoUrl = v; }

    public String getProvider()              { return provider; }
    public void   setProvider(String v)      { this.provider = v; }

    // ── Convenience helpers ───────────────────────────────────────────────

    /** @return true if the user signed in via Google (hides email/password UI). */
    public boolean isGoogleUser() {
        return "google".equals(provider);
    }

    /** @return Display name — falls back to email prefix if fullName is null. */
    public String getDisplayName() {
        if (fullName != null && !fullName.isEmpty()) return fullName;
        if (email != null && email.contains("@")) return email.split("@")[0];
        return "Traveller";
    }
}
