package com.opus.dispute.management.controller;

import com.opus.dispute.management.service.MastercardApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Mastercard API integration endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/mastercard")
public class MastercardApiController {

    private final MastercardApiClient mastercardApiClient;

    public MastercardApiController(MastercardApiClient mastercardApiClient) {
        this.mastercardApiClient = mastercardApiClient;
    }

    /**
     * Test the Mastercard API connection
     * Endpoint: GET /api/mastercard/test
     */
    @GetMapping("/test")
    public ResponseEntity<String> testConnection() {
        try {
            log.info("Testing Mastercard API connection...");
            // Try a simple endpoint that doesn't require parameters
            // Test Mastercom healthcheck endpoint
            String response = mastercardApiClient.get("/mastercom/v6/healthcheck");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to connect to Mastercard API", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Get account risk assessment from Fraud API
     * Endpoint: GET /api/mastercard/fraud-risk/{accountId}
     */
    @GetMapping("/fraud-risk/{accountId}")
    public ResponseEntity<String> getAccountRisk(@PathVariable String accountId) {
        try {
            log.info("Retrieving fraud risk assessment for account: {}", accountId);
            String endpoint = "/fraud/v2/account-risk?account-id=" + accountId;
            String response = mastercardApiClient.get(endpoint);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get fraud risk assessment", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Search for transactions using Mastercom API
     * Endpoint: POST /api/mastercard/transactions/search
     */
    @PostMapping("/transactions/search")
    public ResponseEntity<String> searchTransactions(@RequestBody String searchRequestJson) {
        try {
            log.info("Searching transactions with request: {}", searchRequestJson);
            String response = mastercardApiClient.post("/mastercom/transaction/v1/search", searchRequestJson);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to search transactions", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * File a dispute claim
     * Endpoint: POST /api/mastercard/disputes/file
     */
    @PostMapping("/disputes/file")
    public ResponseEntity<String> fileDispute(@RequestBody String disputeRequestJson) {
        try {
            log.info("Filing dispute with request: {}", disputeRequestJson);
            String response = mastercardApiClient.post("/settlement/mastercom/v1/claims", disputeRequestJson);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to file dispute", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Get dispute/claim details
     * Endpoint: GET /api/mastercard/disputes/{claimId}
     */
    @GetMapping("/disputes/{claimId}")
    public ResponseEntity<String> getDisputeDetails(@PathVariable String claimId) {
        try {
            log.info("Retrieving dispute details for claim: {}", claimId);
            String endpoint = "/settlement/mastercom/v1/claims/" + claimId;
            String response = mastercardApiClient.get(endpoint);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get dispute details", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
