package com.uit.vitour.network;

import com.uit.vitour.model.Tour;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * TourApiService.java — Retrofit HTTP interface for tour endpoints.
 *
 * Naming convention: method names describe the action + resource.
 * Add new endpoints here as the API grows.
 *
 * Each method returns a Call<T>. The Repository calls .enqueue() on it
 * and maps the result to a Resource<T> via LiveData.
 */
public interface TourApiService {

    /**
     * GET /tours
     * Query params: page, size, tag (optional filter)
     */
    @GET("tours")
    Call<List<Tour>> getTours(
            @Query("page") int page,
            @Query("size") int size,
            @Query("tag") String tag        // nullable — omit to get all
    );

    /**
     * GET /tours/{id}
     * Fetch a single tour's full detail.
     */
    @GET("tours/{id}")
    Call<Tour> getTourById(@Path("id") String tourId);

    /**
     * GET /tours/search
     * Search tours by keyword.
     */
    @GET("tours/search")
    Call<List<Tour>> searchTours(@Query("q") String query);
}
