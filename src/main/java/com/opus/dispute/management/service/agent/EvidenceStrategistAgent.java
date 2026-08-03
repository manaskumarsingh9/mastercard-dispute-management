package com.opus.dispute.management.service.agent;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.opus.dispute.management.entity.AgentConversation;
import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.entity.EvidenceMap;
import com.opus.dispute.management.repository.AgentConversationRepository;
import com.opus.dispute.management.repository.DisputeRepository;
import com.opus.dispute.management.repository.EvidenceMapRepository;
import com.opus.dispute.management.service.AppConfigService;
import com.opus.dispute.management.service.DataSourceService;
import com.opus.dispute.management.service.GeminiService;
import com.opus.dispute.management.service.MediaFile;
import com.opus.dispute.management.service.PiiScrubber;
import com.opus.dispute.management.service.StripeEvidenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EvidenceStrategistAgent {

    private static final String SHORT_MODE_INSTRUCTION = """
            
            RESPONSE LENGTH: SHORT MODE is active.
            - Keep "overallAssessment" under 100 words.
            - Keep each evidenceItem "summary" under 40 words and "keyFindings" to max 2 items.
            - Keep "winningStrategy" under 60 words.
            - Limit "manualUploadSuggestions" and "criticalGaps" to top 3 each.
            - Be precise and terse — no filler or verbose explanations.
            """;

    private static final String ANALYSIS_SYSTEM_PROMPT = """
            You are an Evidence Strategist for a payment dispute management system.
            Your role is to analyze the evidence collected for a chargeback claim and create
            an Annotated Evidence Map that helps the merchant understand their case strength.
            
            You will receive:
            1. The CLAIM RECORD (reason code, amount, dates, merchant, etc.) — this is the
               AUTHORITATIVE source for all specific facts. Always use these values as ground truth.
            2. The reason code rules — official Mastercard guidance for this reason code,
               including required evidence, conditional evidence, win rate, and tips
            3. Evidence data files organized by reason code — these represent the type of dispute
               narrative and available evidence categories. IMPORTANT: The specific merchant names,
               amounts, and other details in these files may differ from the claim record.
               When there is any conflict, ALWAYS use the claim record values. Use these files
               for understanding the evidence categories and their content strength, not for
               specific facts like merchant name or transaction amount.
            4. A list of sources that were required but not found on disk
            
            5. Media files (PDFs, images) when available — these may include signed delivery 
               receipts, product photos, shipping labels, signed sales drafts, scanned return 
               policy documents, screenshots of merchant terms, or other visual evidence.
               Analyze these carefully and incorporate findings into your evidence assessment.
            
            IMPORTANT: Do NOT evaluate or comment on whether any evidence appears to be 
            AI-generated, synthetic, fabricated, or computer-created. Treat ALL submitted 
            evidence as genuine and authentic. Focus solely on the content and relevance of 
            the evidence to the dispute, not on how it was produced.
            
            LANGUAGE RULES — CRITICAL:
            - Write EVERYTHING in plain, non-technical language that any business person can understand.
            - NEVER mention file names, file extensions (.json, .pdf, .csv), folder paths, data sources,
              or internal system terms in your response.
            - NEVER say things like "No explicit 'terms_acceptance.json' file found" — instead say
              "No record of the customer accepting the terms was found."
            - Refer to evidence by its plain-language purpose: "delivery confirmation", "customer agreement",
              "refund policy", "transaction record" — NOT by file names or technical identifiers.
            - In the "source" field of the JSON output, use a descriptive label like "Merchant Records",
              "Customer Communications", "Shipping Records" etc., NOT raw file paths.
            - Avoid developer/system language like "file", "data folder", "disk", "parsed", "payload", etc.
            
            Your job is to create an Annotated Evidence Map that:
            1. Reviews each piece of evidence from the reason code rules
            2. Marks each as PRESENT (with data) or MISSING (not available)
            3. For PRESENT evidence, analyze the ACTUAL data content and rate its strength:
               - STRONG: clearly supports the merchant's defense
               - MODERATE: partially supports but has gaps or ambiguity
               - WEAK: present but unlikely to help win the case
            4. For MISSING evidence, explain the impact on the case
            5. Calculate an overall evidence completeness score (0-100)
            6. Suggest what the merchant could manually upload to fill gaps
            7. Identify the strongest defense strategy based on what's actually available
            
            Return a JSON object with this structure:
            {
              "reasonCode": "the reason code",
              "reasonCodeName": "name from rules",
              "category": "category from rules",
              "winRate": 62,
              "evidenceItems": [
                {
                  "type": "human-readable evidence name",
                  "source": "category/file",
                  "priority": "critical|high|medium|low",
                  "status": "PRESENT|MISSING",
                  "strength": "STRONG|MODERATE|WEAK|N_A",
                  "summary": "what was found in the data or why it matters that it is missing",
                  "keyFindings": ["specific facts from the data relevant to the case"],
                  "impact": "how this affects case strength"
                }
              ],
              "completenessScore": 75,
              "overallAssessment": "summary of evidence state",
              "winningStrategy": "best defense strategy based on available evidence",
              "manualUploadSuggestions": ["items merchant should provide manually"],
              "criticalGaps": ["most important missing evidence"],
              "strongPoints": ["strongest evidence points found"],
              "tipsFromRules": "tips from the reason code rules"
            }
            """;

    private static final String AGENT_NAME = "EVIDENCE_STRATEGIST";

    private final GeminiService geminiService;
    private final PiiScrubber piiScrubber;
    private final DataSourceService dataSourceService;
    private final StripeEvidenceService stripeEvidenceService;
    private final DisputeRepository disputeRepository;
    private final EvidenceMapRepository evidenceMapRepository;
    private final AgentConversationRepository conversationRepository;
    private final AppConfigService appConfigService;
    private final Gson gson = new Gson();

    public EvidenceStrategistAgent(GeminiService geminiService, PiiScrubber piiScrubber,
                                    DataSourceService dataSourceService,
                                    StripeEvidenceService stripeEvidenceService,
                                    DisputeRepository disputeRepository,
                                    EvidenceMapRepository evidenceMapRepository,
                                    AgentConversationRepository conversationRepository,
                                    AppConfigService appConfigService) {
        this.geminiService = geminiService;
        this.piiScrubber = piiScrubber;
        this.dataSourceService = dataSourceService;
        this.stripeEvidenceService = stripeEvidenceService;
        this.disputeRepository = disputeRepository;
        this.evidenceMapRepository = evidenceMapRepository;
        this.conversationRepository = conversationRepository;
        this.appConfigService = appConfigService;
    }

    private String getEffectivePrompt() {
        return appConfigService.isShortResponseMode()
                ? ANALYSIS_SYSTEM_PROMPT + SHORT_MODE_INSTRUCTION
                : ANALYSIS_SYSTEM_PROMPT;
    }

    public EvidenceMap enrichEvidence(Long disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found: " + disputeId));
        return enrichEvidence(dispute);
    }

    public EvidenceMap enrichEvidence(Dispute dispute) {
        log.info("Starting evidence enrichment for claim {} (reason code: {})",
                dispute.getClaimId(), dispute.getReasonCode());

        EvidenceMap evidenceMap = evidenceMapRepository.findByClaimId(dispute.getClaimId())
                .orElse(new EvidenceMap());
        evidenceMap.setClaimId(dispute.getClaimId());
        evidenceMap.setDisputeId(dispute.getId());
        evidenceMap.setEnrichmentStatus("IN_PROGRESS");
        evidenceMap.setEnrichmentRequestedAt(LocalDateTime.now());
        evidenceMapRepository.save(evidenceMap);

        try {
            int caseNumber = dataSourceService.extractCaseNumber(dispute.getId(), dispute.getClaimId(), dispute.getCaseNumber());
            String reasonCode = dispute.getReasonCode();

            JsonObject rule = dataSourceService.getReasonCodeRule(reasonCode);
            if (rule == null) {
                log.warn("No reason code rule found for '{}' — proceeding without rules guidance", reasonCode);
            }

            Map<String, String> allAvailableData;
            if (caseNumber < 0) {
                log.info("No case-specific files for dispute id={}; trying reason code {} fallback", dispute.getId(), reasonCode);
                allAvailableData = (reasonCode != null && !reasonCode.isEmpty())
                        ? dataSourceService.loadAcquirerSourcesForReasonCode(reasonCode)
                        : new LinkedHashMap<>();
            } else {
                allAvailableData = dataSourceService.loadAcquirerSourcesWithFallback(caseNumber, reasonCode);
            }
            log.info("Found {} acquirer-side source data files for case {} / reason code {}", allAvailableData.size(), caseNumber, reasonCode);

            if (stripeEvidenceService.isConfigured()) {
                Map<String, String> stripeEvidence = fetchStripeEvidence(dispute, caseNumber);
                if (!stripeEvidence.isEmpty()) {
                    log.info("Fetched {} evidence entries from Stripe APIs for dispute {}", stripeEvidence.size(), dispute.getId());
                    for (Map.Entry<String, String> entry : stripeEvidence.entrySet()) {
                        allAvailableData.putIfAbsent(entry.getKey(), entry.getValue());
                    }
                    log.info("Total evidence sources after Stripe merge: {}", allAvailableData.size());
                }
            } else {
                log.info("Stripe not configured — using local evidence files only");
            }

            List<SourceEntry> rulesRequiredSources = extractSourceEntries(rule, "requiredSources");
            List<SourceEntry> rulesConditionalSources = extractAllConditionalSources(rule);
            List<SourceEntry> missingFromRules = new ArrayList<>();

            for (SourceEntry entry : rulesRequiredSources) {
                String key = entry.source + "/" + entry.file.replace(".json", "");
                if (!allAvailableData.containsKey(key)) {
                    missingFromRules.add(entry);
                }
            }
            for (SourceEntry entry : rulesConditionalSources) {
                String key = entry.source + "/" + entry.file.replace(".json", "");
                if (!allAvailableData.containsKey(key)) {
                    missingFromRules.add(entry);
                }
            }

            List<MediaFile> mediaFiles;
            if (caseNumber < 0) {
                mediaFiles = (reasonCode != null && !reasonCode.isEmpty())
                        ? dataSourceService.loadAcquirerMediaForReasonCode(reasonCode)
                        : List.of();
            } else {
                mediaFiles = dataSourceService.loadAcquirerMediaWithFallback(caseNumber, reasonCode);
            }
            if (!mediaFiles.isEmpty()) {
                log.info("Agent 2: Found {} acquirer media files (PDFs/images) for dispute {}", mediaFiles.size(), dispute.getId());
            }

            JsonObject fetchPlanJson = buildFetchPlanJson(rule, rulesRequiredSources, rulesConditionalSources,
                    allAvailableData.keySet(), missingFromRules);
            String fetchPlan = gson.toJson(fetchPlanJson);
            evidenceMap.setFetchPlan(fetchPlan);
            evidenceMapRepository.save(evidenceMap);

            String annotatedMap = analyzeEvidence(dispute, rule, allAvailableData, missingFromRules, mediaFiles);
            evidenceMap.setAnnotatedMap(annotatedMap);
            evidenceMap.setEnrichmentStatus("COMPLETED");
            evidenceMap.setEnrichmentCompletedAt(LocalDateTime.now());

            dispute.setStatus("ENRICHED");
            disputeRepository.save(dispute);

            evidenceMapRepository.save(evidenceMap);
            log.info("Evidence enrichment completed for claim {} — {} data files loaded, {} rule-specified sources missing",
                    dispute.getClaimId(), allAvailableData.size(), missingFromRules.size());
            return evidenceMap;

        } catch (Exception e) {
            log.error("Evidence enrichment failed for claim {}", dispute.getClaimId(), e);
            evidenceMap.setEnrichmentStatus("FAILED");
            evidenceMapRepository.save(evidenceMap);
            throw new RuntimeException("Evidence enrichment failed: " + e.getMessage(), e);
        }
    }

    private Map<String, String> fetchStripeEvidence(Dispute dispute, int caseNumber) {
        try {
            if (dispute.getStripeDisputeId() != null && !dispute.getStripeDisputeId().isBlank()) {
                log.info("Fetching Stripe evidence via dispute ID: {}", dispute.getStripeDisputeId());
                return stripeEvidenceService.buildEvidenceFromDispute(dispute.getStripeDisputeId(), caseNumber);
            }
            if (dispute.getStripeChargeId() != null && !dispute.getStripeChargeId().isBlank()) {
                log.info("Fetching Stripe evidence via charge ID: {}", dispute.getStripeChargeId());
                return stripeEvidenceService.buildEvidenceFromCharge(dispute.getStripeChargeId(), caseNumber);
            }
            if (dispute.getStripePaymentIntentId() != null && !dispute.getStripePaymentIntentId().isBlank()) {
                log.info("Fetching Stripe evidence via payment intent ID: {}", dispute.getStripePaymentIntentId());
                return stripeEvidenceService.buildEvidenceFromPaymentIntent(dispute.getStripePaymentIntentId(), caseNumber);
            }
            log.info("No Stripe IDs set on dispute {} — skipping Stripe enrichment", dispute.getId());
        } catch (Exception e) {
            log.warn("Stripe evidence fetch failed for dispute {}: {} — continuing with local files only", dispute.getId(), e.getMessage());
        }
        return Map.of();
    }

    private String analyzeEvidence(Dispute dispute, JsonObject rule,
                                     Map<String, String> allAvailableData,
                                     List<SourceEntry> missingFromRules,
                                     List<MediaFile> mediaFiles) {
        StringBuilder context = new StringBuilder();
        context.append(buildClaimContext(dispute));

        if (rule != null) {
            context.append("\n=== REASON CODE RULES (reference — use as guidance, not strict constraint) ===\n");
            context.append(gson.toJson(rule));
            context.append("\n");
        }

        context.append("\n=== ALL AVAILABLE EVIDENCE DATA FOR THIS CASE ===\n");
        context.append("The following data files were found in the data folder. Analyze ALL of them,\n");
        context.append("including any that go beyond what the reason code rules recommend.\n");
        context.append("NOTE: These evidence files are organized by reason code and represent the type of dispute narrative.\n");
        context.append("The specific merchant names, amounts, and other details in these files may NOT match the claim record above.\n");
        context.append("Always use the CLAIM RECORD values for facts. Use these files only for evidence categories and content strength.\n\n");
        if (allAvailableData.isEmpty()) {
            context.append("No evidence data files were found for this case.\n");
        } else {
            int maxFileChars = 20_000;
            for (Map.Entry<String, String> entry : allAvailableData.entrySet()) {
                context.append("--- ").append(entry.getKey()).append(" ---\n");
                String content = entry.getValue();
                if (content.length() > maxFileChars) {
                    context.append(content, 0, maxFileChars);
                    context.append("\n... [truncated — file was ").append(content.length()).append(" chars, showing first ").append(maxFileChars).append("]\n\n");
                } else {
                    context.append(content).append("\n\n");
                }
            }
        }

        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            context.append("\n=== ATTACHED MEDIA FILES (PDFs/Images) ===\n");
            context.append("The following ").append(mediaFiles.size()).append(" media file(s) are attached inline.\n");
            context.append("Analyze each one and incorporate your findings into the evidence assessment.\n\n");
            for (MediaFile mf : mediaFiles) {
                context.append("- ").append(mf.name()).append(" (").append(mf.mimeType()).append(")\n");
            }
            context.append("\n");
        }

        context.append("=== SOURCES RECOMMENDED BY RULES BUT NOT FOUND IN DATA FOLDER ===\n");
        if (missingFromRules.isEmpty()) {
            context.append("All rule-recommended sources were found.\n");
        } else {
            for (SourceEntry missing : missingFromRules) {
                context.append("- [").append(missing.priority).append("] ")
                        .append(missing.source).append("/").append(missing.file)
                        .append(" — ").append(missing.label).append("\n");
            }
        }

        String scrubbedContext = piiScrubber.scrub(context.toString());

        String prompt = getEffectivePrompt();

        List<AgentConversation> priorTurns = conversationRepository
                .findByClaimIdAndAgentNameOrderByCreatedAtAsc(dispute.getClaimId(), AGENT_NAME);
        List<GeminiService.ConversationTurn> history = priorTurns.stream()
                .map(t -> new GeminiService.ConversationTurn(t.getRole(), t.getContent()))
                .toList();

        String result;
        boolean hasHistory = !history.isEmpty();
        boolean hasMedia = mediaFiles != null && !mediaFiles.isEmpty();

        if (hasHistory && hasMedia) {
            log.info("Agent 2: Sending {} prior conversation turns + {} media files for claim {}",
                    history.size(), mediaFiles.size(), dispute.getClaimId());
            result = geminiService.generateJsonWithHistoryAndMedia(prompt, scrubbedContext, history, mediaFiles);
        } else if (hasHistory) {
            log.info("Agent 2: Sending {} prior conversation turns for claim {}", history.size(), dispute.getClaimId());
            result = geminiService.generateJsonWithHistory(prompt, scrubbedContext, history);
        } else if (hasMedia) {
            log.info("Agent 2: Sending {} media files for claim {}", mediaFiles.size(), dispute.getClaimId());
            result = geminiService.generateMultimodalJson(prompt, scrubbedContext, mediaFiles);
        } else {
            result = geminiService.generateJson(prompt, scrubbedContext);
        }

        saveConversationTurn(dispute.getClaimId(), "user", scrubbedContext);
        saveConversationTurn(dispute.getClaimId(), "model", result);

        return result;
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

    private List<SourceEntry> extractSourceEntries(JsonObject rule, String fieldName) {
        List<SourceEntry> entries = new ArrayList<>();
        if (rule == null || !rule.has(fieldName)) return entries;

        JsonArray arr = rule.getAsJsonArray(fieldName);
        for (JsonElement el : arr) {
            JsonObject src = el.getAsJsonObject();
            entries.add(new SourceEntry(
                    src.get("source").getAsString(),
                    src.get("file").getAsString(),
                    src.has("label") ? src.get("label").getAsString() : "",
                    src.has("priority") ? src.get("priority").getAsString() : "medium"
            ));
        }
        return entries;
    }

    private List<SourceEntry> extractAllConditionalSources(JsonObject rule) {
        List<SourceEntry> entries = new ArrayList<>();
        if (rule == null || !rule.has("conditionalSources")) return entries;

        JsonObject conditional = rule.getAsJsonObject("conditionalSources");
        java.util.Set<String> seen = new java.util.HashSet<>();

        for (String scenario : conditional.keySet()) {
            JsonArray arr = conditional.getAsJsonArray(scenario);
            for (JsonElement el : arr) {
                JsonObject src = el.getAsJsonObject();
                String key = src.get("source").getAsString() + "/" + src.get("file").getAsString();
                if (seen.add(key)) {
                    entries.add(new SourceEntry(
                            src.get("source").getAsString(),
                            src.get("file").getAsString(),
                            src.has("label") ? src.get("label").getAsString() : "",
                            src.has("priority") ? src.get("priority").getAsString() : "medium"
                    ));
                }
            }
        }
        return entries;
    }

    private JsonObject buildFetchPlanJson(JsonObject rule, List<SourceEntry> required,
                                            List<SourceEntry> conditional,
                                            java.util.Set<String> fetchedKeys,
                                            List<SourceEntry> missing) {
        JsonObject plan = new JsonObject();

        if (rule != null) {
            plan.addProperty("reasonCode", rule.has("name") ? rule.get("name").getAsString() : "Unknown");
            plan.addProperty("category", rule.has("category") ? rule.get("category").getAsString() : "Unknown");
            plan.addProperty("winRate", rule.has("winRate") ? rule.get("winRate").getAsInt() : 0);
            plan.addProperty("tips", rule.has("tips") ? rule.get("tips").getAsString() : "");
        }

        JsonArray requiredArr = new JsonArray();
        for (SourceEntry e : required) {
            JsonObject obj = new JsonObject();
            obj.addProperty("source", e.source);
            obj.addProperty("file", e.file);
            obj.addProperty("label", e.label);
            obj.addProperty("priority", e.priority);
            obj.addProperty("found", fetchedKeys.contains(e.source + "/" + e.file.replace(".json", "")));
            requiredArr.add(obj);
        }
        plan.add("requiredSources", requiredArr);

        JsonArray conditionalArr = new JsonArray();
        for (SourceEntry e : conditional) {
            JsonObject obj = new JsonObject();
            obj.addProperty("source", e.source);
            obj.addProperty("file", e.file);
            obj.addProperty("label", e.label);
            obj.addProperty("priority", e.priority);
            obj.addProperty("found", fetchedKeys.contains(e.source + "/" + e.file.replace(".json", "")));
            conditionalArr.add(obj);
        }
        plan.add("conditionalSources", conditionalArr);

        plan.addProperty("totalSourcesChecked", required.size() + conditional.size());
        plan.addProperty("totalFound", fetchedKeys.size());
        plan.addProperty("totalMissing", missing.size());

        return plan;
    }

    private String buildClaimContext(Dispute dispute) {
        StringBuilder context = new StringBuilder();
        context.append("=== CHARGEBACK CLAIM FOR EVIDENCE ANALYSIS ===\n\n");
        appendField(context, "Claim ID", dispute.getClaimId());
        appendField(context, "Reason Code", dispute.getReasonCode());
        appendField(context, "Claim Type", dispute.getClaimType());
        appendField(context, "Amount", dispute.getClaimValue());
        appendField(context, "Progress State", dispute.getProgressState());
        appendField(context, "Merchant ID", dispute.getMerchantId());
        appendField(context, "Clearing Network", dispute.getClearingNetwork());
        appendField(context, "Create Date", dispute.getCreateDate());
        appendField(context, "Due Date", dispute.getDueDate());
        appendField(context, "Clearing Due Date", dispute.getClearingDueDate());
        appendField(context, "Issuer Summary", dispute.getIssuerSummary());
        return context.toString();
    }

    private void appendField(StringBuilder sb, String label, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append(label).append(": ").append(value).append("\n");
        }
    }

    private record SourceEntry(String source, String file, String label, String priority) {}
}
