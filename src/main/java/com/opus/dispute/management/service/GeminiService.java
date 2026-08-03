package com.opus.dispute.management.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GeminiService {

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final String apiKey;
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();

    public GeminiService(@Value("${gemini.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key not configured. AI agent features will be unavailable.");
        } else {
            log.info("Gemini service initialized successfully");
        }
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 4000;

    public String generateContent(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            throw new RuntimeException("Gemini API key not configured");
        }

        JsonObject requestBody = buildRequest(systemPrompt, userPrompt);
        return executeWithRetry(requestBody, "generateContent");
    }

    public String generateJson(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            throw new RuntimeException("Gemini API key not configured");
        }

        JsonObject requestBody = buildRequest(systemPrompt, userPrompt);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("responseMimeType", "application/json");
        requestBody.add("generationConfig", generationConfig);

        return executeWithRetry(requestBody, "generateJson");
    }

    public String generateContentWithHistory(String systemPrompt, String userPrompt, List<ConversationTurn> history) {
        if (!isAvailable()) {
            throw new RuntimeException("Gemini API key not configured");
        }

        JsonObject requestBody = buildRequestWithHistory(systemPrompt, userPrompt, history);
        return executeWithRetry(requestBody, "generateContentWithHistory");
    }

    public String generateJsonWithHistory(String systemPrompt, String userPrompt, List<ConversationTurn> history) {
        return generateJsonWithHistoryAndMedia(systemPrompt, userPrompt, history, null);
    }

    public String generateJsonWithHistoryAndMedia(String systemPrompt, String userPrompt,
                                                   List<ConversationTurn> history, List<MediaFile> mediaFiles) {
        if (!isAvailable()) {
            throw new RuntimeException("Gemini API key not configured");
        }

        mediaFiles = filterMediaFiles(mediaFiles);

        JsonObject requestBody = buildRequestWithHistory(systemPrompt, userPrompt, history, mediaFiles);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("responseMimeType", "application/json");
        requestBody.add("generationConfig", generationConfig);

        return executeWithRetry(requestBody, "generateJsonWithHistoryAndMedia");
    }

    public record ConversationTurn(String role, String content) {}

    private static final int MAX_MEDIA_FILES = 10;
    private static final long MAX_MEDIA_FILE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_TOTAL_MEDIA_SIZE = 30 * 1024 * 1024;

    private List<MediaFile> filterMediaFiles(List<MediaFile> mediaFiles) {
        if (mediaFiles == null || mediaFiles.isEmpty()) return List.of();
        List<MediaFile> filtered = new java.util.ArrayList<>();
        long totalSize = 0;
        for (MediaFile mf : mediaFiles) {
            if (filtered.size() >= MAX_MEDIA_FILES) {
                log.warn("Skipping remaining media files — max file count ({}) reached", MAX_MEDIA_FILES);
                break;
            }
            if (mf.data().length > MAX_MEDIA_FILE_SIZE) {
                log.warn("Skipping oversized media file: {} ({} bytes > {} limit)", mf.name(), mf.data().length, MAX_MEDIA_FILE_SIZE);
                continue;
            }
            if (totalSize + mf.data().length > MAX_TOTAL_MEDIA_SIZE) {
                log.warn("Skipping media file {} — total payload would exceed {} bytes", mf.name(), MAX_TOTAL_MEDIA_SIZE);
                continue;
            }
            totalSize += mf.data().length;
            filtered.add(mf);
        }
        return filtered;
    }

    public String generateMultimodalContent(String systemPrompt, String userPrompt, List<MediaFile> mediaFiles) {
        mediaFiles = filterMediaFiles(mediaFiles);
        if (mediaFiles.isEmpty()) {
            return generateContent(systemPrompt, userPrompt);
        }
        if (!isAvailable()) {
            throw new RuntimeException("Gemini API key not configured");
        }

        JsonObject requestBody = buildMultimodalRequest(systemPrompt, userPrompt, mediaFiles);
        return executeWithRetry(requestBody, "generateMultimodalContent");
    }

    public String generateMultimodalJson(String systemPrompt, String userPrompt, List<MediaFile> mediaFiles) {
        mediaFiles = filterMediaFiles(mediaFiles);
        if (mediaFiles.isEmpty()) {
            return generateJson(systemPrompt, userPrompt);
        }
        if (!isAvailable()) {
            throw new RuntimeException("Gemini API key not configured");
        }

        JsonObject requestBody = buildMultimodalRequest(systemPrompt, userPrompt, mediaFiles);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("responseMimeType", "application/json");
        requestBody.add("generationConfig", generationConfig);

        return executeWithRetry(requestBody, "generateMultimodalJson");
    }

    private String executeWithRetry(JsonObject requestBody, String caller) {
        String url = GEMINI_API_URL + "?key=" + apiKey;
        String body = gson.toJson(requestBody);

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(body, JSON))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        return extractTextFromResponse(responseBody);
                    }

                    if ((response.code() == 429 || response.code() == 503) && attempt < MAX_RETRIES) {
                        long delay = extractRetryDelay(responseBody, attempt);
                        log.warn("Gemini API {} ({}) on {} — retrying in {}ms (attempt {}/{})",
                                response.code() == 429 ? "rate limited" : "unavailable",
                                response.code(), caller, delay, attempt + 1, MAX_RETRIES);
                        Thread.sleep(delay);
                        continue;
                    }

                    log.error("Gemini API request failed [{}]: {} {} - {}", caller, response.code(), response.message(), responseBody);
                    throw new RuntimeException("Gemini API error: " + response.code() + " - " + responseBody);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Gemini retry interrupted", ie);
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                log.error("Failed to call Gemini API [{}]", caller, e);
                throw new RuntimeException("Gemini API call failed: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("Gemini API call failed after " + MAX_RETRIES + " retries due to rate limiting");
    }

    private long extractRetryDelay(String responseBody, int attempt) {
        try {
            JsonObject error = JsonParser.parseString(responseBody).getAsJsonObject();
            if (error.has("error") && error.getAsJsonObject("error").has("details")) {
                JsonArray details = error.getAsJsonObject("error").getAsJsonArray("details");
                for (int i = 0; i < details.size(); i++) {
                    JsonObject detail = details.get(i).getAsJsonObject();
                    if (detail.has("retryDelay")) {
                        String delayStr = detail.get("retryDelay").getAsString();
                        if (delayStr.endsWith("s")) {
                            double seconds = Double.parseDouble(delayStr.replace("s", ""));
                            return (long) (seconds * 1000) + 500;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not parse retry delay from response, using default backoff", e);
        }
        return INITIAL_RETRY_DELAY_MS * (attempt + 1);
    }

    private JsonObject buildMultimodalRequest(String systemPrompt, String userPrompt, List<MediaFile> mediaFiles) {
        JsonObject requestBody = new JsonObject();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemInstruction = new JsonObject();
            JsonObject systemPart = new JsonObject();
            systemPart.addProperty("text", systemPrompt);
            JsonArray systemParts = new JsonArray();
            systemParts.add(systemPart);
            systemInstruction.add("parts", systemParts);
            requestBody.add("systemInstruction", systemInstruction);
        }

        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        JsonArray parts = new JsonArray();

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", userPrompt);
        parts.add(textPart);

        for (MediaFile mf : mediaFiles) {
            JsonObject mediaPart = new JsonObject();
            JsonObject inlineData = new JsonObject();
            inlineData.addProperty("mimeType", mf.mimeType());
            inlineData.addProperty("data", mf.base64Data());
            mediaPart.add("inlineData", inlineData);
            parts.add(mediaPart);

            JsonObject labelPart = new JsonObject();
            labelPart.addProperty("text", "[Attached file: " + mf.name() + " (" + mf.mimeType() + ")]");
            parts.add(labelPart);

            log.debug("Added media part: {} ({}, {} bytes)", mf.name(), mf.mimeType(), mf.data().length);
        }

        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        return requestBody;
    }

    private static final int MAX_TOTAL_CHARS = 800_000;

    private int charLen(String text) {
        return text == null ? 0 : text.length();
    }

    private List<ConversationTurn> trimHistoryToFit(String systemPrompt, String userPrompt, List<ConversationTurn> history) {
        if (history == null || history.isEmpty()) return List.of();

        int fixedChars = charLen(systemPrompt) + charLen(userPrompt);
        int historyBudget = MAX_TOTAL_CHARS - fixedChars;

        log.info("Trim check: fixedChars={} (system={}, user={}), historyBudget={}, historyTurns={}",
                fixedChars, charLen(systemPrompt), charLen(userPrompt), historyBudget, history.size());

        if (historyBudget <= 0) {
            log.warn("System prompt ({}) + user prompt ({}) = {} chars exceeds {} char budget; dropping ALL {} history turns",
                    charLen(systemPrompt), charLen(userPrompt), fixedChars, MAX_TOTAL_CHARS, history.size());
            return List.of();
        }

        java.util.LinkedList<ConversationTurn> kept = new java.util.LinkedList<>();
        int usedChars = 0;

        for (int i = history.size() - 1; i >= 0; i--) {
            ConversationTurn turn = history.get(i);
            int turnChars = charLen(turn.content());
            if (usedChars + turnChars > historyBudget) {
                log.info("Dropping {} oldest history turns (turn {} has {} chars, would exceed budget {}/{})",
                        i + 1, i, turnChars, usedChars + turnChars, historyBudget);
                break;
            }
            usedChars += turnChars;
            kept.addFirst(turn);
        }

        log.info("History trimmed from {} to {} turns ({} chars used of {} budget). Total payload ~{} chars",
                history.size(), kept.size(), usedChars, historyBudget, fixedChars + usedChars);

        return kept;
    }

    private JsonObject buildRequestWithHistory(String systemPrompt, String userPrompt, List<ConversationTurn> history) {
        return buildRequestWithHistory(systemPrompt, userPrompt, history, null);
    }

    private JsonObject buildRequestWithHistory(String systemPrompt, String userPrompt,
                                                List<ConversationTurn> history, List<MediaFile> mediaFiles) {
        JsonObject requestBody = new JsonObject();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemInstruction = new JsonObject();
            JsonObject systemPart = new JsonObject();
            systemPart.addProperty("text", systemPrompt);
            JsonArray systemParts = new JsonArray();
            systemParts.add(systemPart);
            systemInstruction.add("parts", systemParts);
            requestBody.add("systemInstruction", systemInstruction);
        }

        List<ConversationTurn> trimmedHistory = trimHistoryToFit(systemPrompt, userPrompt, history);

        JsonArray contents = new JsonArray();

        for (ConversationTurn turn : trimmedHistory) {
            JsonObject turnContent = new JsonObject();
            turnContent.addProperty("role", turn.role());
            JsonArray turnParts = new JsonArray();
            JsonObject turnPart = new JsonObject();
            turnPart.addProperty("text", turn.content());
            turnParts.add(turnPart);
            turnContent.add("parts", turnParts);
            contents.add(turnContent);
        }

        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", userPrompt);
        parts.add(part);

        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            for (MediaFile mf : mediaFiles) {
                JsonObject mediaPart = new JsonObject();
                JsonObject inlineData = new JsonObject();
                inlineData.addProperty("mimeType", mf.mimeType());
                inlineData.addProperty("data", mf.base64Data());
                mediaPart.add("inlineData", inlineData);
                parts.add(mediaPart);

                JsonObject labelPart = new JsonObject();
                labelPart.addProperty("text", "[Attached file: " + mf.name() + " (" + mf.mimeType() + ")]");
                parts.add(labelPart);

                log.debug("Added media part to history request: {} ({}, {} bytes)", mf.name(), mf.mimeType(), mf.data().length);
            }
            log.info("Included {} media file(s) in history+media request", mediaFiles.size());
        }

        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        return requestBody;
    }

    private JsonObject buildRequest(String systemPrompt, String userPrompt) {
        JsonObject requestBody = new JsonObject();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemInstruction = new JsonObject();
            JsonObject systemPart = new JsonObject();
            systemPart.addProperty("text", systemPrompt);
            JsonArray systemParts = new JsonArray();
            systemParts.add(systemPart);
            systemInstruction.add("parts", systemParts);
            requestBody.add("systemInstruction", systemInstruction);
        }

        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", userPrompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        return requestBody;
    }

    private String extractTextFromResponse(String responseBody) {
        JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();

        if (response.has("candidates")) {
            JsonArray candidates = response.getAsJsonArray("candidates");
            if (candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content.has("parts")) {
                        JsonArray parts = content.getAsJsonArray("parts");
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < parts.size(); i++) {
                            JsonObject part = parts.get(i).getAsJsonObject();
                            if (part.has("text")) {
                                sb.append(part.get("text").getAsString());
                            }
                        }
                        if (!sb.isEmpty()) {
                            return sb.toString();
                        }
                    }
                }
            }
        }

        throw new RuntimeException("Unable to extract text from Gemini response: " + responseBody);
    }
}
