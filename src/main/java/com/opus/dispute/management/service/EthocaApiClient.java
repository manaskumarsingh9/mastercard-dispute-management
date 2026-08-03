package com.opus.dispute.management.service;

import com.opus.dispute.management.config.EthocaProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Slf4j
@Service
public class EthocaApiClient {

    private final OkHttpClient httpClient;
    private final EthocaProperties ethocaProperties;
    private final boolean initialized;

    public EthocaApiClient(EthocaProperties ethocaProperties) {
        this.ethocaProperties = ethocaProperties;
        this.httpClient = new OkHttpClient.Builder().build();
        this.initialized = ethocaProperties.getApiKeyId() != null
                && !ethocaProperties.getApiKeyId().isEmpty()
                && ethocaProperties.getApiSecret() != null
                && !ethocaProperties.getApiSecret().isEmpty();
        if (initialized) {
            log.info("Ethoca API Client initialized successfully");
        } else {
            log.warn("Ethoca API Client not configured. Set ethoca.api-key-id and ethoca.api-secret to enable Ethoca features.");
        }
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new RuntimeException("Ethoca API Client is not configured. Set ethoca.api-key-id and ethoca.api-secret.");
        }
    }

    private String generateHmacSignature(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                ethocaProperties.getApiSecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA1"
        );
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
    }

    private String buildAuthorizationHeader(String requestBody) throws Exception {
        String dataToSign = (requestBody != null && !requestBody.isEmpty()) ? requestBody : "";
        String signature = generateHmacSignature(dataToSign);
        return "ETHOCA-SHA1 KeyRef=" + ethocaProperties.getApiKeyId() + ",Signature=" + signature;
    }

    private String getEthDateHeader() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
    }

    public String get(String endpoint) throws Exception {
        checkInitialized();
        String url = ethocaProperties.getBaseUrl() + endpoint;
        String authHeader = buildAuthorizationHeader("");
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", authHeader)
                .addHeader("X-Eth-Date", getEthDateHeader())
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("Ethoca GET failed: {} {} - {}", response.code(), response.message(), responseBody);
                throw new RuntimeException("Ethoca API request failed: " + response.code() + " " + response.message() + " - " + responseBody);
            }
            return responseBody;
        }
    }

    public String post(String endpoint, String jsonBody) throws Exception {
        checkInitialized();
        String url = ethocaProperties.getBaseUrl() + endpoint;
        String authHeader = buildAuthorizationHeader(jsonBody);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", authHeader)
                .addHeader("X-Eth-Date", getEthDateHeader())
                .addHeader("Content-Type", "application/json")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("Ethoca POST failed: {} {} - {}", response.code(), response.message(), responseBody);
                throw new RuntimeException("Ethoca API request failed: " + response.code() + " " + response.message() + " - " + responseBody);
            }
            return responseBody;
        }
    }

    public String put(String endpoint, String jsonBody) throws Exception {
        checkInitialized();
        String url = ethocaProperties.getBaseUrl() + endpoint;
        String authHeader = buildAuthorizationHeader(jsonBody);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .addHeader("Authorization", authHeader)
                .addHeader("X-Eth-Date", getEthDateHeader())
                .addHeader("Content-Type", "application/json")
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("Ethoca PUT failed: {} {} - {}", response.code(), response.message(), responseBody);
                throw new RuntimeException("Ethoca API request failed: " + response.code() + " " + response.message() + " - " + responseBody);
            }
            return responseBody;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
