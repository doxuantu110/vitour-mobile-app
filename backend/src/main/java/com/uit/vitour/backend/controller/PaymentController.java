package com.uit.vitour.backend.controller;

import com.uit.vitour.backend.dto.CreatePaymentRequest;
import com.uit.vitour.backend.dto.CreatePaymentResponse;
import com.uit.vitour.backend.dto.SePayWebhookRequest;
import com.uit.vitour.backend.repository.PaymentRepository;
import com.uit.vitour.backend.service.SePayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@Slf4j
public class PaymentController {

    @Autowired
    private SePayService sePayService;

    @Autowired
    private PaymentRepository paymentRepository;

    @PostMapping("/create")
    public ResponseEntity<CreatePaymentResponse> createPayment(@RequestBody CreatePaymentRequest request) {
        log.info("Received request to create payment for booking: {}", request.getBookingId());

        // 1. Verify if booking exists and amount matches before initiating payment
        String validationError = paymentRepository.verifyBookingForPayment(request.getBookingId(), request.getAmount());
        if (validationError != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CreatePaymentResponse.builder().message(validationError).build());
        }

        // 2. Call SePay Service
        CreatePaymentResponse response = sePayService.createPayment(request);
        response.setSuccess(true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sepay/webhook")
    public ResponseEntity<Void> handleSePayWebhook(@RequestBody SePayWebhookRequest webhookRequest) {
        log.info("[SEPAY_WEBHOOK] Received SePay Webhook: {}", webhookRequest);

        if (!"in".equalsIgnoreCase(webhookRequest.getTransferType())) {
            log.info("[SEPAY_WEBHOOK] Ignoring out transfer.");
            return ResponseEntity.ok().build();
        }

        String content = webhookRequest.getContent();
        log.info("[SEPAY_WEBHOOK] Step 1: Webhook received. Extracted transfer content: '{}'", content);

        if (content == null || !content.contains("BOOKING_")) {
            log.warn("[SEPAY_WEBHOOK] Content does not contain BOOKING_: {}", content);
            return ResponseEntity.ok().build();
        }

        // Extract booking ID using Regex. E.g. "O5CH7JFRAMQF-BOOKING_1ce70f1a" -> "1ce70f1a"
        String bookingId = null;
        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("BOOKING_([A-Za-z0-9]{8})").matcher(content);
            if (matcher.find()) {
                bookingId = matcher.group(1);
                log.info("[SEPAY_WEBHOOK] Step 2: Regex successfully matched booking ID: '{}'", bookingId);
            } else {
                log.warn("[SEPAY_WEBHOOK] Regex could not find an 8-character ID following BOOKING_");
            }
        } catch (Exception e) {
            log.error("[SEPAY_WEBHOOK] Error extracting bookingId with Regex", e);
            return ResponseEntity.ok().build();
        }

        if (bookingId == null || bookingId.isEmpty()) {
            log.warn("[SEPAY_WEBHOOK] Could not extract bookingId from content: {}", content);
            return ResponseEntity.ok().build();
        }

        log.info("[SEPAY_WEBHOOK] Step 3: Processing payment for booking: {}", bookingId);

        // Verify amount and ensure it is not already PAID
        String validationError = paymentRepository.verifyBookingForPayment(bookingId, webhookRequest.getTransferAmount());
        if (validationError == null) {
            paymentRepository.updateBookingStatus(bookingId, "PAID", webhookRequest.getReferenceCode());
            log.info("[SEPAY_WEBHOOK] Step 4: Firebase updated successfully for booking: {}", bookingId);
        } else {
            log.warn("[PAYMENT_FAILED] Booking {} was not valid for webhook update (may be already PAID or amount mismatch): {}", bookingId, validationError);
            // Ignore duplicate webhook by returning 200 OK.
        }

        return ResponseEntity.ok().build();
    }
}
