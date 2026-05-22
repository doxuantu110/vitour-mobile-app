package com.uit.vitour.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;
import com.uit.vitour.model.User;
import com.uit.vitour.repository.AuthRepository;
import com.uit.vitour.utils.Resource;

/**
 * AuthViewModel.java — Owned by LoginActivity, RegisterActivity, and ProfileFragment.
 *
 * WHY ViewModel: survives screen rotations. If the user rotates while a login
 * request is in flight, the request isn't cancelled and the result is still
 * posted to the new Activity instance.
 *
 * SIGN-IN METHODS:
 *   1. login(email, password)    — email/password
 *   2. register(name, email, pw) — email/password registration
 *   3. signInWithGoogle(idToken) — Google Sign-In (NEW)
 *
 * RULE: Each method here is a thin wrapper. No business logic in ViewModels.
 */
public class AuthViewModel extends ViewModel {

    private static final String TAG = "AuthViewModel";

    private final AuthRepository authRepository;

    public AuthViewModel() {
        authRepository = new AuthRepository();
        Log.d(TAG, "AuthViewModel created");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Email / Password
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sign in with email + password.
     * Observe the returned LiveData: LOADING → SUCCESS | ERROR.
     */
    public LiveData<Resource<FirebaseUser>> login(String email, String password) {
        return authRepository.login(email, password);
    }

    /**
     * Register a new account.
     * On success, navigate to MainActivity (user is already signed in).
     */
    public LiveData<Resource<FirebaseUser>> register(String fullName, String email, String password) {
        return authRepository.register(fullName, email, password);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Google Sign-In
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sign in with a Google ID token obtained from the GoogleSignInAccount.
     *
     * FLOW:
     *   LoginActivity gets GoogleSignInAccount from onActivityResult
     *   → calls account.getIdToken()
     *   → calls this method with the token
     *   → observe result for SUCCESS/ERROR
     *
     * @param idToken The ID token from GoogleSignInAccount.getIdToken()
     * @return LiveData: LOADING → SUCCESS(FirebaseUser) | ERROR(message)
     */
    public LiveData<Resource<FirebaseUser>> firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "firebaseAuthWithGoogle() called");
        return authRepository.firebaseAuthWithGoogle(idToken);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Session / Profile
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the current signed-in FirebaseUser.
     * Used by LoginActivity.onCreate() to skip the login screen.
     */
    public FirebaseUser getCurrentUser() {
        return authRepository.getCurrentUser();
    }

    /**
     * Sign out from Firebase Auth.
     * For Google Sign-Out (with account picker reset), use SessionManager.logoutGoogle().
     */
    public void logout() {
        authRepository.logout();
    }

    /**
     * Fetch the user's Firestore profile document.
     * Used by ProfileFragment to display name, email, and photo.
     *
     * @param uid The Firebase Auth UID
     */
    public LiveData<Resource<User>> getUserProfile(String uid) {
        return authRepository.getUserProfile(uid);
    }
}
