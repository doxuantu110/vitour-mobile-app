package com.uit.vitour.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.uit.vitour.model.Tour;
import com.uit.vitour.repository.TourRepository;
import com.uit.vitour.utils.Resource;

import java.util.List;

/**
 * ExploreViewModel.java — Owned by ExploreFragment.
 *
 * Search flow:
 * 1. User types in SearchBar → Fragment calls setSearchQuery(q)
 * 2. _searchQuery LiveData updates → Transformations.switchMap
 *    fires searchTours(q) in the Repository
 * 3. ExploreFragment observes searchResults and updates the RecyclerView
 *
 * WHY switchMap: cancels the previous network call when the query
 * changes, avoiding out-of-order result bugs.
 */
public class ExploreViewModel extends ViewModel {

    private final TourRepository tourRepository = new TourRepository();

    // Internal trigger — Fragment writes here, switchMap listens
    private final MutableLiveData<String> _searchQuery = new MutableLiveData<>();

    // Public LiveData — Fragment observes this
    public final LiveData<Resource<List<Tour>>> searchResults =
            Transformations.switchMap(_searchQuery, tourRepository::searchTours);

    public ExploreViewModel() {
        // tourRepository is initialized inline above
    }

    /**
     * Called from ExploreFragment when the user submits a search query.
     * Passing an empty string resets the results.
     */
    public void setSearchQuery(String query) {
        _searchQuery.setValue(query);
    }

    /**
     * Load the default browse list (first page, no filter).
     * Call this in onViewCreated() before the user types anything.
     */
    public LiveData<Resource<List<Tour>>> getBrowseTours() {
        return tourRepository.getTours(0);
    }
}
