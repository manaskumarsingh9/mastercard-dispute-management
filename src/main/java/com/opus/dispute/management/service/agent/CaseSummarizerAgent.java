package com.opus.dispute.management.service.agent;

import com.opus.dispute.management.entity.ChargebackDetail;
import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.repository.ChargebackDetailRepository;
import com.opus.dispute.management.repository.DisputeRepository;
import com.opus.dispute.management.service.AppConfigService;
import com.opus.dispute.management.service.DataSourceService;
import com.opus.dispute.management.service.GeminiService;
import com.opus.dispute.management.service.MediaFile;
import com.opus.dispute.management.service.PiiScrubber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CaseSummarizerAgent {

    private static final String SHORT_MODE_INSTRUCTION = """
            
            RESPONSE LENGTH: SHORT MODE is active.
            - Keep the entire response under 300 words.
            - Use terse bullet points instead of full sentences.
            - Skip the "Notable Observations" and "Media Evidence Analysis" sections unless they contain critical findings.
            - Limit "Evidence Snapshot" to the 3 most important items only.
            - No filler phrases or verbose explanations — every word must add value.
            """;

    private static final String SYSTEM_PROMPT = """
            You are an expert dispute analyst for a payment dispute management system.
            Your role is to create clear, concise summaries of chargeback claims raised by card issuers.
            
            You will receive two types of information:
            1. The CLAIM RECORD from the Mastercard dispute system — use this ONLY for specific
               identifiers and numbers: merchant name/ID, amounts, currency, dates, reason code,
               claim ID, and chargeback detail records. These are the factual anchors.
            2. EVIDENCE DATA FILES — these are the PRIMARY SOURCE for the dispute narrative.
               The evidence files tell you what actually happened: why the cardholder is disputing,
               what the customer complained about, what the issuer found, what the merchant said, etc.
               
               CRITICAL RULES FOR EVIDENCE FILES:
               - The evidence files ARE the story. Build your entire narrative from them.
               - The cardholder dispute statement tells you the REAL reason for the dispute — use it.
                 For example, if the evidence says the customer was charged for a renewal without
                 notification, that IS the dispute reason — do NOT replace it with a generic
                 interpretation of the reason code name.
               - The issuer chargeback documentation tells you what the issuer is claiming — use it.
               - The transaction records tell you what the authorization data shows — use it.
               - When quoting specific identifiers (merchant name/ID, claim ID, amounts, currency,
                 dates), substitute the values from the CLAIM RECORD. But the story, the complaint,
                 the customer's words, the issuer's commentary — all come from the evidence files.
               - Do NOT invent or generalize the dispute reason from the reason code name alone.
                 The reason code is a category; the evidence files contain the specific complaint.
            3. Supporting media files (PDFs, images) when available — these may include scanned 
               cardholder dispute forms, copies of bank statements, police reports, screenshots of 
               unauthorized transactions, photos of damaged goods, or any other visual evidence.
               Analyze these carefully and include relevant findings in your summary.
            
            LANGUAGE RULES — CRITICAL:
            - Write EVERYTHING in plain, non-technical language that any business person can understand.
            - NEVER mention file names, file extensions (.json, .pdf, .csv), folder paths, data sources,
              or internal system terms in your response.
            - NEVER say things like "No explicit 'terms_acceptance.json' file found" — instead say
              "No record of the customer accepting the terms was found."
            - Refer to evidence by its plain-language purpose: "delivery confirmation", "customer agreement",
              "refund policy", "transaction record" — NOT by file names or technical identifiers.
            - Avoid developer/system language like "file", "data folder", "source", "parsed", "payload", etc.
            
            The summary should:
            1. Be readable in under 2 minutes by a merchant who may not be a dispute expert
            2. Clearly explain WHY the cardholder raised the dispute — based on the evidence files,
               not a generic interpretation of the reason code
            3. Describe what the issuer is claiming — based on the issuer documentation in the evidence
            4. Highlight key dates, amounts, and deadlines (from the claim record)
            5. Note the reason code and its meaning in plain language
            6. Identify any unusual or noteworthy aspects of the claim
            7. When evidence data is available, include key findings from it — for example:
               - What specific complaint did the cardholder make?
               - Did the cardholder contact the merchant first? What was the merchant's response?
               - Was 3D Secure authentication successful?
               - Was delivery confirmed and signed for?
               - What does the PSP authorization log show?
               - Are there any red flags from fraud tools (VPN, proxy, high risk score)?
               - What is the issuer's internal risk assessment and recommended action?
            8. When media files (PDFs/images) are attached, describe what you see in them:
               - What type of document is it (receipt, form, photo, statement)?
               - What key information does it contain?
               - Does it support or weaken the cardholder's claim?
               - Are there any notable details (signatures, timestamps, amounts, conditions)?
            
            Write in a professional but accessible tone. Use bullet points for key facts.
            Do NOT include any PII, card numbers, or sensitive data in the summary.
            Structure the summary with these sections:
            - Overview (1-2 sentences)
            - Cardholder's Claim
            - Key Facts (bullet points)
            - Evidence Snapshot (brief summary of available evidence — only if evidence data was provided)
            - Media Evidence Analysis (findings from PDFs/images — only if media files were attached)
            - Deadlines & Important Dates
            - Notable Observations (if any)
            """;

    private final GeminiService geminiService;
    private final PiiScrubber piiScrubber;
    private final DisputeRepository disputeRepository;
    private final DataSourceService dataSourceService;
    private final AppConfigService appConfigService;
    private final ChargebackDetailRepository chargebackDetailRepository;

    public CaseSummarizerAgent(GeminiService geminiService, PiiScrubber piiScrubber,
                                DisputeRepository disputeRepository,
                                DataSourceService dataSourceService,
                                AppConfigService appConfigService,
                                ChargebackDetailRepository chargebackDetailRepository) {
        this.geminiService = geminiService;
        this.piiScrubber = piiScrubber;
        this.disputeRepository = disputeRepository;
        this.dataSourceService = dataSourceService;
        this.appConfigService = appConfigService;
        this.chargebackDetailRepository = chargebackDetailRepository;
    }

    private String getEffectivePrompt() {
        return appConfigService.isShortResponseMode()
                ? SYSTEM_PROMPT + SHORT_MODE_INSTRUCTION
                : SYSTEM_PROMPT;
    }

    public String summarize(Dispute dispute) {
        if (!geminiService.isAvailable()) {
            log.warn("Gemini not available, skipping summarization for claim {}", dispute.getClaimId());
            return null;
        }

        try {
            String claimContext = buildClaimContext(dispute);

            String sourceData = loadSourceData(dispute);
            if (sourceData != null && !sourceData.isEmpty()) {
                claimContext += "\n" + sourceData;
            }

            List<MediaFile> mediaFiles = loadMediaFiles(dispute);

            String scrubbedContext = piiScrubber.scrub(claimContext);
            String prompt = getEffectivePrompt();

            String summary;
            if (!mediaFiles.isEmpty()) {
                log.info("Agent 1: Sending {} media files (PDFs/images) to Gemini for claim {}",
                        mediaFiles.size(), dispute.getClaimId());
                summary = geminiService.generateMultimodalContent(prompt, scrubbedContext, mediaFiles);
            } else {
                summary = geminiService.generateContent(prompt, scrubbedContext);
            }

            dispute.setIssuerSummary(summary);
            disputeRepository.save(dispute);

            log.info("Generated summary for claim {} (with{} source data, {} media files)",
                    dispute.getClaimId(), sourceData != null ? "" : "out", mediaFiles.size());
            return summary;
        } catch (Exception e) {
            log.error("Failed to generate summary for claim {}", dispute.getClaimId(), e);
            return null;
        }
    }

    public String summarize(Long disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found: " + disputeId));
        return summarize(dispute);
    }

    private String resolveReasonCode(Dispute dispute) {
        if (dispute.getReasonCode() != null && !dispute.getReasonCode().isEmpty()) {
            return dispute.getReasonCode();
        }
        if (dispute.getProgressState() != null) {
            String extracted = com.opus.dispute.management.service.ClaimIngestionService
                    .extractReasonCodeFromProgressState(dispute.getProgressState());
            if (extracted != null) {
                log.info("Agent 1: Recovered reason code {} from progressState for dispute {}", extracted, dispute.getId());
                dispute.setReasonCode(extracted);
                disputeRepository.save(dispute);
                return extracted;
            }
        }
        List<ChargebackDetail> details = chargebackDetailRepository.findByDisputeId(dispute.getId());
        for (ChargebackDetail cbd : details) {
            if (cbd.getReasonCode() != null && !cbd.getReasonCode().isEmpty()) {
                log.info("Agent 1: Recovered reason code {} from chargeback detail for dispute {}", cbd.getReasonCode(), dispute.getId());
                dispute.setReasonCode(cbd.getReasonCode());
                disputeRepository.save(dispute);
                return cbd.getReasonCode();
            }
        }
        return null;
    }

    private String loadSourceData(Dispute dispute) {
        int caseNumber = dataSourceService.extractCaseNumber(dispute.getId(), dispute.getClaimId(), dispute.getCaseNumber());
        String rc = resolveReasonCode(dispute);

        Map<String, String> issuerSources;
        if (caseNumber < 0) {
            if (rc != null && !rc.isEmpty()) {
                log.info("No case-specific files for dispute id={}; trying reason code {} fallback", dispute.getId(), rc);
                issuerSources = dataSourceService.loadIssuerSourcesForReasonCode(rc);
            } else {
                log.warn("Could not extract case number and no reason code for dispute id={}", dispute.getId());
                return null;
            }
        } else {
            issuerSources = dataSourceService.loadIssuerSourcesWithFallback(caseNumber, rc);
        }
        if (issuerSources.isEmpty()) {
            log.info("No issuer evidence files found for case {} / reason code {} (dispute id={}, claimId={})",
                    caseNumber, dispute.getReasonCode(), dispute.getId(), dispute.getClaimId());
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== ISSUER EVIDENCE DATA ===\n");
        sb.append("THESE FILES ARE THE PRIMARY SOURCE FOR THE DISPUTE NARRATIVE.\n");
        sb.append("Build the entire story of what happened from these files — what the cardholder complained about,\n");
        sb.append("what the issuer found, what the merchant said, what the evidence shows.\n");
        sb.append("Only substitute identifiers (merchant name/ID, amounts, currency, dates, claim ID) from the CLAIM RECORD above.\n");
        sb.append("The dispute reason, customer complaint, issuer commentary, and evidence findings come from HERE.\n\n");

        for (Map.Entry<String, String> entry : issuerSources.entrySet()) {
            sb.append("--- issuer/").append(entry.getKey()).append(" ---\n");
            sb.append(entry.getValue()).append("\n\n");
        }

        log.info("Agent 1: Loaded {} issuer evidence files for case {} (acquirer evidence excluded)",
                issuerSources.size(), caseNumber);
        return sb.toString();
    }

    private List<MediaFile> loadMediaFiles(Dispute dispute) {
        int caseNumber = dataSourceService.extractCaseNumber(dispute.getId(), dispute.getClaimId(), dispute.getCaseNumber());
        String rc = resolveReasonCode(dispute);
        List<MediaFile> mediaFiles;
        if (caseNumber < 0) {
            if (rc != null && !rc.isEmpty()) {
                mediaFiles = dataSourceService.loadIssuerMediaForReasonCode(rc);
            } else {
                return List.of();
            }
        } else {
            mediaFiles = dataSourceService.loadIssuerMediaWithFallback(caseNumber, rc);
        }
        if (!mediaFiles.isEmpty()) {
            log.info("Agent 1: Found {} issuer media files (PDFs/images) for dispute {}", mediaFiles.size(), dispute.getId());
        }
        return mediaFiles;
    }

    private String buildClaimContext(Dispute dispute) {
        StringBuilder context = new StringBuilder();
        context.append("=== CHARGEBACK CLAIM DETAILS ===\n\n");

        String effectiveReasonCode = resolveReasonCode(dispute);

        appendField(context, "Claim ID", dispute.getClaimId());
        appendField(context, "Claim Type", dispute.getClaimType());
        appendField(context, "Claim Value/Amount", dispute.getClaimValue());
        appendField(context, "Reason Code", effectiveReasonCode);
        appendField(context, "Progress State", dispute.getProgressState());
        appendField(context, "Queue", dispute.getQueueName());
        appendField(context, "Clearing Network", dispute.getClearingNetwork());
        appendField(context, "Primary Account Number", dispute.getPrimaryAccountNum());
        appendField(context, "Merchant ID", dispute.getMerchantId());
        appendField(context, "Acquirer ID", dispute.getAcquirerId());
        appendField(context, "Acquirer Reference Number", dispute.getAcquirerRefNum());
        appendField(context, "Issuer ID", dispute.getIssuerId());
        appendField(context, "Transaction ID", dispute.getTransactionId());
        appendField(context, "Case Open", dispute.getIsOpen() != null ? dispute.getIsOpen().toString() : null);
        appendField(context, "Is Issuer", dispute.getIsIssuer() != null ? dispute.getIsIssuer().toString() : null);
        appendField(context, "Is Acquirer", dispute.getIsAcquirer() != null ? dispute.getIsAcquirer().toString() : null);
        appendField(context, "Clearing Due Date", dispute.getClearingDueDate());
        appendField(context, "Due Date", dispute.getDueDate());
        appendField(context, "Create Date", dispute.getCreateDate());
        appendField(context, "Last Modified By", dispute.getLastModifiedBy());
        appendField(context, "Last Modified Date", dispute.getLastModifiedMcDate());

        List<ChargebackDetail> chargebackDetails = chargebackDetailRepository.findByDisputeId(dispute.getId());
        if (!chargebackDetails.isEmpty()) {
            context.append("\n=== CHARGEBACK DETAIL RECORDS (").append(chargebackDetails.size()).append(") ===\n\n");
            for (int i = 0; i < chargebackDetails.size(); i++) {
                ChargebackDetail cbd = chargebackDetails.get(i);
                context.append("--- Chargeback Record ").append(i + 1).append(" ---\n");
                appendField(context, "Chargeback Type", cbd.getChargebackType());
                appendField(context, "Reason Code", cbd.getReasonCode());
                appendField(context, "Amount", cbd.getAmount());
                appendField(context, "Currency", cbd.getCurrency());
                appendField(context, "Message", cbd.getMessageText());
                appendField(context, "Document Status", cbd.getDocumentStatus());
                appendField(context, "Reject Reason", cbd.getRejectReason());
                appendField(context, "Is Partial", cbd.getIsPartialChargeback() != null ? cbd.getIsPartialChargeback().toString() : null);
                appendField(context, "Reversed", cbd.getReversed() != null ? cbd.getReversed().toString() : null);
                appendField(context, "Create Date", cbd.getCreateDate());
                context.append("\n");
            }
        }

        return context.toString();
    }

    private void appendField(StringBuilder sb, String label, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append(label).append(": ").append(value).append("\n");
        }
    }
}
