package com.uit.vitour.model;

public class PaymentRequest {
    private String bookingId;
    private long amount;
    private String orderInfo;

    public PaymentRequest(String bookingId, long amount, String orderInfo) {
        this.bookingId = bookingId;
        this.amount = amount;
        this.orderInfo = orderInfo;
    }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    
    public String getOrderInfo() { return orderInfo; }
    public void setOrderInfo(String orderInfo) { this.orderInfo = orderInfo; }
}
