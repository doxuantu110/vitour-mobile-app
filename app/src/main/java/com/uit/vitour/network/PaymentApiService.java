package com.uit.vitour.network;

import com.uit.vitour.model.PaymentRequest;
import com.uit.vitour.model.PaymentResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface PaymentApiService {
    @POST("api/payment/create")
    Call<PaymentResponse> createPayment(@Body PaymentRequest request);
}
