package com.opus.dispute.management.service;

import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.repository.DisputeRepository;
import com.opus.dispute.management.service.agent.CaseSummarizerAgent;
import com.opus.dispute.management.service.agent.ChallengeAdvisorAgent;
import com.opus.dispute.management.service.agent.EvidenceStrategistAgent;
import com.opus.dispute.management.service.agent.MerchantSummaryAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PostIngestionPipelineService {

    private final AppConfigService configService;
    private final AutoAcceptService autoAcceptService;
    private final AcceptanceStatusResolver acceptanceStatusResolver;
    private final DisputeRepository disputeRepository;
    private final CaseSummarizerAgent caseSummarizerAgent;
    private final EvidenceStrategistAgent evidenceStrategistAgent;
    private final MerchantSummaryAgent merchantSummaryAgent;
    private final ChallengeAdvisorAgent challengeAdvisorAgent;

    public PostIngestionPipelineService(AppConfigService configService,
                                         AutoAcceptService autoAcceptService,
                                         AcceptanceStatusResolver acceptanceStatusResolver,
                                         DisputeRepository disputeRepository,
                                         CaseSummarizerAgent caseSummarizerAgent,
                                         EvidenceStrategistAgent evidenceStrategistAgent,
                                         MerchantSummaryAgent merchantSummaryAgent,
                                         ChallengeAdvisorAgent challengeAdvisorAgent) {
        this.configService = configService;
        this.autoAcceptService = autoAcceptService;
        this.acceptanceStatusResolver = acceptanceStatusResolver;
        this.disputeRepository = disputeRepository;
        this.caseSummarizerAgent = caseSummarizerAgent;
        this.evidenceStrategistAgent = evidenceStrategistAgent;
        this.merchantSummaryAgent = merchantSummaryAgent;
        this.challengeAdvisorAgent = challengeAdvisorAgent;
    }

    @Async
    public void runPostIngestionHook(List<Long> newDisputeIds) {
        if (newDisputeIds == null || newDisputeIds.isEmpty()) {
            return;
        }

        boolean autoEnrich = configService.isAutoEnrichEnabled();

        log.info("Post-ingestion hook: {} new disputes, auto-enrich={}", newDisputeIds.size(), autoEnrich);

        for (Long disputeId : newDisputeIds) {
            try {
                Dispute dispute = disputeRepository.findById(disputeId).orElse(null);
                if (dispute == null) {
                    log.warn("Post-ingestion: dispute {} not found, skipping", disputeId);
                    continue;
                }

                try {
                    caseSummarizerAgent.summarize(disputeId);
                    log.info("Phase 1 [{}]: Agent 1 (Case Summarizer) complete — summary pre-stored", disputeId);
                } catch (Exception e) {
                    log.error("Phase 1 [{}]: Agent 1 (Case Summarizer) failed: {}", disputeId, e.getMessage());
                }

                if (autoAcceptService.shouldAutoAccept(dispute)) {
                    dispute.setPreviousStatus(dispute.getStatus());
                    dispute.setPreviousAction(dispute.getAction());
                    String acceptStatus = acceptanceStatusResolver.resolve(dispute.getProgressState());
                    dispute.setStatus(acceptStatus);
                    dispute.setAction("Auto-accepted by rules engine");
                    disputeRepository.save(dispute);
                    log.info("Dispute {} auto-accepted with status '{}' (progressState={}), skipping Phase 2 enrichment",
                            disputeId, acceptStatus, dispute.getProgressState());
                    continue;
                }

                if (!autoEnrich) {
                    log.debug("Auto-enrich disabled, dispute {} ready for manual Phase 2 enrichment", disputeId);
                    continue;
                }

                log.info("Phase 2: Auto-enriching dispute {} via agents 2→3a→4", disputeId);
                dispute.setStatus("INITIATED");
                disputeRepository.save(dispute);

                try {
                    evidenceStrategistAgent.enrichEvidence(disputeId);
                    log.info("Phase 2 [{}]: Agent 2 (Evidence Strategist) complete", disputeId);

                    merchantSummaryAgent.generateSummary(disputeId);
                    log.info("Phase 2 [{}]: Agent 3a (Merchant Summary) complete", disputeId);

                    challengeAdvisorAgent.recommend(disputeId);
                    log.info("Phase 2 [{}]: Agent 4 (Challenge Advisor) complete", disputeId);

                    dispute = disputeRepository.findById(disputeId).orElse(dispute);
                    dispute.setStatus("ENRICHED");
                    disputeRepository.save(dispute);
                    log.info("Phase 2 complete for dispute {}", disputeId);

                } catch (Exception e) {
                    log.error("Phase 2 failed for dispute {}: {}", disputeId, e.getMessage());
                    dispute = disputeRepository.findById(disputeId).orElse(dispute);
                    dispute.setStatus("MISSING_DATA");
                    dispute.setAction("Phase 2 enrichment failed: " + e.getMessage());
                    disputeRepository.save(dispute);
                }

            } catch (Exception e) {
                log.error("Post-ingestion hook error for dispute {}: {}", disputeId, e.getMessage());
            }
        }

        log.info("Post-ingestion hook completed for {} disputes", newDisputeIds.size());
    }

}
