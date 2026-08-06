package com.opus.dispute.management.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class ReasonCodeRulesService {

    private static final String OUT_OF_SCOPE_REASON_CODE = "4871";

    private final DataSourceService dataSourceService;

    private volatile Set<String> cachedCodes;

    public ReasonCodeRulesService(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

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
        String json = dataSourceService.loadReasonCodeRules();
        if (json == null) {
            log.error("Failed to load reason-code-rules.json via DataSourceService");
            return Collections.emptySet();
        }
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Set<String> codes = new HashSet<>(root.keySet());
        codes.remove(OUT_OF_SCOPE_REASON_CODE);
        return Collections.unmodifiableSet(codes);
    }
}
