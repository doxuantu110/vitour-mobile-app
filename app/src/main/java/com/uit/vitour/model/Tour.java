package com.uit.vitour.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

/**
 * Tour.java — Data model for a travel tour / destination.
 *
 * Naming convention: plain POJO with no-arg constructor (required by Firestore).
 * The @DocumentId annotation automatically maps the Firestore doc ID to this field.
 *
 * Where to add new fields: just add the field + getter/setter here and in
 * Firestore. Firestore's SDK handles missing fields gracefully (they become null).
 */
public class Tour {

    @DocumentId
    private String id;

    private String name;
    private String location;         // "Hội An, Quảng Nam"
    private String description;
    private String coverImageUrl;    // Remote URL loaded by Glide
    private double price;            // Price per person (VND)
    private double rating;           // Average rating 0.0 – 5.0
    private int reviewCount;
    private int durationDays;
    private boolean isFeatured;
    private List<String> tags;       // ["beach", "cultural", "food"]

    // ── Detail Screen Fields (Nullable to prevent crashes) ────────
    private List<String> imageUrls;  // For ViewPager2 gallery
    private Double latitude;         // For Google Maps
    private Double longitude;        // For Google Maps

    @ServerTimestamp
    private Date createdAt;

    // ── Required no-arg constructor for Firestore deserialization ─────────
    public Tour() {}

    // ── Convenience constructor for Retrofit / local testing ─────────────
    public Tour(String id, String name, String location, String coverImageUrl,
                double price, double rating) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.coverImageUrl = coverImageUrl;
        this.price = price;
        this.rating = rating;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }

    public boolean isFeatured() { return isFeatured; }
    public void setFeatured(boolean featured) { isFeatured = featured; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
