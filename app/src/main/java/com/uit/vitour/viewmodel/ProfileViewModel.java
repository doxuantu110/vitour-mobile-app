package com.uit.vitour.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.uit.vitour.model.Tour;
import com.uit.vitour.model.User;
import com.uit.vitour.repository.AuthRepository;
import com.uit.vitour.repository.TourRepository;
import com.uit.vitour.utils.Resource;

import java.util.List;

/**
 * ProfileViewModel.java — Owned by ProfileFragment.
 *
 * Combines:
 * - User profile data (from AuthRepository / Firestore)
 * - User's bookmarked tours (from TourRepository / Firestore)
 * - Logout action
 */
public class ProfileViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final TourRepository tourRepository;

    public ProfileViewModel() {
        authRepository = new AuthRepository();
        tourRepository = new TourRepository();
    }

    /** Load the signed-in user's Firestore profile. */
    public LiveData<Resource<User>> getUserProfile(String uid) {
        return authRepository.getUserProfile(uid);
    }

    /** Load the user's bookmarked tours from Firestore sub-collection. */
    public LiveData<Resource<List<Tour>>> getBookmarks(String uid) {
        return tourRepository.getBookmarks(uid);
    }

    /** Sign out and clear session. Fragment then navigates to LoginActivity. */
    public void logout() {
        authRepository.logout();
    }
}
