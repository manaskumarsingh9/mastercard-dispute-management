package com.opus.dispute.management.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

@Slf4j
@Service
public class ReasonCodeRulesService {

    private static final Path RULES_FILE = Paths.get("src/data/reason-code-rules.json");

    private volatile Set<String> cachedCodes;

    public Set<String> getSupportedReasonCodes() {
        if (cachedCodes == null) {
            synchronized (this) {
                if (cachedCodes == null) {
                    cachedCodes = loadReasonCodes();
                }
            }
        }
        return cachedCodes;
    }

    private Set<String> loadReasonCodes() {
        try {
            String json = Files.readString(RULES_FILE);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return Collections.unmodifiableSet(root.keySet());
        } catch (IOException e) {
            log.error("Failed to load reason-code-rules.json from {}", RULES_FILE, e);
            return Collections.emptySet();
        }
    }
}
