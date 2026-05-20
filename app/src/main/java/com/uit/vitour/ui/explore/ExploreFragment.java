package com.uit.vitour.ui.explore;

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
import com.uit.vitour.databinding.FragmentExploreBinding;
import com.uit.vitour.viewmodel.ExploreViewModel;

/**
 * ExploreFragment.java — Tab 2.
 *
 * Search flow:
 * 1. onViewCreated → load default browse list (all tours, page 0)
 * 2. User types in SearchBar → TextWatcher → viewModel.setSearchQuery()
 * 3. ExploreViewModel's switchMap fires → searchResults LiveData updates
 * 4. Fragment updates the RecyclerView
 *
 * The search is driven by LiveData + Transformations.switchMap in the
 * ViewModel — this Fragment only forwards the query string.
 */
public class ExploreFragment extends Fragment {

    private FragmentExploreBinding binding;
    private ExploreViewModel exploreViewModel;
    private TourAdapter tourAdapter;

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
        observeViewModel();

        // Load default list on first open
        exploreViewModel.getBrowseTours().observe(getViewLifecycleOwner(), resource -> {
            if (resource.data != null) {
                tourAdapter.submitList(resource.data);
            }
        });
    }

    private void setupRecyclerView() {
        tourAdapter = new TourAdapter(tour -> {
            // TODO: navigate to TourDetailFragment
        });
        binding.rvExplore.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExplore.setAdapter(tourAdapter);
    }

    private void setupSearchBar() {
        // SearchBar uses EditText inside; listen for text changes
        binding.searchBar.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Trigger search on every keystroke (debounce can be added later)
                exploreViewModel.setSearchQuery(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        // Observe search results driven by ExploreViewModel.searchResults
        exploreViewModel.searchResults.observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    break;
                case SUCCESS:
                    if (resource.data != null) {
                        tourAdapter.submitList(resource.data);
                    }
                    break;
                case ERROR:
                    if (getView() != null) {
                        Snackbar.make(getView(),
                                resource.message != null ? resource.message : "Search failed",
                                Snackbar.LENGTH_SHORT).show();
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
