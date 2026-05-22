package com.uit.vitour.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.uit.vitour.model.Tour;
import com.uit.vitour.repository.TourRepository;
import com.uit.vitour.utils.Resource;

import java.util.List;

/**
 * HomeViewModel.java — Serves data to HomeFragment.
 *
 * MVVM:  HomeFragment → HomeViewModel → TourRepository → Firestore
 *
 * LiveData instances are created ONCE and cached so the same Firestore
 * listener is reused across configuration changes (screen rotation).
 *
 * onCleared() removes Firestore listeners to prevent quota waste.
 */
public class HomeViewModel extends ViewModel {

    private static final String TAG = "HomeViewModel";

    private final TourRepository tourRepository;

    // Cached LiveData — created once, reused across config changes
    private LiveData<Resource<List<Tour>>> featuredToursLiveData;
    private LiveData<Resource<List<Tour>>> recommendedToursLiveData;

    public HomeViewModel() {
        tourRepository = new TourRepository();
        Log.d(TAG, "HomeViewModel created");
    }

    /**
     * LiveData stream for featured tours (horizontal carousel).
     * Cached so the Firestore listener is not re-created on rotation.
     */
    public LiveData<Resource<List<Tour>>> getFeaturedTours() {
        if (featuredToursLiveData == null) {
            Log.d(TAG, "getFeaturedTours() — creating new LiveData from repository");
            featuredToursLiveData = tourRepository.getFeaturedTours();
        } else {
            Log.d(TAG, "getFeaturedTours() — returning cached LiveData");
        }
        return featuredToursLiveData;
    }

    /**
     * LiveData stream for recommended tours (vertical list).
     * Returns non-featured tours from Firestore in real-time.
     */
    public LiveData<Resource<List<Tour>>> getRecommendedTours() {
        if (recommendedToursLiveData == null) {
            Log.d(TAG, "getRecommendedTours() — creating new LiveData from repository");
            recommendedToursLiveData = tourRepository.getRecommendedTours();
        } else {
            Log.d(TAG, "getRecommendedTours() — returning cached LiveData");
        }
        return recommendedToursLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Remove Firestore listeners to stop quota consumption when ViewModel is destroyed
        tourRepository.removeListeners();
        Log.d(TAG, "HomeViewModel.onCleared() — listeners removed");
    }
}
