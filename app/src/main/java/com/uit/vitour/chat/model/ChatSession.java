package com.uit.vitour.chat.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class ChatSession {

    @DocumentId
    private String id;
    private String title;
    private String lastMessage;
    private Timestamp updatedAt;
    private int unreadCount;
    private Timestamp lastAssistantMessage;

    public ChatSession() {
        // Required empty constructor for Firestore
    }

    public ChatSession(String title, String lastMessage, Timestamp updatedAt, int unreadCount, Timestamp lastAssistantMessage) {
        this.title = title;
        this.lastMessage = lastMessage;
        this.updatedAt = updatedAt;
        this.unreadCount = unreadCount;
        this.lastAssistantMessage = lastAssistantMessage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Timestamp getLastAssistantMessage() {
        return lastAssistantMessage;
    }

    public void setLastAssistantMessage(Timestamp lastAssistantMessage) {
        this.lastAssistantMessage = lastAssistantMessage;
    }
}
