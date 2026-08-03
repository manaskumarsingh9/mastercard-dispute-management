package com.opus.dispute.management.controller;

import com.opus.dispute.management.service.EthocaApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/ethoca")
public class EthocaController {

    private final EthocaApiClient ethocaApiClient;

    public EthocaController(EthocaApiClient ethocaApiClient) {
        this.ethocaApiClient = ethocaApiClient;
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        boolean configured = ethocaApiClient.isInitialized();
        String status = configured
                ? "{\"status\":\"configured\",\"message\":\"Ethoca API Client is ready\"}"
                : "{\"status\":\"not_configured\",\"message\":\"Ethoca API credentials not set. Configure ethoca.api-key-id and ethoca.api-secret.\"}";
        return ResponseEntity.ok(status);
    }

    @GetMapping("/healthcheck")
    public ResponseEntity<String> healthCheck() {
        try {
            log.info("Checking Ethoca Consumer Clarity health...");
            String response = ethocaApiClient.get("/api/orders/healthchecks");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ethoca health check failed", e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/orders/search")
    public ResponseEntity<String> searchOrders(@RequestBody String body) {
        try {
            log.info("Searching Ethoca Consumer Clarity orders...");
            String response = ethocaApiClient.post("/api/orders", body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ethoca order search failed", e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/merchants/reports")
    public ResponseEntity<String> sendMerchantReports(@RequestBody String body) {
        try {
            log.info("Sending Ethoca merchant reports...");
            String response = ethocaApiClient.post("/merchants/reports", body);
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            log.error("Ethoca merchant reports failed", e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/merchants/notifications")
    public ResponseEntity<String> pushDescriptorNotification(@RequestBody String body) {
        try {
            log.info("Sending Ethoca descriptor notifications...");
            String response = ethocaApiClient.post("/merchants/notifications", body);
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            log.error("Ethoca descriptor notification failed", e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PutMapping("/subscriptions/actions/{actionId}")
    public ResponseEntity<String> updateSubscriptionAction(
            @PathVariable String actionId,
            @RequestBody String body) {
        try {
            log.info("Updating Ethoca subscription action: {}", actionId);
            String response = ethocaApiClient.put("/merchants/subscriptions/actions/" + actionId, body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ethoca subscription action update failed for action: {}", actionId, e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
