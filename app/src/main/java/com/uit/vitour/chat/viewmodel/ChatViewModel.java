package com.uit.vitour.chat.viewmodel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.uit.vitour.BuildConfig;
import com.uit.vitour.chat.model.ChatMessage;
import com.uit.vitour.chat.repository.ChatRepository;
import com.uit.vitour.chat.repository.ChatRepositoryImpl;
import com.uit.vitour.chat.repository.GeminiConfigValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatViewModel extends ViewModel {

    private static final String TAG = "ChatViewModel";

    private final ChatRepository chatRepository;
    
    private final MutableLiveData<ChatUiState> uiState;
    private final MutableLiveData<List<ChatMessage>> messagesLiveData = new MutableLiveData<>(new ArrayList<>());
    
    private boolean isRequestPending = false;
    private DocumentSnapshot lastVisible;
    private String currentRequestId;
    private boolean isConfigValid;

    // Streaming throttle mechanism
    private final Handler throttleHandler = new Handler(Looper.getMainLooper());
    private Runnable throttleRunnable;
    private String pendingStreamChunk = "";
    private long lastUpdateTime = 0;
    private static final long THROTTLE_MS = 50;

    public ChatViewModel(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
        
        // Hard validate API key on initialization
        this.isConfigValid = GeminiConfigValidator.isValidConfiguration(BuildConfig.GEMINI_API_KEY);
        this.uiState = new MutableLiveData<>(new ChatUiState(new ArrayList<>(), false, "", isConfigValid));
        
        if (!this.isConfigValid) {
            Log.e(TAG, "ChatViewModel initialized with INVALID API key configuration.");
        }
    }

    public LiveData<ChatUiState> getUiState() {
        return uiState;
    }

    public void loadMoreMessages(String sessionId) {
        // Pagination logic here
    }

    private void updateState(List<ChatMessage> msgs, boolean isTyping, String streamText) {
        uiState.setValue(new ChatUiState(msgs, isTyping, streamText, isConfigValid));
    }

    public void sendMessage(String sessionId, String text, Map<String, Object> context) {
        if (!isConfigValid) {
            Log.e(TAG, "sendMessage rejected: Invalid API Key configuration.");
            
            // Add fake user message and instant error message
            List<ChatMessage> currentList = new ArrayList<>(uiState.getValue().getMessages());
            String requestScopeId = UUID.randomUUID().toString();
            
            ChatMessage userMsg = new ChatMessage();
            userMsg.setId(UUID.randomUUID().toString());
            userMsg.setRole("user");
            userMsg.setText(text);
            userMsg.setTimestamp(System.currentTimeMillis());
            userMsg.setStatus(ChatMessage.STATUS_SENT);
            userMsg.setRequestId(requestScopeId);
            currentList.add(userMsg);
            
            ChatMessage errorMsg = new ChatMessage();
            errorMsg.setId(UUID.randomUUID().toString());
            errorMsg.setRole("assistant");
            errorMsg.setText("Gemini API chưa được cấu hình đúng.");
            errorMsg.setTimestamp(System.currentTimeMillis() + 10);
            errorMsg.setStatus(ChatMessage.STATUS_ERROR);
            errorMsg.setRequestId(requestScopeId);
            currentList.add(errorMsg);
            
            updateState(currentList, false, "");
            return;
        }

        if (isRequestPending) {
            Log.w(TAG, "sendMessage ignored: Request already pending");
            return;
        }

        Log.d(TAG, "API_REQUEST_STARTED: Sending message: " + text);
        isRequestPending = true;
        currentRequestId = UUID.randomUUID().toString();
        final String requestScopeId = currentRequestId;

        // 1. Instantly create and append USER message
        List<ChatMessage> currentList = new ArrayList<>(uiState.getValue().getMessages());
        
        ChatMessage userMsg = new ChatMessage();
        userMsg.setId(UUID.randomUUID().toString());
        userMsg.setRole("user");
        userMsg.setText(text);
        userMsg.setTimestamp(System.currentTimeMillis());
        userMsg.setStatus(ChatMessage.STATUS_SENT);
        userMsg.setRequestId(requestScopeId);
        
        // Add at the END of the list
        currentList.add(userMsg);
        updateState(currentList, true, "");
        
        // Persist User Message to DB (if we cast repo to Impl, or add to interface)
        if (chatRepository instanceof ChatRepositoryImpl) {
            ((ChatRepositoryImpl) chatRepository).persistSingleMessage(sessionId, userMsg);
        }

        // Token Optimization: Slice the history to max 6 messages
        List<ChatMessage> slicedHistory = new ArrayList<>();
        int startIndex = Math.max(0, currentList.size() - 6);
        for (int i = startIndex; i < currentList.size(); i++) {
            slicedHistory.add(currentList.get(i));
        }

        chatRepository.sendMessageToGemini(slicedHistory, new ChatRepository.ApiCallback() {
            @Override
            public void onPartialResponse(String chunk) {
                if (!requestScopeId.equals(currentRequestId)) return; // Ignore outdated
                
                pendingStreamChunk = chunk;
                long now = System.currentTimeMillis();
                
                if (now - lastUpdateTime >= THROTTLE_MS) {
                    lastUpdateTime = now;
                    updateState(uiState.getValue().getMessages(), true, pendingStreamChunk);
                } else {
                    throttleHandler.removeCallbacksAndMessages(null);
                    throttleHandler.postDelayed(() -> {
                        lastUpdateTime = System.currentTimeMillis();
                        updateState(uiState.getValue().getMessages(), true, pendingStreamChunk);
                    }, THROTTLE_MS - (now - lastUpdateTime));
                }
            }

            @Override
            public void onSuccess(String aiResponse, boolean isOffline) {
                if (!requestScopeId.equals(currentRequestId)) return;
                
                throttleHandler.removeCallbacksAndMessages(null); // Clear throttle
                
                List<ChatMessage> updatedList = new ArrayList<>(uiState.getValue().getMessages());
                
                ChatMessage aiMsg = new ChatMessage();
                aiMsg.setId(UUID.randomUUID().toString());
                aiMsg.setRole("assistant");
                aiMsg.setText(aiResponse);
                aiMsg.setTimestamp(System.currentTimeMillis());
                aiMsg.setStatus(isOffline ? ChatMessage.STATUS_OFFLINE_RESPONSE : ChatMessage.STATUS_SENT);
                aiMsg.setRequestId(requestScopeId);
                aiMsg.setOfflineFallback(isOffline);
                
                updatedList.add(aiMsg);
                updateState(updatedList, false, ""); // Ensure typing indicator removed
                isRequestPending = false;
                
                if (chatRepository instanceof ChatRepositoryImpl) {
                    ((ChatRepositoryImpl) chatRepository).persistSingleMessage(sessionId, aiMsg);
                }
            }

            @Override
            public void onError(String error) {
                if (!requestScopeId.equals(currentRequestId)) return;
                
                throttleHandler.removeCallbacksAndMessages(null);
                
                List<ChatMessage> updatedList = new ArrayList<>(uiState.getValue().getMessages());
                
                ChatMessage errorMsg = new ChatMessage();
                errorMsg.setId(UUID.randomUUID().toString());
                errorMsg.setRole("assistant");
                errorMsg.setText(error);
                errorMsg.setTimestamp(System.currentTimeMillis());
                errorMsg.setStatus(ChatMessage.STATUS_ERROR);
                errorMsg.setRequestId(requestScopeId);
                
                updatedList.add(errorMsg);
                updateState(updatedList, false, ""); // Ensure typing indicator removed
                isRequestPending = false;
                
                if (chatRepository instanceof ChatRepositoryImpl) {
                    ((ChatRepositoryImpl) chatRepository).persistSingleMessage(sessionId, errorMsg);
                }
            }
        });
    }

    public void retryMessage(String sessionId, ChatMessage failedMessage) {
        // Implementation for retry
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        chatRepository.cancelStream();
        throttleHandler.removeCallbacksAndMessages(null);
    }
}
