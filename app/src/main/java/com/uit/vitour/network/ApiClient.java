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

    private ApiClient() {
        // Log HTTP requests/responses in debug builds.
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
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
}
