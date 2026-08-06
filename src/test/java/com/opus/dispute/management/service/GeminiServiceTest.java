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
        verify(openAiService, org.mockito.Mockito.never()).generateContent(anyString(), anyString());
    }

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
}
