package com.uit.vitour.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentStatusResponse {
    private String bookingId;
    private String status; // PENDING, SUCCESS, FAILED, CANCELLED, EXPIRED
    private String message;
}
