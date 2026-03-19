package com.opus.dispute.management.service;

import com.mastercard.developer.interceptors.OkHttpOAuth1Interceptor;
import com.mastercard.developer.utils.AuthenticationUtils;
import com.opus.dispute.management.config.MastercardProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

@Slf4j
@Service
public class MastercardApiClient {

    private OkHttpClient httpClient;
    private final MastercardProperties mastercardProperties;
    private PrivateKey signingKey;
    private boolean initialized = false;

    public MastercardApiClient(MastercardProperties mastercardProperties) {
        this.mastercardProperties = mastercardProperties;
        try {
            String keystorePath = mastercardProperties.getKeystorePath();
            if (keystorePath != null) {
                keystorePath = keystorePath.replace("\\", "/");
            }
            File keystoreFile = new File(keystorePath != null ? keystorePath : "");
            if (keystoreFile.exists()) {
                this.signingKey = AuthenticationUtils.loadSigningKey(
                        keystorePath,
                        "opus-dispute-sandbox-key",
                        mastercardProperties.getKeystorePassword()
                );
                this.httpClient = createOkHttpClient();
                this.initialized = true;
                log.info("Mastercard API Client initialized successfully");
            } else {
                log.warn("Mastercard keystore file not found at: {}. Mastercard API features will be unavailable.", keystorePath);
                this.httpClient = new OkHttpClient.Builder().build();
            }
        } catch (Exception e) {
            log.warn("Failed to initialize Mastercard API Client: {}. Mastercard API features will be unavailable.", e.getMessage());
            this.httpClient = new OkHttpClient.Builder().build();
        }
    }

    private OkHttpClient createOkHttpClient() throws Exception {
        OkHttpOAuth1Interceptor oauth1Interceptor = new OkHttpOAuth1Interceptor(
                mastercardProperties.getConsumerKey(),
                signingKey
        );
        return new OkHttpClient.Builder()
                .addInterceptor(oauth1Interceptor)
                .build();
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new RuntimeException("Mastercard API Client is not initialized. Keystore file is missing.");
        }
    }

    public String get(String endpoint) throws Exception {
        checkInitialized();
        String url = mastercardProperties.getBaseUrl() + endpoint;
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                log.error("GET request failed: {} {} - {}", response.code(), response.message(), errorBody);
                throw new RuntimeException("API request failed: " + response.code() + " " + response.message() + " - " + errorBody);
            }
            return response.body().string();
        }
    }

    public String post(String endpoint, String jsonBody) throws Exception {
        checkInitialized();
        String url = mastercardProperties.getBaseUrl() + endpoint;
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder().url(url).post(body).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                log.error("POST request failed: {} {} - {}", response.code(), response.message(), errorBody);
                throw new RuntimeException("API request failed: " + response.code() + " " + response.message() + " - " + errorBody);
            }
            return response.body().string();
        }
    }

    public String put(String endpoint, String jsonBody) throws Exception {
        checkInitialized();
        String url = mastercardProperties.getBaseUrl() + endpoint;
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder().url(url).put(body).build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                log.error("PUT request failed: {} {} - {}", response.code(), response.message(), errorBody);
                throw new RuntimeException("API request failed: " + response.code() + " " + response.message() + " - " + errorBody);
            }
            return response.body().string();
        }
    }

    public String delete(String endpoint) throws Exception {
        checkInitialized();
        String url = mastercardProperties.getBaseUrl() + endpoint;
        Request request = new Request.Builder().url(url).delete().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                log.error("DELETE request failed: {} {} - {}", response.code(), response.message(), errorBody);
                throw new RuntimeException("API request failed: " + response.code() + " " + response.message() + " - " + errorBody);
            }
            return response.body().string();
        }
    }
}
