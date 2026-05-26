package com.uit.vitour.backend.service;

import com.uit.vitour.backend.dto.CreatePaymentRequest;
import com.uit.vitour.backend.dto.CreatePaymentResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class MomoService {

    @Value("${momo.endpoint}")
    private String endpoint;

    @Value("${momo.partnerCode}")
    private String partnerCode;

    @Value("${momo.accessKey}")
    private String accessKey;

    @Value("${momo.secretKey}")
    private String secretKey;

    @Value("${momo.redirectUrl}")
    private String redirectUrl;

    @Value("${momo.ipnUrl}")
    private String ipnUrl;

    @Value("${momo.requestType}")
    private String requestType;

    @Autowired
    private SignatureService signatureService;

    @PostConstruct
    public void init() {
        if ("YOUR_ACCESS_KEY".equals(accessKey) || "YOUR_SECRET_KEY".equals(secretKey) || accessKey.isEmpty() || secretKey.isEmpty()) {
            System.err.println("INVALID MOMO SANDBOX CONFIG: Please update application.yml with real credentials.");
        }
    }

    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        RestTemplate restTemplate = new RestTemplate();

        String orderId = request.getBookingId() + "_" + System.currentTimeMillis();
        String requestId = orderId;
        String extraData = ""; // Must not be null, use empty string
        String orderInfo = request.getOrderInfo() != null ? request.getOrderInfo() : "Payment for booking " + request.getBookingId();

        // 1. Construct raw signature string in the EXACT order required by MoMo v2/v3
        String rawSignature = "accessKey=" + accessKey
                + "&amount=" + request.getAmount()
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;

        // 2. Generate HMAC SHA256 Signature
        String signature = signatureService.generateHmacSha256(rawSignature, secretKey);

        // 3. Detailed logging BEFORE sending request
        log.info("[MOMO_REQUEST] \npartnerCode={}\nrequestId={}\norderId={}\namount={}\nendpoint={}\nrawSignature={}\nsignature={}",
                partnerCode, requestId, orderId, request.getAmount(), endpoint, rawSignature, signature);

        // 4. Prepare JSON Request Body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("partnerCode", partnerCode);
        requestBody.put("partnerName", "ViTour");
        requestBody.put("storeId", "ViTourStore");
        requestBody.put("requestId", requestId);
        requestBody.put("amount", request.getAmount());
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", redirectUrl);
        requestBody.put("ipnUrl", ipnUrl);
        requestBody.put("lang", "vi");
        requestBody.put("extraData", extraData);
        requestBody.put("requestType", requestType);
        requestBody.put("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, entity, Map.class);
            Map<String, Object> body = response.getBody();
            
            // 5. Log FULL MoMo response body
            log.info("[MOMO_RESPONSE] \n{}", body);

            int resultCode = body != null && body.get("resultCode") != null ? (Integer) body.get("resultCode") : -1;
            String message = body != null && body.get("message") != null ? (String) body.get("message") : "Unknown Error";

            if (body != null && body.containsKey("payUrl") && resultCode == 0) {
                return CreatePaymentResponse.builder()
                        .success(true)
                        .payUrl((String) body.get("payUrl"))
                        .deeplink((String) body.get("deeplink"))
                        .qrCodeUrl((String) body.get("qrCodeUrl"))
                        .resultCode(resultCode)
                        .message(message)
                        .build();
            } else {
                return CreatePaymentResponse.builder()
                        .success(false)
                        .resultCode(resultCode)
                        .message(message)
                        .build();
            }

        } catch (Exception e) {
            log.error("[MOMO_ERROR] Error calling MoMo API", e);
            return CreatePaymentResponse.builder()
                    .success(false)
                    .resultCode(-99)
                    .message("Internal server error during MoMo request: " + e.getMessage())
                    .build();
        }
    }
}
