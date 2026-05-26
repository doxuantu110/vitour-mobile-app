package com.uit.vitour.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Formatter;

@Service
@Slf4j
public class SignatureService {

    public String generateHmacSha256(String rawSignature, String secretKey) {
        try {
            log.info("rawSignature (to be signed): {}", rawSignature);
            
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] hash = sha256_HMAC.doFinal(rawSignature.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = bytesToHex(hash);
            
            log.info("generatedSignature: {}", generatedSignature);
            return generatedSignature;
            
        } catch (Exception e) {
            log.error("Failed to generate HMAC SHA256 signature", e);
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    public boolean verifyIpnSignature(String rawSignature, String secretKey, String providedSignature) {
        String expectedSignature = generateHmacSha256(rawSignature, secretKey);
        boolean isValid = expectedSignature.equals(providedSignature);
        
        if (!isValid) {
            log.error("Signature VERIFICATION FAILED!");
            log.error("Provided Signature: {}", providedSignature);
            log.error("Expected Signature: {}", expectedSignature);
            log.error("Check field order or missing extraData in rawSignature.");
        } else {
            log.info("Signature VERIFICATION SUCCESS.");
        }
        
        return isValid;
    }
}
