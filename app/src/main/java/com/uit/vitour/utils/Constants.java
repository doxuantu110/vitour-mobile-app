package com.uit.vitour.utils;

/**
 * Constants.java — App-wide constant values.
 *
 * Naming convention: ALL_CAPS for constants, grouped by category.
 *
 * Do NOT put context-dependent values here (use resources instead).
 */
public final class Constants {

    // Prevent instantiation
    private Constants() {}

    // ── Firestore Collection Names ────────────────────────────────────────
    public static final String COLLECTION_TOURS = "tours";
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_BOOKMARKS = "bookmarks";

    // ── Firestore Field Names ─────────────────────────────────────────────
    public static final String FIELD_FEATURED = "isFeatured";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_RATING = "rating";

    // ── Retrofit Base URL ─────────────────────────────────────────────────
    // Replace with your actual API base URL.
    public static final String BASE_URL = "https://api.vitour.example.com/v1/";

    // ── Shared Preferences ────────────────────────────────────────────────
    public static final String PREFS_NAME = "vitour_prefs";
    public static final String PREF_USER_ID = "user_id";

    // ── Pagination ────────────────────────────────────────────────────────
    public static final long PAGE_SIZE = 20;

    // ── Intent Extras ─────────────────────────────────────────────────────
    public static final String EXTRA_TOUR_ID = "extra_tour_id";
}
