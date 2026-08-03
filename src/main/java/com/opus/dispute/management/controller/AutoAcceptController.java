package com.opus.dispute.management.controller;

import com.opus.dispute.management.service.AutoAcceptService;
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
@RequestMapping("/api/auto-accept-rules")
public class AutoAcceptController {

    private final AutoAcceptService autoAcceptService;

    public AutoAcceptController(AutoAcceptService autoAcceptService) {
        this.autoAcceptService = autoAcceptService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAutoAcceptRules() {
        return ResponseEntity.ok(autoAcceptService.getRulesConfig());
    }

    @PutMapping
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
