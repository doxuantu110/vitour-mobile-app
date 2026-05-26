package com.uit.vitour.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PaymentSharedViewModel extends ViewModel {

    public static class PaymentResult {
        public final int status; // 0 = Success, 1 = Failed, 2 = Cancelled
        public final String orderId;

        public PaymentResult(int status, String orderId) {
            this.status = status;
            this.orderId = orderId;
        }
    }

    private final MutableLiveData<PaymentResult> paymentResult = new MutableLiveData<>();

    public void setPaymentResult(int status, String orderId) {
        paymentResult.setValue(new PaymentResult(status, orderId));
    }

    public LiveData<PaymentResult> getPaymentResult() {
        return paymentResult;
    }
    
    public void clearPaymentResult() {
        paymentResult.setValue(null);
    }
}
