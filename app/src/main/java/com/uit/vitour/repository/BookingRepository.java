package com.uit.vitour.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.uit.vitour.model.Booking;
import com.uit.vitour.model.Tour;
import com.uit.vitour.utils.NetworkUtils;
import com.uit.vitour.utils.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BookingRepository {

    private static final String TAG = "BookingRepository";
    private final FirebaseFirestore db;

    public BookingRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public LiveData<Resource<Booking>> createBooking(Context context, Booking booking) {
        MutableLiveData<Resource<Booking>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (!NetworkUtils.isConnected(context)) {
            result.setValue(Resource.error("No internet connection", null));
            return result;
        }

        String generatedBookingId = UUID.randomUUID().toString().substring(0, 8);
        booking.setBookingId(generatedBookingId);
        
        DocumentReference tourRef = db.collection("tours").document(booking.getTourId());
        DocumentReference bookingRef = db.collection("bookings").document(generatedBookingId);

        db.runTransaction(transaction -> {
            Tour tour = transaction.get(tourRef).toObject(Tour.class);
            if (tour == null) {
                throw new FirebaseFirestoreException("Tour not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            int requestedSlots = booking.getAdultsCount() + booking.getChildrenCount();
            int remaining = tour.getMaxCapacity() - tour.getBookedSlots();

            if (requestedSlots > remaining) {
                throw new FirebaseFirestoreException("Not enough capacity. Remaining slots: " + remaining, FirebaseFirestoreException.Code.ABORTED);
            }

            // Update Tour
            transaction.update(tourRef, "bookedSlots", tour.getBookedSlots() + requestedSlots);

            // Set Server Timestamps using FieldValue since @ServerTimestamp only works on set/update natively sometimes, 
            // but we can just use set() and Firebase will handle @ServerTimestamp annotations.
            booking.setStatus(Booking.STATUS_PENDING); // Or CONFIRMED
            transaction.set(bookingRef, booking);

            return booking;
        }).addOnSuccessListener(savedBooking -> {
            Log.d(TAG, "Booking created successfully: " + savedBooking.getBookingId());
            result.setValue(Resource.success(savedBooking));
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to create booking: " + e.getMessage(), e);
            result.setValue(Resource.error(e.getMessage(), null));
        });

        return result;
    }

    public LiveData<Resource<Boolean>> cancelBooking(Context context, String bookingId) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (!NetworkUtils.isConnected(context)) {
            result.setValue(Resource.error("No internet connection", null));
            return result;
        }

        DocumentReference bookingRef = db.collection("bookings").document(bookingId);

        db.runTransaction(transaction -> {
            Booking booking = transaction.get(bookingRef).toObject(Booking.class);
            if (booking == null) {
                throw new FirebaseFirestoreException("Booking not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            if (Booking.STATUS_CANCELLED.equals(booking.getStatus())) {
                throw new FirebaseFirestoreException("Booking is already cancelled", FirebaseFirestoreException.Code.ABORTED);
            }

            DocumentReference tourRef = db.collection("tours").document(booking.getTourId());
            Tour tour = transaction.get(tourRef).toObject(Tour.class);
            if (tour == null) {
                throw new FirebaseFirestoreException("Associated tour not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            int slotsToFree = booking.getAdultsCount() + booking.getChildrenCount();

            // Update Tour bookedSlots
            transaction.update(tourRef, "bookedSlots", Math.max(0, tour.getBookedSlots() - slotsToFree));

            // Update Booking status
            transaction.update(bookingRef, "status", Booking.STATUS_CANCELLED);
            transaction.update(bookingRef, "updatedAt", FieldValue.serverTimestamp());

            return true;
        }).addOnSuccessListener(success -> {
            Log.d(TAG, "Booking cancelled successfully: " + bookingId);
            result.setValue(Resource.success(true));
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to cancel booking: " + e.getMessage(), e);
            result.setValue(Resource.error(e.getMessage(), null));
        });

        return result;
    }

    public LiveData<Resource<Boolean>> updateBookingStatus(String bookingId, String newStatus) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        if (bookingId == null || bookingId.isEmpty()) {
            result.setValue(Resource.error("Invalid booking ID", null));
            return result;
        }

        db.collection("bookings")
                .document(bookingId)
                .update("status", newStatus, "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Booking status updated successfully: " + bookingId);
                    result.setValue(Resource.success(true));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update booking status: " + e.getMessage(), e);
                    result.setValue(Resource.error(e.getMessage(), null));
                });

        return result;
    }

    public LiveData<Resource<List<Booking>>> getUserBookings(Context context, String userId) {
        MutableLiveData<Resource<List<Booking>>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading(null));
        
        if (!NetworkUtils.isConnected(context)) {
            liveData.setValue(Resource.error("No internet connection", null));
            return liveData;
        }

        db.collection("bookings")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "getUserBookings() Firestore error: " + error.getMessage(), error);
                        liveData.setValue(Resource.error(error.getMessage(), null));
                        return;
                    }

                    if (snapshot == null) {
                        liveData.setValue(Resource.success(new ArrayList<>()));
                        return;
                    }

                    List<Booking> bookings = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        try {
                            Booking booking = doc.toObject(Booking.class);
                            if (booking != null) {
                                if (booking.getBookingId() == null) {
                                    booking.setBookingId(doc.getId());
                                }
                                bookings.add(booking);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error mapping booking doc " + doc.getId(), e);
                        }
                    }

                    liveData.setValue(Resource.success(bookings));
                });

        return liveData;
    }
}
