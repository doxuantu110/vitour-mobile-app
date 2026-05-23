package com.uit.vitour.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.uit.vitour.model.Review;
import com.uit.vitour.utils.ImageCompressor;
import com.uit.vitour.utils.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ReviewRepository.java — All Firestore + Storage operations for reviews.
 *
 * Firestore schema:
 *   tours/{tourId}/reviews/{reviewId}
 *     ├── userId      : string
 *     ├── userName    : string
 *     ├── userAvatar  : string | null
 *     ├── rating      : number (1-5)
 *     ├── comment     : string
 *     ├── imageUrl    : string | null
 *     └── createdAt   : timestamp
 *
 * Storage schema (review images):
 *   reviews/{tourId}/{userId}/{uuid}.jpg
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * LISTENER LIFECYCLE (memory-leak prevention)
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Listeners are tracked in a HashMap keyed by tourId:
 *
 *   activeListeners : Map<tourId, ListenerRegistration>
 *
 * This design guarantees:
 *
 *   1. DEDUPLICATION: Calling getReviewsForTour(tourId) a second time for the
 *      same tourId first removes the previous listener before attaching a new
 *      one. No two listeners can exist for the same tourId simultaneously.
 *
 *   2. FRAGMENT RECREATION SAFETY: ReviewViewModel survives configuration
 *      changes (rotation). The guard in ReviewViewModel.setTourId() uses
 *      equals() to skip re-triggering switchMap when the tourId has not
 *      changed, so getReviewsForTour() is normally never called twice for the
 *      same tourId during a single ViewModel lifetime.
 *      The HashMap dedup is a second safety net in case that guard is bypassed.
 *
 *   3. SELECTIVE REMOVAL: removeListenerForTour(tourId) lets the ViewModel
 *      cleanly detach one specific listener without touching others.
 *
 *   4. FULL CLEANUP: removeAllListeners() is called from ReviewViewModel
 *      .onCleared(), which fires when the Fragment is permanently destroyed
 *      (not on rotation), ensuring all Firestore sockets are closed.
 *
 * Security rules (add to Firebase Console):
 *   match /tours/{tourId}/reviews/{reviewId} {
 *     allow read: if true;
 *     allow create: if request.auth != null;
 *     allow update, delete: if request.auth.uid == resource.data.userId;
 *   }
 */
public class ReviewRepository {

    private static final String TAG = "ReviewRepository";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final ExecutorService executor;

    /**
     * Active snapshot listeners keyed by tourId.
     *
     * WHY HashMap instead of List:
     *   A List gives no way to find the existing listener for a specific tourId.
     *   A Map lets us look it up in O(1) and remove it before adding a new one,
     *   preventing duplicate listeners for the same tour.
     */
    private final Map<String, ListenerRegistration> activeListeners = new HashMap<>();

    /**
     * Active upload tasks keyed by tourId.
     * Used to cancel uploads if the user navigates away or cancels.
     */
    private final Map<String, String> activeUploads = new HashMap<>();

    public ReviewRepository() {
        this.db      = FirebaseFirestore.getInstance();
        this.auth    = FirebaseAuth.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read: Realtime listener for reviews of a specific tour
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a LiveData that emits the review list in real-time.
     * Reviews are ordered newest-first by createdAt.
     *
     * DEDUPLICATION: If a listener for {@code tourId} is already registered,
     * it is removed before the new one is attached. This prevents double
     * Firestore reads if this method is called more than once for the same tour.
     *
     * @param tourId Firestore document ID of the parent tour
     */
    public LiveData<Resource<List<Review>>> getReviewsForTour(String tourId) {
        MutableLiveData<Resource<List<Review>>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading(null));

        // ── Guard: remove any pre-existing listener for this tourId ────────
        if (activeListeners.containsKey(tourId)) {
            ListenerRegistration existing = activeListeners.get(tourId);
            if (existing != null) {
                existing.remove();
                Log.d(TAG, "getReviewsForTour() — removed EXISTING listener before re-attach"
                        + " [tourId=" + tourId + "]");
            }
            activeListeners.remove(tourId);
        }

        Log.d(TAG, "getReviewsForTour() — ATTACHING new snapshot listener"
                + " [tourId=" + tourId + "]"
                + " | total active listeners after attach: " + (activeListeners.size() + 1));

        ListenerRegistration reg = db
                .collection("tours")
                .document(tourId)
                .collection("reviews")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "getReviewsForTour() listener error"
                                + " [tourId=" + tourId + "]: " + e.getMessage(), e);
                        liveData.postValue(Resource.error("Failed to load reviews", null));
                        return;
                    }

                    if (snapshots == null) {
                        Log.w(TAG, "getReviewsForTour() received null snapshot"
                                + " [tourId=" + tourId + "] — posting empty list");
                        liveData.postValue(Resource.success(new ArrayList<>()));
                        return;
                    }

                    if (snapshots.getMetadata().hasPendingWrites()) {
                        Log.d(TAG, "getReviewsForTour() snapshot has pending writes — ignoring optimistic update"
                                + " [tourId=" + tourId + "]");
                        return;
                    }

                    List<Review> reviews = new ArrayList<>();
                    for (var doc : snapshots.getDocuments()) {
                        try {
                            Review r = doc.toObject(Review.class);
                            if (r != null) {
                                reviews.add(r);
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Review parse error for doc " + doc.getId()
                                    + " [tourId=" + tourId + "]: " + ex.getMessage());
                        }
                    }

                    Log.d(TAG, "getReviewsForTour() snapshot received"
                            + " [tourId=" + tourId + "]"
                            + " — " + reviews.size() + " reviews"
                            + " | fromCache=" + snapshots.getMetadata().isFromCache());
                    liveData.postValue(Resource.success(reviews));
                });

        // Store listener by tourId for deduplication and selective removal
        activeListeners.put(tourId, reg);
        return liveData;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Write: Add a new review (with optional image upload)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Submits a new review to Firestore.
     * If {@code imageUri} is not null, the image is uploaded to Firebase Storage first,
     * and the resulting download URL is stored in the review document.
     *
     * @param context  Context for ImageCompressor (must be Application Context)
     * @param tourId   Parent tour ID
     * @param rating   1–5 stars
     * @param comment  Review text
     * @param imageUri Optional local image URI selected by the user
     * @return LiveData emitting LOADING → SUCCESS(Review) | ERROR(message)
     */
    public LiveData<Resource<Review>> addReview(
            Context context, String tourId, int rating, String comment, Uri imageUri) {

        MutableLiveData<Resource<Review>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "addReview() called but user is not authenticated");
            result.setValue(Resource.error("You must be logged in to leave a review", null));
            return result;
        }

        String userId     = currentUser.getUid();
        String userName   = currentUser.getDisplayName() != null
                ? currentUser.getDisplayName() : "Anonymous";
        String userAvatar = currentUser.getPhotoUrl() != null
                ? currentUser.getPhotoUrl().toString() : null;

        Log.d(TAG, "addReview() — tourId=" + tourId + ", userId=" + userId
                + ", rating=" + rating + ", hasImage=" + (imageUri != null));

        Review review = new Review(tourId, userId, userName, userAvatar, rating, comment);

        if (imageUri != null) {
            // Step 1: Upload image → Step 2: Write review with imageUrl
            uploadReviewImage(context, tourId, userId, imageUri, result, review);
        } else {
            // No image — write directly
            writeReviewToFirestore(tourId, review, result);
        }

        return result;
    }

    private void uploadReviewImage(Context context, String tourId, String userId, Uri imageUri,
                                   MutableLiveData<Resource<Review>> result, Review review) {

        Log.d("Cloudinary", "CLOUDINARY_UPLOAD_STARTED");

        // Run compression on background thread to prevent UI jank
        executor.execute(() -> {
            byte[] compressedBytes = ImageCompressor.compress(context, imageUri);

            if (compressedBytes == null) {
                Log.e("Cloudinary", "Compression failed, byte array is null");
                result.postValue(Resource.error("Failed to compress image (too large or unsupported)", null));
                return;
            }

            Log.d("Cloudinary", "File byte size: " + compressedBytes.length);

            String requestId = MediaManager.get().upload(compressedBytes)
                    .unsigned("vitour_reviews")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d("Cloudinary", "CLOUDINARY_UPLOAD_STARTED: " + requestId);
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            double progress = (100.0 * bytes) / totalBytes;
                            Log.d("Cloudinary", "CLOUDINARY_UPLOAD_PROGRESS: " + String.format("%.1f%%", progress));
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            activeUploads.remove(tourId);
                            Log.d("Cloudinary", "CLOUDINARY_UPLOAD_SUCCESS");
                            
                            String secureUrl = (String) resultData.get("secure_url");
                            if (secureUrl != null) {
                                review.setImageUrl(secureUrl);
                                writeReviewToFirestore(tourId, review, result);
                            } else {
                                Log.e("Cloudinary", "CLOUDINARY_UPLOAD_FAILED: missing secure_url");
                                result.postValue(Resource.error("Upload succeeded but secure_url missing", null));
                            }
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            activeUploads.remove(tourId);
                            Log.e("Cloudinary", "CLOUDINARY_UPLOAD_FAILED: " + error.getDescription());
                            result.postValue(Resource.error("Image upload failed: " + error.getDescription(), null));
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.w("Cloudinary", "Upload rescheduled: " + error.getDescription());
                        }
                    })
                    .dispatch();
                    
            activeUploads.put(tourId, requestId);
        });
    }

    private void writeReviewToFirestore(String tourId, Review review,
                                        MutableLiveData<Resource<Review>> result) {
        Log.d("FirestoreReview", "Starting writeReviewToFirestore for tourId=" + tourId);
        Log.d("FirestoreReview", "FIRESTORE_WRITE_STARTED");
        
        db.collection("tours").document(tourId).collection("reviews")
                .add(review)
                .addOnSuccessListener(docRef -> {
                    review.setId(docRef.getId());
                    Log.d("FirestoreReview", "FIRESTORE_WRITE_SUCCESS");
                    result.postValue(Resource.success(review)); // Only report success AFTER Firestore succeeds
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreReview", "FIRESTORE_WRITE_FAILED: " + e.getMessage(), e);
                    result.postValue(Resource.error("Failed to submit review: " + e.getMessage(), null));
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Removes the Firestore snapshot listener for a specific tour.
     *
     * Use this when navigating away from a detail screen while keeping the
     * ViewModel alive (e.g. the user pushes a second detail screen on the
     * back stack). Normally you don't need to call this directly — let
     * {@link #removeAllListeners()} handle cleanup from {@code onCleared()}.
     *
     * @param tourId The tour whose listener should be detached.
     */
    public void removeListenerForTour(String tourId) {
        ListenerRegistration reg = activeListeners.remove(tourId);
        if (reg != null) {
            reg.remove();
            Log.d(TAG, "removeListenerForTour() — detached listener"
                    + " [tourId=" + tourId + "]"
                    + " | remaining active listeners: " + activeListeners.size());
        } else {
            Log.w(TAG, "removeListenerForTour() called for tourId=" + tourId
                    + " but no listener was registered for it");
        }
    }

    /**
     * Cancels any active upload task for the given tour.
     */
    public void cancelUploadForTour(String tourId) {
        String requestId = activeUploads.remove(tourId);
        if (requestId != null) {
            MediaManager.get().cancelRequest(requestId);
            Log.d(TAG, "cancelUploadForTour() — Canceled active Cloudinary upload for tourId=" + tourId);
        }
    }

    /**
     * Removes ALL active Firestore snapshot listeners and cancels uploads.
     *
     * MUST be called from {@link ReviewViewModel#onCleared()} to release
     * Firestore sockets when the Fragment is permanently destroyed.
     * This method is safe to call multiple times (idempotent).
     */
    public void removeAllListeners() {
        int count = activeListeners.size();
        for (Map.Entry<String, ListenerRegistration> entry : activeListeners.entrySet()) {
            entry.getValue().remove();
            Log.d(TAG, "removeAllListeners() — detached listener"
                    + " [tourId=" + entry.getKey() + "]");
        }
        for (Map.Entry<String, String> entry : activeUploads.entrySet()) {
            MediaManager.get().cancelRequest(entry.getValue());
            Log.d(TAG, "removeAllListeners() — canceled Cloudinary upload for tourId=" + entry.getKey());
        }
        activeUploads.clear();
        Log.d(TAG, "removeAllListeners() — done. Detached " + count + " listener(s).");
    }
}
