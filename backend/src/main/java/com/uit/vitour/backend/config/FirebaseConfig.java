package com.uit.vitour.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            System.out.println("Starting Firebase initialization...");

            if (FirebaseApp.getApps().isEmpty()) {
                System.out.println("Loading firebase-service-account.json");
                
                InputStream serviceAccount =
                    new ClassPathResource("firebase-service-account.json")
                        .getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

                FirebaseApp.initializeApp(options);

                System.out.println("Firebase initialized successfully");
            }

        } catch (Exception e) {
            System.err.println("FIREBASE INIT FAILED");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
