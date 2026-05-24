package com.uit.vitour.chat.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.uit.vitour.BuildConfig;
import com.uit.vitour.chat.network.GeminiChatService;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GeminiConfigValidator {

    private static final String TAG = "GeminiConfigValidator";

    /**
     * Validates the Gemini API key.
     * Ensures it is not null, not empty, starts with "AIza", and has a valid length.
     */
    public static boolean isValidConfiguration(String apiKey) {
        if (apiKey == null) {
            Log.e(TAG, "API Key is null. Check local.properties file.");
            return false;
        }
        
        String trimmedKey = apiKey.trim();
        
        if (trimmedKey.isEmpty()) {
            Log.e(TAG, "API Key is empty. Ensure GEMINI_API_KEY is set in local.properties.");
            return false;
        }

        if (!trimmedKey.startsWith("AIza")) {
            Log.e(TAG, "API Key must start with 'AIza'. The current key is malformed.");
            return false;
        }

        if (trimmedKey.length() < 35) {
            Log.e(TAG, "API Key is suspiciously short (length < 35). Check for accidental truncation.");
            return false;
        }

        return true;
    }

    /**
     * Prints a comprehensive diagnostic report to Logcat.
     */
    public static void debugCheck(Context context) {
        if (!BuildConfig.DEBUG) return;

        Log.d(TAG, "--- GEMINI CONFIGURATION DEBUG CHECK ---");

        String apiKey = BuildConfig.GEMINI_API_KEY;
        Log.d(TAG, "1. BuildConfig Key Exists: " + (apiKey != null && !apiKey.isEmpty()));
        
        if (apiKey != null) {
            String trimmedKey = apiKey.trim();
            Log.d(TAG, "2. Key Length: " + trimmedKey.length());
            Log.d(TAG, "3. Starts with 'AIza': " + trimmedKey.startsWith("AIza"));
            Log.d(TAG, "4. Key Masked: " + maskApiKey(trimmedKey));
        }

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isConnected = false;
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null && activeNetwork.isConnected();
        }
        Log.d(TAG, "5. Internet Available: " + isConnected);
        Log.d(TAG, "6. Endpoint Configured: https://generativelanguage.googleapis.com/");
        
        Log.d(TAG, "--- END DEBUG CHECK ---");
    }

    /**
     * Tests available models and their capabilities (stream vs non-stream).
     */
    public static void testAvailableModels(String apiKey, GeminiChatService service) {
        if (!BuildConfig.DEBUG || apiKey == null || service == null) return;
        
        Log.d(TAG, "--- TESTING AVAILABLE MODELS ---");
        service.getAvailableModels(apiKey.trim()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JSONObject root = new JSONObject(json);
                        JSONArray models = root.optJSONArray("models");
                        if (models != null) {
                            for (int i = 0; i < models.length(); i++) {
                                JSONObject model = models.getJSONObject(i);
                                String name = model.optString("name").replace("models/", "");
                                JSONArray methods = model.optJSONArray("supportedGenerationMethods");
                                
                                boolean supportsGenerate = false;
                                boolean supportsStream = false;
                                
                                if (methods != null) {
                                    for (int j = 0; j < methods.length(); j++) {
                                        String m = methods.getString(j);
                                        if ("generateContent".equals(m)) supportsGenerate = true;
                                        if ("streamGenerateContent".equals(m)) supportsStream = true;
                                    }
                                }
                                
                                if (supportsGenerate || supportsStream) {
                                    Log.d(TAG, "MODEL: " + name + " | Generate: " + supportsGenerate + " | Stream: " + supportsStream);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse models JSON: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "Failed to fetch models: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Network failure fetching models: " + t.getMessage());
            }
        });
    }

    /**
     * Returns a masked version of the API key for safe logging (e.g., AIzaSy***********abcd).
     */
    public static String maskApiKey(String key) {
        if (key == null || key.length() < 10) return "INVALID_KEY";
        return key.substring(0, 6) + "***********" + key.substring(key.length() - 4);
    }
}
