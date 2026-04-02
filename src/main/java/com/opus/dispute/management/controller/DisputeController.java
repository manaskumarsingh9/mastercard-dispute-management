package com.opus.dispute.management.controller;

import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.service.ClaimDetailService;
import com.opus.dispute.management.service.DisputeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    @Autowired
    private DisputeService disputeService;

    @Autowired
    private ClaimDetailService claimDetailService;

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
            if (e.getMessage() != null && e.getMessage().contains("Dispute not found")) {
                return ResponseEntity.status(404).body(Map.of(
                        "status", "ERROR",
                        "disputeId", id,
                        "error", "Dispute not found: " + id
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
}
