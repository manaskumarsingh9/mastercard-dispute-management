package com.opus.dispute.management.service;

import com.opus.dispute.management.entity.AppSetting;
import com.opus.dispute.management.repository.AppSettingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class AppConfigService {

    private final AppSettingRepository repo;

    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>(Map.ofEntries(
            Map.entry("hide-tabs.pre-chargeback-alerts", "true"),
            Map.entry("hide-tabs.policy-diff", "true"),
            Map.entry("hide-tabs.analytics-insights", "true"),
            Map.entry("hide-tabs.integrations", "true"),
            Map.entry("hide-tabs.business-profile", "true"),
            Map.entry("hide-tabs.data-points", "true"),
            Map.entry("pipeline.auto-enrich", "false"),
            Map.entry("auto-accept.enabled", "false"),
            Map.entry("ingestion.auto-sync.enabled", "false"),
            Map.entry("ingestion.auto-sync.frequency", "once_daily"),
            Map.entry("urgency.enabled", "true"),
            Map.entry("urgency.critical-days", "2"),
            Map.entry("urgency.warning-days", "5"),
            Map.entry("urgency.buffer-days", "0"),
            Map.entry("agents.response-length", "long")
    ));

    private static final Set<String> ALLOWED_KEYS = Set.of(
            "hide-tabs.pre-chargeback-alerts",
            "hide-tabs.policy-diff",
            "hide-tabs.analytics-insights",
            "hide-tabs.integrations",
            "hide-tabs.business-profile",
            "hide-tabs.data-points",
            "pipeline.auto-enrich",
            "auto-accept.enabled",
            "ingestion.auto-sync.enabled",
            "ingestion.auto-sync.frequency",
            "urgency.enabled",
            "urgency.critical-days",
            "urgency.warning-days",
            "urgency.buffer-days",
            "agents.response-length"
    );

    private static final Set<String> VALID_RESPONSE_LENGTHS = Set.of("short", "long");

    private static final Set<String> VALID_FREQUENCIES = Set.of("once_daily", "twice_daily");

    public AppConfigService(AppSettingRepository repo) {
        this.repo = repo;
    }

    public Map<String, String> getAllSettings() {
        Map<String, String> result = new LinkedHashMap<>(DEFAULTS);
        repo.findAll().forEach(s -> result.put(s.getKey(), s.getValue()));
        return result;
    }

    public String getSetting(String key) {
        return repo.findById(key)
                .map(AppSetting::getValue)
                .orElse(DEFAULTS.get(key));
    }

    public boolean isAutoEnrichEnabled() {
        return "true".equals(getSetting("pipeline.auto-enrich"));
    }

    private static final Set<String> URGENCY_NUMERIC_KEYS = Set.of(
            "urgency.critical-days", "urgency.warning-days", "urgency.buffer-days"
    );

    public Map<String, String> updateSettings(Map<String, String> updates) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            if (!ALLOWED_KEYS.contains(entry.getKey())) {
                throw new IllegalArgumentException("Unknown config key: " + entry.getKey());
            }
            if ("ingestion.auto-sync.frequency".equals(entry.getKey())
                    && !VALID_FREQUENCIES.contains(entry.getValue())) {
                throw new IllegalArgumentException(
                        "Invalid frequency: " + entry.getValue() + ". Must be one of: " + VALID_FREQUENCIES);
            }
            if ("agents.response-length".equals(entry.getKey())
                    && !VALID_RESPONSE_LENGTHS.contains(entry.getValue())) {
                throw new IllegalArgumentException(
                        "Invalid response length: " + entry.getValue() + ". Must be one of: " + VALID_RESPONSE_LENGTHS);
            }
            if (URGENCY_NUMERIC_KEYS.contains(entry.getKey())) {
                try {
                    int val = Integer.parseInt(entry.getValue());
                    if (val < 0 || val > 90) {
                        throw new IllegalArgumentException(
                                "Invalid value for " + entry.getKey() + ": must be 0-90");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Invalid value for " + entry.getKey() + ": must be a number");
                }
            }
            AppSetting setting = new AppSetting(entry.getKey(), entry.getValue());
            setting.setUpdatedAt(LocalDateTime.now());
            repo.save(setting);
            log.info("Updated config: {} = {}", entry.getKey(), entry.getValue());
        }
        validateUrgencyThresholds();
        return getAllSettings();
    }

    private void validateUrgencyThresholds() {
        try {
            int critical = Integer.parseInt(getSetting("urgency.critical-days"));
            int warning = Integer.parseInt(getSetting("urgency.warning-days"));
            if (warning < critical) {
                throw new IllegalArgumentException(
                        "urgency.warning-days (" + warning + ") must be >= urgency.critical-days (" + critical + ")");
            }
        } catch (NumberFormatException ignored) {
        }
    }

    public boolean isShortResponseMode() {
        return "short".equals(getSetting("agents.response-length"));
    }

    public boolean isAutoSyncEnabled() {
        return "true".equals(getSetting("ingestion.auto-sync.enabled"));
    }

    public String getAutoSyncFrequency() {
        String freq = getSetting("ingestion.auto-sync.frequency");
        return VALID_FREQUENCIES.contains(freq) ? freq : "once_daily";
    }
}
