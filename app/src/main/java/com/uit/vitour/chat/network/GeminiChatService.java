package com.uit.vitour.chat.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.GET;
import retrofit2.http.Streaming;

import java.util.Map;

public interface GeminiChatService {

    /**
     * Endpoint to fetch available models
     */
    @GET("v1beta/models")
    Call<ResponseBody> getAvailableModels(@Query("key") String apiKey);

    /**
     * Endpoint for non-streaming standard requests to Gemini API
     */
    @POST("v1beta/models/{model}:generateContent")
    Call<ResponseBody> sendMessage(
            @Path("model") String model,
            @Query("key") String apiKey,
            @Body Map<String, Object> body
    );

    /**
     * Endpoint for streaming requests to Gemini API via SSE
     */
    @Streaming
    @POST("v1beta/models/{model}:streamGenerateContent?alt=sse")
    Call<ResponseBody> streamMessage(
            @Path("model") String model,
            @Query("key") String apiKey,
            @Body Map<String, Object> body
    );
}
