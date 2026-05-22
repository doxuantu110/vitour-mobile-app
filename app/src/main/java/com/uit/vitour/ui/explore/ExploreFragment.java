package com.uit.vitour.ui.explore;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.uit.vitour.adapter.TourAdapter;
import com.uit.vitour.databinding.FragmentExploreBinding;
import com.uit.vitour.viewmodel.ExploreViewModel;

/**
 * ExploreFragment.java — Tab 2 (Explore / Search).
 *
 * Search flow:
 *   1. onViewCreated → observe getBrowseTours() for real-time all-tours list.
 *   2. User types in TextInputEditText → TextWatcher → viewModel.setSearchQuery(q)
 *   3. ExploreViewModel's switchMap fires → searchResults LiveData updates
 *   4. Fragment updates the RecyclerView.
 *
 * NOTE: We bind directly to `binding.etSearchExplore` (TextInputEditText),
 * NOT `binding.searchBar.getEditText()`. TextInputLayout.getEditText() requires
 * a specific Material library version and can return null. Using the direct
 * child ID is safer and simpler.
 */
public class ExploreFragment extends Fragment {

    private static final String TAG = "ExploreFragment";

    private FragmentExploreBinding binding;
    private ExploreViewModel exploreViewModel;
    private TourAdapter tourAdapter;

    // Track whether the user is actively searching (to choose which stream to show)
    private boolean isSearchActive = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        exploreViewModel = new ViewModelProvider(this).get(ExploreViewModel.class);

        setupRecyclerView();
        setupSearchBar();
        observeBrowseTours();
        observeSearchResults();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        tourAdapter = new TourAdapter(tour -> {
            // TODO: navigate to TourDetailFragment with tour.getId()
        });
        binding.rvExplore.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExplore.setAdapter(tourAdapter);
        Log.d(TAG, "RecyclerView + TourAdapter initialized");
    }

    private void setupSearchBar() {
        // Attach TextWatcher directly to the TextInputEditText (id: et_search_explore).
        // This avoids calling TextInputLayout.getEditText() which can return null
        // depending on the Material library version used.
        binding.etSearchExplore.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString().trim() : "";
                isSearchActive = !query.isEmpty();
                exploreViewModel.setSearchQuery(query);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Observers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Observes the real-time all-tours stream.
     * Updates the RecyclerView only when the user has NOT typed a search query,
     * so the search results take priority over the browse stream.
     */
    private void observeBrowseTours() {
        exploreViewModel.getBrowseTours().observe(getViewLifecycleOwner(), resource -> {
            if (isSearchActive) {
                Log.d(TAG, "observeBrowseTours: search is active, skipping update");
                return;
            }
            if (resource == null) {
                Log.w(TAG, "observeBrowseTours: resource is null");
                return;
            }

            Log.d(TAG, "observeBrowseTours fired. status=" + resource.status
                    + ", data=" + (resource.data != null ? resource.data.size() + " items" : "null"));

            switch (resource.status) {
                case LOADING:
                    showLoading(true);
                    break;
                case SUCCESS:
                    showLoading(false);
                    if (resource.data != null && !resource.data.isEmpty()) {
                        Log.d(TAG, "Submitting " + resource.data.size() + " browse tours to adapter");
                        tourAdapter.submitList(resource.data);
                        showEmpty(false);
                    } else {
                        Log.w(TAG, "Browse tours list is null or empty");
                        tourAdapter.submitList(null);
                        showEmpty(true);
                    }
                    break;
                case ERROR:
                    showLoading(false);
                    Log.e(TAG, "Browse tours error: " + resource.message);
                    showError(resource.message);
                    break;
            }
        });
    }

    /**
     * Observes search results driven by ExploreViewModel.searchResults.
     * Search results only render when the user has typed something.
     */
    private void observeSearchResults() {
        exploreViewModel.searchResults.observe(getViewLifecycleOwner(), resource -> {
            if (!isSearchActive) return; // browse results take priority
            if (resource == null) return;

            switch (resource.status) {
                case LOADING:
                    showLoading(true);
                    break;
                case SUCCESS:
                    showLoading(false);
                    if (resource.data != null && !resource.data.isEmpty()) {
                        tourAdapter.submitList(resource.data);
                        showEmpty(false);
                    } else {
                        tourAdapter.submitList(null);
                        showEmpty(true);
                    }
                    break;
                case ERROR:
                    showLoading(false);
                    showError(resource.message);
                    break;
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI state helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void showLoading(boolean loading) {
        if (binding == null) return;
        binding.progressExplore.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showEmpty(boolean empty) {
        if (binding == null) return;
        binding.tvExploreEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvExplore.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(),
                    message != null ? message : "Failed to load tours",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
