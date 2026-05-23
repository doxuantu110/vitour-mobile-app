package com.uit.vitour.viewmodel;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.uit.vitour.model.Review;
import com.uit.vitour.repository.ReviewRepository;
import com.uit.vitour.utils.Resource;

import java.util.List;

/**
 * ReviewViewModel.java
 *
 * Manages review state for a single tour.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * LISTENER DEDUPLICATION ACROSS FRAGMENT RECREATION
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * ViewModels survive configuration changes (screen rotation). This means the
 * ViewModel — and its ReviewRepository — are NOT recreated on rotation. Only the
 * Fragment is recreated.
 *
 * Deduplication is enforced at TWO levels:
 *
 *   Level 1 — ViewModel (setTourId guard):
 *     setTourId() compares the new ID against the current value. If they match,
 *     tourIdLiveData.setValue() is skipped, so switchMap does NOT fire, and
 *     getReviewsForTour() is never called again. This is the primary guard.
 *
 *   Level 2 — Repository (HashMap guard):
 *     If somehow getReviewsForTour() IS called twice for the same tourId, the
 *     Repository removes the existing listener before attaching the new one.
 *     This acts as a safety net against logic bugs that bypass Level 1.
 *
 * Result: No duplicate Firestore listeners can accumulate during the ViewModel's
 * lifetime, regardless of how many times the Fragment is recreated.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * CLEANUP
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * onCleared() fires when the Fragment is PERMANENTLY destroyed (e.g. back
 * pressed, or the Activity finishes). It calls removeAllListeners() on the
 * repository, closing all open Firestore sockets and preventing memory leaks.
 *
 * LiveData chains:
 *   tourIdLiveData  ─switchMap→  reviewsLiveData  (realtime snapshot)
 *   addReview()     ──────────→  one-shot LiveData (write operation)
 */
public class ReviewViewModel extends ViewModel {

    private static final String TAG = "ReviewViewModel";

    private final ReviewRepository repository;

    /**
     * Triggers the switchMap whenever a new tourId is set.
     * The Level 1 deduplication guard lives in setTourId().
     */
    private final MutableLiveData<String> tourIdLiveData = new MutableLiveData<>();

    /**
     * Realtime stream of reviews for the current tourId.
     * Backed by a Firestore addSnapshotListener in ReviewRepository.
     */
    private final LiveData<Resource<List<Review>>> reviewsLiveData;

    public ReviewViewModel() {
        this.repository = new ReviewRepository();

        // switchMap re-subscribes only when tourIdLiveData emits a NEW value.
        // Because setTourId() uses an equals() guard, this only fires once
        // per unique tourId during the ViewModel's lifetime.
        this.reviewsLiveData = Transformations.switchMap(tourIdLiveData, tourId -> {
            if (tourId == null || tourId.isEmpty()) {
                Log.w(TAG, "switchMap: tourId is null/empty — returning error LiveData");
                MutableLiveData<Resource<List<Review>>> err = new MutableLiveData<>();
                err.setValue(Resource.error("Invalid tour ID", null));
                return err;
            }
            Log.d(TAG, "switchMap: firing getReviewsForTour() for tourId=" + tourId);
            return repository.getReviewsForTour(tourId);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sets the tour whose reviews should be streamed.
     *
     * DEDUPLICATION (Level 1): If the tourId has not changed since the last
     * call, this method is a no-op — it does NOT re-trigger switchMap and does
     * NOT create a new Firestore listener.
     *
     * Safe to call every time the Fragment's onViewCreated() runs (i.e. on
     * rotation), because the ViewModel retains the previous tourId value and
     * the equals() guard short-circuits.
     *
     * @param tourId Firestore document ID of the tour whose reviews to stream.
     */
    public void setTourId(String tourId) {
        String current = tourIdLiveData.getValue();
        if (current != null && current.equals(tourId)) {
            // Level 1 guard: same tourId — skip re-subscription entirely.
            Log.d(TAG, "setTourId: tourId unchanged (" + tourId + ") — skipping re-attach");
            return;
        }
        Log.d(TAG, "setTourId: new tourId=" + tourId
                + (current != null ? " (was: " + current + ")" : " (first call)"));
        tourIdLiveData.setValue(tourId);
    }

    /**
     * Observe this to receive the live review list for the current tour.
     * Emits: LOADING → SUCCESS(List<Review>) | ERROR(message)
     */
    public LiveData<Resource<List<Review>>> getReviews() {
        return reviewsLiveData;
    }

    /**
     * Submit a new review to Firestore (with optional image upload to Storage).
     *
     * Returns a one-shot LiveData. Observe in AddReviewDialogFragment to react
     * to SUCCESS (dismiss + show snackbar) or ERROR (show error message).
     * The realtime listener will automatically push the new review into
     * getReviews() — no manual refresh needed.
     *
     * @param context  Context for ImageCompressor (Application Context recommended)
     * @param tourId   Parent tour Firestore document ID
     * @param rating   1–5 stars
     * @param comment  Review text (must not be empty)
     * @param imageUri Optional local image URI; null = no photo attached
     */
    public LiveData<Resource<Review>> addReview(
            Context context, String tourId, int rating, String comment, Uri imageUri) {
        Log.d(TAG, "addReview() — tourId=" + tourId + ", rating=" + rating);
        return repository.addReview(context, tourId, rating, comment, imageUri);
    }

    /**
     * Cancels any ongoing image upload for this tour.
     * Call this if the user dismisses the review dialog before completion.
     */
    public void cancelCurrentUpload(String tourId) {
        if (tourId != null && !tourId.isEmpty()) {
            repository.cancelUploadForTour(tourId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called when the Fragment is permanently destroyed.
     * Removes ALL Firestore snapshot listeners via the repository.
     *
     * NOT called on screen rotation — the ViewModel survives rotation, so
     * listeners remain active and continue pushing updates to the cached
     * LiveData, which the recreated Fragment immediately re-observes.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        repository.removeAllListeners();
        Log.d(TAG, "onCleared() — all Firestore listeners detached via repository");
    }
}
