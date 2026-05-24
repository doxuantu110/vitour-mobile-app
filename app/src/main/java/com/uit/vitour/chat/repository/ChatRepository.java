package com.uit.vitour.chat.repository;

import androidx.lifecycle.LiveData;
import com.google.firebase.firestore.DocumentSnapshot;
import com.uit.vitour.chat.model.ChatMessage;
import com.uit.vitour.chat.model.ChatSession;

import java.util.List;

public interface ChatRepository {

    interface ApiCallback {
        void onPartialResponse(String chunk);
        void onSuccess(String aiResponse, boolean isOffline);
        void onError(String error);
    }

    /**
     * Fetches paginated chat messages from Firestore
     */
    LiveData<List<ChatMessage>> getChatMessages(String sessionId, DocumentSnapshot startAfter);

    /**
     * Sends message history to Gemini and returns parsed response via callback
     */
    void sendMessageToGemini(List<ChatMessage> history, ApiCallback callback);

    /**
     * Creates or updates a chat session
     */
    void updateChatSession(ChatSession session);

    /**
     * Cancels an ongoing stream/call
     */
    void cancelStream();
}
