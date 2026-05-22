package com.uit.vitour.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.uit.vitour.model.Tour;
import com.uit.vitour.repository.TourRepository;
import com.uit.vitour.utils.Resource;

import java.util.List;

/**
 * ExploreViewModel.java — Owned by SearchFragment (Tab 2).
 *
 * Two data streams:
 *  1. browseTours   — real-time Firestore snapshot of all tours.
 *                     Shown when search box is empty.
 *  2. searchResults — one-shot Firestore fetch filtered by user query.
 *                     Updated via switchMap every time the query changes.
 *
 * Search flow:
 *   1. Fragment calls setSearchQuery("") to load default list on start
 *   2. User types in TextInputEditText → Fragment calls setSearchQuery(q)
 *   3. _searchQuery LiveData updates → switchMap fires searchTours(q)
 *   4. Fragment observes searchResults and updates RecyclerView
 */
public class ExploreViewModel extends ViewModel {

    private static final String TAG = "ExploreViewModel";

    private final TourRepository tourRepository = new TourRepository();

    // Internal trigger — Fragment writes here, switchMap listens
    private final MutableLiveData<String> _searchQuery = new MutableLiveData<>();

    // Public LiveData — Fragment observes this for search results
    public final LiveData<Resource<List<Tour>>> searchResults =
            Transformations.switchMap(_searchQuery, tourRepository::searchTours);

    // Real-time all-tours LiveData (cached)
    private LiveData<Resource<List<Tour>>> browseTours;

    public ExploreViewModel() {
        Log.d(TAG, "ExploreViewModel created");
    }

    /**
     * Called from Fragment when the user changes the search query.
     * Passing an empty string resets results to show all tours.
     */
    public void setSearchQuery(String query) {
        Log.d(TAG, "setSearchQuery: '" + query + "'");
        _searchQuery.setValue(query != null ? query : "");
    }

    /**
     * Returns the current search query value.
     */
    public String getCurrentQuery() {
        return _searchQuery.getValue();
    }

    /**
     * Real-time LiveData of all tours from Firestore.
     * Call this in onViewCreated() for the default browse view.
     */
    public LiveData<Resource<List<Tour>>> getBrowseTours() {
        if (browseTours == null) {
            Log.d(TAG, "getBrowseTours() — creating new LiveData from repository");
            browseTours = tourRepository.getTours();
        } else {
            Log.d(TAG, "getBrowseTours() — returning cached LiveData");
        }
        return browseTours;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        tourRepository.removeListeners();
        Log.d(TAG, "ExploreViewModel.onCleared() — listeners removed");
    }
}
