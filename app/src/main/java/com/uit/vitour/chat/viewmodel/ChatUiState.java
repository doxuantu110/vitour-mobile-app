package com.uit.vitour.chat.viewmodel;

import com.uit.vitour.chat.model.ChatMessage;
import java.util.List;

public class ChatUiState {
    private final List<ChatMessage> messages;
    private final boolean isTyping;
    private final String currentStreamingMessage;
    private final boolean isConfigValid;

    public ChatUiState(List<ChatMessage> messages, boolean isTyping, String currentStreamingMessage) {
        this(messages, isTyping, currentStreamingMessage, true);
    }

    public ChatUiState(List<ChatMessage> messages, boolean isTyping, String currentStreamingMessage, boolean isConfigValid) {
        this.messages = messages;
        this.isTyping = isTyping;
        this.currentStreamingMessage = currentStreamingMessage;
        this.isConfigValid = isConfigValid;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public boolean isTyping() {
        return isTyping;
    }

    public String getCurrentStreamingMessage() {
        return currentStreamingMessage;
    }

    public boolean isConfigValid() {
        return isConfigValid;
    }
}
