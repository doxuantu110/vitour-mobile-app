package com.uit.vitour.model;

/**
 * User.java — Profile data stored in Firestore under /users/{uid}.
 *
 * Firebase Auth handles credentials (email/password).
 * This model holds extra profile info we store ourselves.
 *
 * Firestore document ID = Firebase Auth UID.
 */
public class User {

    private String uid;
    private String fullName;
    private String email;
    private String photoUrl;         // Storage URL, loaded by Glide

    // ── Required no-arg constructor for Firestore ─────────────────────────
    public User() {}

    public User(String uid, String fullName, String email) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}
