package com.uit.vitour.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.uit.vitour.MainActivity;
import com.uit.vitour.databinding.FragmentProfileBinding;
import com.uit.vitour.ui.auth.LoginActivity;
import com.uit.vitour.viewmodel.ProfileViewModel;

/**
 * ProfileFragment.java — Tab 4.
 *
 * Displays:
 * - Circular avatar (loaded by Glide from Firestore photoUrl)
 * - Display name and email
 * - Logout button with confirmation dialog
 *
 * Logout flow:
 * ProfileViewModel.logout() → FirebaseAuth.signOut()
 * → Fragment starts LoginActivity with FLAG_ACTIVITY_CLEAR_TASK
 * (clears the entire back stack so user can't go back)
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel profileViewModel;

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

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        loadUserProfile();
        setupLogoutButton();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Display email from Firebase Auth (always available)
        binding.tvEmail.setText(currentUser.getEmail());

        // Fetch full profile (name, photo) from Firestore
        profileViewModel.getUserProfile(currentUser.getUid())
                .observe(getViewLifecycleOwner(), resource -> {
                    if (resource.status == com.uit.vitour.utils.Resource.Status.SUCCESS
                            && resource.data != null) {
                        binding.tvDisplayName.setText(resource.data.getFullName());

                        // Load avatar with Glide (gracefully handles null URL)
                        Glide.with(this)
                                .load(resource.data.getPhotoUrl())
                                .circleCrop()
                                .placeholder(com.uit.vitour.R.drawable.ic_nav_profile)
                                .into(binding.ivAvatar);
                    }
                });
    }

    private void setupLogoutButton() {
        binding.btnLogout.setOnClickListener(v -> {
            // Confirmation dialog — prevents accidental logouts
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Log out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Log out", (dialog, which) -> performLogout())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void performLogout() {
        profileViewModel.logout();

        // Navigate to LoginActivity and clear entire back stack
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
