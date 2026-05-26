package com.uit.vitour.network;

import com.uit.vitour.utils.Constants;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * ApiClient.java — Retrofit singleton.
 *
 * WHY singleton: Retrofit + OkHttp are expensive to create; reusing one instance
 * across the app avoids wasted memory and thread-pool churn.
 *
 * How to use:
 *   TourApiService service = ApiClient.getInstance().getTourApiService();
 *
 * Extending: add more service getters (e.g. getUserApiService()) here.
 */
public class ApiClient {

    private static ApiClient instance;
    private final Retrofit retrofit;
    private final Retrofit paymentRetrofit;

    private boolean isEmulator() {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(android.os.Build.PRODUCT));
    }

    private ApiClient() {
        // Log HTTP requests/responses in debug builds.
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Add detailed logging for OkHttp base URL
        okhttp3.Interceptor baseUrlLogger = new okhttp3.Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws java.io.IOException {
                okhttp3.Request request = chain.request();
                android.util.Log.d("ApiClient", "-> Executing request to: " + request.url());
                return chain.proceed(request);
            }
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(baseUrlLogger)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
                
        // Detect emulator vs physical device dynamically
        String backendBaseUrl = isEmulator() ? "http://10.0.2.2:8080/" : "http://10.99.67.71:8080/";
        android.util.Log.i("ApiClient", "Initializing Payment API with URL: " + backendBaseUrl + " (Emulator: " + isEmulator() + ")");

        paymentRetrofit = new Retrofit.Builder()
                .baseUrl(backendBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /** Thread-safe lazy singleton accessor. */
    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    /** Returns the service for tour-related endpoints. */
    public TourApiService getTourApiService() {
        return retrofit.create(TourApiService.class);
    }
    
    /** Returns the service for local Spring Boot Payment endpoints. */
    public PaymentApiService getPaymentApiService() {
        return paymentRetrofit.create(PaymentApiService.class);
    }
}
