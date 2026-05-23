package com.uit.vitour;

import android.app.Application;
import android.util.Log;

import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

public class ViTourApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            Map<String, String> config = new HashMap<>();
            // Only configure cloud_name for unsigned uploads
            config.put("cloud_name", "dnkejogbh"); 
            MediaManager.init(this, config);
            Log.d("Cloudinary", "Cloudinary MediaManager initialized successfully.");
        } catch (Exception e) {
            Log.e("Cloudinary", "Failed to initialize Cloudinary MediaManager: " + e.getMessage());
        }
    }
}
