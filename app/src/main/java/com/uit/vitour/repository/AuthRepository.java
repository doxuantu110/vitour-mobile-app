package com.uit.vitour.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.uit.vitour.model.User;
import com.uit.vitour.utils.Constants;
import com.uit.vitour.utils.Resource;

/**
 * AuthRepository.java — Single source of truth for all authentication logic.
 *
 * RESPONSIBILITIES:
 * - Firebase Auth: login, register, logout, current user
 * - Firestore: create/read user profile document
 *
 * RULE: Activities/Fragments NEVER touch FirebaseAuth directly.
 *       They call methods here via AuthViewModel.
 *
 * PATTERN: Methods return LiveData<Resource<T>> so the ViewModel can
 *          expose them to the UI without caring about the data source.
 */
public class AuthRepository {

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    public AuthRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Sign in with email and password.
     *
     * @return LiveData wrapping LOADING → SUCCESS(FirebaseUser) or ERROR(message)
     */
    public LiveData<Resource<FirebaseUser>> login(String email, String password) {
        MutableLiveData<Resource<FirebaseUser>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult ->
                        result.setValue(Resource.success(authResult.getUser())))
                .addOnFailureListener(e ->
                        result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Register
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Create a new account, then save the user profile to Firestore.
     */
    public LiveData<Resource<FirebaseUser>> register(String fullName, String email, String password) {
        MutableLiveData<Resource<FirebaseUser>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        result.setValue(Resource.error("User creation failed", null));
                        return;
                    }
                    // Save extra profile info to Firestore
                    saveUserToFirestore(firebaseUser, fullName, email, result);
                })
                .addOnFailureListener(e ->
                        result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }

    /** Writes the new user's profile document to /users/{uid}. */
    private void saveUserToFirestore(FirebaseUser firebaseUser, String fullName,
                                     String email, MutableLiveData<Resource<FirebaseUser>> result) {
        User user = new User(firebaseUser.getUid(), fullName, email);

        firestore.collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(unused ->
                        result.setValue(Resource.success(firebaseUser)))
                .addOnFailureListener(e ->
                        result.setValue(Resource.error(e.getMessage(), null)));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Logout / Current User
    // ─────────────────────────────────────────────────────────────────────

    public void logout() {
        firebaseAuth.signOut();
    }

    /** Returns the currently authenticated Firebase user, or null if signed out. */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Fetch the user's Firestore profile.
     * Call this in ProfileViewModel to populate the profile screen.
     */
    public LiveData<Resource<User>> getUserProfile(String uid) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.toObject(User.class);
                    result.setValue(Resource.success(user));
                })
                .addOnFailureListener(e ->
                        result.setValue(Resource.error(e.getMessage(), null)));

        return result;
    }
}
