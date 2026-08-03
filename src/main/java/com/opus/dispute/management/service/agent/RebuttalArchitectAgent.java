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
public class RebuttalArchitectAgent {

    private static final String AGENT_NAME = "REBUTTAL_ARCHITECT";

    private static final String SHORT_MODE_INSTRUCTION = """
            
            RESPONSE LENGTH: SHORT MODE is active.
            - Keep the entire rebuttal under 400 words.
            - Executive Summary: 1-2 sentences max.
            - Supporting Evidence: top 3 items only, one line each.
            - Skip "Evidence Gap Acknowledgment" unless critical.
            - Conclusion: 1-2 sentences.
            - Be assertive and concise — every sentence must advance the argument.
            """;

    private static final String SYSTEM_PROMPT = """
            You are the Rebuttal Architect Agent (Agent 3b) in a dispute management system.
            Your role is to generate a professional, compelling rebuttal document that will be
            submitted to the card network (Mastercard) to challenge a chargeback.
            
            The document must:
            1. Be optimized for issuer personnel to review in 2-3 minutes
            2. Present the merchant's case clearly and logically
            3. Reference specific evidence with clear labels
            4. Address the specific reason code and cardholder's claim directly
            5. Be professional, factual, and persuasive without being aggressive
            6. Follow a clean, scannable structure
            
            Document Structure:
            - Case Reference (claim ID, reason code, amount)
            - Executive Summary (2-3 sentences stating the merchant's position)
            - Response to Cardholder's Claim (directly address what was alleged)
            - Supporting Evidence
              - For each key evidence piece: what it proves and why it matters
            - Evidence Gap Acknowledgment (transparent about what could not be obtained)
            - Conclusion (clear statement of why the chargeback should be reversed)
            
            IMPORTANT:
            - This is a DYNAMIC document, NOT a template fill.
            - Tailor the argument to the specific claim and available evidence.
            - Do NOT include any PII, full card numbers, or sensitive data.
            - Be factual — do not fabricate or embellish evidence.
            - Reference evidence by type (e.g., "Authorization Record", "Delivery Confirmation")
            
            CONVERSATION HISTORY:
            - If you have generated a prior rebuttal for this case, it will appear in the conversation history.
            - When the user asks you to refine or improve, use your prior rebuttal as the baseline.
            - Apply the user's specific instructions (tone changes, additional points, different angle) on top of your prior work.
            - Do NOT start from scratch unless the user explicitly asks for a completely new rebuttal.
            - Preserve strong arguments from the prior version while incorporating the requested changes.
            
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
    private final AgentConversationRepository conversationRepository;

    public RebuttalArchitectAgent(GeminiService geminiService, PiiScrubber piiScrubber,
                                   DisputeRepository disputeRepository,
                                   EvidenceMapRepository evidenceMapRepository,
                                   AppConfigService appConfigService,
                                   AgentConversationRepository conversationRepository) {
        this.geminiService = geminiService;
        this.piiScrubber = piiScrubber;
        this.disputeRepository = disputeRepository;
        this.evidenceMapRepository = evidenceMapRepository;
        this.appConfigService = appConfigService;
        this.conversationRepository = conversationRepository;
    }

    private String getEffectivePrompt() {
        return appConfigService.isShortResponseMode()
                ? SYSTEM_PROMPT + SHORT_MODE_INSTRUCTION
                : SYSTEM_PROMPT;
    }

    public String generateRebuttal(Long disputeId) {
        return generateRebuttal(disputeId, null);
    }

    public String generateRebuttal(Long disputeId, String userInstructions) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found: " + disputeId));

        EvidenceMap evidenceMap = evidenceMapRepository.findByClaimId(dispute.getClaimId())
                .orElseThrow(() -> new RuntimeException("Evidence map not found. Run enrichment first."));

        if (!"COMPLETED".equals(evidenceMap.getEnrichmentStatus())) {
            throw new RuntimeException("Enrichment not yet completed for claim: " + dispute.getClaimId());
        }

        return generateRebuttal(dispute, evidenceMap, userInstructions);
    }

    public String generateRebuttal(Dispute dispute, EvidenceMap evidenceMap) {
        return generateRebuttal(dispute, evidenceMap, null);
    }

    public String generateRebuttal(Dispute dispute, EvidenceMap evidenceMap, String userInstructions) {
        log.info("Generating rebuttal document for claim {}", dispute.getClaimId());

        try {
            String context = buildContext(dispute, evidenceMap);

            if (userInstructions != null && !userInstructions.isBlank()) {
                context += "\n=== USER INSTRUCTIONS ===\n";
                context += "The user has provided the following specific instructions for this rebuttal. ";
                context += "Incorporate these into the rebuttal document:\n";
                context += userInstructions + "\n";
            }

            String scrubbedContext = piiScrubber.scrub(context);

            List<AgentConversation> priorTurns = conversationRepository
                    .findByClaimIdAndAgentNameOrderByCreatedAtAsc(dispute.getClaimId(), AGENT_NAME);
            List<GeminiService.ConversationTurn> history = priorTurns.stream()
                    .map(t -> new GeminiService.ConversationTurn(t.getRole(), t.getContent()))
                    .toList();

            String rebuttal;
            if (!history.isEmpty()) {
                log.info("Agent 3b: Sending {} prior conversation turns for claim {}", history.size(), dispute.getClaimId());
                rebuttal = geminiService.generateContentWithHistory(getEffectivePrompt(), scrubbedContext, history);
            } else {
                rebuttal = geminiService.generateContent(getEffectivePrompt(), scrubbedContext);
            }

            saveConversationTurn(dispute.getClaimId(), "user", scrubbedContext);
            saveConversationTurn(dispute.getClaimId(), "model", rebuttal);

            evidenceMap.setRebuttalDocument(rebuttal);
            evidenceMap.setRebuttalGeneratedAt(LocalDateTime.now());
            evidenceMapRepository.save(evidenceMap);

            dispute.setStatus("REBUTTAL_READY");
            disputeRepository.save(dispute);

            log.info("Rebuttal document generated for claim {}", dispute.getClaimId());
            return rebuttal;
        } catch (Exception e) {
            log.error("Failed to generate rebuttal for claim {}", dispute.getClaimId(), e);
            throw new RuntimeException("Rebuttal generation failed: " + e.getMessage(), e);
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

    private String buildContext(Dispute dispute, EvidenceMap evidenceMap) {
        StringBuilder context = new StringBuilder();

        context.append("=== CASE INFORMATION ===\n");
        context.append("Claim ID: ").append(dispute.getClaimId()).append("\n");
        context.append("Reason Code: ").append(dispute.getReasonCode()).append("\n");
        context.append("Amount: ").append(dispute.getClaimValue()).append("\n");
        context.append("Clearing Network: ").append(dispute.getClearingNetwork()).append("\n");
        context.append("Due Date: ").append(dispute.getDueDate()).append("\n\n");

        context.append("=== ISSUER'S CLAIM ===\n");
        context.append(dispute.getIssuerSummary() != null ? dispute.getIssuerSummary() : "No issuer summary available").append("\n\n");

        context.append("=== EVIDENCE STRATEGY ===\n");
        context.append(evidenceMap.getFetchPlan() != null ? evidenceMap.getFetchPlan() : "Not available").append("\n\n");

        context.append("=== ANNOTATED EVIDENCE MAP ===\n");
        context.append(evidenceMap.getAnnotatedMap() != null ? evidenceMap.getAnnotatedMap() : "Not available").append("\n\n");

        context.append("=== MERCHANT SIDE SUMMARY ===\n");
        context.append(evidenceMap.getMerchantSummary() != null ? evidenceMap.getMerchantSummary() : "Not available").append("\n\n");

        if (evidenceMap.getChallengeRecommendation() != null) {
            context.append("=== CHALLENGE RECOMMENDATION ===\n");
            context.append(evidenceMap.getChallengeRecommendation()).append("\n");
        }

        return context.toString();
    }
}
