package com.uit.vitour.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.uit.vitour.model.Tour;
import com.uit.vitour.utils.Constants;
import com.uit.vitour.utils.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * TourRepository.java — Single source of truth for tour data.
 *
 * DATA SOURCE: Firebase Firestore — real-time updates via addSnapshotListener.
 *
 * ARCHITECTURE:
 *   UI Fragment → ViewModel → TourRepository → Firestore
 *
 * DESIGN (simplified for reliability):
 *   All public methods return LiveData<Resource<List<Tour>>>.
 *   The Resource wrapper carries three states: LOADING, SUCCESS, ERROR.
 *
 *   Listeners are attached immediately when the LiveData is created
 *   and stored for explicit removal via removeListener().
 *   The ViewModel calls removeListener() in onCleared() to avoid leaks.
 *
 *   This avoids the subtle lifecycle bugs of the custom FirestoreLiveData
 *   inner class approach (onActive/onInactive race conditions).
 */
public class TourRepository {

    private static final String TAG = "TourRepository";

    private final FirebaseFirestore db;

    // Keep references so the ViewModel can remove them in onCleared()
    private ListenerRegistration featuredReg;
    private ListenerRegistration recommendedReg;
    private ListenerRegistration allToursReg;

    public TourRepository() {
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "TourRepository created. Firestore project: "
                + db.getApp().getOptions().getProjectId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. All Tours
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns LiveData backed by a real-time Firestore snapshot listener.
     * Starts in LOADING state, transitions to SUCCESS or ERROR.
     */
    public LiveData<Resource<List<Tour>>> getTours() {
        MutableLiveData<Resource<List<Tour>>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading(null));

        Log.d(TAG, "getTours() — attaching Firestore snapshot listener to 'tours' collection");

        allToursReg = db.collection(Constants.COLLECTION_TOURS)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "getTours() Firestore error: " + error.getMessage(), error);
                        liveData.setValue(Resource.error(error.getMessage(), null));
                        return;
                    }
                    handleSnapshot(snapshot, liveData, null);
                });

        return liveData;
    }

    /**
     * Overload accepting a limit parameter (limit is ignored — all docs returned).
     * Kept for back-compat with ExploreViewModel.
     */
    public LiveData<Resource<List<Tour>>> getTours(int limit) {
        return getTours();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Featured Tours
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns LiveData streaming only featured tours (isFeatured == true).
     * Filtering is done client-side after each snapshot.
     */
    public LiveData<Resource<List<Tour>>> getFeaturedTours() {
        MutableLiveData<Resource<List<Tour>>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading(null));

        Log.d(TAG, "getFeaturedTours() — attaching Firestore listener (filter=featured)");

        featuredReg = db.collection(Constants.COLLECTION_TOURS)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "getFeaturedTours() Firestore error: " + error.getMessage(), error);
                        liveData.setValue(Resource.error(error.getMessage(), null));
                        return;
                    }
                    handleSnapshot(snapshot, liveData, "featured");
                });

        return liveData;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Recommended Tours
    // ─────────────────────────────────────────────────────────────────────────

    public LiveData<Resource<List<Tour>>> getRecommendedTours() {
        MutableLiveData<Resource<List<Tour>>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading(null));

        Log.d(TAG, "getRecommendedTours() — attaching Firestore listener (filter=recommended)");

        recommendedReg = db.collection(Constants.COLLECTION_TOURS)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "getRecommendedTours() Firestore error: " + error.getMessage(), error);
                        liveData.setValue(Resource.error(error.getMessage(), null));
                        return;
                    }
                    handleSnapshot(snapshot, liveData, "recommended");
                });

        return liveData;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Search Tours
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Searches tours by name, location, or tags.
     * Fetches all tours once then filters locally.
     * If query is empty/null → returns all tours.
     */
    public LiveData<Resource<List<Tour>>> searchTours(String query) {
        MutableLiveData<Resource<List<Tour>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        Log.d(TAG, "searchTours() — query: '" + query + "'");

        db.collection(Constants.COLLECTION_TOURS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Tour> allTours = parseTours(snapshot.getDocuments());

                    Log.d(TAG, "searchTours: fetched " + allTours.size() + " tours from Firestore");

                    if (query == null || query.trim().isEmpty()) {
                        Log.d(TAG, "searchTours: empty query — returning all " + allTours.size() + " tours");
                        result.setValue(Resource.success(allTours));
                        return;
                    }

                    String lowerQuery = query.toLowerCase().trim();
                    List<Tour> filtered = new ArrayList<>();
                    for (Tour t : allTours) {
                        if (matchesQuery(t, lowerQuery)) filtered.add(t);
                    }
                    Log.d(TAG, "searchTours: query='" + query + "' matched " + filtered.size() + " tours");
                    result.setValue(Resource.success(filtered));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "searchTours failed: " + e.getMessage(), e);
                    result.setValue(Resource.error(e.getMessage(), null));
                });

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Bookmarks — placeholder
    // ─────────────────────────────────────────────────────────────────────────

    public LiveData<Resource<List<Tour>>> getBookmarks(String uid) {
        MutableLiveData<Resource<List<Tour>>> data = new MutableLiveData<>();
        data.setValue(Resource.success(new ArrayList<>()));
        return data;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Listener cleanup — call from ViewModel.onCleared()
    // ─────────────────────────────────────────────────────────────────────────

    public void removeListeners() {
        if (featuredReg != null)     { featuredReg.remove();     featuredReg = null; }
        if (recommendedReg != null)  { recommendedReg.remove();  recommendedReg = null; }
        if (allToursReg != null)     { allToursReg.remove();     allToursReg = null; }
        Log.d(TAG, "removeListeners() — all Firestore listeners removed");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Processes a Firestore snapshot, applies the filter, and posts to LiveData.
     * @param snapshot  The Firestore QuerySnapshot (may be null)
     * @param liveData  Target LiveData to update
     * @param filter    null = all, "featured" = isFeatured==true, "recommended" = isFeatured==false
     */
    private void handleSnapshot(QuerySnapshot snapshot,
                                MutableLiveData<Resource<List<Tour>>> liveData,
                                String filter) {
        if (snapshot == null) {
            Log.w(TAG, "handleSnapshot: snapshot is null — posting empty list. filter=" + filter);
            liveData.setValue(Resource.success(new ArrayList<>()));
            return;
        }

        Log.d(TAG, "handleSnapshot: docs=" + snapshot.size()
                + ", fromCache=" + snapshot.getMetadata().isFromCache()
                + ", filter=" + filter);

        List<Tour> allTours = parseTours(snapshot.getDocuments());
        Log.d(TAG, "handleSnapshot: parsed " + allTours.size() + " valid Tour objects");

        List<Tour> result;
        if ("featured".equals(filter)) {
            result = new ArrayList<>();
            for (Tour t : allTours) {
                if (t.isFeatured()) result.add(t);
            }
            Log.d(TAG, "Loaded " + result.size() + " featured tours from Firestore");
        } else if ("recommended".equals(filter)) {
            result = new ArrayList<>();
            for (Tour t : allTours) {
                if (!t.isFeatured()) result.add(t);
            }
            Log.d(TAG, "Loaded " + result.size() + " recommended tours from Firestore");
        } else {
            result = allTours;
            Log.d(TAG, "Loaded " + result.size() + " tours from Firestore (no filter)");
        }

        liveData.setValue(Resource.success(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Detail (Single Tour)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loads a single tour by ID. Uses get() for a one-time fetch (since details
     * rarely change rapidly while viewed, but we can also use SnapshotListener if preferred).
     * For now, using get() for simple fetching.
     */
    public LiveData<Resource<Tour>> getTourById(String tourId) {
        MutableLiveData<Resource<Tour>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading(null));

        Log.d(TAG, "getTourById() called for tourId=" + tourId);

        db.collection(Constants.COLLECTION_TOURS).document(tourId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        try {
                            Tour tour = snapshot.toObject(Tour.class);
                            if (tour != null) {
                                if (tour.getId() == null) tour.setId(snapshot.getId());
                                Log.d(TAG, "getTourById() SUCCESS — id=" + tour.getId());
                                liveData.setValue(Resource.success(tour));
                            } else {
                                liveData.setValue(Resource.error("Failed to parse tour", null));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "getTourById() Exception parsing: " + e.getMessage(), e);
                            liveData.setValue(Resource.error("Parsing error: " + e.getMessage(), null));
                        }
                    } else {
                        Log.w(TAG, "getTourById() FAILED: Document does not exist");
                        liveData.setValue(Resource.error("Tour not found", null));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getTourById() FAILED: " + e.getMessage(), e);
                    liveData.setValue(Resource.error("Failed to load tour details", null));
                });

        return liveData;
    }

    /**
     * Converts a list of Firestore DocumentSnapshots to a list of Tour objects.
     * Null-safe: skips documents that fail to deserialize.
     */
    private List<Tour> parseTours(List<com.google.firebase.firestore.DocumentSnapshot> docs) {
        List<Tour> tours = new ArrayList<>();
        for (com.google.firebase.firestore.DocumentSnapshot doc : docs) {
            Log.d(TAG, "  doc[" + doc.getId() + "] raw data: " + doc.getData());
            try {
                Tour tour = doc.toObject(Tour.class);
                if (tour != null) {
                    // @DocumentId handles mapping, but set manually as fallback
                    if (tour.getId() == null || tour.getId().isEmpty()) {
                        tour.setId(doc.getId());
                        Log.w(TAG, "  @DocumentId was null for " + doc.getId() + " — set manually");
                    }
                    Log.d(TAG, "  ✓ Mapped tour: id=" + tour.getId()
                            + ", name=" + tour.getName()
                            + ", isFeatured=" + tour.isFeatured());
                    tours.add(tour);
                } else {
                    Log.w(TAG, "  ✗ doc.toObject(Tour.class) returned null for: " + doc.getId());
                }
            } catch (Exception e) {
                Log.e(TAG, "  ✗ Exception mapping doc " + doc.getId() + ": " + e.getMessage(), e);
            }
        }
        return tours;
    }

    /** Null-safe search match across name, location, and tags. */
    private boolean matchesQuery(Tour t, String lowerQuery) {
        if (t == null) return false;
        boolean matchName = t.getName() != null && t.getName().toLowerCase().contains(lowerQuery);
        boolean matchLoc  = t.getLocation() != null && t.getLocation().toLowerCase().contains(lowerQuery);
        boolean matchTag  = false;
        if (t.getTags() != null) {
            for (String tag : t.getTags()) {
                if (tag != null && tag.toLowerCase().contains(lowerQuery)) {
                    matchTag = true;
                    break;
                }
            }
        }
        return matchName || matchLoc || matchTag;
    }
}
