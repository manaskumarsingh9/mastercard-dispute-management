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
