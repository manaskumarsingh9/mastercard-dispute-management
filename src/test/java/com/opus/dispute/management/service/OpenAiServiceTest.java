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
}
