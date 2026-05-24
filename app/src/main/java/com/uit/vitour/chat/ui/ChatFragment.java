package com.uit.vitour.chat.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.uit.vitour.R;
import com.uit.vitour.chat.repository.ChatRepositoryImpl;
import com.uit.vitour.chat.viewmodel.ChatViewModel;
import com.uit.vitour.chat.viewmodel.ChatViewModelFactory;

import java.util.HashMap;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";

    private ChatViewModel chatViewModel;
    private ChatAdapter chatAdapter;
    private RecyclerView recyclerView;
    
    private final String currentSessionId = "session_123";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Run configuration diagnostic check in debug builds
        com.uit.vitour.chat.repository.GeminiConfigValidator.debugCheck(requireContext());

        ChatRepositoryImpl repo = new ChatRepositoryImpl();
        repo.setContext(requireContext());
        ChatViewModelFactory factory = new ChatViewModelFactory(repo);
        chatViewModel = new ViewModelProvider(this, factory).get(ChatViewModel.class);

        setupRecyclerView(view);
        observeViewModel();
        setupInput(view);
    }

    private void setupRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // Start drawing from the bottom
        recyclerView.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter();
        recyclerView.setAdapter(chatAdapter);

        // Setup pagination listener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (!recyclerView.canScrollVertically(-1)) { // Scrolled to top
                    chatViewModel.loadMoreMessages(currentSessionId);
                }
            }
        });
    }

    private void observeViewModel() {
        chatViewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || !isAdded() || recyclerView == null) return;
            
            chatAdapter.submitList(state.getMessages(), () -> {
                // Ensure we smoothly scroll to the bottom (last index) 
                // only if user is already near bottom or a new message was added.
                // For simplicity, always scroll on submit list finish.
                if (state.getMessages() != null && !state.getMessages().isEmpty()) {
                    int lastPos = chatAdapter.getItemCount() - 1;
                    if (lastPos >= 0) {
                        recyclerView.smoothScrollToPosition(lastPos);
                    }
                }
            });

            // Update typing & streaming text
            chatAdapter.setTypingState(state.isTyping(), state.getCurrentStreamingMessage(), recyclerView);
            
            // Graceful fallback UI for invalid API Key
            View view = getView();
            if (view != null) {
                View buttonSend = view.findViewById(R.id.buttonSend);
                EditText editTextMessage = view.findViewById(R.id.editTextMessage);
                if (!state.isConfigValid()) {
                    if (buttonSend != null) buttonSend.setEnabled(false);
                    if (editTextMessage != null) {
                        editTextMessage.setEnabled(false);
                        editTextMessage.setHint("Gemini API chưa được cấu hình đúng.");
                    }
                }
            }
        });
    }

    private void setupInput(View view) {
        View buttonSend = view.findViewById(R.id.buttonSend);
        EditText editTextMessage = view.findViewById(R.id.editTextMessage);

        buttonSend.setOnClickListener(v -> {
            String text = editTextMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                Log.d(TAG, "SEND_BUTTON_CLICKED: User tapped send");
                
                // Prevent duplicate/spam requests
                buttonSend.setEnabled(false);
                buttonSend.postDelayed(() -> {
                    if (isAdded() && buttonSend != null) {
                        buttonSend.setEnabled(true);
                    }
                }, 1000);
                
                // Immediately clear the EditText so user feels instant response
                editTextMessage.setText("");
                
                chatViewModel.sendMessage(currentSessionId, text, new HashMap<>());
            }
        });
    }
}
