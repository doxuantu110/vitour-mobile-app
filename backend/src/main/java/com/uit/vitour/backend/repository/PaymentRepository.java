package com.uit.vitour.backend.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
@Slf4j
public class PaymentRepository {

    /**
     * Verify booking exists, amount matches, and it is not already PAID/SUCCESS.
     */
    public String verifyBookingForPayment(String bookingId, Long requestAmount) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            DocumentReference docRef = db.collection("bookings").document(bookingId);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();

            if (!document.exists()) {
                log.warn("Booking not found: {}", bookingId);
                return "Booking not found";
            }

            // Verify status is not already completed
            String status = document.getString("status");
            if ("SUCCESS".equals(status) || "CONFIRMED".equals(status)) {
                log.warn("Booking {} is already paid/confirmed.", bookingId);
                return "Booking already paid";
            }
            if ("CANCELLED".equals(status)) {
                log.warn("Booking {} is cancelled.", bookingId);
                return "Booking is cancelled";
            }

            // Verify amount
            Double dbAmount = document.getDouble("totalPrice");
            if (dbAmount == null) {
                log.error("Missing totalPrice for booking: {}", bookingId);
                return "Missing totalPrice";
            }

            long expectedAmount = Math.round(dbAmount);
            
            log.info("[PAYMENT_VERIFY] bookingId={} dbAmount={} expectedAmount={} requestAmount={}", 
                     bookingId, dbAmount, expectedAmount, requestAmount);

            if (expectedAmount != requestAmount) {
                log.error("Amount mismatch for {}. Expected: {}, Received: {}", bookingId, expectedAmount, requestAmount);
                return "Amount mismatch: expected " + expectedAmount + " received " + requestAmount;
            }

            return null; // valid
        } catch (Exception e) {
            log.error("Error verifying booking: " + e.getMessage(), e);
            return "Internal validation error";
        }
    }

    /**
     * Updates the status of the booking in Firestore.
     */
    public void updateBookingStatus(String bookingId, String paymentStatus, String transId) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            DocumentReference docRef = db.collection("bookings").document(bookingId);

            Map<String, Object> updates = new HashMap<>();
            updates.put("paymentStatus", paymentStatus);
            if ("PAID".equals(paymentStatus)) {
                updates.put("status", "CONFIRMED");
                updates.put("paidAt", com.google.cloud.firestore.FieldValue.serverTimestamp());
                log.info("[PAYMENT_SUCCESS] Booking {} confirmed and paid", bookingId);
            } else if ("CANCELLED".equals(paymentStatus)) {
                updates.put("status", "CANCELLED");
                log.info("[PAYMENT_FAILED] Booking {} payment expired/cancelled", bookingId);
            } else {
                updates.put("status", paymentStatus);
            }
            
            if (transId != null) {
                updates.put("transactionId", transId);
            }
            updates.put("updatedAt", com.google.cloud.firestore.FieldValue.serverTimestamp());

            ApiFuture<WriteResult> result = docRef.update(updates);
            log.info("[FIRESTORE_PAYMENT_UPDATED] Booking {} paymentStatus updated to {} at time {}", bookingId, paymentStatus, result.get().getUpdateTime());
        } catch (Exception e) {
            log.error("Error updating booking status: " + e.getMessage(), e);
        }
    }
}
