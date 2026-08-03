package com.opus.dispute.management.controller;

import com.opus.dispute.management.entity.EvidenceMap;
import com.opus.dispute.management.repository.EvidenceMapRepository;
import com.opus.dispute.management.service.DataSourceService;
import com.opus.dispute.management.service.EvidenceUploadService;
import com.opus.dispute.management.service.GeminiService;
import com.opus.dispute.management.service.IssuerEvidenceGenerator;
import com.opus.dispute.management.service.StripeEvidenceService;
import com.opus.dispute.management.service.SummarizationQueueService;
import com.opus.dispute.management.repository.DisputeRepository;
import com.opus.dispute.management.service.agent.AnalyticsInsightAgent;
import com.opus.dispute.management.service.agent.CaseSummarizerAgent;
import com.opus.dispute.management.service.agent.ChallengeAdvisorAgent;
import com.opus.dispute.management.service.agent.EvidenceStrategistAgent;
import com.opus.dispute.management.service.agent.MerchantSummaryAgent;
import com.opus.dispute.management.service.agent.PolicyIntelligenceAgent;
import com.opus.dispute.management.service.agent.RebuttalArchitectAgent;
import com.opus.dispute.management.service.PolicyDocumentService;
import com.opus.dispute.management.entity.PolicyDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final CaseSummarizerAgent caseSummarizerAgent;
    private final EvidenceStrategistAgent evidenceStrategistAgent;
    private final MerchantSummaryAgent merchantSummaryAgent;
    private final ChallengeAdvisorAgent challengeAdvisorAgent;
    private final RebuttalArchitectAgent rebuttalArchitectAgent;
    private final PolicyIntelligenceAgent policyIntelligenceAgent;
    private final AnalyticsInsightAgent analyticsInsightAgent;
    private final EvidenceMapRepository evidenceMapRepository;
    private final DisputeRepository disputeRepository;
    private final DataSourceService dataSourceService;
    private final StripeEvidenceService stripeEvidenceService;
    private final GeminiService geminiService;
    private final SummarizationQueueService summarizationQueueService;
    private final IssuerEvidenceGenerator issuerEvidenceGenerator;
    private final EvidenceUploadService evidenceUploadService;
    private final PolicyDocumentService policyDocumentService;

    public AgentController(CaseSummarizerAgent caseSummarizerAgent,
                            EvidenceStrategistAgent evidenceStrategistAgent,
                            MerchantSummaryAgent merchantSummaryAgent,
                            ChallengeAdvisorAgent challengeAdvisorAgent,
                            RebuttalArchitectAgent rebuttalArchitectAgent,
                            PolicyIntelligenceAgent policyIntelligenceAgent,
                            AnalyticsInsightAgent analyticsInsightAgent,
                            EvidenceMapRepository evidenceMapRepository,
                            DisputeRepository disputeRepository,
                            DataSourceService dataSourceService,
                            StripeEvidenceService stripeEvidenceService,
                            GeminiService geminiService,
                            SummarizationQueueService summarizationQueueService,
                            IssuerEvidenceGenerator issuerEvidenceGenerator,
                            EvidenceUploadService evidenceUploadService,
                            PolicyDocumentService policyDocumentService) {
        this.caseSummarizerAgent = caseSummarizerAgent;
        this.evidenceStrategistAgent = evidenceStrategistAgent;
        this.merchantSummaryAgent = merchantSummaryAgent;
        this.challengeAdvisorAgent = challengeAdvisorAgent;
        this.rebuttalArchitectAgent = rebuttalArchitectAgent;
        this.policyIntelligenceAgent = policyIntelligenceAgent;
        this.analyticsInsightAgent = analyticsInsightAgent;
        this.evidenceMapRepository = evidenceMapRepository;
        this.disputeRepository = disputeRepository;
        this.dataSourceService = dataSourceService;
        this.stripeEvidenceService = stripeEvidenceService;
        this.geminiService = geminiService;
        this.issuerEvidenceGenerator = issuerEvidenceGenerator;
        this.summarizationQueueService = summarizationQueueService;
        this.evidenceUploadService = evidenceUploadService;
        this.policyDocumentService = policyDocumentService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAgentStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("geminiAvailable", geminiService.isAvailable());
        status.put("agents", Map.of(
                "agent1_caseSummarizer", "ACTIVE",
                "agent2_evidenceStrategist", "ACTIVE",
                "agent3a_merchantSummary", "ACTIVE",
                "agent3b_rebuttalArchitect", "ACTIVE",
                "agent4_challengeAdvisor", "ACTIVE",
                "agent5_policyIntelligence", "ACTIVE",
                "agent6_analyticsInsight", "ACTIVE"
        ));
        return ResponseEntity.ok(status);
    }

    @PostMapping("/summarize/{disputeId}")
    public ResponseEntity<?> summarizeCase(@PathVariable Long disputeId) {
        try {
            log.info("Agent 1: Queuing summarization for dispute {}", disputeId);
            SummarizationQueueService.JobStatus job = summarizationQueueService.enqueue(disputeId);
            Map<String, Object> response = new HashMap<>();
            response.put("agent", "CaseSummarizer");
            response.put("disputeId", disputeId);
            response.put("status", job.getStatus());
            response.put("queuedAt", job.getQueuedAt() != null ? job.getQueuedAt().toString() : null);
            response.put("message", "Summarization request queued. Poll GET /api/agents/summarize/status/" + disputeId + " for result.");
            return ResponseEntity.accepted().body(response);
        } catch (Exception e) {
            log.error("Agent 1 failed to queue dispute {}", disputeId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/summarize/batch")
    public ResponseEntity<?> summarizeBatch(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> disputeIds = request.get("disputeIds");
            if (disputeIds == null || disputeIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "'disputeIds' list is required"));
            }
            log.info("Agent 1: Queuing batch summarization for {} disputes", disputeIds.size());
            List<SummarizationQueueService.JobStatus> jobs = summarizationQueueService.enqueueBatch(disputeIds);

            List<Map<String, Object>> jobResponses = new ArrayList<>();
            for (SummarizationQueueService.JobStatus job : jobs) {
                Map<String, Object> jr = new HashMap<>();
                jr.put("disputeId", job.getDisputeId());
                jr.put("status", job.getStatus());
                jr.put("queuedAt", job.getQueuedAt() != null ? job.getQueuedAt().toString() : null);
                jobResponses.add(jr);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("agent", "CaseSummarizer");
            response.put("totalQueued", disputeIds.size());
            response.put("jobs", jobResponses);
            response.put("message", "All requests queued. Poll GET /api/agents/summarize/queue for overall status or GET /api/agents/summarize/status/{disputeId} per dispute.");
            return ResponseEntity.accepted().body(response);
        } catch (Exception e) {
            log.error("Agent 1 batch queueing failed", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/summarize/status/{disputeId}")
    public ResponseEntity<?> getSummarizationStatus(@PathVariable Long disputeId) {
        SummarizationQueueService.JobStatus job = summarizationQueueService.getJobStatus(disputeId);
        if (job == null) {
            return ResponseEntity.status(404).body(Map.of("error", "No summarization job found for dispute " + disputeId));
        }
        Map<String, Object> response = new HashMap<>();
        response.put("disputeId", job.getDisputeId());
        response.put("status", job.getStatus());
        response.put("queuedAt", job.getQueuedAt() != null ? job.getQueuedAt().toString() : null);
        response.put("startedAt", job.getStartedAt() != null ? job.getStartedAt().toString() : null);
        response.put("completedAt", job.getCompletedAt() != null ? job.getCompletedAt().toString() : null);
        if ("COMPLETED".equals(job.getStatus()) && job.getResult() != null) {
            response.put("summary", job.getResult());
        }
        if ("FAILED".equals(job.getStatus()) && job.getError() != null) {
            response.put("error", job.getError());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summarize/queue")
    public ResponseEntity<?> getSummarizationQueueStatus() {
        return ResponseEntity.ok(summarizationQueueService.getQueueStatus());
    }

    @PostMapping("/enrich/{disputeId}")
    public ResponseEntity<?> enrichEvidence(@PathVariable Long disputeId) {
        try {
            log.info("Agent 2: Enriching evidence for dispute {}", disputeId);
            EvidenceMap evidenceMap = evidenceStrategistAgent.enrichEvidence(disputeId);
            return ResponseEntity.ok(Map.of(
                    "agent", "EvidenceStrategist",
                    "disputeId", disputeId,
                    "claimId", evidenceMap.getClaimId(),
                    "enrichmentStatus", evidenceMap.getEnrichmentStatus(),
                    "fetchPlan", evidenceMap.getFetchPlan() != null ? evidenceMap.getFetchPlan() : "",
                    "annotatedMap", evidenceMap.getAnnotatedMap() != null ? evidenceMap.getAnnotatedMap() : ""
            ));
        } catch (Exception e) {
            log.error("Agent 2 failed for dispute {}", disputeId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/merchant-summary/{disputeId}")
    public ResponseEntity<?> generateMerchantSummary(@PathVariable Long disputeId) {
        try {
            log.info("Agent 3a: Generating merchant summary for dispute {}", disputeId);
            String summary = merchantSummaryAgent.generateSummary(disputeId);
            return ResponseEntity.ok(Map.of(
                    "agent", "MerchantSummary",
                    "disputeId", disputeId,
                    "merchantSummary", summary
            ));
        } catch (Exception e) {
            log.error("Agent 3a failed for dispute {}", disputeId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/recommend/{disputeId}")
    public ResponseEntity<?> getRecommendation(@PathVariable Long disputeId) {
        try {
            log.info("Agent 4: Generating recommendation for dispute {}", disputeId);
            String recommendation = challengeAdvisorAgent.recommend(disputeId);
            return ResponseEntity.ok(Map.of(
                    "agent", "ChallengeAdvisor",
                    "disputeId", disputeId,
                    "recommendation", recommendation
            ));
        } catch (Exception e) {
            log.error("Agent 4 failed for dispute {}", disputeId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/rebuttal/{disputeId}")
    public ResponseEntity<?> generateRebuttal(@PathVariable Long disputeId,
                                               @RequestBody(required = false) Map<String, String> body) {
        try {
            String userInstructions = (body != null) ? body.get("userInstructions") : null;
            log.info("Agent 3b: Generating rebuttal for dispute {}{}", disputeId,
                    (userInstructions != null && !userInstructions.isBlank()) ? " with user instructions" : "");
            String rebuttal = rebuttalArchitectAgent.generateRebuttal(disputeId, userInstructions);
            return ResponseEntity.ok(Map.of(
                    "agent", "RebuttalArchitect",
                    "disputeId", disputeId,
                    "rebuttalDocument", rebuttal
            ));
        } catch (Exception e) {
            log.error("Agent 3b failed for dispute {}", disputeId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/full-pipeline/{disputeId}")
    public ResponseEntity<?> runFullPipeline(@PathVariable Long disputeId) {
        try {
            log.info("Running full agent pipeline for dispute {}", disputeId);
            Map<String, Object> results = new HashMap<>();

            String issuerSummary = caseSummarizerAgent.summarize(disputeId);
            results.put("step1_issuerSummary", issuerSummary != null ? issuerSummary : "Unavailable");

            EvidenceMap evidenceMap = evidenceStrategistAgent.enrichEvidence(disputeId);
            results.put("step2_fetchPlan", evidenceMap.getFetchPlan());
            results.put("step2_annotatedMap", evidenceMap.getAnnotatedMap());

            String merchantSummary = merchantSummaryAgent.generateSummary(disputeId);
            results.put("step3a_merchantSummary", merchantSummary);

            String recommendation = challengeAdvisorAgent.recommend(disputeId);
            results.put("step4_recommendation", recommendation);

            results.put("disputeId", disputeId);
            results.put("claimId", evidenceMap.getClaimId());
            results.put("pipelineStatus", "COMPLETED");
            results.put("note", "Rebuttal generation (step 3b) requires merchant confirmation. Call POST /api/agents/rebuttal/" + disputeId + " when ready.");

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Full pipeline failed for dispute {}", disputeId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/evidence-map/{disputeId}")
    public ResponseEntity<?> getEvidenceMap(@PathVariable Long disputeId) {
        try {
            EvidenceMap evidenceMap = evidenceMapRepository.findByDisputeId(disputeId)
                    .orElse(null);
            if (evidenceMap == null) {
                return ResponseEntity.status(404).body(Map.of("error", "No evidence map found for dispute " + disputeId));
            }
            return ResponseEntity.ok(evidenceMap);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/policy/diff")
    public ResponseEntity<?> analyzePolicyDiff(@RequestBody Map<String, String> request) {
        try {
            String previousPolicy = request.get("previousPolicy");
            String newPolicy = request.get("newPolicy");

            if (previousPolicy == null || newPolicy == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Both 'previousPolicy' and 'newPolicy' are required"));
            }

            log.info("Agent 5: Analyzing policy diff");
            String analysis = policyIntelligenceAgent.analyzePolicyDiff(previousPolicy, newPolicy);
            return ResponseEntity.ok(Map.of(
                    "agent", "PolicyIntelligence",
                    "type", "diff",
                    "analysis", analysis
            ));
        } catch (Exception e) {
            log.error("Agent 5 (diff) failed", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/policy/analyze")
    public ResponseEntity<?> analyzePolicy(@RequestBody Map<String, String> request) {
        try {
            String policyDocument = request.get("policyDocument");
            String networkName = request.getOrDefault("networkName", "Mastercard");

            if (policyDocument == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "'policyDocument' is required"));
            }

            log.info("Agent 5: Analyzing {} policy", networkName);
            String analysis = policyIntelligenceAgent.analyzePolicy(policyDocument, networkName);
            return ResponseEntity.ok(Map.of(
                    "agent", "PolicyIntelligence",
                    "type", "analysis",
                    "networkName", networkName,
                    "analysis", analysis
            ));
        } catch (Exception e) {
            log.error("Agent 5 (analyze) failed", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/policy/merchant-analyze")
    public ResponseEntity<?> analyzeMerchantPolicy(@RequestBody Map<String, String> request) {
        try {
            String policyDocument = request.get("policyDocument");

            if (policyDocument == null || policyDocument.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "'policyDocument' is required"));
            }

            log.info("Agent 5: Analyzing merchant policy for settings recommendations");
            String rawJson = policyIntelligenceAgent.analyzeMerchantPolicy(policyDocument);

            Object parsed;
            try {
                parsed = new com.google.gson.Gson().fromJson(rawJson, Object.class);
            } catch (Exception parseEx) {
                log.warn("Could not parse merchant policy response as JSON, returning as string");
                parsed = rawJson;
            }

            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("agent", "PolicyIntelligence");
            response.put("type", "merchant-policy");
            response.put("recommendations", parsed);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Agent 5 (merchant policy) failed", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/policy/merchant/upload")
    public ResponseEntity<?> uploadMerchantPolicy(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }
            log.info("Agent 5: Uploading merchant policy file: {}", file.getOriginalFilename());
            Map<String, Object> result = policyDocumentService.uploadMerchantPolicy(file);
            result.put("agent", "PolicyIntelligence");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Agent 5 (merchant upload) failed", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/policy/network/upload")
    public ResponseEntity<?> uploadNetworkPolicy(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "networkName", defaultValue = "Mastercard") String networkName) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }
            log.info("Agent 5: Uploading {} network policy file: {}", networkName, file.getOriginalFilename());
            Map<String, Object> result = policyDocumentService.uploadNetworkPolicy(file, networkName);
            result.put("agent", "PolicyIntelligence");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Agent 5 (network upload) failed", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/policy/merchant/history")
    public ResponseEntity<?> getMerchantPolicyHistory() {
        return ResponseEntity.ok(Map.of(
                "policyType", "MERCHANT",
                "versions", policyDocumentService.getMerchantHistory()
        ));
    }

    @GetMapping("/policy/networks")
    public ResponseEntity<?> getNetworkNames() {
        return ResponseEntity.ok(Map.of(
                "networks", policyDocumentService.getDistinctNetworkNames()
        ));
    }

    @GetMapping("/policy/network/history")
    public ResponseEntity<?> getNetworkPolicyHistory(
            @RequestParam(value = "networkName", defaultValue = "Mastercard") String networkName) {
        return ResponseEntity.ok(Map.of(
                "policyType", "NETWORK",
                "networkName", networkName,
                "versions", policyDocumentService.getNetworkHistory(networkName)
        ));
    }

    @GetMapping("/policy/merchant/latest")
    public ResponseEntity<?> getMerchantPolicyLatest() {
        return policyDocumentService.getMerchantLatest()
                .map(doc -> ResponseEntity.ok(buildPolicyDetailResponse(doc)))
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of("error", "No merchant policy uploaded yet")));
    }

    @GetMapping("/policy/network/latest")
    public ResponseEntity<?> getNetworkPolicyLatest(
            @RequestParam(value = "networkName", defaultValue = "Mastercard") String networkName) {
        return policyDocumentService.getNetworkLatest(networkName)
                .map(doc -> ResponseEntity.ok(buildPolicyDetailResponse(doc)))
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of("error", "No " + networkName + " network policy uploaded yet")));
    }

    @GetMapping("/policy/{id}")
    public ResponseEntity<?> getPolicyById(@PathVariable Long id) {
        return policyDocumentService.getById(id)
                .map(doc -> ResponseEntity.ok(buildPolicyDetailResponse(doc)))
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(Map.of("error", "Policy document not found: " + id)));
    }

    private Map<String, Object> buildPolicyDetailResponse(PolicyDocument doc) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", doc.getId());
        response.put("policyType", doc.getPolicyType());
        response.put("version", doc.getVersion());
        response.put("filename", doc.getFilename());
        response.put("networkName", doc.getNetworkName());
        response.put("contentLength", doc.getContent().length());
        response.put("content", doc.getContent());
        response.put("uploadedAt", doc.getUploadedAt().toString());

        if (doc.getDiffSummary() != null) {
            response.put("diffSummary", doc.getDiffSummary());
        }

        if (doc.getSettingsRecommendations() != null) {
            Object parsed;
            try {
                parsed = new com.google.gson.Gson().fromJson(doc.getSettingsRecommendations(), Object.class);
            } catch (Exception e) {
                parsed = doc.getSettingsRecommendations();
            }
            response.put("settingsRecommendations", parsed);
        }

        return response;
    }

    @GetMapping("/analytics/insights")
    public ResponseEntity<?> getAnalyticsInsights() {
        try {
            log.info("Agent 6: Generating analytics insights");
            String insights = analyticsInsightAgent.generateInsights();
            return ResponseEntity.ok(Map.of(
                    "agent", "AnalyticsInsight",
                    "insights", insights
            ));
        } catch (Exception e) {
            log.error("Agent 6 failed", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/analytics/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            Map<String, Object> stats = analyticsInsightAgent.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stripe/status")
    public ResponseEntity<?> getStripeStatus() {
        return ResponseEntity.ok(Map.of(
                "configured", stripeEvidenceService.isConfigured(),
                "message", stripeEvidenceService.isConfigured()
                        ? "Stripe API key is configured. Set stripeDisputeId or stripeChargeId on a dispute, then run enrichment."
                        : "Stripe API key not configured. Set STRIPE_SECRET_KEY environment variable."
        ));
    }

    @PostMapping("/stripe/evidence/{disputeId}")
    public ResponseEntity<?> fetchStripeEvidence(@PathVariable Long disputeId) {
        if (!stripeEvidenceService.isConfigured()) {
            return ResponseEntity.status(503).body(Map.of("error", "Stripe API key not configured"));
        }

        return disputeRepository.findById(disputeId)
                .map(dispute -> {
                    String stripeId = dispute.getStripeDisputeId();
                    String chargeId = dispute.getStripeChargeId();

                    String piId = dispute.getStripePaymentIntentId();

                    if ((stripeId == null || stripeId.isBlank()) && (chargeId == null || chargeId.isBlank()) && (piId == null || piId.isBlank())) {
                        return ResponseEntity.badRequest().body(Map.of(
                                "error", "No Stripe IDs set on this dispute. Use PATCH /api/disputes/" + disputeId
                                        + " to set stripeDisputeId, stripeChargeId, or stripePaymentIntentId first."
                        ));
                    }

                    try {
                        int caseNum = dataSourceService.extractCaseNumber(dispute.getId(), dispute.getClaimId(), dispute.getCaseNumber());
                        if (caseNum < 0) caseNum = dispute.getId().intValue();

                        Map<String, String> evidence;
                        if (stripeId != null && !stripeId.isBlank()) {
                            evidence = stripeEvidenceService.buildEvidenceFromDispute(stripeId, caseNum);
                        } else if (chargeId != null && !chargeId.isBlank()) {
                            evidence = stripeEvidenceService.buildEvidenceFromCharge(chargeId, caseNum);
                        } else {
                            evidence = stripeEvidenceService.buildEvidenceFromPaymentIntent(piId, caseNum);
                        }

                        Map<String, Object> response = new java.util.LinkedHashMap<>();
                        response.put("disputeId", disputeId);
                        response.put("stripeDisputeId", stripeId);
                        response.put("stripeChargeId", chargeId);
                        response.put("evidenceCount", evidence.size());
                        response.put("categories", evidence.keySet());
                        response.put("evidence", evidence);
                        return ResponseEntity.ok(response);
                    } catch (Exception e) {
                        return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch Stripe evidence: " + e.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Dispute not found: " + disputeId)));
    }

    @GetMapping(value = "/reason-codes", produces = "application/json")
    public ResponseEntity<?> getReasonCodeRules() {
        String rules = dataSourceService.loadReasonCodeRules();
        if (rules == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Reason code rules file not found"));
        }
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(rules);
    }

    @GetMapping(value = "/reason-codes/{code}", produces = "application/json")
    public ResponseEntity<?> getReasonCodeRule(@PathVariable String code) {
        com.google.gson.JsonObject rule = dataSourceService.getReasonCodeRule(code);
        if (rule == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Reason code not found: " + code));
        }
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(rule.toString());
    }

    @PostMapping("/generate-issuer-evidence/{disputeId}")
    public ResponseEntity<?> generateIssuerEvidence(@PathVariable Long disputeId) {
        try {
            Map<String, Object> result = issuerEvidenceGenerator.generateForCase(disputeId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/generate-issuer-evidence-all")
    public ResponseEntity<?> generateAllIssuerEvidence() {
        try {
            Map<String, Object> result = issuerEvidenceGenerator.generateForAllLocalCases();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/evidence-gaps/{disputeId}")
    public ResponseEntity<?> getEvidenceGaps(@PathVariable Long disputeId) {
        try {
            Map<String, Object> gaps = evidenceUploadService.getEvidenceGaps(disputeId);
            return ResponseEntity.ok(gaps);
        } catch (Exception e) {
            log.error("Failed to get evidence gaps for dispute {}", disputeId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/upload-evidence/{disputeId}")
    public ResponseEntity<?> uploadEvidence(
            @PathVariable Long disputeId,
            @RequestParam("category") String category,
            @RequestParam("evidenceName") String evidenceName,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "reRunAgents", defaultValue = "true") boolean reRunAgents) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }
            log.info("Evidence upload for dispute {}: category={}, name={}, size={} bytes, reRun={}",
                    disputeId, category, evidenceName, file.getSize(), reRunAgents);
            Map<String, Object> result = evidenceUploadService.uploadEvidence(
                    disputeId, category, evidenceName, file, reRunAgents);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Evidence upload failed for dispute {}", disputeId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/upload-evidence-batch/{disputeId}")
    public ResponseEntity<?> uploadMultipleEvidence(
            @PathVariable Long disputeId,
            @RequestParam("categories") String categoriesCsv,
            @RequestParam("evidenceNames") String evidenceNamesCsv,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "reRunAgents", defaultValue = "true") boolean reRunAgents) {
        try {
            String[] categories = categoriesCsv.split(",");
            String[] evidenceNames = evidenceNamesCsv.split(",");

            if (categories.length != files.size() || evidenceNames.length != files.size()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Number of categories, evidenceNames, and files must match. " +
                                "Got " + categories.length + " categories, " +
                                evidenceNames.length + " names, " + files.size() + " files."
                ));
            }

            List<EvidenceUploadService.UploadItem> items = new ArrayList<>();
            for (int i = 0; i < files.size(); i++) {
                items.add(new EvidenceUploadService.UploadItem(
                        categories[i].trim(), evidenceNames[i].trim(), files.get(i)));
            }

            Map<String, Object> result = evidenceUploadService.uploadMultipleEvidence(
                    disputeId, items, reRunAgents);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Batch evidence upload failed for dispute {}", disputeId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reassess/{disputeId}")
    public ResponseEntity<?> reassessAfterUpload(@PathVariable Long disputeId) {
        try {
            log.info("Reassessing dispute {} after evidence changes", disputeId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("disputeId", disputeId);

            EvidenceMap updatedMap = evidenceStrategistAgent.enrichEvidence(disputeId);
            result.put("reEnrichmentStatus", "COMPLETED");
            result.put("enrichmentCompletedAt", updatedMap.getEnrichmentCompletedAt().toString());

            String recommendation = challengeAdvisorAgent.recommend(disputeId);
            result.put("reRecommendationStatus", "COMPLETED");
            result.put("updatedRecommendation", recommendation);
            result.put("updatedRecommendationStrength", updatedMap.getRecommendationStrength());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Reassessment failed for dispute {}", disputeId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
