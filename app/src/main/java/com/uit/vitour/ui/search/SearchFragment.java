package com.uit.vitour.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.uit.vitour.databinding.FragmentSearchBinding;
import com.uit.vitour.viewmodel.ExploreViewModel;

/**
 * SearchFragment.java — Tab 2: Search
 * ─────────────────────────────────────────────────────────────────
 * PURPOSE: Lets the user search for tours and destinations.
 *
 * HOW IT WORKS (data flow):
 *   User types in SearchBar
 *       │
 *       ▼ TextWatcher.onTextChanged()
 *   viewModel.setSearchQuery(query)
 *       │
 *       ▼ ExploreViewModel.switchMap fires searchTours() in Repository
 *   searchResults LiveData updates
 *       │
 *       ▼ Fragment observer
 *   adapter.submitList(tours) → RecyclerView updates
 *
 * USES: ExploreViewModel (already has search logic via switchMap).
 * Reusing an existing ViewModel is fine when the logic is identical.
 * Create a dedicated SearchViewModel when the logic diverges.
 *
 * NAMING CONVENTION: Fragment classes end in "Fragment".
 * PACKAGE: ui/search/ — one package per tab feature.
 */
public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private ExploreViewModel searchViewModel;
    private TourAdapter tourAdapter;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Scope ViewModel to THIS fragment — each tab gets its own instance
        searchViewModel = new ViewModelProvider(this).get(ExploreViewModel.class);

        setupRecyclerView();
        setupSearchBar();
        observeResults();
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        tourAdapter = new TourAdapter(tour -> {
            // 1. Đóng gói ID của tour vào Bundle để truyền sang màn hình chi tiết
            android.os.Bundle bundle = new android.os.Bundle();
            bundle.putString("tourId", tour.getId());

            // 2. Dùng NavController để chuyển hướng sang TourDetailFragment
            try {
                androidx.navigation.Navigation.findNavController(requireView())
                        .navigate(com.uit.vitour.R.id.tourDetailFragment, bundle);
                // LƯU Ý: Đảm bảo 'tourDetailFragment' khớp với ID bạn đặt trong file nav_graph.xml
            } catch (Exception e) {
                android.util.Log.e("Navigation", "Lỗi chuyển trang: " + e.getMessage());
            }
        });
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSearchResults.setAdapter(tourAdapter);
    }

    private void setupSearchBar() {
        // SearchBar wraps a TextInputEditText internally.
        // addTextChangedListener fires on every keystroke.
        // Debouncing is handled inside ExploreViewModel via switchMap.
        if (binding.searchBar.getEditText() != null) {
            binding.searchBar.getEditText().addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim();
                    searchViewModel.setSearchQuery(query);

                    // Show/hide empty state based on query presence
                    if (query.isEmpty()) {
                        showEmptyState("🔍", getString(
                                com.uit.vitour.R.string.search_empty));
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    // ── Observe LiveData ──────────────────────────────────────────────────────

    private void observeResults() {
        searchViewModel.searchResults.observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            switch (resource.status) {
                case LOADING:
                    // Optional: show a progress bar inside the SearchBar
                    break;

                case SUCCESS:
                    if (resource.data == null || resource.data.isEmpty()) {
                        showEmptyState("😕", getString(
                                com.uit.vitour.R.string.search_no_results));
                    } else {
                        showResults();
                        tourAdapter.submitList(resource.data);
                    }
                    break;

                case ERROR:
                    if (getView() != null) {
                        Snackbar.make(getView(),
                                resource.message != null
                                        ? resource.message
                                        : getString(com.uit.vitour.R.string.error_generic),
                                Snackbar.LENGTH_LONG).show();
                    }
                    break;
            }
        });
    }

    // ── View State Helpers ────────────────────────────────────────────────────

    /** Switch to the results list view. */
    private void showResults() {
        binding.rvSearchResults.setVisibility(View.VISIBLE);
        binding.layoutSearchEmpty.setVisibility(View.GONE);
    }

    /** Switch to the empty/idle state view with a custom icon + message. */
    private void showEmptyState(String icon, String message) {
        binding.rvSearchResults.setVisibility(View.GONE);
        binding.layoutSearchEmpty.setVisibility(View.VISIBLE);
        binding.tvSearchEmptyIcon.setText(icon);
        binding.tvSearchEmptyMessage.setText(message);
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // IMPORTANT: null binding to prevent memory leaks.
        // Fragment views are destroyed before the Fragment itself.
        binding = null;
    }
}
