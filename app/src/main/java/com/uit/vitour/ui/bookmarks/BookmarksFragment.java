package com.uit.vitour.ui.bookmarks;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.uit.vitour.adapter.TourAdapter;
import com.uit.vitour.databinding.FragmentBookmarksBinding;
import com.uit.vitour.viewmodel.ProfileViewModel;

/**
 * BookmarksFragment.java — Tab 3.
 *
 * Shows the signed-in user's bookmarked tours fetched from Firestore
 * sub-collection: /users/{uid}/bookmarks.
 *
 * Reuses ProfileViewModel because bookmarks are user-scoped data
 * that ProfileViewModel already knows how to fetch.
 * (Alternatively, you could create a dedicated BookmarksViewModel.)
 *
 * Empty state: shows a centered message when the list is empty.
 */
public class BookmarksFragment extends Fragment {

    private FragmentBookmarksBinding binding;
    private ProfileViewModel profileViewModel;
    private TourAdapter tourAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBookmarksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupRecyclerView();
        loadBookmarks();
    }

    private void setupRecyclerView() {
        tourAdapter = new TourAdapter(tour -> {
            // TODO: navigate to TourDetailFragment
        });
        binding.rvBookmarks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvBookmarks.setAdapter(tourAdapter);
    }

    private void loadBookmarks() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        profileViewModel.getBookmarks(currentUser.getUid())
                .observe(getViewLifecycleOwner(), resource -> {
                    switch (resource.status) {
                        case LOADING:
                            break;
                        case SUCCESS:
                            if (resource.data == null || resource.data.isEmpty()) {
                                // Show empty state
                                binding.layoutEmpty.setVisibility(View.VISIBLE);
                                binding.rvBookmarks.setVisibility(View.GONE);
                            } else {
                                binding.layoutEmpty.setVisibility(View.GONE);
                                binding.rvBookmarks.setVisibility(View.VISIBLE);
                                tourAdapter.submitList(resource.data);
                            }
                            break;
                        case ERROR:
                            if (getView() != null) {
                                Snackbar.make(getView(),
                                        resource.message != null ? resource.message : "Failed to load bookmarks",
                                        Snackbar.LENGTH_LONG).show();
                            }
                            break;
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
