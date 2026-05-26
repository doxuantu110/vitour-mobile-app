package com.uit.vitour.backend.dto;

import lombok.Data;

@Data
public class SePayWebhookRequest {
    private Long id;
    private String gateway;
    private String transactionDate;
    private String accountNumber;
    private String code;
    private String content; // This will contain BOOKING_{bookingId}
    private String transferType; // "in" or "out"
    private Long transferAmount;
    private Long accumulated;
    private String subAccount;
    private String referenceCode;
    private String description;
}
