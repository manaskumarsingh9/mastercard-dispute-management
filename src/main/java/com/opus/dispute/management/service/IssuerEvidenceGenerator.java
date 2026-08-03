package com.opus.dispute.management.service;

import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.repository.DisputeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class IssuerEvidenceGenerator {

    private static final Path ISSUER_ROOT = Paths.get("src/data/sources/issuer");

    private static final Map<String, String> REASON_CODE_NAMES = Map.ofEntries(
            Map.entry("4808", "Authorization-Related Chargeback"),
            Map.entry("4831", "Transaction Amount Differs"),
            Map.entry("4834", "Duplicate Processing"),
            Map.entry("4837", "No Cardholder Authorization (Fraud)"),
            Map.entry("4841", "Cancelled Recurring Transaction"),
            Map.entry("4853", "Goods/Services Not as Described or Defective"),
            Map.entry("4855", "Goods or Services Not Provided"),
            Map.entry("4859", "Addendum, No-show, or ATM Dispute"),
            Map.entry("4862", "Counterfeit Transaction"),
            Map.entry("4863", "Cardholder Does Not Recognize Transaction"),
            Map.entry("4871", "Chip/PIN Transaction — Lost/Stolen/Not Received"),
            Map.entry("4900", "Mastercard Compliance")
    );

    private final GeminiService geminiService;
    private final DisputeRepository disputeRepository;
    private final DataSourceService dataSourceService;

    public IssuerEvidenceGenerator(GeminiService geminiService,
                                    DisputeRepository disputeRepository,
                                    DataSourceService dataSourceService) {
        this.geminiService = geminiService;
        this.disputeRepository = disputeRepository;
        this.dataSourceService = dataSourceService;
    }

    public Map<String, Object> generateForCase(Long disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found: " + disputeId));

        int caseNumber = dataSourceService.extractCaseNumber(dispute.getId(), dispute.getClaimId(), dispute.getCaseNumber());
        if (caseNumber < 0) caseNumber = dispute.getId().intValue();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("disputeId", disputeId);
        result.put("caseNumber", caseNumber);

        String reasonCode = dispute.getReasonCode() != null ? dispute.getReasonCode() : "4834";
        String reasonName = REASON_CODE_NAMES.getOrDefault(reasonCode, "Unknown Dispute Type");
        String merchantName = dispute.getMerchantName() != null ? dispute.getMerchantName() : "Unknown Merchant";
        String amount = dispute.getAmount() != null ? String.valueOf(dispute.getAmount()) : "0";
        String currency = dispute.getCurrency() != null ? dispute.getCurrency() : "USD";

        List<FileSpec> filesToGenerate = List.of(
                new FileSpec("customer-comms", "cardholder_dispute_statement",
                        buildCardholderStatementPrompt(caseNumber, reasonCode, reasonName, merchantName, amount, currency)),
                new FileSpec("merchant", "issuer_chargeback_documentation",
                        buildChargebackDocPrompt(caseNumber, reasonCode, reasonName, merchantName, amount, currency)),
                new FileSpec("psp", "issuer_transaction_record",
                        buildIssuerTransactionPrompt(caseNumber, reasonCode, reasonName, merchantName, amount, currency))
        );

        List<String> generated = new java.util.ArrayList<>();
        List<String> skipped = new java.util.ArrayList<>();
        List<String> errors = new java.util.ArrayList<>();

        for (FileSpec spec : filesToGenerate) {
            Path outputFile = ISSUER_ROOT.resolve(spec.category)
                    .resolve(spec.fileType + "_case_" + caseNumber + ".json");

            if (Files.exists(outputFile)) {
                skipped.add(spec.category + "/" + spec.fileType);
                continue;
            }

            try {
                String content = geminiService.generateContent(
                        "You are a data generator creating realistic issuer-side evidence documents for a Mastercard chargeback dispute management system. "
                                + "Generate ONLY valid JSON output. No markdown, no code fences, no explanations. Just a single JSON object.",
                        spec.prompt
                );

                String cleanContent = content.trim();
                if (cleanContent.startsWith("```")) {
                    cleanContent = cleanContent.replaceAll("^```[a-z]*\\n?", "").replaceAll("\\n?```$", "").trim();
                }

                Files.createDirectories(outputFile.getParent());
                Files.writeString(outputFile, cleanContent);
                generated.add(spec.category + "/" + spec.fileType);
                log.info("Generated issuer evidence: {}", outputFile);
            } catch (Exception e) {
                errors.add(spec.category + "/" + spec.fileType + ": " + e.getMessage());
                log.error("Failed to generate {}: {}", outputFile, e.getMessage());
            }
        }

        result.put("generated", generated);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }

    public Map<String, Object> generateForAllLocalCases() {
        List<Dispute> localDisputes = disputeRepository.findAll().stream()
                .filter(d -> d.getClaimId() != null && d.getClaimId().startsWith("2154-"))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCases", localDisputes.size());

        List<Map<String, Object>> caseResults = new java.util.ArrayList<>();
        int totalGenerated = 0;
        int totalSkipped = 0;
        int totalErrors = 0;

        for (Dispute dispute : localDisputes) {
            try {
                Map<String, Object> caseResult = generateForCase(dispute.getId());
                caseResults.add(caseResult);

                List<?> gen = (List<?>) caseResult.get("generated");
                List<?> skip = (List<?>) caseResult.get("skipped");
                List<?> err = (List<?>) caseResult.get("errors");
                totalGenerated += gen.size();
                totalSkipped += skip.size();
                totalErrors += err.size();
            } catch (Exception e) {
                Map<String, Object> errorResult = new LinkedHashMap<>();
                errorResult.put("disputeId", dispute.getId());
                errorResult.put("error", e.getMessage());
                caseResults.add(errorResult);
                totalErrors++;
            }
        }

        result.put("totalGenerated", totalGenerated);
        result.put("totalSkipped", totalSkipped);
        result.put("totalErrors", totalErrors);
        result.put("cases", caseResults);
        return result;
    }

    private String buildCardholderStatementPrompt(int caseNumber, String reasonCode, String reasonName,
                                                    String merchantName, String amount, String currency) {
        return "Generate a realistic JSON document representing a CARDHOLDER DISPUTE STATEMENT for a Mastercard chargeback case.\n\n"
                + "Case details:\n"
                + "- Case reference: CASE-" + caseNumber + "\n"
                + "- Reason code: " + reasonCode + " (" + reasonName + ")\n"
                + "- Merchant: " + merchantName + "\n"
                + "- Amount: " + amount + " " + currency + "\n\n"
                + "The JSON should include these fields:\n"
                + "- source: \"Issuing Bank — Cardholder Services\"\n"
                + "- dataType: \"Cardholder Dispute Statement\"\n"
                + "- caseReference: \"CASE-" + caseNumber + "\"\n"
                + "- statement object with: cardholderName (fake name), dateSubmitted, disputeReason (matching the reason code), "
                + "detailedDescription (2-3 paragraphs of what the cardholder told the bank — make it realistic and specific to the reason code), "
                + "contactedMerchant (boolean), merchantResponseSummary (if contacted), previousDisputeHistory, "
                + "requestedResolution, cardholderDeclaration (formal statement that the info is true)\n"
                + "- bankInternalNotes object with: riskAssessment, recommendedAction, caseOfficer (fake name), priority\n\n"
                + "Make the narrative realistic and specific to reason code " + reasonCode + " (" + reasonName + "). "
                + "Use realistic dates in early 2026. Do NOT use real PII — generate fake but realistic-looking names and details.";
    }

    private String buildChargebackDocPrompt(int caseNumber, String reasonCode, String reasonName,
                                             String merchantName, String amount, String currency) {
        return "Generate a realistic JSON document representing an ISSUER CHARGEBACK DOCUMENTATION filed by the issuing bank for a Mastercard dispute.\n\n"
                + "Case details:\n"
                + "- Case reference: CASE-" + caseNumber + "\n"
                + "- Reason code: " + reasonCode + " (" + reasonName + ")\n"
                + "- Merchant: " + merchantName + "\n"
                + "- Amount: " + amount + " " + currency + "\n\n"
                + "The JSON should include:\n"
                + "- source: \"Issuing Bank — Chargeback Department\"\n"
                + "- dataType: \"Issuer Chargeback Documentation\"\n"
                + "- caseReference: \"CASE-" + caseNumber + "\"\n"
                + "- chargebackFiling object with: filingDate, chargebackAmount, currency, reasonCode, reasonCodeDescription, "
                + "issuerBIN (fake 6-digit), acquirerBIN (fake 6-digit), "
                + "cardholderNotificationDate, chargebackDeadline, "
                + "supportingDocuments array (each with documentType, description, dateProvided), "
                + "issuerCommentary (2 paragraphs explaining why the issuer is filing the chargeback, referencing specific Mastercard rules), "
                + "representmentDeadline, arbitrationEligible (boolean)\n"
                + "- transactionDetails object with: originalTransactionDate, postingDate, authorizationCode (fake), "
                + "cardPresent (boolean), ecommerceIndicator, recurringPayment (boolean)\n\n"
                + "Make it realistic for reason code " + reasonCode + " (" + reasonName + "). Use dates in early 2026.";
    }

    private String buildIssuerTransactionPrompt(int caseNumber, String reasonCode, String reasonName,
                                                  String merchantName, String amount, String currency) {
        return "Generate a realistic JSON document representing an ISSUER TRANSACTION RECORD — the issuing bank's internal view of the authorization and posting of a disputed transaction.\n\n"
                + "Case details:\n"
                + "- Case reference: CASE-" + caseNumber + "\n"
                + "- Reason code: " + reasonCode + " (" + reasonName + ")\n"
                + "- Merchant: " + merchantName + "\n"
                + "- Amount: " + amount + " " + currency + "\n\n"
                + "The JSON should include:\n"
                + "- source: \"Issuing Bank — Transaction Processing\"\n"
                + "- dataType: \"Issuer Transaction Record\"\n"
                + "- caseReference: \"CASE-" + caseNumber + "\"\n"
                + "- authorization object with: authorizationCode (fake), authorizationDate, responseCode (approved/declined), "
                + "amount, currency, merchantName, merchantCategoryCode (realistic MCC), terminalId (fake), "
                + "posEntryMode, cardNotPresent (boolean matching e-commerce), "
                + "avsResult (code + description), cvvResult (code + description)\n"
                + "- posting object with: postingDate, settledAmount, settlementDate, batchNumber (fake), "
                + "interchangeFee, networkProcessingFee\n"
                + "- cardholderAccount object with: accountType, cardProduct (e.g. World Elite, Standard), "
                + "accountStanding, previousChargebacks (count), accountAge\n"
                + "- riskFlags object with: velocityCheck, geolocationMismatch, unusualAmount, "
                + "firstTimeAtMerchant (boolean), deviceTrustScore\n\n"
                + "Make it realistic for reason code " + reasonCode + " (" + reasonName + "). Use dates in early 2026. "
                + "Include some risk signals that are relevant to this dispute type.";
    }

    private record FileSpec(String category, String fileType, String prompt) {}
}
