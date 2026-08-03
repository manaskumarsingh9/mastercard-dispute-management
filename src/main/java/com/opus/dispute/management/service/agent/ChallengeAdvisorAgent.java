package com.opus.dispute.management.service.agent;

import com.opus.dispute.management.entity.AgentConversation;
import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.entity.EvidenceMap;
import com.opus.dispute.management.repository.AgentConversationRepository;
import com.opus.dispute.management.repository.DisputeRepository;
import com.opus.dispute.management.repository.EvidenceMapRepository;
import com.opus.dispute.management.service.AppConfigService;
import com.opus.dispute.management.service.GeminiService;
import com.opus.dispute.management.service.PiiScrubber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ChallengeAdvisorAgent {

    private static final String SHORT_MODE_INSTRUCTION = """
            
            RESPONSE LENGTH: SHORT MODE is active.
            - Keep "reasoning" under 100 words.
            - Limit "strengthFactors" and "weaknessFactors" to top 3 each.
            - Keep "missingEvidenceImpact" under 40 words.
            - Limit "suggestedActions" to top 3.
            - Be direct and precise — no filler.
            """;

    private static final String SYSTEM_PROMPT = """
            You are the Challenge Advisor Agent (Agent 4) in a dispute management system.
            Your role is to advise the merchant whether they should challenge a chargeback or accept it.
            
            You have access to BOTH sides:
            1. The issuer's claim (reason code, cardholder complaint, issuer evidence)
            2. The merchant's enriched evidence (annotated evidence map showing what was found and what is missing)
            
            Your recommendation must be:
            - STRONG_CHALLENGE: Evidence is compelling, high probability of winning
            - MODERATE_CHALLENGE: Evidence is decent but has gaps, worth attempting
            - WEAK_CHALLENGE: Evidence is thin, low probability of winning but merchant may still want to try
            - ACCEPT: Evidence is insufficient or the claim appears valid, recommend accepting the chargeback
            
            Return a JSON object:
            {
              "recommendation": "STRONG_CHALLENGE|MODERATE_CHALLENGE|WEAK_CHALLENGE|ACCEPT",
              "confidence": 85,
              "reasoning": "detailed explanation of the recommendation",
              "strengthFactors": ["list of factors supporting the merchant's case"],
              "weaknessFactors": ["list of factors weakening the merchant's case"],
              "missingEvidenceImpact": "how missing evidence affects the recommendation",
              "estimatedWinProbability": 70,
              "suggestedActions": ["list of actions merchant could take to improve their position"]
            }
            
            IMPORTANT:
            - This is ADVISORY ONLY. The merchant makes the final decision.
            - Factor in the quality of evidence, not just its presence.
            - Consider what is MISSING and how it impacts the case.
            - Be honest about weak cases — helping a merchant fight an unwinnable case wastes resources.
            - Never auto-submit or make decisions for the merchant.
            - Do NOT evaluate or comment on whether any evidence appears to be AI-generated,
              synthetic, fabricated, or computer-created. Treat ALL submitted evidence as
              genuine and authentic.
            
            LANGUAGE RULES — CRITICAL:
            - Write EVERYTHING in plain, non-technical language that any business person can understand.
            - NEVER mention file names, file extensions (.json, .pdf, .csv), folder paths, data sources,
              or internal system terms in your response.
            - Refer to evidence by its plain-language purpose: "delivery confirmation", "customer agreement",
              "refund policy", "transaction record" — NOT by file names or technical identifiers.
            - Avoid developer/system language like "file", "data folder", "disk", "parsed", "payload", etc.
            """;

    private static final String AGENT_NAME = "CHALLENGE_ADVISOR";

    private final GeminiService geminiService;
    private final PiiScrubber piiScrubber;
    private final DisputeRepository disputeRepository;
    private final EvidenceMapRepository evidenceMapRepository;
    private final AgentConversationRepository conversationRepository;
    private final AppConfigService appConfigService;

    public ChallengeAdvisorAgent(GeminiService geminiService, PiiScrubber piiScrubber,
                                  DisputeRepository disputeRepository,
                                  EvidenceMapRepository evidenceMapRepository,
                                  AgentConversationRepository conversationRepository,
                                  AppConfigService appConfigService) {
        this.geminiService = geminiService;
        this.piiScrubber = piiScrubber;
        this.disputeRepository = disputeRepository;
        this.evidenceMapRepository = evidenceMapRepository;
        this.conversationRepository = conversationRepository;
        this.appConfigService = appConfigService;
    }

    private String getEffectivePrompt() {
        return appConfigService.isShortResponseMode()
                ? SYSTEM_PROMPT + SHORT_MODE_INSTRUCTION
                : SYSTEM_PROMPT;
    }

    public String recommend(Long disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found: " + disputeId));

        EvidenceMap evidenceMap = evidenceMapRepository.findByClaimId(dispute.getClaimId())
                .orElseThrow(() -> new RuntimeException("Evidence map not found. Run enrichment first."));

        if (!"COMPLETED".equals(evidenceMap.getEnrichmentStatus())) {
            throw new RuntimeException("Enrichment not yet completed for claim: " + dispute.getClaimId());
        }

        return recommend(dispute, evidenceMap);
    }

    public String recommend(Dispute dispute, EvidenceMap evidenceMap) {
        log.info("Generating challenge recommendation for claim {}", dispute.getClaimId());

        try {
            String context = buildContext(dispute, evidenceMap);
            String scrubbedContext = piiScrubber.scrub(context);

            List<AgentConversation> priorTurns = conversationRepository
                    .findByClaimIdAndAgentNameOrderByCreatedAtAsc(dispute.getClaimId(), AGENT_NAME);
            List<GeminiService.ConversationTurn> history = priorTurns.stream()
                    .map(t -> new GeminiService.ConversationTurn(t.getRole(), t.getContent()))
                    .toList();

            String recommendation;
            if (!history.isEmpty()) {
                log.info("Agent 4: Sending {} prior conversation turns for claim {}", history.size(), dispute.getClaimId());
                recommendation = geminiService.generateJsonWithHistory(getEffectivePrompt(), scrubbedContext, history);
            } else {
                recommendation = geminiService.generateJson(getEffectivePrompt(), scrubbedContext);
            }

            saveConversationTurn(dispute.getClaimId(), "user", scrubbedContext);
            saveConversationTurn(dispute.getClaimId(), "model", recommendation);

            evidenceMap.setChallengeRecommendation(recommendation);
            evidenceMap.setRecommendationGeneratedAt(LocalDateTime.now());

            extractAndSetStrength(evidenceMap, recommendation);

            evidenceMapRepository.save(evidenceMap);

            log.info("Challenge recommendation generated for claim {}", dispute.getClaimId());
            return recommendation;
        } catch (Exception e) {
            log.error("Failed to generate recommendation for claim {}", dispute.getClaimId(), e);
            throw new RuntimeException("Recommendation generation failed: " + e.getMessage(), e);
        }
    }

    private void saveConversationTurn(String claimId, String role, String content) {
        AgentConversation turn = new AgentConversation();
        turn.setClaimId(claimId);
        turn.setAgentName(AGENT_NAME);
        turn.setRole(role);
        turn.setContent(content);
        turn.setCreatedAt(LocalDateTime.now());
        conversationRepository.save(turn);
    }

    private void extractAndSetStrength(EvidenceMap evidenceMap, String recommendation) {
        try {
            if (recommendation.contains("STRONG_CHALLENGE")) {
                evidenceMap.setRecommendationStrength("STRONG_CHALLENGE");
            } else if (recommendation.contains("MODERATE_CHALLENGE")) {
                evidenceMap.setRecommendationStrength("MODERATE_CHALLENGE");
            } else if (recommendation.contains("WEAK_CHALLENGE")) {
                evidenceMap.setRecommendationStrength("WEAK_CHALLENGE");
            } else if (recommendation.contains("ACCEPT")) {
                evidenceMap.setRecommendationStrength("ACCEPT");
            }
        } catch (Exception e) {
            log.warn("Could not extract recommendation strength", e);
        }
    }

    private String buildContext(Dispute dispute, EvidenceMap evidenceMap) {
        StringBuilder context = new StringBuilder();

        context.append("=== ISSUER'S SIDE ===\n");
        context.append("Claim ID: ").append(dispute.getClaimId()).append("\n");
        context.append("Reason Code: ").append(dispute.getReasonCode()).append("\n");
        context.append("Amount: ").append(dispute.getClaimValue()).append("\n");
        context.append("Progress State: ").append(dispute.getProgressState()).append("\n");
        context.append("Due Date: ").append(dispute.getDueDate()).append("\n");
        context.append("Issuer Summary:\n").append(dispute.getIssuerSummary() != null ? dispute.getIssuerSummary() : "Not available").append("\n\n");

        context.append("=== MERCHANT'S SIDE (Enriched Evidence) ===\n");
        context.append("Evidence Fetch Plan:\n").append(evidenceMap.getFetchPlan() != null ? evidenceMap.getFetchPlan() : "Not available").append("\n\n");
        context.append("Annotated Evidence Map:\n").append(evidenceMap.getAnnotatedMap() != null ? evidenceMap.getAnnotatedMap() : "Not available").append("\n\n");

        if (evidenceMap.getMerchantSummary() != null) {
            context.append("Merchant Side Summary:\n").append(evidenceMap.getMerchantSummary()).append("\n");
        }

        return context.toString();
    }
}
