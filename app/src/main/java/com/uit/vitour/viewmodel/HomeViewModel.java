package com.uit.vitour.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.uit.vitour.model.Tour;
import com.uit.vitour.repository.TourRepository;
import com.uit.vitour.utils.Resource;

import java.util.List;

/**
 * HomeViewModel.java — Serves data to the HomeFragment.
 *
 * MVVM architecture:
 * Fragment observes this ViewModel.
 * This ViewModel pulls from TourRepository.
 * TourRepository manages the actual Firebase real-time listeners.
 */
public class HomeViewModel extends ViewModel {

    private final TourRepository tourRepository;

    public HomeViewModel() {
        tourRepository = new TourRepository();
    }

    /**
     * LiveData stream for featured tours (Horizontal carousel).
     * The underlying Firestore listener automatically turns on/off
     * when the Fragment is visible/hidden.
     */
    public LiveData<Resource<List<Tour>>> getFeaturedTours() {
        return tourRepository.getFeaturedTours();
    }

    /**
     * LiveData stream for recommended tours (Vertical list).
     */
    public LiveData<Resource<List<Tour>>> getRecommendedTours() {
        return tourRepository.getRecommendedTours();
    }
}
