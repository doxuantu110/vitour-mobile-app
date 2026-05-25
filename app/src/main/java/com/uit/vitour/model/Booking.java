package com.uit.vitour.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Booking.java — Data model for a Tour Booking.
 */
public class Booking {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @DocumentId
    private String bookingId;

    private String userId;
    private String tourId;
    private String tourName;
    private String coverImageUrl;
    private Date selectedDate;
    
    private int adultsCount;
    private int childrenCount;
    
    private double adultPriceSnapshot;
    private double childPriceSnapshot;
    private double totalPrice;
    
    private String status;
    private String requestToken;

    @ServerTimestamp
    private Date createdAt;
    
    @ServerTimestamp
    private Date updatedAt;

    // Required no-arg constructor for Firestore deserialization
    public Booking() {}

    // Getters and Setters

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTourId() { return tourId; }
    public void setTourId(String tourId) { this.tourId = tourId; }

    public String getTourName() { return tourName; }
    public void setTourName(String tourName) { this.tourName = tourName; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public Date getSelectedDate() { return selectedDate; }
    public void setSelectedDate(Date selectedDate) { this.selectedDate = selectedDate; }

    public int getAdultsCount() { return adultsCount; }
    public void setAdultsCount(int adultsCount) { this. adultsCount = adultsCount; }

    public int getChildrenCount() { return childrenCount; }
    public void setChildrenCount(int childrenCount) { this.childrenCount = childrenCount; }

    public double getAdultPriceSnapshot() { return adultPriceSnapshot; }
    public void setAdultPriceSnapshot(double adultPriceSnapshot) { this.adultPriceSnapshot = adultPriceSnapshot; }

    public double getChildPriceSnapshot() { return childPriceSnapshot; }
    public void setChildPriceSnapshot(double childPriceSnapshot) { this.childPriceSnapshot = childPriceSnapshot; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequestToken() { return requestToken; }
    public void setRequestToken(String requestToken) { this.requestToken = requestToken; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
