package com.uit.vitour.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.uit.vitour.model.Tour;
import com.uit.vitour.utils.Constants;
import com.uit.vitour.utils.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * TourRepository.java — Single source of truth for tour data.
 *
 * DATA SOURCES:
 * - Firebase Firestore — Real-time updates via addSnapshotListener.
 *
 * REALTIME ARCHITECTURE:
 * We return a custom MutableLiveData that attaches the Firestore listener
 * ONLY when a Fragment is actively observing (onActive), and automatically
 * removes it when the user leaves the screen (onInactive).
 * This completely prevents memory leaks and saves battery/data.
 */
public class TourRepository {

    private final FirebaseFirestore firestore;

    public TourRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Mock Data Implementation
    // ─────────────────────────────────────────────────────────────────────

    public LiveData<Resource<List<Tour>>> getFeaturedTours() {
        MutableLiveData<Resource<List<Tour>>> data = new MutableLiveData<>();
        List<Tour> tours = com.uit.vitour.model.MockData.getTours();
        List<Tour> featured = new java.util.ArrayList<>();
        for (Tour t : tours) {
            if (t.isFeatured()) {
                featured.add(t);
            }
        }
        data.setValue(Resource.success(featured));
        return data;
    }

    public LiveData<Resource<List<Tour>>> getRecommendedTours() {
        MutableLiveData<Resource<List<Tour>>> data = new MutableLiveData<>();
        List<Tour> tours = com.uit.vitour.model.MockData.getTours();
        List<Tour> recommended = new java.util.ArrayList<>();
        for (Tour t : tours) {
            if (!t.isFeatured()) {
                recommended.add(t);
            }
        }
        data.setValue(Resource.success(recommended));
        return data;
    }

    public LiveData<Resource<List<Tour>>> getBookmarks(String uid) {
        MutableLiveData<Resource<List<Tour>>> data = new MutableLiveData<>();
        data.setValue(Resource.success(new java.util.ArrayList<>()));
        return data;
    }

    public LiveData<Resource<List<Tour>>> getTours(int limit) {
        return searchTours("");
    }

    public LiveData<Resource<List<Tour>>> searchTours(String query) {
        MutableLiveData<Resource<List<Tour>>> data = new MutableLiveData<>();
        List<Tour> allTours = com.uit.vitour.model.MockData.getTours();
        
        if (query == null || query.trim().isEmpty()) {
            data.setValue(Resource.success(allTours));
            return data;
        }

        String lowerQuery = query.toLowerCase();
        List<Tour> filtered = new java.util.ArrayList<>();
        
        for (Tour t : allTours) {
            boolean matchName = t.getName() != null && t.getName().toLowerCase().contains(lowerQuery);
            boolean matchLoc = t.getLocation() != null && t.getLocation().toLowerCase().contains(lowerQuery);
            boolean matchTag = false;
            if (t.getTags() != null) {
                for (String tag : t.getTags()) {
                    if (tag.toLowerCase().contains(lowerQuery)) {
                        matchTag = true;
                        break;
                    }
                }
            }
            
            if (matchName || matchLoc || matchTag) {
                filtered.add(t);
            }
        }
        
        data.setValue(Resource.success(filtered));
        return data;
    }
}
