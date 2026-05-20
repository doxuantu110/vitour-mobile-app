package com.uit.vitour.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;
import com.uit.vitour.model.User;
import com.uit.vitour.repository.AuthRepository;
import com.uit.vitour.utils.Resource;

/**
 * AuthViewModel.java — Owned by LoginActivity and RegisterActivity.
 *
 * WHY ViewModel: survives screen rotations. If the user rotates while
 * a login request is in flight, the request isn't cancelled and the
 * result is still posted to the new Activity instance.
 *
 * RULE: Each method here is a thin wrapper that calls the repository
 *       and returns its LiveData. No business logic in ViewModels.
 */
public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;

    public AuthViewModel() {
        authRepository = new AuthRepository();
    }

    /**
     * Kick off login. Observe the returned LiveData to react to result.
     * Loading → show progress bar
     * Success → navigate to MainActivity
     * Error   → show error snackbar
     */
    public LiveData<Resource<FirebaseUser>> login(String email, String password) {
        return authRepository.login(email, password);
    }

    /**
     * Kick off registration.
     * On success, navigate to MainActivity (user is already signed in).
     */
    public LiveData<Resource<FirebaseUser>> register(String fullName, String email, String password) {
        return authRepository.register(fullName, email, password);
    }

    /** Returns the current signed-in user. Used by LoginActivity to skip login. */
    public FirebaseUser getCurrentUser() {
        return authRepository.getCurrentUser();
    }

    public void logout() {
        authRepository.logout();
    }

    /**
     * Fetch Firestore profile. Used by ProfileFragment.
     * Call as: authViewModel.getUserProfile(uid).observe(...)
     */
    public LiveData<Resource<User>> getUserProfile(String uid) {
        return authRepository.getUserProfile(uid);
    }
}
