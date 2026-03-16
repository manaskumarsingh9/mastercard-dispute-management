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

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

/**
 * Mastercard API Client for making authenticated requests to Mastercard endpoints.
 * Uses OAuth 1.0a signing for request authentication.
 */
@Slf4j
@Service
public class MastercardApiClient {

    private final OkHttpClient httpClient;
    private final MastercardProperties mastercardProperties;
    private final PrivateKey signingKey;

    public MastercardApiClient(MastercardProperties mastercardProperties) throws Exception {
        this.mastercardProperties = mastercardProperties;
        this.signingKey = AuthenticationUtils.loadSigningKey(
                mastercardProperties.getKeystorePath(),
                "opus-dispute-sandbox-key", // Corrected key alias
                mastercardProperties.getKeystorePassword()
        );
        this.httpClient = createOkHttpClient();
    }

    /**
     * Creates an OkHttpClient configured with OAuth 1.0a interceptor for Mastercard API
     */
    private OkHttpClient createOkHttpClient() throws Exception {
        // Create OAuth1 interceptor with consumer key and private key
        OkHttpOAuth1Interceptor oauth1Interceptor = new OkHttpOAuth1Interceptor(
                mastercardProperties.getConsumerKey(),
                signingKey
        );

        // Create and configure OkHttpClient
        return new OkHttpClient.Builder()
                .addInterceptor(oauth1Interceptor)
                .build();
    }

    /**
     * Perform a GET request to Mastercard API
     * @param endpoint API endpoint path (e.g., "/fraud/v2/account-risk")
     * @return Response body as string
     */
    public String get(String endpoint) throws Exception {
        String url = mastercardProperties.getBaseUrl() + endpoint;
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("GET request failed: {} {}", response.code(), response.message());
                throw new RuntimeException("API request failed: " + response.code() + " " + response.message());
            }
            return response.body().string();
        }
    }

    /**
     * Perform a POST request to Mastercard API
     * @param endpoint API endpoint path
     * @param jsonBody Request body as JSON string
     * @return Response body as string
     */
    public String post(String endpoint, String jsonBody) throws Exception {
        String url = mastercardProperties.getBaseUrl() + endpoint;
        
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                log.error("POST request failed: {} {} - {}", response.code(), response.message(), errorBody);
                throw new RuntimeException("API request failed: " + response.code() + " " + response.message());
            }
            return response.body().string();
        }
    }

    /**
     * Perform a PUT request to Mastercard API
     * @param endpoint API endpoint path
     * @param jsonBody Request body as JSON string
     * @return Response body as string
     */
    public String put(String endpoint, String jsonBody) throws Exception {
        String url = mastercardProperties.getBaseUrl() + endpoint;
        
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("PUT request failed: {} {}", response.code(), response.message());
                throw new RuntimeException("API request failed: " + response.code() + " " + response.message());
            }
            return response.body().string();
        }
    }

    /**
     * Perform a DELETE request to Mastercard API
     * @param endpoint API endpoint path
     * @return Response body as string
     */
    public String delete(String endpoint) throws Exception {
        String url = mastercardProperties.getBaseUrl() + endpoint;
        
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("DELETE request failed: {} {}", response.code(), response.message());
                throw new RuntimeException("API request failed: " + response.code() + " " + response.message());
            }
            return response.body().string();
        }
    }
}
