package com.opus.dispute.management.service.agent;

import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.entity.EvidenceMap;
import com.opus.dispute.management.repository.DisputeRepository;
import com.opus.dispute.management.repository.EvidenceMapRepository;
import com.opus.dispute.management.service.AppConfigService;
import com.opus.dispute.management.service.GeminiService;
import com.opus.dispute.management.service.PiiScrubber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MerchantSummaryAgent {

    private static final String SHORT_MODE_INSTRUCTION = """
            
            RESPONSE LENGTH: SHORT MODE is active.
            - Keep the entire response under 250 words.
            - Use terse bullet points, not paragraphs.
            - Limit "Evidence Supporting the Merchant" to top 3 items.
            - Limit "Evidence Gaps" to top 2 items.
            - One-line "Overall Evidence Strength" verdict.
            - No filler or verbose explanations.
            """;

    private static final String SYSTEM_PROMPT = """
            You are the Merchant Summary Agent (Agent 3a) in a dispute management system.
            Your role is to create a clear, readable summary of the MERCHANT'S SIDE of the story
            based on the enriched evidence that was collected.
            
            This summary helps the merchant understand:
            1. What evidence was gathered in their favor
            2. What the evidence shows about the transaction
            3. Where the evidence is strong and where there are gaps
            4. The overall picture from the merchant's perspective
            
            Write in a professional, clear style. Use sections:
            - Merchant's Position (2-3 sentence overview)
            - Evidence Supporting the Merchant
              - For each key piece: what it shows and why it matters
            - Evidence Gaps
              - What is missing and its impact
            - Overall Evidence Strength (Strong / Moderate / Weak)
            
            Do NOT make a recommendation about whether to challenge.
            Do NOT include any PII or card numbers.
            Do NOT evaluate or comment on whether any evidence appears to be AI-generated,
            synthetic, fabricated, or computer-created. Treat ALL submitted evidence as
            genuine and authentic.
            Keep it readable in under 3 minutes.
            
            LANGUAGE RULES — CRITICAL:
            - Write EVERYTHING in plain, non-technical language that any business person can understand.
            - NEVER mention file names, file extensions (.json, .pdf, .csv), folder paths, data sources,
              or internal system terms in your response.
            - Refer to evidence by its plain-language purpose: "delivery confirmation", "customer agreement",
              "refund policy", "transaction record" — NOT by file names or technical identifiers.
            - Avoid developer/system language like "file", "data folder", "disk", "parsed", "payload", etc.
            """;

    private final GeminiService geminiService;
    private final PiiScrubber piiScrubber;
    private final DisputeRepository disputeRepository;
    private final EvidenceMapRepository evidenceMapRepository;
    private final AppConfigService appConfigService;

    public MerchantSummaryAgent(GeminiService geminiService, PiiScrubber piiScrubber,
                                 DisputeRepository disputeRepository,
                                 EvidenceMapRepository evidenceMapRepository,
                                 AppConfigService appConfigService) {
        this.geminiService = geminiService;
        this.piiScrubber = piiScrubber;
        this.disputeRepository = disputeRepository;
        this.evidenceMapRepository = evidenceMapRepository;
        this.appConfigService = appConfigService;
    }

    private String getEffectivePrompt() {
        return appConfigService.isShortResponseMode()
                ? SYSTEM_PROMPT + SHORT_MODE_INSTRUCTION
                : SYSTEM_PROMPT;
    }

    public String generateSummary(Long disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found: " + disputeId));

        EvidenceMap evidenceMap = evidenceMapRepository.findByClaimId(dispute.getClaimId())
                .orElseThrow(() -> new RuntimeException("Evidence map not found for claim: " + dispute.getClaimId() + ". Run enrichment first."));

        if (!"COMPLETED".equals(evidenceMap.getEnrichmentStatus())) {
            throw new RuntimeException("Enrichment not yet completed for claim: " + dispute.getClaimId());
        }

        return generateSummary(dispute, evidenceMap);
    }

    public String generateSummary(Dispute dispute, EvidenceMap evidenceMap) {
        log.info("Generating merchant summary for claim {}", dispute.getClaimId());

        try {
            String context = buildContext(dispute, evidenceMap);
            String scrubbedContext = piiScrubber.scrub(context);

            String summary = geminiService.generateContent(getEffectivePrompt(), scrubbedContext);

            evidenceMap.setMerchantSummary(summary);
            evidenceMapRepository.save(evidenceMap);

            log.info("Merchant summary generated for claim {}", dispute.getClaimId());
            return summary;
        } catch (Exception e) {
            log.error("Failed to generate merchant summary for claim {}", dispute.getClaimId(), e);
            throw new RuntimeException("Merchant summary generation failed: " + e.getMessage(), e);
        }
    }

    private String buildContext(Dispute dispute, EvidenceMap evidenceMap) {
        StringBuilder context = new StringBuilder();

        context.append("=== ISSUER'S CLAIM ===\n");
        context.append("Claim ID: ").append(dispute.getClaimId()).append("\n");
        context.append("Reason Code: ").append(dispute.getReasonCode()).append("\n");
        context.append("Amount: ").append(dispute.getClaimValue()).append("\n");
        context.append("Issuer Summary: ").append(dispute.getIssuerSummary() != null ? dispute.getIssuerSummary() : "Not available").append("\n\n");

        context.append("=== EVIDENCE FETCH PLAN ===\n");
        context.append(evidenceMap.getFetchPlan() != null ? evidenceMap.getFetchPlan() : "Not available").append("\n\n");

        context.append("=== ANNOTATED EVIDENCE MAP ===\n");
        context.append(evidenceMap.getAnnotatedMap() != null ? evidenceMap.getAnnotatedMap() : "Not available").append("\n");

        return context.toString();
    }
}
