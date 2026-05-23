package com.uit.vitour.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Review.java — Data model for a tour review.
 *
 * Firestore Path: tours/{tourId}/reviews/{reviewId}
 *
 * Fields:
 *   id         — auto-set from @DocumentId
 *   tourId     — parent tour document ID
 *   userId     — Firebase Auth UID of the reviewer
 *   userName   — Denormalized display name (avoids extra Firestore reads)
 *   userAvatar — Denormalized photo URL (nullable; may be null for email users)
 *   rating     — Integer 1–5
 *   comment    — Review text body
 *   imageUrl   — Optional photo uploaded to Cloudinary (nullable)
 *   createdAt  — Server-side timestamp set by Firestore
 *
 * WHY DENORMALIZE userName / userAvatar:
 *   Reading a user document for each review in a list would be N+1 queries.
 *   Instead we snapshot the name/avatar at write time (acceptable for reviews).
 */
public class Review {

    @DocumentId
    private String id;

    private String tourId;
    private String userId;
    private String userName;
    private String userAvatar;  // Nullable — email-only users may not have a photo
    private int    rating;      // 1–5 stars
    private String comment;
    private String imageUrl;    // Nullable — only present if user uploaded a photo

    @ServerTimestamp
    private Date createdAt;

    // ── Required no-arg constructor for Firestore ──────────────────────────
    public Review() {}

    public Review(String tourId, String userId, String userName,
                  String userAvatar, int rating, String comment) {
        this.tourId     = tourId;
        this.userId     = userId;
        this.userName   = userName;
        this.userAvatar = userAvatar;
        this.rating     = rating;
        this.comment    = comment;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getId()                  { return id; }
    public void   setId(String id)         { this.id = id; }

    public String getTourId()              { return tourId; }
    public void   setTourId(String v)      { this.tourId = v; }

    public String getUserId()              { return userId; }
    public void   setUserId(String v)      { this.userId = v; }

    public String getUserName()            { return userName; }
    public void   setUserName(String v)    { this.userName = v; }

    public String getUserAvatar()          { return userAvatar; }
    public void   setUserAvatar(String v)  { this.userAvatar = v; }

    public int    getRating()              { return rating; }
    public void   setRating(int v)         { this.rating = v; }

    public String getComment()             { return comment; }
    public void   setComment(String v)     { this.comment = v; }

    public String getImageUrl()            { return imageUrl; }
    public void   setImageUrl(String v)    { this.imageUrl = v; }

    public Date   getCreatedAt()           { return createdAt; }
    public void   setCreatedAt(Date v)     { this.createdAt = v; }
}
