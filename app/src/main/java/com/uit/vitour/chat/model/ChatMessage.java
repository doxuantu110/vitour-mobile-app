package com.uit.vitour.chat.model;

import java.util.Date;
import java.util.UUID;

public class ChatMessage {

    public static final String STATUS_SENDING = "sending";
    public static final String STATUS_SENT = "sent";
    public static final String STATUS_ERROR = "error";
    public static final String STATUS_RETRYING = "retrying";
    public static final String STATUS_OFFLINE_RESPONSE = "offline_response";

    private String id;
    private String text;
    private String role; // "user" or "assistant"
    private String status;
    private long timestamp;
    private String requestId;
    private boolean isOfflineFallback;

    public ChatMessage() {
        // Default constructor
    }

    public ChatMessage(String text, String role, String status, long timestamp) {
        this.id = UUID.randomUUID().toString();
        this.text = text;
        this.role = role;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public boolean isOfflineFallback() {
        return isOfflineFallback;
    }

    public void setOfflineFallback(boolean offlineFallback) {
        isOfflineFallback = offlineFallback;
    }
    
    // Helper to generate dummy data
    public static java.util.List<ChatMessage> getDummyData() {
        java.util.List<ChatMessage> list = new java.util.ArrayList<>();
        long now = System.currentTimeMillis();
        list.add(new ChatMessage("Hello! I'm planning a trip to Vietnam.", "user", "sent", now - 600000));
        list.add(new ChatMessage("Xin chào! I am ViTour AI Assistant. I can help you find the best tours, recommend itineraries, or answer any questions about traveling in Vietnam. Where would you like to go?", "assistant", "sent", now - 590000));
        list.add(new ChatMessage("Are there any good tours for Da Lat?", "user", "sent", now - 300000));
        list.add(new ChatMessage("Yes, Da Lat is wonderful! Here are some options:\n\n**1. Cloud Hunting Tour**\nExperience the sunrise above the clouds.\n\n**2. Romantic Couple Tour**\nVisit the Valley of Love and Langbiang mountain.\n\nWhich one sounds interesting to you?", "assistant", "sent", now - 290000));
        return list;
    }
}
