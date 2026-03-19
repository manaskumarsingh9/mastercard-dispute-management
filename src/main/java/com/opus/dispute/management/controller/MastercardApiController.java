package com.opus.dispute.management.controller;

import com.opus.dispute.management.service.MastercardApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/mastercard")
public class MastercardApiController {

    private static final String BASE = "/mastercom/v6";
    private final MastercardApiClient mastercardApiClient;

    public MastercardApiController(MastercardApiClient mastercardApiClient) {
        this.mastercardApiClient = mastercardApiClient;
    }

    @GetMapping("/test")
    public ResponseEntity<String> testConnection() {
        try {
            log.info("Testing Mastercard API connection...");
            String response = mastercardApiClient.get(BASE + "/healthcheck");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to connect to Mastercard API", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ===== 1. TRANSACTION SEARCH =====

    @PostMapping("/transactions/search")
    public ResponseEntity<String> searchTransactions(@RequestBody String body) {
        try {
            log.info("Searching transactions...");
            String response = mastercardApiClient.post(BASE + "/transactions/search", body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to search transactions", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ===== 2. RETRIEVE TRANSACTION DETAILS =====

    @GetMapping("/claims/{claimId}/transactions/clearing/{transactionId}")
    public ResponseEntity<String> getTransactionClearingDetail(
            @PathVariable String claimId,
            @PathVariable String transactionId) {
        try {
            log.info("Retrieving clearing detail for claim: {}, transaction: {}", claimId, transactionId);
            String response = mastercardApiClient.get(
                    BASE + "/claims/" + claimId + "/transactions/clearing/" + transactionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get clearing detail", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/claims/{claimId}/transactions/authorization/{transactionId}")
    public ResponseEntity<String> retrieveAuthorizationDetail(
            @PathVariable String claimId,
            @PathVariable String transactionId) {
        try {
            log.info("Retrieving authorization detail for claim: {}, transaction: {}", claimId, transactionId);
            String response = mastercardApiClient.get(
                    BASE + "/claims/" + claimId + "/transactions/authorization/" + transactionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get authorization detail", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ===== 3. CLAIMS =====

    @PostMapping("/claims")
    public ResponseEntity<String> createClaim(@RequestBody String body) {
        try {
            log.info("Creating new claim...");
            String response = mastercardApiClient.post(BASE + "/claims", body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create claim", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/claims/{claimId}")
    public ResponseEntity<String> getClaimDetail(@PathVariable String claimId) {
        try {
            log.info("Retrieving claim details for: {}", claimId);
            String response = mastercardApiClient.get(BASE + "/claims/" + claimId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get claim details", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ===== 4. RETRIEVAL REQUESTS =====

    @GetMapping("/claims/{claimId}/retrievalrequests/loaddataforretrievalrequests")
    public ResponseEntity<String> getDataForCreateRetrievalRequest(@PathVariable String claimId) {
        try {
            log.info("Loading data for retrieval request, claim: {}", claimId);
            String response = mastercardApiClient.get(
                    BASE + "/claims/" + claimId + "/retrievalrequests/loaddataforretrievalrequests");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to load data for retrieval request", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/claims/{claimId}/retrievalrequests")
    public ResponseEntity<String> createRetrievalRequest(
            @PathVariable String claimId,
            @RequestBody String body) {
        try {
            log.info("Creating retrieval request for claim: {}", claimId);
            String response = mastercardApiClient.post(
                    BASE + "/claims/" + claimId + "/retrievalrequests", body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create retrieval request", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ===== 5. RETRIEVAL REQUEST FULFILLMENT RESPONSE =====

    @PostMapping("/claims/{claimId}/retrievalrequests/{requestId}/fulfillments/response")
    public ResponseEntity<String> issuerResponseRetrievalRequest(
            @PathVariable String claimId,
            @PathVariable String requestId,
            @RequestBody String body) {
        try {
            log.info("Issuer responding to retrieval fulfillment, claim: {}, request: {}", claimId, requestId);
            String response = mastercardApiClient.post(
                    BASE + "/claims/" + claimId + "/retrievalrequests/" + requestId + "/fulfillments/response", body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to respond to retrieval fulfillment", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/claims/{claimId}/retrievalrequests/{requestId}/documents")
    public ResponseEntity<String> getRetrievalDoc(
            @PathVariable String claimId,
            @PathVariable String requestId,
            @RequestParam(defaultValue = "ORIGINAL") String format) {
        try {
            log.info("Retrieving documents for claim: {}, request: {}, format: {}", claimId, requestId, format);
            String response = mastercardApiClient.get(
                    BASE + "/claims/" + claimId + "/retrievalrequests/" + requestId + "/documents?format=" + format);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get retrieval documents", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/retrievalrequests/status")
    public ResponseEntity<String> retrieveFulfillmentStatus(@RequestBody String body) {
        try {
            log.info("Retrieving fulfillment status...");
            String response = mastercardApiClient.put(BASE + "/retrievalrequests/status", body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve fulfillment status", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ===== 6. CHARGEBACKS =====

    @PostMapping("/claims/{claimId}/chargebacks/loaddataforchargebacks")
    public ResponseEntity<String> getDataForCreateChargeback(
            @PathVariable String claimId,
            @RequestBody String body) {
        try {
            log.info("Loading data for chargeback, claim: {}", claimId);
            String response = mastercardApiClient.post(
                    BASE + "/claims/" + claimId + "/chargebacks/loaddataforchargebacks", body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to load data for chargeback", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/claims/{claimId}/chargebacks")
    public ResponseEntity<String> createChargeback(
            @PathVariable String claimId,
            @RequestBody String body) {
        try {
            log.info("Creating chargeback for claim: {}", claimId);
            String response = mastercardApiClient.post(
                    BASE + "/claims/" + claimId + "/chargebacks", body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create chargeback", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ===== 7. CHARGEBACK REVERSAL =====

    @PostMapping("/claims/{claimId}/chargebacks/{chargebackId}/reversal")
    public ResponseEntity<String> createChargebackReversal(
            @PathVariable String claimId,
            @PathVariable String chargebackId) {
        try {
            log.info("Reversing chargeback: {} for claim: {}", chargebackId, claimId);
            String response = mastercardApiClient.post(
                    BASE + "/claims/" + claimId + "/chargebacks/" + chargebackId + "/reversal", "{}");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to reverse chargeback", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ===== 8. UPDATE CHARGEBACK =====

    @PutMapping("/claims/{claimId}/chargebacks/{chargebackId}")
    public ResponseEntity<String> updateChargeback(
            @PathVariable String claimId,
            @PathVariable String chargebackId,
            @RequestBody String body) {
        try {
            log.info("Updating chargeback: {} for claim: {}", chargebackId, claimId);
            String response = mastercardApiClient.put(
                    BASE + "/claims/" + claimId + "/chargebacks/" + chargebackId, body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to update chargeback", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ===== 9. CHARGEBACK STATUS & ACKNOWLEDGE =====

    @PutMapping("/chargebacks/status")
    public ResponseEntity<String> retrieveChargebackStatus(@RequestBody String body) {
        try {
            log.info("Retrieving chargeback status...");
            String response = mastercardApiClient.put(BASE + "/chargebacks/status", body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve chargeback status", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/chargebacks/acknowledge")
    public ResponseEntity<String> acknowledgeChargebacks(@RequestBody String body) {
        try {
            log.info("Acknowledging chargebacks...");
            String response = mastercardApiClient.put(BASE + "/chargebacks/acknowledge", body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to acknowledge chargebacks", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ===== 10. CASE FILING =====

    @PostMapping("/cases")
    public ResponseEntity<String> createCaseFiling(@RequestBody String body) {
        try {
            log.info("Creating case filing...");
            String response = mastercardApiClient.post(BASE + "/cases", body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create case filing", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ===== 11. UPDATE CASE =====

    @PutMapping("/cases/{caseId}")
    public ResponseEntity<String> updateCaseFiling(
            @PathVariable String caseId,
            @RequestBody String body) {
        try {
            log.info("Updating case: {}", caseId);
            String response = mastercardApiClient.put(BASE + "/cases/" + caseId, body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to update case", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ===== 12. CASE DOCUMENTS =====

    @GetMapping("/cases/{caseId}/documents")
    public ResponseEntity<String> getCaseFilingDoc(
            @PathVariable String caseId,
            @RequestParam(defaultValue = "ORIGINAL") String format) {
        try {
            log.info("Retrieving case documents for case: {}, format: {}", caseId, format);
            String response = mastercardApiClient.get(
                    BASE + "/cases/" + caseId + "/documents?format=" + format);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get case documents", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ===== 13. FRAUD =====

    @GetMapping("/claims/{claimId}/fraud/loaddataforfraud")
    public ResponseEntity<String> getDataForCreateFraud(@PathVariable String claimId) {
        try {
            log.info("Loading data for fraud, claim: {}", claimId);
            String response = mastercardApiClient.get(
                    BASE + "/claims/" + claimId + "/fraud/loaddataforfraud");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to load data for fraud", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/claims/{claimId}/fraud/mastercard")
    public ResponseEntity<String> createFraudMasterCard(
            @PathVariable String claimId,
            @RequestBody String body) {
        try {
            log.info("Creating fraud item for claim: {}", claimId);
            String response = mastercardApiClient.post(
                    BASE + "/claims/" + claimId + "/fraud/mastercard", body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create fraud item", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ===== 14. FEE COLLECTION =====

    @PostMapping("/claims/{claimId}/fees/loaddataforfees")
    public ResponseEntity<String> getDataForCreateFee(
            @PathVariable String claimId,
            @RequestBody String body) {
        try {
            log.info("Loading data for fee, claim: {}", claimId);
            String response = mastercardApiClient.post(
                    BASE + "/claims/" + claimId + "/fees/loaddataforfees", body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to load data for fee", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/claims/{claimId}/fee")
    public ResponseEntity<String> createFee(
            @PathVariable String claimId,
            @RequestBody String body) {
        try {
            log.info("Creating fee for claim: {}", claimId);
            String response = mastercardApiClient.post(
                    BASE + "/claims/" + claimId + "/fee", body);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create fee", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
