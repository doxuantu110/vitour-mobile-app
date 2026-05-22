package com.uit.vitour.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.uit.vitour.model.User;
import com.uit.vitour.utils.Constants;
import com.uit.vitour.utils.Resource;

/**
 * AuthRepository.java — Single source of truth for all authentication logic.
 *
 * SUPPORTED SIGN-IN METHODS:
 *   1. Email + Password  (existing, unchanged)
 *   2. Google Sign-In    (new — receives Google ID token from Activity,
 *                         exchanges it for a Firebase credential)
 *
 * GOOGLE SIGN-IN FLOW:
 *   Activity                         AuthRepository              Firebase
 *   ───────                          ──────────────              ────────
 *   btnGoogle click
 *   → startActivityForResult(
 *       googleSignInClient.signInIntent)
 *
 *   onActivityResult():
 *   GoogleSignInAccount account
 *   account.getIdToken() → idToken
 *   → authRepo.signInWithGoogle(idToken)
 *                                    → credential = GoogleAuthProvider
 *                                        .getCredential(idToken, null)
 *                                    → auth.signInWithCredential(credential)
 *                                                                    ↓
 *                                                          Firebase verifies token
 *                                                          Returns FirebaseUser
 *                                    → saveUserToFirestore (if new user)
 *                                    → setValue(Resource.success(user))
 *
 * RULE: Activities/Fragments NEVER touch FirebaseAuth directly.
 *       All auth calls go through AuthViewModel → AuthRepository.
 */
public class AuthRepository {

    private static final String TAG = "AuthRepository";

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public AuthRepository() {
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Email / Password Login
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sign in with email + password.
     *
     * @return LiveData: LOADING → SUCCESS(FirebaseUser) | ERROR(message)
     */
    public LiveData<Resource<FirebaseUser>> login(String email, String password) {
        MutableLiveData<Resource<FirebaseUser>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Log.d(TAG, "login() — email=" + email);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    Log.d(TAG, "login() SUCCESS — uid=" + (user != null ? user.getUid() : "null"));
                    result.setValue(Resource.success(user));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "login() FAILED: " + e.getMessage());
                    result.setValue(Resource.error(friendlyAuthError(e.getMessage()), null));
                });

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Email / Password Registration
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Create a new account, then save the user profile to Firestore.
     */
    public LiveData<Resource<FirebaseUser>> register(String fullName, String email, String password) {
        MutableLiveData<Resource<FirebaseUser>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Log.d(TAG, "register() — email=" + email);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        result.setValue(Resource.error("Account creation failed", null));
                        return;
                    }
                    Log.d(TAG, "register() SUCCESS — uid=" + firebaseUser.getUid());
                    saveUserToFirestore(firebaseUser, fullName, email,
                            null, "email", result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "register() FAILED: " + e.getMessage());
                    result.setValue(Resource.error(friendlyAuthError(e.getMessage()), null));
                });

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Google Sign-In
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Exchange a Google ID token for a Firebase credential and sign in.
     *
     * CALLED FROM: LoginActivity.handleGoogleSignInResult()
     *
     * @param idToken The ID token from GoogleSignInAccount.getIdToken().
     *                Obtain it in onActivityResult after the Google sign-in intent.
     * @return LiveData: LOADING → SUCCESS(FirebaseUser) | ERROR(message)
     */
    public LiveData<Resource<FirebaseUser>> firebaseAuthWithGoogle(String idToken) {
        MutableLiveData<Resource<FirebaseUser>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Log.d(TAG, "firebaseAuthWithGoogle() — exchanging Google ID token for Firebase credential");

        // Convert Google ID token → Firebase credential
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        result.setValue(Resource.error("Google sign-in failed", null));
                        return;
                    }

                    boolean isNewUser = authResult.getAdditionalUserInfo() != null
                            && authResult.getAdditionalUserInfo().isNewUser();

                    Log.d(TAG, "firebaseAuthWithGoogle() SUCCESS — uid=" + firebaseUser.getUid()
                            + ", newUser=" + isNewUser);

                    if (isNewUser) {
                        // First time — create Firestore profile from Google account data
                        String displayName = firebaseUser.getDisplayName();
                        String email       = firebaseUser.getEmail();
                        String photoUrl    = firebaseUser.getPhotoUrl() != null
                                ? firebaseUser.getPhotoUrl().toString() : null;

                        saveUserToFirestore(firebaseUser,
                                displayName != null ? displayName : "Google User",
                                email, photoUrl, "google", result);
                    } else {
                        // Returning user — profile already exists in Firestore
                        result.setValue(Resource.success(firebaseUser));
                    }
                })
                .addOnFailureListener(e -> {
                    if (e instanceof com.google.firebase.auth.FirebaseAuthException) {
                        Log.e(TAG, "firebaseAuthWithGoogle() FirebaseAuthException: " + ((com.google.firebase.auth.FirebaseAuthException) e).getErrorCode());
                    }
                    Log.e(TAG, "firebaseAuthWithGoogle() FAILED: " + e.getMessage(), e);
                    result.setValue(Resource.error(friendlyAuthError(e.getMessage()), null));
                });

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Logout / Current User
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Signs out from Firebase Auth.
     * NOTE: Call SessionManager.logoutGoogle() instead of this directly
     *       if you need to also sign out from the Google account picker.
     */
    public void logout() {
        Log.d(TAG, "logout() — signing out uid=" + (auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : "null"));
        auth.signOut();
    }

    /** Returns the currently authenticated Firebase user, or null if signed out. */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. User Profile (Firestore)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetch the user's Firestore profile document.
     * Used by ProfileFragment via AuthViewModel.getUserProfile(uid).
     */
    public LiveData<Resource<User>> getUserProfile(String uid) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Log.d(TAG, "getUserProfile() — uid=" + uid);

        db.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.toObject(User.class);
                    Log.d(TAG, "getUserProfile() SUCCESS — " + (user != null
                            ? "name=" + user.getFullName() + ", provider=" + user.getProvider()
                            : "null"));
                    result.setValue(Resource.success(user));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getUserProfile() FAILED: " + e.getMessage());
                    result.setValue(Resource.error(e.getMessage(), null));
                });

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Writes a new user profile document to /users/{uid}.
     * Called on registration (email/password) and first-time Google sign-in.
     */
    private void saveUserToFirestore(FirebaseUser firebaseUser, String fullName,
                                     String email, String photoUrl, String provider,
                                     MutableLiveData<Resource<FirebaseUser>> result) {
        User user = new User(firebaseUser.getUid(), fullName, email, photoUrl, provider);

        db.collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "saveUserToFirestore() SUCCESS — uid=" + firebaseUser.getUid());
                    result.setValue(Resource.success(firebaseUser));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveUserToFirestore() FAILED: " + e.getMessage());
                    // Auth succeeded — don't block the user for a Firestore write failure
                    result.setValue(Resource.success(firebaseUser));
                });
    }

    /**
     * Maps Firebase Auth error messages to user-friendly strings.
     * Firebase's default messages are technical; this makes them readable.
     */
    private String friendlyAuthError(String message) {
        if (message == null) return "Authentication failed. Please try again.";
        if (message.contains("no user record")) return "No account found with this email.";
        if (message.contains("password is invalid")) return "Incorrect password. Please try again.";
        if (message.contains("email address is already")) return "This email is already registered.";
        if (message.contains("badly formatted")) return "Please enter a valid email address.";
        if (message.contains("network error")) return "Network error. Check your connection.";
        return message;  // fallback to raw message
    }
}
