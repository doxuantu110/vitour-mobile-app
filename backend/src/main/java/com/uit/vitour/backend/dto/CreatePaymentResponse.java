package com.uit.vitour.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreatePaymentResponse {
    private boolean success;
    private String payUrl;
    private String deeplink;
    private String qrCodeUrl;
    private String accountNumber;
    private String bankCode;
    private String message;
    private String bookingId;
    private long amount;
    private int resultCode;
}
