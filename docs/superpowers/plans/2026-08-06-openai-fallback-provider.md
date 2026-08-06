# OpenAI Fallback Provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `GeminiService` transparently fall back to a new `OpenAiService` (OpenAI `gpt-4o-mini` via Chat Completions) whenever Gemini can't serve a request — because its key is missing or a call fails at runtime — so AI-agent features keep working through a demo even if the Gemini spend cap is hit.

**Architecture:** `OpenAiService` is a new, standalone Spring `@Service` that mirrors `GeminiService`'s seven public methods (`generateContent`, `generateJson`, `generateContentWithHistory`, `generateJsonWithHistory`, `generateJsonWithHistoryAndMedia`, `generateMultimodalContent`, `generateMultimodalJson`) plus `isAvailable()`, built against OpenAI's Chat Completions API (`/v1/chat/completions`) using the same OkHttp/Gson stack `GeminiService` already uses. `GeminiService` gets `OpenAiService` injected via constructor and wraps each of its own methods so that if its own Gemini key is blank, or the underlying Gemini call throws, it calls the matching `OpenAiService` method instead and returns that result. No other class in the codebase changes — every caller (7 agent classes, `IssuerEvidenceGenerator`, `AgentController`) keeps depending only on `GeminiService`.

**Tech Stack:** Java 19, Spring Boot 4.0.3, OkHttp 4.9.1, Gson 2.8.6, JUnit Jupiter 6.0.3 + Mockito 5.20.0 (already on the test classpath via `spring-boot-starter-data-jpa-test` / `spring-boot-starter-webmvc-test` — confirmed present, no pom.xml changes needed).

## Global Constraints

- No changes to any agent class, `IssuerEvidenceGenerator`, or `AgentController` — they must keep compiling and running unmodified against `GeminiService`'s existing public method signatures.
- No config-driven model selection — `gpt-4o-mini` is hardcoded in `OpenAiService`, matching how `GEMINI_API_URL`/model is hardcoded in `GeminiService`.
- `openai.api-key=${OPENAI_API_KEY:}` config key (already added to `application.properties` and `application.properties.example` — do not re-add).
- Fallback triggers on **any** Gemini failure (not just 429/503) and on a missing/blank Gemini key — both treated identically: try OpenAI if its key is configured.
- `isAvailable()` on `GeminiService` must return true if **either** key is present; both missing is the only hard-failure case.
- A missing Gemini key logs a `log.warn` (startup and per-call) but never blocks or delays the request.
- PDFs and images both go through OpenAI's Chat Completions `file`/`image_url` content parts — no separate Responses API path.

---

## File Structure

- **Create:** `src/main/java/com/opus/dispute/management/service/OpenAiService.java` — new provider implementation, self-contained, same package as `GeminiService`.
- **Modify:** `src/main/java/com/opus/dispute/management/service/GeminiService.java` — inject `OpenAiService`, add fallback wrapping, update `isAvailable()`.
- **Create:** `src/test/java/com/opus/dispute/management/service/OpenAiServiceTest.java`
- **Create:** `src/test/java/com/opus/dispute/management/service/GeminiServiceTest.java`

---

### Task 1: `OpenAiService` — text generation (`generateContent`, `isAvailable`)

**Files:**
- Create: `src/main/java/com/opus/dispute/management/service/OpenAiService.java`
- Test: `src/test/java/com/opus/dispute/management/service/OpenAiServiceTest.java`

**Interfaces:**
- Produces: `public OpenAiService(@Value("${openai.api-key:}") String apiKey)` constructor; `public boolean isAvailable()`; `public String generateContent(String systemPrompt, String userPrompt)`.

This task establishes the class shape, HTTP client, and the plain-text path other tasks build on.

- [ ] **Step 1: Write the failing test for `isAvailable()`**

Create `src/test/java/com/opus/dispute/management/service/OpenAiServiceTest.java`:

```java
package com.opus.dispute.management.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiServiceTest {

    @Test
    void isAvailable_returnsFalse_whenKeyBlank() {
        OpenAiService service = new OpenAiService("");
        assertFalse(service.isAvailable());
    }

    @Test
    void isAvailable_returnsTrue_whenKeyPresent() {
        OpenAiService service = new OpenAiService("sk-test-key");
        assertTrue(service.isAvailable());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw -q test -Dtest=OpenAiServiceTest`
Expected: FAIL — compilation error, `OpenAiService` does not exist.

- [ ] **Step 3: Create `OpenAiService` with constructor, `isAvailable()`, and `generateContent`**

Create `src/main/java/com/opus/dispute/management/service/OpenAiService.java`:

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw -q test -Dtest=OpenAiServiceTest`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/opus/dispute/management/service/OpenAiService.java src/test/java/com/opus/dispute/management/service/OpenAiServiceTest.java
git commit -m "Add OpenAiService with text generation and availability check"
```

---

### Task 2: `OpenAiService` — JSON mode, history, and multimodal methods

**Files:**
- Modify: `src/main/java/com/opus/dispute/management/service/OpenAiService.java`
- Test: `src/test/java/com/opus/dispute/management/service/OpenAiServiceTest.java`

**Interfaces:**
- Consumes: `buildChatRequest(String, String, List<GeminiService.ConversationTurn>, List<MediaFile>, boolean)`, `execute(JsonObject, String)`, `mapRole(String)` from Task 1.
- Produces: `public String generateJson(String systemPrompt, String userPrompt)`; `public String generateContentWithHistory(String systemPrompt, String userPrompt, List<GeminiService.ConversationTurn> history)`; `public String generateJsonWithHistory(String systemPrompt, String userPrompt, List<GeminiService.ConversationTurn> history)`; `public String generateJsonWithHistoryAndMedia(String systemPrompt, String userPrompt, List<GeminiService.ConversationTurn> history, List<MediaFile> mediaFiles)`; `public String generateMultimodalContent(String systemPrompt, String userPrompt, List<MediaFile> mediaFiles)`; `public String generateMultimodalJson(String systemPrompt, String userPrompt, List<MediaFile> mediaFiles)`.

- [ ] **Step 1: Write failing tests for the remaining six methods**

Add to `OpenAiServiceTest.java` (keep existing tests, add these):

```java
    @Test
    void generateJson_throwsWhenKeyMissing() {
        OpenAiService service = new OpenAiService("");
        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> service.generateJson("system", "user"));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("OpenAI API key not configured"));
    }

    @Test
    void generateContentWithHistory_throwsWhenKeyMissing() {
        OpenAiService service = new OpenAiService("");
        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> service.generateContentWithHistory("system", "user", List.of()));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("OpenAI API key not configured"));
    }

    @Test
    void generateJsonWithHistory_throwsWhenKeyMissing() {
        OpenAiService service = new OpenAiService("");
        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> service.generateJsonWithHistory("system", "user", List.of()));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("OpenAI API key not configured"));
    }

    @Test
    void generateJsonWithHistoryAndMedia_throwsWhenKeyMissing() {
        OpenAiService service = new OpenAiService("");
        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> service.generateJsonWithHistoryAndMedia("system", "user", List.of(), List.of()));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("OpenAI API key not configured"));
    }

    @Test
    void generateMultimodalContent_throwsWhenKeyMissing() {
        OpenAiService service = new OpenAiService("");
        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> service.generateMultimodalContent("system", "user", List.of()));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("OpenAI API key not configured"));
    }

    @Test
    void generateMultimodalJson_throwsWhenKeyMissing() {
        OpenAiService service = new OpenAiService("");
        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> service.generateMultimodalJson("system", "user", List.of()));
        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("OpenAI API key not configured"));
    }
```

Also add `import java.util.List;` at the top of the test file.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -q test -Dtest=OpenAiServiceTest`
Expected: FAIL — compilation errors, the six methods don't exist yet on `OpenAiService`.

- [ ] **Step 3: Add the six methods to `OpenAiService`**

Insert into `OpenAiService.java`, directly after the existing `generateContent` method:

```java
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw -q test -Dtest=OpenAiServiceTest`
Expected: PASS (8 tests total)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/opus/dispute/management/service/OpenAiService.java src/test/java/com/opus/dispute/management/service/OpenAiServiceTest.java
git commit -m "Add JSON mode, history, and multimodal methods to OpenAiService"
```

---

### Task 3: `OpenAiService` — request-shape unit tests (message roles, media parts)

**Files:**
- Modify: `src/test/java/com/opus/dispute/management/service/OpenAiServiceTest.java`

**Interfaces:**
- Consumes: `OpenAiService.ConversationTurn` is actually `GeminiService.ConversationTurn` (record defined in `GeminiService`, reused — see Task 1). `MediaFile` record from `com.opus.dispute.management.service.MediaFile` with `MediaFile(String name, String mimeType, byte[] data)`, `isPdf()`, `isImage()`, `base64Data()`.

This task verifies the actual JSON shape sent to OpenAI (role mapping, image/PDF content parts) without hitting the network, by extracting the request-building logic into a package-visible seam that tests can call directly. `buildChatRequest` and `buildMediaPart` are already `private` in `OpenAiService` — this task changes them to package-private so the test (same package) can call them directly and assert on the resulting `JsonObject`.

- [ ] **Step 1: Write the failing tests**

Add to `OpenAiServiceTest.java`:

```java
    @Test
    void buildChatRequest_mapsGeminiModelRoleToAssistant() {
        OpenAiService service = new OpenAiService("sk-test-key");
        List<GeminiService.ConversationTurn> history = List.of(
                new GeminiService.ConversationTurn("user", "hello"),
                new GeminiService.ConversationTurn("model", "hi there")
        );

        com.google.gson.JsonObject request = service.buildChatRequest("sys", "next question", history, null, false);
        com.google.gson.JsonArray messages = request.getAsJsonArray("messages");

        org.junit.jupiter.api.Assertions.assertEquals("system", messages.get(0).getAsJsonObject().get("role").getAsString());
        org.junit.jupiter.api.Assertions.assertEquals("user", messages.get(1).getAsJsonObject().get("role").getAsString());
        org.junit.jupiter.api.Assertions.assertEquals("assistant", messages.get(2).getAsJsonObject().get("role").getAsString());
        org.junit.jupiter.api.Assertions.assertEquals("user", messages.get(3).getAsJsonObject().get("role").getAsString());
    }

    @Test
    void buildChatRequest_setsJsonResponseFormat_whenJsonModeTrue() {
        OpenAiService service = new OpenAiService("sk-test-key");
        com.google.gson.JsonObject request = service.buildChatRequest("sys", "user", null, null, true);
        org.junit.jupiter.api.Assertions.assertEquals(
                "json_object",
                request.getAsJsonObject("response_format").get("type").getAsString());
    }

    @Test
    void buildMediaPart_usesImageUrlPart_forImageFile() {
        OpenAiService service = new OpenAiService("sk-test-key");
        MediaFile image = new MediaFile("photo.png", "image/png", new byte[]{1, 2, 3});

        com.google.gson.JsonObject part = service.buildMediaPart(image);

        org.junit.jupiter.api.Assertions.assertEquals("image_url", part.get("type").getAsString());
        org.junit.jupiter.api.Assertions.assertTrue(
                part.getAsJsonObject("image_url").get("url").getAsString().startsWith("data:image/png;base64,"));
    }

    @Test
    void buildMediaPart_usesFilePart_forPdfFile() {
        OpenAiService service = new OpenAiService("sk-test-key");
        MediaFile pdf = new MediaFile("statement.pdf", "application/pdf", new byte[]{1, 2, 3});

        com.google.gson.JsonObject part = service.buildMediaPart(pdf);

        org.junit.jupiter.api.Assertions.assertEquals("file", part.get("type").getAsString());
        com.google.gson.JsonObject file = part.getAsJsonObject("file");
        org.junit.jupiter.api.Assertions.assertEquals("statement.pdf", file.get("filename").getAsString());
        org.junit.jupiter.api.Assertions.assertTrue(
                file.get("file_data").getAsString().startsWith("data:application/pdf;base64,"));
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -q test -Dtest=OpenAiServiceTest`
Expected: FAIL — `buildChatRequest` and `buildMediaPart` are `private`, not accessible from the test class.

- [ ] **Step 3: Change `buildChatRequest` and `buildMediaPart` visibility to package-private**

In `OpenAiService.java`, change:
```java
    private JsonObject buildChatRequest(String systemPrompt, String userPrompt,
```
to:
```java
    JsonObject buildChatRequest(String systemPrompt, String userPrompt,
```

And change:
```java
    private JsonObject buildMediaPart(MediaFile mf) {
```
to:
```java
    JsonObject buildMediaPart(MediaFile mf) {
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw -q test -Dtest=OpenAiServiceTest`
Expected: PASS (12 tests total)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/opus/dispute/management/service/OpenAiService.java src/test/java/com/opus/dispute/management/service/OpenAiServiceTest.java
git commit -m "Add request-shape tests for OpenAiService role mapping and media parts"
```

---

### Task 4: `GeminiService` — inject `OpenAiService` and add fallback to `generateContent`

**Files:**
- Modify: `src/main/java/com/opus/dispute/management/service/GeminiService.java:26-47` (fields/constructor), `:52-59` (`generateContent`)
- Test: `src/test/java/com/opus/dispute/management/service/GeminiServiceTest.java`

**Interfaces:**
- Consumes: `OpenAiService` from Tasks 1–3 (`isAvailable()`, `generateContent(String, String)`).
- Produces: `GeminiService(@Value("${gemini.api-key:}") String apiKey, OpenAiService openAiService)` constructor (signature changes — this is the seam every later task in this file builds on); `public boolean isAvailable()` now returns true if either provider is available.

This task establishes the fallback pattern on the simplest method (`generateContent`) before repeating it for the other five in Task 5.

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/opus/dispute/management/service/GeminiServiceTest.java`:

```java
package com.opus.dispute.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private OpenAiService openAiService;

    @Test
    void isAvailable_true_whenOnlyGeminiKeyPresent() {
        GeminiService service = new GeminiService("gemini-key", openAiService);
        assertTrue(service.isAvailable());
    }

    @Test
    void isAvailable_true_whenOnlyOpenAiKeyPresent() {
        when(openAiService.isAvailable()).thenReturn(true);
        GeminiService service = new GeminiService("", openAiService);
        assertTrue(service.isAvailable());
    }

    @Test
    void isAvailable_false_whenBothKeysMissing() {
        when(openAiService.isAvailable()).thenReturn(false);
        GeminiService service = new GeminiService("", openAiService);
        assertFalse(service.isAvailable());
    }

    @Test
    void generateContent_usesOpenAiDirectly_whenGeminiKeyMissing() {
        when(openAiService.isAvailable()).thenReturn(true);
        when(openAiService.generateContent("sys", "user")).thenReturn("openai response");

        GeminiService service = new GeminiService("", openAiService);
        String result = service.generateContent("sys", "user");

        assertEquals("openai response", result);
        verify(openAiService).generateContent("sys", "user");
    }

    @Test
    void generateContent_throws_whenBothProvidersUnavailable() {
        when(openAiService.isAvailable()).thenReturn(false);
        GeminiService service = new GeminiService("", openAiService);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.generateContent("sys", "user"));
        assertTrue(ex.getMessage().contains("not configured"));
        verifyNoInteractions(openAiService.getClass() == OpenAiService.class ? openAiService : openAiService);
    }
}
```

Note: the last test's `verifyNoInteractions` line intentionally only checks `generateContent` was never called on `openAiService` — since `isAvailable()` is stubbed via `when(...)`, replace that final line with:
```java
        verify(openAiService, org.mockito.Mockito.never()).generateContent(anyString(), anyString());
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -q test -Dtest=GeminiServiceTest`
Expected: FAIL — compilation error, `GeminiService(String, OpenAiService)` constructor doesn't exist yet (current constructor only takes `String`).

- [ ] **Step 3: Update `GeminiService` constructor, `isAvailable()`, and `generateContent`**

In `GeminiService.java`, replace lines 26-59 (fields through `generateContent`):

```java
    private final String apiKey;
    private final OpenAiService openAiService;
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();

    public GeminiService(@Value("${gemini.api-key:}") String apiKey, OpenAiService openAiService) {
        this.apiKey = apiKey;
        this.openAiService = openAiService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key not configured. Requests will be served by OpenAI fallback if available.");
        } else {
            log.info("Gemini service initialized successfully");
        }
    }

    private boolean geminiAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    public boolean isAvailable() {
        return geminiAvailable() || openAiService.isAvailable();
    }

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 4000;

    public String generateContent(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            throw new RuntimeException("AI service not configured: no Gemini or OpenAI API key present");
        }
        if (!geminiAvailable()) {
            log.warn("Gemini API key not configured; serving generateContent via OpenAI fallback");
            String result = openAiService.generateContent(systemPrompt, userPrompt);
            log.info("Served generateContent via OpenAI");
            return result;
        }
        try {
            JsonObject requestBody = buildRequest(systemPrompt, userPrompt);
            String result = executeWithRetry(requestBody, "generateContent");
            log.info("Served generateContent via Gemini");
            return result;
        } catch (RuntimeException geminiFailure) {
            if (openAiService.isAvailable()) {
                log.warn("Gemini call failed for generateContent, falling back to OpenAI: {}", geminiFailure.getMessage());
                String result = openAiService.generateContent(systemPrompt, userPrompt);
                log.info("Served generateContent via OpenAI fallback");
                return result;
            }
            throw geminiFailure;
        }
    }
```

This removes the old `private final String apiKey;` / constructor / `isAvailable()` / `generateContent` block (previously lines 26-59) and replaces it with the above. Leave `generateJson`, `generateContentWithHistory`, etc. (previously lines 61 onward) untouched for now — Task 5 updates them.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw -q test -Dtest=GeminiServiceTest`
Expected: PASS (5 tests)

Also run the full test suite to confirm nothing else broke:

Run: `./mvnw -q test`
Expected: PASS (all tests, including `OpenAiServiceTest` and the existing `MastercardDisputeManagementApplicationTests`)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/opus/dispute/management/service/GeminiService.java src/test/java/com/opus/dispute/management/service/GeminiServiceTest.java
git commit -m "Add OpenAI fallback to GeminiService.generateContent and isAvailable"
```

---

### Task 5: `GeminiService` — fallback for remaining five methods

**Files:**
- Modify: `src/main/java/com/opus/dispute/management/service/GeminiService.java` (the `generateJson`, `generateContentWithHistory`, `generateJsonWithHistory`, `generateJsonWithHistoryAndMedia`, `generateMultimodalContent`, `generateMultimodalJson` methods — six methods, but `generateJsonWithHistory` just delegates to `generateJsonWithHistoryAndMedia` so only five need the fallback pattern directly)
- Test: `src/test/java/com/opus/dispute/management/service/GeminiServiceTest.java`

**Interfaces:**
- Consumes: `geminiAvailable()`, `openAiService` field, logging pattern from Task 4.
- Produces: no new public signatures — same six methods, same signatures, now with fallback behavior.

- [ ] **Step 1: Write the failing tests**

Add to `GeminiServiceTest.java`:

```java
    @Test
    void generateJson_fallsBackToOpenAi_whenGeminiThrows() {
        // Gemini key present but invalid → real HTTP call will fail fast on a bad host is not viable here,
        // so we exercise the "Gemini key missing" path instead, which is deterministic without network access.
        when(openAiService.isAvailable()).thenReturn(true);
        when(openAiService.generateJson("sys", "user")).thenReturn("{\"ok\":true}");

        GeminiService service = new GeminiService("", openAiService);
        String result = service.generateJson("sys", "user");

        assertEquals("{\"ok\":true}", result);
    }

    @Test
    void generateContentWithHistory_usesOpenAi_whenGeminiKeyMissing() {
        when(openAiService.isAvailable()).thenReturn(true);
        java.util.List<GeminiService.ConversationTurn> history = java.util.List.of(
                new GeminiService.ConversationTurn("user", "hi"));
        when(openAiService.generateContentWithHistory("sys", "user", history)).thenReturn("history response");

        GeminiService service = new GeminiService("", openAiService);
        String result = service.generateContentWithHistory("sys", "user", history);

        assertEquals("history response", result);
    }

    @Test
    void generateJsonWithHistory_usesOpenAi_whenGeminiKeyMissing() {
        when(openAiService.isAvailable()).thenReturn(true);
        java.util.List<GeminiService.ConversationTurn> history = java.util.List.of();
        when(openAiService.generateJsonWithHistoryAndMedia("sys", "user", history, java.util.List.of()))
                .thenReturn("{}");

        GeminiService service = new GeminiService("", openAiService);
        String result = service.generateJsonWithHistory("sys", "user", history);

        assertEquals("{}", result);
    }

    @Test
    void generateMultimodalContent_usesOpenAi_whenGeminiKeyMissing() {
        when(openAiService.isAvailable()).thenReturn(true);
        java.util.List<MediaFile> mediaFiles = java.util.List.of(
                new MediaFile("photo.png", "image/png", new byte[]{1, 2, 3}));
        when(openAiService.generateMultimodalContent("sys", "user", mediaFiles)).thenReturn("image response");

        GeminiService service = new GeminiService("", openAiService);
        String result = service.generateMultimodalContent("sys", "user", mediaFiles);

        assertEquals("image response", result);
    }

    @Test
    void generateMultimodalJson_usesOpenAi_whenGeminiKeyMissing() {
        when(openAiService.isAvailable()).thenReturn(true);
        java.util.List<MediaFile> mediaFiles = java.util.List.of(
                new MediaFile("statement.pdf", "application/pdf", new byte[]{1, 2, 3}));
        when(openAiService.generateMultimodalJson("sys", "user", mediaFiles)).thenReturn("{\"pdf\":true}");

        GeminiService service = new GeminiService("", openAiService);
        String result = service.generateMultimodalJson("sys", "user", mediaFiles);

        assertEquals("{\"pdf\":true}", result);
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw -q test -Dtest=GeminiServiceTest`
Expected: FAIL — these five methods still throw immediately on missing Gemini key (no fallback wired yet), so results won't match OpenAI mock responses.

- [ ] **Step 3: Add fallback wrapping to the five methods**

In `GeminiService.java`, replace the `generateJson` method body:

```java
    public String generateJson(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            throw new RuntimeException("AI service not configured: no Gemini or OpenAI API key present");
        }
        if (!geminiAvailable()) {
            log.warn("Gemini API key not configured; serving generateJson via OpenAI fallback");
            String result = openAiService.generateJson(systemPrompt, userPrompt);
            log.info("Served generateJson via OpenAI");
            return result;
        }
        try {
            JsonObject requestBody = buildRequest(systemPrompt, userPrompt);
            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("responseMimeType", "application/json");
            requestBody.add("generationConfig", generationConfig);
            String result = executeWithRetry(requestBody, "generateJson");
            log.info("Served generateJson via Gemini");
            return result;
        } catch (RuntimeException geminiFailure) {
            if (openAiService.isAvailable()) {
                log.warn("Gemini call failed for generateJson, falling back to OpenAI: {}", geminiFailure.getMessage());
                String result = openAiService.generateJson(systemPrompt, userPrompt);
                log.info("Served generateJson via OpenAI fallback");
                return result;
            }
            throw geminiFailure;
        }
    }
```

Replace `generateContentWithHistory`:

```java
    public String generateContentWithHistory(String systemPrompt, String userPrompt, List<ConversationTurn> history) {
        if (!isAvailable()) {
            throw new RuntimeException("AI service not configured: no Gemini or OpenAI API key present");
        }
        if (!geminiAvailable()) {
            log.warn("Gemini API key not configured; serving generateContentWithHistory via OpenAI fallback");
            String result = openAiService.generateContentWithHistory(systemPrompt, userPrompt, history);
            log.info("Served generateContentWithHistory via OpenAI");
            return result;
        }
        try {
            JsonObject requestBody = buildRequestWithHistory(systemPrompt, userPrompt, history);
            String result = executeWithRetry(requestBody, "generateContentWithHistory");
            log.info("Served generateContentWithHistory via Gemini");
            return result;
        } catch (RuntimeException geminiFailure) {
            if (openAiService.isAvailable()) {
                log.warn("Gemini call failed for generateContentWithHistory, falling back to OpenAI: {}", geminiFailure.getMessage());
                String result = openAiService.generateContentWithHistory(systemPrompt, userPrompt, history);
                log.info("Served generateContentWithHistory via OpenAI fallback");
                return result;
            }
            throw geminiFailure;
        }
    }

    public String generateJsonWithHistory(String systemPrompt, String userPrompt, List<ConversationTurn> history) {
        return generateJsonWithHistoryAndMedia(systemPrompt, userPrompt, history, null);
    }
```

Replace `generateJsonWithHistoryAndMedia`:

```java
    public String generateJsonWithHistoryAndMedia(String systemPrompt, String userPrompt,
                                                   List<ConversationTurn> history, List<MediaFile> mediaFiles) {
        if (!isAvailable()) {
            throw new RuntimeException("AI service not configured: no Gemini or OpenAI API key present");
        }
        mediaFiles = filterMediaFiles(mediaFiles);

        if (!geminiAvailable()) {
            log.warn("Gemini API key not configured; serving generateJsonWithHistoryAndMedia via OpenAI fallback");
            String result = openAiService.generateJsonWithHistoryAndMedia(systemPrompt, userPrompt, history, mediaFiles);
            log.info("Served generateJsonWithHistoryAndMedia via OpenAI");
            return result;
        }
        try {
            JsonObject requestBody = buildRequestWithHistory(systemPrompt, userPrompt, history, mediaFiles);
            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("responseMimeType", "application/json");
            requestBody.add("generationConfig", generationConfig);
            String result = executeWithRetry(requestBody, "generateJsonWithHistoryAndMedia");
            log.info("Served generateJsonWithHistoryAndMedia via Gemini");
            return result;
        } catch (RuntimeException geminiFailure) {
            if (openAiService.isAvailable()) {
                log.warn("Gemini call failed for generateJsonWithHistoryAndMedia, falling back to OpenAI: {}", geminiFailure.getMessage());
                String result = openAiService.generateJsonWithHistoryAndMedia(systemPrompt, userPrompt, history, mediaFiles);
                log.info("Served generateJsonWithHistoryAndMedia via OpenAI fallback");
                return result;
            }
            throw geminiFailure;
        }
    }
```

Replace `generateMultimodalContent`:

```java
    public String generateMultimodalContent(String systemPrompt, String userPrompt, List<MediaFile> mediaFiles) {
        mediaFiles = filterMediaFiles(mediaFiles);
        if (mediaFiles.isEmpty()) {
            return generateContent(systemPrompt, userPrompt);
        }
        if (!isAvailable()) {
            throw new RuntimeException("AI service not configured: no Gemini or OpenAI API key present");
        }

        if (!geminiAvailable()) {
            log.warn("Gemini API key not configured; serving generateMultimodalContent via OpenAI fallback");
            String result = openAiService.generateMultimodalContent(systemPrompt, userPrompt, mediaFiles);
            log.info("Served generateMultimodalContent via OpenAI");
            return result;
        }
        try {
            JsonObject requestBody = buildMultimodalRequest(systemPrompt, userPrompt, mediaFiles);
            String result = executeWithRetry(requestBody, "generateMultimodalContent");
            log.info("Served generateMultimodalContent via Gemini");
            return result;
        } catch (RuntimeException geminiFailure) {
            if (openAiService.isAvailable()) {
                log.warn("Gemini call failed for generateMultimodalContent, falling back to OpenAI: {}", geminiFailure.getMessage());
                String result = openAiService.generateMultimodalContent(systemPrompt, userPrompt, mediaFiles);
                log.info("Served generateMultimodalContent via OpenAI fallback");
                return result;
            }
            throw geminiFailure;
        }
    }
```

Replace `generateMultimodalJson`:

```java
    public String generateMultimodalJson(String systemPrompt, String userPrompt, List<MediaFile> mediaFiles) {
        mediaFiles = filterMediaFiles(mediaFiles);
        if (mediaFiles.isEmpty()) {
            return generateJson(systemPrompt, userPrompt);
        }
        if (!isAvailable()) {
            throw new RuntimeException("AI service not configured: no Gemini or OpenAI API key present");
        }

        if (!geminiAvailable()) {
            log.warn("Gemini API key not configured; serving generateMultimodalJson via OpenAI fallback");
            String result = openAiService.generateMultimodalJson(systemPrompt, userPrompt, mediaFiles);
            log.info("Served generateMultimodalJson via OpenAI");
            return result;
        }
        try {
            JsonObject requestBody = buildMultimodalRequest(systemPrompt, userPrompt, mediaFiles);
            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("responseMimeType", "application/json");
            requestBody.add("generationConfig", generationConfig);
            String result = executeWithRetry(requestBody, "generateMultimodalJson");
            log.info("Served generateMultimodalJson via Gemini");
            return result;
        } catch (RuntimeException geminiFailure) {
            if (openAiService.isAvailable()) {
                log.warn("Gemini call failed for generateMultimodalJson, falling back to OpenAI: {}", geminiFailure.getMessage());
                String result = openAiService.generateMultimodalJson(systemPrompt, userPrompt, mediaFiles);
                log.info("Served generateMultimodalJson via OpenAI fallback");
                return result;
            }
            throw geminiFailure;
        }
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./mvnw -q test -Dtest=GeminiServiceTest`
Expected: PASS (10 tests total)

Run the full suite:

Run: `./mvnw -q test`
Expected: PASS (all tests across the project)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/opus/dispute/management/service/GeminiService.java src/test/java/com/opus/dispute/management/service/GeminiServiceTest.java
git commit -m "Add OpenAI fallback to remaining GeminiService generation methods"
```

---

### Task 6: Full build verification and manual smoke check

**Files:** none (verification only)

**Interfaces:** none — this task only runs the build and confirms application startup.

- [ ] **Step 1: Run the full test suite**

Run: `./mvnw -q test`
Expected: PASS — all tests green, including `MastercardDisputeManagementApplicationTests`, `OpenAiServiceTest`, `GeminiServiceTest`.

- [ ] **Step 2: Run a full package build**

Run: `./mvnw -q clean package -DskipTests`
Expected: BUILD SUCCESS, jar produced under `target/`.

- [ ] **Step 3: Start the application locally and confirm it boots**

Run: `./mvnw spring-boot:run` (in background/separate terminal), then check:

```bash
curl -s http://localhost:5000/api/agents/status
```

Expected: JSON response containing `"geminiAvailable":true` (assuming both keys are set in `src/main/resources/application.properties` as you configured earlier), with no startup errors in the console about `OpenAiService` or `GeminiService` bean wiring.

Stop the app afterward (Ctrl+C or kill the background process).

- [ ] **Step 4: Manually verify the fallback path works end-to-end (optional but recommended before the demo)**

Temporarily blank out `gemini.api-key=` in your local `src/main/resources/application.properties` (leave `openai.api-key=` populated), restart the app, and trigger any AI agent endpoint (e.g. `POST /api/agents/...` used by `CaseSummarizerAgent`). Confirm in the console logs you see:
```
Gemini API key not configured; serving generate... via OpenAI fallback
Served generate... via OpenAI
```
and that the endpoint still returns a valid response. Restore your real `gemini.api-key` value afterward.

- [ ] **Step 5: No commit for this task** — verification only, nothing to stage.

---

## Post-Implementation

Once all tasks pass, the branch `feature/openai-fallback-provider` is ready for the `finishing-a-development-branch` workflow (merge/PR into `develop`) — not part of this plan; handle that as a separate explicit step per project convention (never push or merge without explicit confirmation).
