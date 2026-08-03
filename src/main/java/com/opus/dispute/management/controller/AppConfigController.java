package com.opus.dispute.management.controller;

import com.opus.dispute.management.service.AppConfigService;
import com.opus.dispute.management.service.AutoAcceptService;
import com.opus.dispute.management.service.ScheduledIngestionTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/config")
public class AppConfigController {

    private final AppConfigService configService;
    private final AutoAcceptService autoAcceptService;
    private final ScheduledIngestionTask scheduledIngestionTask;

    public AppConfigController(AppConfigService configService,
                                AutoAcceptService autoAcceptService,
                                ScheduledIngestionTask scheduledIngestionTask) {
        this.configService = configService;
        this.autoAcceptService = autoAcceptService;
        this.scheduledIngestionTask = scheduledIngestionTask;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> getConfig() {
        return ResponseEntity.ok(configService.getAllSettings());
    }

    @PutMapping
    public ResponseEntity<?> updateConfig(@RequestBody Map<String, String> updates) {
        try {
            Map<String, String> result = configService.updateSettings(updates);

            if (updates.containsKey("ingestion.auto-sync.enabled")
                    || updates.containsKey("ingestion.auto-sync.frequency")) {
                scheduledIngestionTask.reschedule();
                log.info("Auto-sync schedule updated after config change");
            }

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/auto-sync-status")
    public ResponseEntity<ScheduledIngestionTask.ScheduleInfo> getAutoSyncStatus() {
        return ResponseEntity.ok(scheduledIngestionTask.getScheduleInfo());
    }

    @GetMapping("/auto-accept-rules")
    public ResponseEntity<Map<String, Object>> getAutoAcceptRules() {
        return ResponseEntity.ok(autoAcceptService.getRulesConfig());
    }

    @PutMapping("/auto-accept-rules")
    public ResponseEntity<?> updateAutoAcceptRules(@RequestBody Map<String, Object> config) {
        try {
            Map<String, Object> result = autoAcceptService.updateRulesConfig(config);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to update auto-accept rules: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
