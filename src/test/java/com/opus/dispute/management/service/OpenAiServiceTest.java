package com.opus.dispute.management.service;

import org.junit.jupiter.api.Test;

import java.util.List;

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
}
