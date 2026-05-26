package com.uit.vitour.backend.service;

import com.uit.vitour.backend.dto.CreatePaymentRequest;
import com.uit.vitour.backend.dto.CreatePaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class SePayService {

    @Value("${sepay.bank}")
    private String bankCode;

    @Value("${sepay.account-number}")
    private String accountNumber;

    @Value("${sepay.account-name}")
    private String accountName;

    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        log.info("[SEPAY_CREATE] Creating payment for booking: {}", request.getBookingId());

        long amount = request.getAmount();
        String bookingId = request.getBookingId();
        String shortBookingId = bookingId.length() >= 8 ? bookingId.substring(0, 8) : bookingId;

        String description = "BOOKING_" + shortBookingId;
        String encodedDescription = "";
        try {
            encodedDescription = URLEncoder.encode(description, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            encodedDescription = description;
            log.error("Failed to URL encode description", e);
        }

        String qrUrl = "https://qr.sepay.vn/img?acc=0358522788&bank=MB&amount="
                + request.getAmount() + "&des=BOOKING_" + request.getBookingId();

        log.info("[SEPAY_QR] Generated QR URL: {}", qrUrl);

        return CreatePaymentResponse.builder()
                .qrCodeUrl(qrUrl)
                .accountNumber(accountNumber)
                .bankCode(bankCode)
                .bookingId(bookingId)
                .amount(amount)
                .message("QR generated")
                .build();
    }
}
