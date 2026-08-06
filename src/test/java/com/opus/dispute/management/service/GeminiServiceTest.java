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
}
