package com.opus.dispute.management.controller;

import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.entity.IngestionState;
import com.opus.dispute.management.repository.CaseFilingDetailRepository;
import com.opus.dispute.management.repository.ChargebackDetailRepository;
import com.opus.dispute.management.repository.DisputeRepository;
import com.opus.dispute.management.repository.EvidenceMapRepository;
import com.opus.dispute.management.repository.FeeDetailRepository;
import com.opus.dispute.management.repository.RetrievalDetailRepository;
import com.opus.dispute.management.service.ClaimIngestionService;
import com.opus.dispute.management.service.PostIngestionPipelineService;
import com.opus.dispute.management.service.ReasonCodeRulesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ingestion")
public class IngestionController {

    private static final String FABRICATED_CLAIM_PREFIX = "900002";

    private final ClaimIngestionService claimIngestionService;
    private final DisputeRepository disputeRepository;
    private final ChargebackDetailRepository chargebackDetailRepository;
    private final RetrievalDetailRepository retrievalDetailRepository;
    private final FeeDetailRepository feeDetailRepository;
    private final CaseFilingDetailRepository caseFilingDetailRepository;
    private final EvidenceMapRepository evidenceMapRepository;
    private final PostIngestionPipelineService postIngestionPipelineService;
    private final ReasonCodeRulesService reasonCodeRulesService;

    public IngestionController(ClaimIngestionService claimIngestionService,
                                DisputeRepository disputeRepository,
                                ChargebackDetailRepository chargebackDetailRepository,
                                RetrievalDetailRepository retrievalDetailRepository,
                                FeeDetailRepository feeDetailRepository,
                                CaseFilingDetailRepository caseFilingDetailRepository,
                                EvidenceMapRepository evidenceMapRepository,
                                PostIngestionPipelineService postIngestionPipelineService,
                                ReasonCodeRulesService reasonCodeRulesService) {
        this.claimIngestionService = claimIngestionService;
        this.disputeRepository = disputeRepository;
        this.chargebackDetailRepository = chargebackDetailRepository;
        this.retrievalDetailRepository = retrievalDetailRepository;
        this.feeDetailRepository = feeDetailRepository;
        this.caseFilingDetailRepository = caseFilingDetailRepository;
        this.evidenceMapRepository = evidenceMapRepository;
        this.postIngestionPipelineService = postIngestionPipelineService;
        this.reasonCodeRulesService = reasonCodeRulesService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingestOnDemand(@RequestBody Map<String, String> request) {
        try {
            log.info("On-demand ingestion triggered");
            String queueName = request.get("queueName");
            String dateFrom = request.get("lastModifiedDateFrom");
            String dateTo = request.get("lastModifiedDateTo");
            String pageNb = request.get("pageNb");

            ClaimIngestionService.IngestionResult result =
                    claimIngestionService.ingestOnDemand(queueName, dateFrom, dateTo, pageNb);

            Map<String, Object> response = new HashMap<>();
            response.put("newClaims", result.newClaims);
            response.put("updatedClaims", result.updatedClaims);
            response.put("skipped", result.skipped);
            response.put("capped", result.capped);
            response.put("errors", result.errors);
            response.put("success", result.errors.isEmpty());
            response.put("newDisputeIds", result.newDisputeIds);

            if (!result.newDisputeIds.isEmpty()) {
                postIngestionPipelineService.runPostIngestionHook(result.newDisputeIds);
                response.put("autoEnrichTriggered", true);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("On-demand ingestion failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getIngestionStatus() {
        try {
            IngestionState state = claimIngestionService.getIngestionState();
            long totalDisputes = disputeRepository.count();

            Map<String, Object> response = new HashMap<>();
            response.put("totalDisputesInDb", totalDisputes);

            if (state != null) {
                Map<String, Object> stateMap = new HashMap<>();
                stateMap.put("lastPolledFrom", state.getLastPolledFrom() != null ? state.getLastPolledFrom().toString() : null);
                stateMap.put("lastPolledTo", state.getLastPolledTo() != null ? state.getLastPolledTo().toString() : null);
                stateMap.put("lastRunAt", state.getLastRunAt() != null ? state.getLastRunAt().toString() : null);
                stateMap.put("totalClaimsIngested", state.getClaimsIngested());
                stateMap.put("totalRuns", state.getTotalRuns());
                response.put("ingestionState", stateMap);
            } else {
                response.put("ingestionState", null);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get ingestion status", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/disputes")
    public ResponseEntity<Map<String, Object>> getIngestedDisputes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Page<Dispute> disputes = disputeRepository.findByReasonCodeIn(
                    reasonCodeRulesService.getSupportedReasonCodes(), PageRequest.of(page, size));

            Map<String, Object> response = new HashMap<>();
            response.put("disputes", disputes.getContent());
            response.put("totalElements", disputes.getTotalElements());
            response.put("totalPages", disputes.getTotalPages());
            response.put("currentPage", disputes.getNumber());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get ingested disputes", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/queues")
    public ResponseEntity<List<String>> getAvailableQueues() {
        try {
            List<String> queues = claimIngestionService.getAvailableQueues();
            return ResponseEntity.ok(queues);
        } catch (Exception e) {
            log.error("Failed to get available queues", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @Transactional
    @DeleteMapping("/cleanup-api-cases")
    public ResponseEntity<Map<String, Object>> cleanupApiCases() {
        try {
            List<Dispute> allDisputes = disputeRepository.findAll();
            List<Long> toDelete = new ArrayList<>();
            List<String> deletedClaimIds = new ArrayList<>();

            for (Dispute d : allDisputes) {
                if (d.getClaimId() != null && !d.getClaimId().startsWith(FABRICATED_CLAIM_PREFIX)) {
                    toDelete.add(d.getId());
                    deletedClaimIds.add(d.getClaimId());
                }
            }

            if (toDelete.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "deleted", 0,
                        "message", "No API-fetched cases found. Only fabricated cases (claim IDs starting with " + FABRICATED_CLAIM_PREFIX + ") exist."
                ));
            }

            int cbCount = 0, retCount = 0, feeCount = 0, cfCount = 0, emCount = 0;
            for (Long id : toDelete) {
                cbCount += chargebackDetailRepository.findByDisputeId(id).size();
                retCount += retrievalDetailRepository.findByDisputeId(id).size();
                feeCount += feeDetailRepository.findByDisputeId(id).size();
                cfCount += caseFilingDetailRepository.findByDisputeId(id).size();
                if (evidenceMapRepository.findByDisputeId(id).isPresent()) emCount++;

                chargebackDetailRepository.deleteByDisputeId(id);
                retrievalDetailRepository.deleteByDisputeId(id);
                feeDetailRepository.deleteByDisputeId(id);
                caseFilingDetailRepository.deleteByDisputeId(id);
                evidenceMapRepository.findByDisputeId(id).ifPresent(evidenceMapRepository::delete);
            }

            disputeRepository.deleteAllById(toDelete);

            log.info("Cleaned up {} API-fetched cases (chargebacks={}, retrievals={}, fees={}, filings={}, evidenceMaps={})",
                    toDelete.size(), cbCount, retCount, feeCount, cfCount, emCount);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("deleted", toDelete.size());
            response.put("deletedClaimIds", deletedClaimIds);
            response.put("subEntitiesDeleted", Map.of(
                    "chargebackDetails", cbCount,
                    "retrievalDetails", retCount,
                    "feeDetails", feeCount,
                    "caseFilingDetails", cfCount,
                    "evidenceMaps", emCount
            ));
            response.put("fabricatedCasesRemaining", allDisputes.size() - toDelete.size());
            response.put("message", "All API-fetched cases deleted. Fabricated cases (claim IDs starting with " + FABRICATED_CLAIM_PREFIX + ") preserved.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Cleanup of API cases failed", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
