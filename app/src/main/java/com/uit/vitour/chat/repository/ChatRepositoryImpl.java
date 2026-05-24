package com.uit.vitour.chat.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.uit.vitour.BuildConfig;
import com.uit.vitour.chat.model.ChatMessage;
import com.uit.vitour.chat.model.ChatSession;
import com.uit.vitour.chat.network.GeminiChatService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatRepositoryImpl implements ChatRepository {

    private static final String TAG = "ChatRepositoryImpl";
    private final String apiKey;

    private final FirebaseFirestore db;
    private final GeminiChatService chatService;
    private Call<ResponseBody> currentCall;
    private final String currentUserId = "mock_user_id";

    private static final int MAX_RETRIES = 3;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Context applicationContext;

    // Dynamic Model Resolution
    private static final String[] PREFERRED_MODELS = {
            "gemini-2.5-flash",
            "gemini-1.5-flash-latest",
            "gemini-1.5-flash",
            "gemini-1.5-pro-latest",
            "gemini-2.0-flash",
            "gemini-pro"
    };
    private int currentModelIndex = 0;
    
    // Map of model name -> supportsStreaming
    private Map<String, Boolean> validModelsCache = null;
    private boolean isFetchingModels = false;

    private static final String SYSTEM_PROMPT = "Bạn là trợ lý du lịch ảo của ViTour, chuyên tư vấn các tour du lịch tại Việt Nam. " +
            "Hãy trả lời tự nhiên, thân thiện bằng tiếng Việt, tư vấn đúng trọng tâm và không dài dòng. Không lặp lại câu trả lời cũ. " +
            "Nếu người dùng hỏi ngoài lề, hãy khéo léo đưa họ quay lại chủ đề du lịch.";

    public ChatRepositoryImpl() {
        this.db = FirebaseFirestore.getInstance();
        this.apiKey = BuildConfig.GEMINI_API_KEY != null ? BuildConfig.GEMINI_API_KEY.trim() : "";

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BASIC : HttpLoggingInterceptor.Level.NONE);

        okhttp3.Interceptor requestLoggingInterceptor = chain -> {
            okhttp3.Request request = chain.request();
            if (BuildConfig.DEBUG) {
                String rawUrl = request.url().toString();
                String maskedUrl = rawUrl.replaceAll("key=[^&]+", "key=" + GeminiConfigValidator.maskApiKey(this.apiKey));
                Log.d(TAG, "GEMINI_REQUEST_URL: " + maskedUrl);
            }
            long start = System.currentTimeMillis();
            okhttp3.Response response = chain.proceed(request);
            if (BuildConfig.DEBUG) {
                long duration = System.currentTimeMillis() - start;
                Log.d(TAG, "GEMINI_RESPONSE_CODE: " + response.code() + " | DURATION: " + duration + "ms");
            }
            return response;
        };

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(requestLoggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS) // Increased to 60s for SSE
                .writeTimeout(60, TimeUnit.SECONDS) // Increased to 60s
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://generativelanguage.googleapis.com/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.chatService = retrofit.create(GeminiChatService.class);
    }
    
    public void setContext(Context context) {
        this.applicationContext = context.getApplicationContext();
    }

    private boolean isNetworkAvailable() {
        if (applicationContext == null) return true;
        ConnectivityManager connectivityManager = (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    @Override
    public LiveData<List<ChatMessage>> getChatMessages(String sessionId, DocumentSnapshot startAfter) {
        MutableLiveData<List<ChatMessage>> messagesLiveData = new MutableLiveData<>();
        
        Query query = db.collection("users").document(currentUserId)
                .collection("chats").document(sessionId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(30);
                
        if (startAfter != null) {
            query = query.startAfter(startAfter);
        }

        query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed.", error);
                return;
            }
            if (value != null) {
                List<ChatMessage> messages = value.toObjects(ChatMessage.class);
                messagesLiveData.setValue(messages);
            }
        });

        return messagesLiveData;
    }

    @Override
    public void sendMessageToGemini(List<ChatMessage> history, ApiCallback callback) {
        if (!GeminiConfigValidator.isValidConfiguration(this.apiKey)) {
            callback.onError("Gemini API chưa được cấu hình đúng.");
            return;
        }

        if (!isNetworkAvailable()) {
            String prompt = history.isEmpty() ? "" : history.get(history.size() - 1).getText();
            callback.onSuccess(getSmartOfflineFallback(prompt), true);
            return;
        }

        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> systemPart = new HashMap<>();
        systemPart.put("text", SYSTEM_PROMPT);
        Map<String, Object> systemContent = new HashMap<>();
        systemContent.put("parts", new Object[]{systemPart});
        requestBody.put("system_instruction", systemContent);

        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatMessage msg : history) {
            if (ChatMessage.STATUS_ERROR.equals(msg.getStatus()) || msg.isOfflineFallback()) continue;
            
            String role = "user".equalsIgnoreCase(msg.getRole()) ? "user" : "model";
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", msg.getText());
            Map<String, Object> contentBlock = new HashMap<>();
            contentBlock.put("role", role);
            contentBlock.put("parts", new Object[]{textPart});
            contents.add(contentBlock);
        }
        requestBody.put("contents", contents);

        String originalPrompt = history.isEmpty() ? "" : history.get(history.size() - 1).getText();
        
        checkModelsAndSend(requestBody, originalPrompt, callback);
    }

    private void checkModelsAndSend(Map<String, Object> requestBody, String lastPrompt, ApiCallback callback) {
        if (validModelsCache != null) {
            executeNextValidModel(requestBody, lastPrompt, 0, callback);
            return;
        }

        if (isFetchingModels) {
            // Very rare race condition - simple fallback to delay
            mainHandler.postDelayed(() -> checkModelsAndSend(requestBody, lastPrompt, callback), 500);
            return;
        }

        isFetchingModels = true;
        Log.d(TAG, "Fetching available Gemini models (Lazy Load)...");
        
        chatService.getAvailableModels(apiKey).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                isFetchingModels = false;
                validModelsCache = new HashMap<>();
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
                                    validModelsCache.put(name, supportsStream);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse models. Proceeding with unverified fallback.", e);
                    }
                } else {
                    Log.e(TAG, "Failed to fetch models (" + response.code() + "). Proceeding with unverified fallback.");
                }
                executeNextValidModel(requestBody, lastPrompt, 0, callback);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                isFetchingModels = false;
                validModelsCache = new HashMap<>(); // Empty cache = proceed unverified
                Log.e(TAG, "Network failure fetching models. Proceeding with unverified fallback.");
                executeNextValidModel(requestBody, lastPrompt, 0, callback);
            }
        });
    }

    private void executeNextValidModel(Map<String, Object> requestBody, String lastPrompt, int retryCount, ApiCallback callback) {
        if (currentModelIndex >= PREFERRED_MODELS.length) {
            Log.e(TAG, "All preferred models exhausted.");
            callback.onSuccess(getSmartOfflineFallback(lastPrompt), true);
            return;
        }

        String model = PREFERRED_MODELS[currentModelIndex];
        
        // If cache exists and is not empty, skip models not in cache
        if (validModelsCache != null && !validModelsCache.isEmpty() && !validModelsCache.containsKey(model)) {
            Log.d(TAG, "CACHE MISS / UNSUPPORTED: Model " + model + " not supported by API. Skipping...");
            currentModelIndex++;
            executeNextValidModel(requestBody, lastPrompt, retryCount, callback);
            return;
        }

        // Determine streaming capability
        boolean supportsStream = validModelsCache == null || validModelsCache.isEmpty() || 
                (validModelsCache.containsKey(model) && validModelsCache.get(model));

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "--- EXECUTING REQUEST ---");
            Log.d(TAG, "Selected Model: " + model);
            Log.d(TAG, "Supports Stream: " + supportsStream);
            Log.d(TAG, "Cache Status: " + (validModelsCache == null || validModelsCache.isEmpty() ? "MISS/UNINITIALIZED" : "HIT"));
            Log.d(TAG, "Endpoint Used: " + (supportsStream ? "streamGenerateContent?alt=sse" : "generateContent"));
            Log.d(TAG, "Fallback Chain Index: " + currentModelIndex);
        }

        if (supportsStream) {
            currentCall = chatService.streamMessage(model, apiKey, requestBody);
        } else {
            currentCall = chatService.sendMessage(model, apiKey, requestBody);
        }

        currentCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (supportsStream) {
                        parseSSEStream(response.body().byteStream(), callback, requestBody, lastPrompt, retryCount, model);
                    } else {
                        parseStandardResponse(response, callback, requestBody, lastPrompt, retryCount);
                    }
                } else {
                    handleApiError(response, requestBody, lastPrompt, retryCount, callback, supportsStream, model);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (call.isCanceled()) return;
                Log.e(TAG, "NETWORK_FAILURE: " + t.getMessage(), t);
                handleRetry(requestBody, lastPrompt, retryCount, "Network failure", 2000, callback);
            }
        });
    }

    private void parseStandardResponse(Response<ResponseBody> response, ApiCallback callback, Map<String, Object> requestBody, String lastPrompt, int retryCount) {
        try {
            String jsonString = response.body().string();
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray candidates = jsonObject.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject content = firstCandidate.optJSONObject("content");
                if (content != null) {
                    JSONArray responseParts = content.optJSONArray("parts");
                    if (responseParts != null && responseParts.length() > 0) {
                        String aiReply = responseParts.getJSONObject(0).optString("text", "");
                        if (BuildConfig.DEBUG) Log.d(TAG, "STANDARD_RESPONSE: " + aiReply);
                        mainHandler.post(() -> callback.onSuccess(aiReply, false));
                        return;
                    }
                }
            }
            mainHandler.post(() -> handleRetry(requestBody, lastPrompt, retryCount, "Empty Standard Response", 2000, callback));
        } catch (Exception e) {
            Log.e(TAG, "JSON_PARSE_ERROR in standard response: " + e.getMessage());
            mainHandler.post(() -> handleRetry(requestBody, lastPrompt, retryCount, "Parse Error", 2000, callback));
        }
    }

    private void parseSSEStream(InputStream inputStream, ApiCallback callback, Map<String, Object> requestBody, String lastPrompt, int retryCount, String currentModelName) {
        new Thread(() -> {
            StringBuilder fullResponse = new StringBuilder();
            boolean streamFailed = false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (currentCall != null && currentCall.isCanceled()) break;
                    
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if (data.equals("[DONE]")) continue;
                        
                        try {
                            JSONObject jsonObject = new JSONObject(data);
                            JSONArray candidates = jsonObject.optJSONArray("candidates");
                            if (candidates != null && candidates.length() > 0) {
                                JSONObject firstCandidate = candidates.getJSONObject(0);
                                JSONObject content = firstCandidate.optJSONObject("content");
                                if (content != null) {
                                    JSONArray responseParts = content.optJSONArray("parts");
                                    if (responseParts != null && responseParts.length() > 0) {
                                        String chunk = responseParts.getJSONObject(0).optString("text", "");
                                        if (!chunk.isEmpty()) {
                                            fullResponse.append(chunk);
                                            final String currentText = fullResponse.toString();
                                            mainHandler.post(() -> callback.onPartialResponse(currentText));
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "JSON_PARSE_ERROR in stream chunk: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "STREAM_READ_ERROR: " + e.getMessage(), e);
                streamFailed = true;
            }

            if (currentCall != null && !currentCall.isCanceled()) {
                if (streamFailed || fullResponse.length() == 0) {
                    Log.w(TAG, "SSE Stream failed or empty. Falling back to generateContent...");
                    if (validModelsCache != null) validModelsCache.put(currentModelName, false); // Mark stream unsupported
                    mainHandler.post(() -> executeNextValidModel(requestBody, lastPrompt, retryCount, callback));
                } else {
                    final String finalOutput = fullResponse.toString();
                    mainHandler.post(() -> callback.onSuccess(finalOutput, false));
                }
            }
        }).start();
    }

    private void handleApiError(Response<ResponseBody> response, Map<String, Object> requestBody, String lastPrompt, int retryCount, ApiCallback callback, boolean wasStream, String modelName) {
        try {
            String err = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
            int code = response.code();
            
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "API_ERROR (" + code + "):\nBODY: " + err);
            }

            if (code == 404) {
                Log.e(TAG, "404 NOT_FOUND: Model " + modelName + " is invalid or missing. Switching immediately.");
                currentModelIndex++;
                executeNextValidModel(requestBody, lastPrompt, retryCount, callback);
                return;
            }

            if (code == 401 || code == 403) {
                Log.e(TAG, "CRITICAL AUTH ERROR: " + code + ". Do not retry.");
                callback.onError("Gemini API chưa được cấu hình đúng hoặc bị từ chối.");
                return;
            }
            
            if (code == 429) {
                if (err.contains("RESOURCE_EXHAUSTED") || err.contains("quota")) {
                    Log.e(TAG, "429 QUOTA EXHAUSTED for " + modelName + ". Switching model.");
                    currentModelIndex++;
                    executeNextValidModel(requestBody, lastPrompt, 0, callback);
                    return;
                }
                
                long delayMs = (long) Math.pow(2, retryCount) * 1000;
                handleRetry(requestBody, lastPrompt, retryCount, err, delayMs, callback);
            } else if (code >= 500) {
                handleRetry(requestBody, lastPrompt, retryCount, "Server Error", 2000, callback);
            } else {
                callback.onError("Lỗi không xác định (" + code + ").");
            }
        } catch (Exception e) {
            callback.onError("Lỗi xử lý phản hồi từ server.");
        }
    }

    private void handleRetry(Map<String, Object> requestBody, String lastPrompt, int retryCount, String reason, long delayMs, ApiCallback callback) {
        if (retryCount < MAX_RETRIES) {
            Log.w(TAG, "Retrying API request in " + delayMs + "ms... (Attempt " + (retryCount + 1) + ") due to: " + reason);
            mainHandler.postDelayed(() -> executeNextValidModel(requestBody, lastPrompt, retryCount + 1, callback), delayMs);
        } else {
            Log.e(TAG, "Max retries reached. Using smart fallback.");
            callback.onSuccess(getSmartOfflineFallback(lastPrompt), true);
        }
    }

    private String getSmartOfflineFallback(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        
        if (lowerPrompt.contains("biển") || lowerPrompt.contains("nha trang") || lowerPrompt.contains("phú quốc")) {
            return "AI đang tạm ngắt kết nối. Gợi ý offline: ViTour có các tour biển tuyệt đẹp như Nha Trang 4N3Đ và Phú Quốc 3N2Đ.";
        } else if (lowerPrompt.contains("núi") || lowerPrompt.contains("sapa") || lowerPrompt.contains("trekking")) {
            return "AI đang tạm ngắt kết nối. Gợi ý offline: Các tour leo núi cực chất như Sapa 2N1Đ hoặc Trekking Tà Năng đang có giá ưu đãi.";
        } else if (lowerPrompt.contains("ăn") || lowerPrompt.contains("food") || lowerPrompt.contains("món")) {
            return "AI đang tạm ngắt kết nối. Gợi ý offline: Nếu bạn yêu thích ẩm thực, tour Foodtour Hải Phòng hoặc Đà Nẵng là lựa chọn tuyệt vời!";
        } else if (lowerPrompt.contains("đà lạt")) {
            return "AI đang tạm ngắt kết nối. Gợi ý offline: Tour Đà Lạt mộng mơ 3N2Đ hiện đang là best-seller của ViTour.";
        } else {
            return "AI hiện đang quá tải hoặc model chưa khả dụng. Vui lòng thử lại sau.";
        }
    }

    @Override
    public void updateChatSession(ChatSession session) {
        db.collection("users").document(currentUserId)
                .collection("chats").document(session.getId())
                .set(session);
    }
    
    public void persistSingleMessage(String sessionId, ChatMessage message) {
        db.collection("users").document(currentUserId)
                .collection("chats").document(sessionId)
                .collection("messages").document(message.getId())
                .set(message);
    }

    @Override
    public void cancelStream() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }
        mainHandler.removeCallbacksAndMessages(null);
    }
}
