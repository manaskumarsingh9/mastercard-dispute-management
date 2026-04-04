package com.opus.dispute.management.controller;

import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.service.ClaimDetailService;
import com.opus.dispute.management.service.DataSourceService;
import com.opus.dispute.management.service.DisputeService;
import com.opus.dispute.management.repository.DisputeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    private static final Set<String> VALID_CATEGORIES = Set.of(
            "merchant", "shipping", "psp", "identity", "device", "fraud-tools", "customer-comms"
    );

    @Autowired
    private DisputeService disputeService;

    @Autowired
    private ClaimDetailService claimDetailService;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private DisputeRepository disputeRepository;

    @PostMapping
    public ResponseEntity<Dispute> createDispute(@RequestBody Dispute dispute) {
        Dispute createdDispute = disputeService.createDispute(dispute);
        return new ResponseEntity<>(createdDispute, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Dispute>> getAllDisputes() {
        List<Dispute> disputes = disputeService.getAllDisputes();
        return new ResponseEntity<>(disputes, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dispute> getDisputeById(@PathVariable Long id) {
        return disputeService.getDisputeById(id)
                .map(dispute -> new ResponseEntity<>(dispute, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateDispute(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return disputeRepository.findById(id)
                .map(dispute -> {
                    try {
                        applyStringField(updates, "merchantCategory", dispute::setMerchantCategory);
                        applyStringField(updates, "customerEmail", dispute::setCustomerEmail);
                        applyStringField(updates, "cardNumber", dispute::setCardNumber);
                        applyStringField(updates, "evidenceFileId", dispute::setEvidenceFileId);
                        applyStringField(updates, "merchantName", dispute::setMerchantName);
                        applyStringField(updates, "cardholderName", dispute::setCardholderName);
                        applyStringField(updates, "itemDescription", dispute::setItemDescription);
                        applyStringField(updates, "disputeType", dispute::setDisputeType);
                        applyStringField(updates, "action", dispute::setAction);
                        applyStringField(updates, "status", dispute::setStatus);
                        applyStringField(updates, "currency", dispute::setCurrency);
                        applyStringField(updates, "stripeDisputeId", dispute::setStripeDisputeId);
                        applyStringField(updates, "stripeChargeId", dispute::setStripeChargeId);
                        applyStringField(updates, "stripePaymentIntentId", dispute::setStripePaymentIntentId);

                        if (updates.containsKey("amount") && updates.get("amount") != null) {
                            Object val = updates.get("amount");
                            if (val instanceof Number) {
                                dispute.setAmount(((Number) val).doubleValue());
                            } else {
                                return ResponseEntity.badRequest().body(Map.of("error", "Field 'amount' must be a number"));
                            }
                        }

                        dispute.setLastUpdatedDate(LocalDateTime.now());
                        Dispute saved = disputeRepository.save(dispute);
                        return ResponseEntity.ok(saved);
                    } catch (ClassCastException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Invalid field type in request body"));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Dispute not found: " + id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDispute(@PathVariable Long id) {
        if (!disputeRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "Dispute not found: " + id));
        }
        disputeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "DELETED", "disputeId", id));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<?> getDisputeDetails(@PathVariable Long id) {
        ClaimDetailService.ClaimDetailResult result = claimDetailService.getClaimDetailSummary(id);
        if (result == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Dispute not found: " + id));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("dispute", result.dispute);
        response.put("chargebackDetails", result.chargebackDetails);
        response.put("retrievalDetails", result.retrievalDetails);
        response.put("feeDetails", result.feeDetails);
        response.put("caseFilingDetails", result.caseFilingDetails);
        response.put("detailsFetched", Boolean.TRUE.equals(result.dispute.getDetailsFetched()));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/fetch-details")
    public ResponseEntity<?> fetchClaimDetails(@PathVariable Long id) {
        try {
            claimDetailService.fetchAndStoreClaimDetail(id);
            ClaimDetailService.ClaimDetailResult result = claimDetailService.getClaimDetailSummary(id);

            if (result == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "status", "ERROR",
                        "disputeId", id,
                        "error", "Dispute not found after detail fetch"
                ));
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "SUCCESS");
            response.put("disputeId", id);
            response.put("reasonCode", result.dispute.getReasonCode());
            response.put("chargebackCount", result.chargebackDetails.size());
            response.put("retrievalCount", result.retrievalDetails.size());
            response.put("feeCount", result.feeDetails.size());
            response.put("caseFilingCount", result.caseFilingDetails.size());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("Dispute not found")) {
                return ResponseEntity.status(404).body(Map.of(
                        "status", "ERROR",
                        "disputeId", id,
                        "error", "Dispute not found: " + id
                ));
            }
            if (msg.contains("INVALID_INPUT_VALUE") || msg.contains("400 Bad Request")) {
                log.warn("Mastercard API rejected claim ID for dispute {}: invalid claim-id on Mastercard side", id);
                return ResponseEntity.status(422).body(Map.of(
                        "status", "INVALID_CLAIM",
                        "disputeId", id,
                        "error", "The claim ID for this dispute is not recognized by the Mastercard API. This dispute may have been manually created and does not exist in Mastercard's system."
                ));
            }
            log.error("Failed to fetch claim details for dispute {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "disputeId", id,
                    "error", "Failed to fetch claim details"
            ));
        } catch (Exception e) {
            log.error("Failed to fetch claim details for dispute {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "disputeId", id,
                    "error", "Failed to fetch claim details"
            ));
        }
    }

    @GetMapping("/{id}/sources")
    public ResponseEntity<?> getDisputeSources(@PathVariable Long id) {
        return disputeRepository.findById(id)
                .map(dispute -> {
                    int caseNum = dataSourceService.extractCaseNumber(dispute.getId(), dispute.getClaimId());
                    if (caseNum < 0) {
                        return ResponseEntity.ok(Map.of(
                                "disputeId", id,
                                "caseNumber", -1,
                                "sources", Map.of(),
                                "message", "No local evidence data files found for this dispute"
                        ));
                    }
                    Map<String, String> sources = dataSourceService.loadAllSourcesForCase(caseNum);
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("disputeId", id);
                    response.put("caseNumber", caseNum);
                    response.put("sourceCount", sources.size());
                    response.put("sources", sources);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Dispute not found: " + id)));
    }

    @GetMapping("/{id}/sources/{side}/{category}")
    public ResponseEntity<?> getDisputeSourcesBySideAndCategory(
            @PathVariable Long id,
            @PathVariable String side,
            @PathVariable String category) {
        if (!side.equals("issuer") && !side.equals("acquirer")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid side: " + side,
                    "validSides", List.of("issuer", "acquirer")
            ));
        }
        if (!VALID_CATEGORIES.contains(category)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid category: " + category,
                    "validCategories", VALID_CATEGORIES
            ));
        }

        return disputeRepository.findById(id)
                .map(dispute -> {
                    int caseNum = dataSourceService.extractCaseNumber(dispute.getId(), dispute.getClaimId());
                    if (caseNum < 0) {
                        return ResponseEntity.ok(Map.of(
                                "disputeId", id,
                                "side", side,
                                "category", category,
                                "sources", Map.of(),
                                "message", "No evidence files found"
                        ));
                    }
                    Map<String, String> sources = dataSourceService.loadSourcesByCategory(side, category, caseNum);
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("disputeId", id);
                    response.put("side", side);
                    response.put("category", category);
                    response.put("caseNumber", caseNum);
                    response.put("sourceCount", sources.size());
                    response.put("sources", sources);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Dispute not found: " + id)));
    }

    @GetMapping("/{id}/sources/{category}")
    public ResponseEntity<?> getDisputeSourcesByCategory(
            @PathVariable Long id,
            @PathVariable String category) {
        if (!VALID_CATEGORIES.contains(category)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid category: " + category,
                    "validCategories", VALID_CATEGORIES
            ));
        }

        return disputeRepository.findById(id)
                .map(dispute -> {
                    int caseNum = dataSourceService.extractCaseNumber(dispute.getId(), dispute.getClaimId());
                    if (caseNum < 0) {
                        return ResponseEntity.ok(Map.of(
                                "disputeId", id,
                                "category", category,
                                "sources", Map.of(),
                                "message", "No evidence files found"
                        ));
                    }
                    Map<String, String> issuer = dataSourceService.loadSourcesByCategory("issuer", category, caseNum);
                    Map<String, String> acquirer = dataSourceService.loadSourcesByCategory("acquirer", category, caseNum);
                    Map<String, String> combined = new LinkedHashMap<>();
                    for (Map.Entry<String, String> e : issuer.entrySet()) {
                        combined.put("issuer/" + e.getKey(), e.getValue());
                    }
                    for (Map.Entry<String, String> e : acquirer.entrySet()) {
                        combined.put("acquirer/" + e.getKey(), e.getValue());
                    }
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("disputeId", id);
                    response.put("category", category);
                    response.put("caseNumber", caseNum);
                    response.put("sourceCount", combined.size());
                    response.put("sources", combined);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Dispute not found: " + id)));
    }

    private void applyStringField(Map<String, Object> updates, String key, java.util.function.Consumer<String> setter) {
        if (updates.containsKey(key)) {
            Object val = updates.get(key);
            if (val == null) {
                setter.accept(null);
            } else {
                setter.accept(val.toString());
            }
        }
    }
}
