package com.uit.vitour.backend.dto;

import lombok.Data;

@Data
public class CreatePaymentRequest {
    private String bookingId;
    private Long amount;
    private String orderInfo;
}
