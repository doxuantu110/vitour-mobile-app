package com.uit.vitour.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;
import com.uit.vitour.R;
import com.uit.vitour.databinding.FragmentProfileBinding;
import com.uit.vitour.ui.auth.LoginActivity;
import com.uit.vitour.utils.Resource;
import com.uit.vitour.utils.SessionManager;
import com.uit.vitour.viewmodel.AuthViewModel;
import com.uit.vitour.viewmodel.ProfileViewModel;

/**
 * ProfileFragment.java — Tab 4: Profile
 *
 * Displays:
 *   - Circular avatar (from Google account or Firebase Storage)
 *   - Display name and email
 *   - Sign-in provider badge ("via Google" / "via Email")
 *   - Logout button with confirmation dialog
 *
 * LOGOUT FLOW:
 *   User taps "Log Out" → confirmation dialog
 *   → SessionManager.logoutGoogle() (signs out from Firebase + Google)
 *   → Navigate to LoginActivity (back stack cleared)
 *
 * For Google users, we sign out from the GoogleSignInClient so the
 * next sign-in shows the account picker (not auto-sign-in to same account).
 *
 * PROFILE LOADING STRATEGY:
 *   1. Show cached values from SessionManager immediately (no loading delay)
 *   2. Fetch fresh data from Firestore in the background
 *   3. Update UI when Firestore data arrives
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private FragmentProfileBinding binding;
    private ProfileViewModel profileViewModel;
    private AuthViewModel authViewModel;
    private SessionManager sessionManager;
    private GoogleSignInClient googleSignInClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel  = new ViewModelProvider(this).get(ProfileViewModel.class);
        authViewModel     = new ViewModelProvider(this).get(AuthViewModel.class);
        sessionManager    = new SessionManager(requireContext());

        setupGoogleSignOutClient();
        showCachedProfile();   // instant display from SharedPreferences
        loadUserProfile();     // async fetch from Firestore
        setupLogoutButton();
    }

    // ── Google Sign-Out Client ─────────────────────────────────────────────

    /**
     * Creates the GoogleSignInClient needed for sign-out.
     * Must match the configuration used in LoginActivity.
     */
    private void setupGoogleSignOutClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
    }

    // ── Profile Display ───────────────────────────────────────────────────

    /**
     * Shows cached profile data from SessionManager instantly.
     * This is displayed before the Firestore round-trip completes.
     */
    private void showCachedProfile() {
        String cachedName  = sessionManager.getCachedDisplayName();
        String cachedEmail = sessionManager.getCachedEmail();
        String cachedPhoto = sessionManager.getCachedPhotoUrl();

        if (cachedName != null && !cachedName.isEmpty()) {
            binding.tvDisplayName.setText(cachedName);
        }
        if (cachedEmail != null && !cachedEmail.isEmpty()) {
            binding.tvEmail.setText(cachedEmail);
        }
        if (cachedPhoto != null) {
            loadAvatar(cachedPhoto);
        }

        Log.d(TAG, "Showing cached profile — name=" + cachedName
                + ", provider=" + sessionManager.getCachedProvider());
    }

    /**
     * Fetches the live profile from Firestore and updates the UI.
     * Falls back to FirebaseUser data (email) if Firestore fetch fails.
     */
    private void loadUserProfile() {
        FirebaseUser currentUser = authViewModel.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "loadUserProfile: currentUser is null — not signed in?");
            return;
        }

        // Always show email from Firebase Auth (always accurate)
        if (currentUser.getEmail() != null) {
            binding.tvEmail.setText(currentUser.getEmail());
        }

        // Fetch Firestore profile for name + photo
        authViewModel.getUserProfile(currentUser.getUid())
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource.status == Resource.Status.SUCCESS && resource.data != null) {
                        String name = resource.data.getDisplayName();
                        binding.tvDisplayName.setText(name);

                        String photoUrl = resource.data.getPhotoUrl();
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            loadAvatar(photoUrl);
                        }

                        Log.d(TAG, "Firestore profile loaded — name=" + name
                                + ", provider=" + resource.data.getProvider());
                    }
                });
    }

    private void loadAvatar(String photoUrl) {
        if (getContext() == null) return;
        Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_nav_profile)
                .error(R.drawable.ic_nav_profile)
                .into(binding.ivAvatar);
    }

    // ── Logout ────────────────────────────────────────────────────────────

    private void setupLogoutButton() {
        binding.btnLogout.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Log out")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Log out", (dialog, which) -> performLogout())
                        .setNegativeButton("Cancel", null)
                        .show());
    }

    /**
     * Signs the user out of Firebase and Google.
     *
     * For Google users: googleSignInClient.signOut() clears the cached
     * Google account so the account picker reappears next time.
     *
     * For email users: only Firebase Auth sign-out is needed.
     */
    private void performLogout() {
        Log.d(TAG, "performLogout() — provider=" + sessionManager.getCachedProvider());

        sessionManager.logoutGoogle(
                googleSignInClient,
                () -> {
                    Log.d(TAG, "Logout complete → navigating to LoginActivity");
                    navigateToLogin();
                }
        );
    }

    private void navigateToLogin() {
        if (getActivity() == null) return;
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        // Clear the entire back stack so user can't press Back to return
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
