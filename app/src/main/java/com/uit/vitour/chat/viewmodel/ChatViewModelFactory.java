package com.uit.vitour.chat.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.uit.vitour.chat.repository.ChatRepository;

public class ChatViewModelFactory implements ViewModelProvider.Factory {
    
    private final ChatRepository repository;

    public ChatViewModelFactory(ChatRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ChatViewModel.class)) {
            return (T) new ChatViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
