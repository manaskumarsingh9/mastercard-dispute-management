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
public class OpenAiService {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final String apiKey;
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();

    public OpenAiService(@Value("${openai.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API key not configured. OpenAI fallback will be unavailable.");
        } else {
            log.info("OpenAI service initialized successfully");
        }
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    public String generateContent(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            throw new RuntimeException("OpenAI API key not configured");
        }
        JsonObject requestBody = buildChatRequest(systemPrompt, userPrompt, null, null, false);
        return execute(requestBody, "generateContent");
    }

    public String generateJson(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            throw new RuntimeException("OpenAI API key not configured");
        }
        JsonObject requestBody = buildChatRequest(systemPrompt, userPrompt, null, null, true);
        return execute(requestBody, "generateJson");
    }

    public String generateContentWithHistory(String systemPrompt, String userPrompt,
                                              List<GeminiService.ConversationTurn> history) {
        if (!isAvailable()) {
            throw new RuntimeException("OpenAI API key not configured");
        }
        JsonObject requestBody = buildChatRequest(systemPrompt, userPrompt, history, null, false);
        return execute(requestBody, "generateContentWithHistory");
    }

    public String generateJsonWithHistory(String systemPrompt, String userPrompt,
                                           List<GeminiService.ConversationTurn> history) {
        return generateJsonWithHistoryAndMedia(systemPrompt, userPrompt, history, null);
    }

    public String generateJsonWithHistoryAndMedia(String systemPrompt, String userPrompt,
                                                   List<GeminiService.ConversationTurn> history,
                                                   List<MediaFile> mediaFiles) {
        if (!isAvailable()) {
            throw new RuntimeException("OpenAI API key not configured");
        }
        JsonObject requestBody = buildChatRequest(systemPrompt, userPrompt, history, mediaFiles, true);
        return execute(requestBody, "generateJsonWithHistoryAndMedia");
    }

    public String generateMultimodalContent(String systemPrompt, String userPrompt, List<MediaFile> mediaFiles) {
        if (!isAvailable()) {
            throw new RuntimeException("OpenAI API key not configured");
        }
        JsonObject requestBody = buildChatRequest(systemPrompt, userPrompt, null, mediaFiles, false);
        return execute(requestBody, "generateMultimodalContent");
    }

    public String generateMultimodalJson(String systemPrompt, String userPrompt, List<MediaFile> mediaFiles) {
        if (!isAvailable()) {
            throw new RuntimeException("OpenAI API key not configured");
        }
        JsonObject requestBody = buildChatRequest(systemPrompt, userPrompt, null, mediaFiles, true);
        return execute(requestBody, "generateMultimodalJson");
    }

    private JsonObject buildChatRequest(String systemPrompt, String userPrompt,
                                         List<GeminiService.ConversationTurn> history,
                                         List<MediaFile> mediaFiles,
                                         boolean jsonMode) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL);

        JsonArray messages = new JsonArray();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", systemPrompt);
            messages.add(systemMessage);
        }

        if (history != null) {
            for (GeminiService.ConversationTurn turn : history) {
                JsonObject turnMessage = new JsonObject();
                turnMessage.addProperty("role", mapRole(turn.role()));
                turnMessage.addProperty("content", turn.content());
                messages.add(turnMessage);
            }
        }

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");

        if (mediaFiles == null || mediaFiles.isEmpty()) {
            userMessage.addProperty("content", userPrompt);
        } else {
            JsonArray contentParts = new JsonArray();
            JsonObject textPart = new JsonObject();
            textPart.addProperty("type", "text");
            textPart.addProperty("text", userPrompt);
            contentParts.add(textPart);

            for (MediaFile mf : mediaFiles) {
                contentParts.add(buildMediaPart(mf));
            }
            userMessage.add("content", contentParts);
        }
        messages.add(userMessage);

        requestBody.add("messages", messages);

        if (jsonMode) {
            JsonObject responseFormat = new JsonObject();
            responseFormat.addProperty("type", "json_object");
            requestBody.add("response_format", responseFormat);
        }

        return requestBody;
    }

    private JsonObject buildMediaPart(MediaFile mf) {
        if (mf.isPdf()) {
            JsonObject filePart = new JsonObject();
            filePart.addProperty("type", "file");
            JsonObject file = new JsonObject();
            file.addProperty("filename", mf.name());
            file.addProperty("file_data", "data:" + mf.mimeType() + ";base64," + mf.base64Data());
            filePart.add("file", file);
            return filePart;
        }
        JsonObject imagePart = new JsonObject();
        imagePart.addProperty("type", "image_url");
        JsonObject imageUrl = new JsonObject();
        imageUrl.addProperty("url", "data:" + mf.mimeType() + ";base64," + mf.base64Data());
        imagePart.add("image_url", imageUrl);
        return imagePart;
    }

    private String mapRole(String geminiRole) {
        return "model".equals(geminiRole) ? "assistant" : "user";
    }

    private String execute(JsonObject requestBody, String caller) {
        String body = gson.toJson(requestBody);

        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(body, JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (response.isSuccessful()) {
                return extractTextFromResponse(responseBody);
            }

            log.error("OpenAI API request failed [{}]: {} {} - {}", caller, response.code(), response.message(), responseBody);
            throw new RuntimeException("OpenAI API error: " + response.code() + " - " + responseBody);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            log.error("Failed to call OpenAI API [{}]", caller, e);
            throw new RuntimeException("OpenAI API call failed: " + e.getMessage(), e);
        }
    }

    private String extractTextFromResponse(String responseBody) {
        JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();

        if (response.has("choices")) {
            JsonArray choices = response.getAsJsonArray("choices");
            if (choices.size() > 0) {
                JsonObject choice = choices.get(0).getAsJsonObject();
                if (choice.has("message")) {
                    JsonObject message = choice.getAsJsonObject("message");
                    if (message.has("content") && !message.get("content").isJsonNull()) {
                        return message.get("content").getAsString();
                    }
                }
            }
        }

        throw new RuntimeException("Unable to extract text from OpenAI response: " + responseBody);
    }
}
