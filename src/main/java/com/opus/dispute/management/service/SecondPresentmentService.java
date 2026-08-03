package com.opus.dispute.management.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.opus.dispute.management.entity.ChargebackDetail;
import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.repository.ChargebackDetailRepository;
import com.opus.dispute.management.repository.DisputeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class SecondPresentmentService {

    private static final String BASE = "/mastercom/v6";

    private final DisputeRepository disputeRepository;
    private final ChargebackDetailRepository chargebackDetailRepository;
    private final DataSourceService dataSourceService;
    private final MastercardApiClient mastercardApiClient;
    private final Gson gson = new Gson();

    public SecondPresentmentService(DisputeRepository disputeRepository,
                                     ChargebackDetailRepository chargebackDetailRepository,
                                     DataSourceService dataSourceService,
                                     MastercardApiClient mastercardApiClient) {
        this.disputeRepository = disputeRepository;
        this.chargebackDetailRepository = chargebackDetailRepository;
        this.dataSourceService = dataSourceService;
        this.mastercardApiClient = mastercardApiClient;
    }

    public Map<String, Object> raiseSecondPresentment(Long disputeId, String messageText) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found: " + disputeId));

        String claimId = dispute.getClaimId();
        if (claimId == null || claimId.isBlank()) {
            throw new RuntimeException("Dispute has no claim ID");
        }

        List<ChargebackDetail> chargebacks = chargebackDetailRepository.findByDisputeId(disputeId);
        ChargebackDetail originalChargeback = chargebacks.stream()
                .filter(cb -> "CHARGEBACK".equals(cb.getChargebackType()))
                .sorted(Comparator.comparing(
                        (ChargebackDetail cb) -> cb.getIngestedAt() != null ? cb.getIngestedAt() : java.time.LocalDateTime.MIN)
                        .reversed())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No original CHARGEBACK found for dispute " + disputeId +
                        ". Cannot raise second presentment without a prior chargeback."));

        String disputeChargebackId = originalChargeback.getChargebackId();
        if (disputeChargebackId == null || disputeChargebackId.isBlank()) {
            throw new IllegalStateException("Original chargeback has no chargebackId. Cannot reference for second presentment.");
        }

        String reasonCode = dispute.getReasonCode() != null ? dispute.getReasonCode() : originalChargeback.getReasonCode();
        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalStateException("No reason code available on dispute or chargeback detail. Cannot submit second presentment.");
        }

        String amount = originalChargeback.getAmount();
        if (amount == null || amount.isBlank()) {
            throw new IllegalStateException("No amount on original chargeback. Cannot submit second presentment.");
        }

        String currency = originalChargeback.getCurrency() != null ? originalChargeback.getCurrency() : "USD";

        int caseNum = dataSourceService.extractCaseNumber(dispute.getId(), dispute.getClaimId(), dispute.getCaseNumber());

        Map<String, String> textSources;
        List<MediaFile> mediaSources;

        if (caseNum >= 0) {
            textSources = dataSourceService.loadAcquirerSourcesWithFallback(caseNum, reasonCode);
            mediaSources = dataSourceService.loadAcquirerMediaWithFallback(caseNum, reasonCode);
        } else if (reasonCode != null) {
            textSources = dataSourceService.loadAcquirerSourcesForReasonCode(reasonCode);
            mediaSources = dataSourceService.loadAcquirerMediaForReasonCode(reasonCode);
        } else {
            textSources = Map.of();
            mediaSources = List.of();
        }

        if (textSources.isEmpty() && mediaSources.isEmpty()) {
            throw new RuntimeException("No acquirer evidence files found for dispute " + disputeId +
                    ". Upload evidence before raising a second presentment.");
        }

        byte[] zipBytes;
        try {
            zipBytes = buildEvidenceZip(textSources, mediaSources);
        } catch (IOException e) {
            throw new RuntimeException("Failed to build evidence ZIP: " + e.getMessage(), e);
        }

        String base64Zip = Base64.getEncoder().encodeToString(zipBytes);
        String zipFilename = claimId + "_" + (reasonCode != null ? reasonCode : "-") + "_second_presentment.zip";

        log.info("Built evidence ZIP for second presentment: dispute={}, claimId={}, " +
                        "textFiles={}, mediaFiles={}, zipSize={} bytes, base64Size={} chars",
                disputeId, claimId, textSources.size(), mediaSources.size(),
                zipBytes.length, base64Zip.length());

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("amount", amount);
        requestBody.addProperty("chargebackType", "SECOND_PRESENTMENT");
        requestBody.addProperty("currency", currency);
        requestBody.addProperty("documentIndicator", "true");
        requestBody.addProperty("reasonCode", reasonCode);
        requestBody.addProperty("disputeChargebackID", disputeChargebackId);
        requestBody.addProperty("isPartialChargeback", false);

        if (messageText != null && !messageText.isBlank()) {
            String truncated = messageText.length() > 100 ? messageText.substring(0, 100) : messageText;
            requestBody.addProperty("messageText", truncated);
        } else {
            requestBody.addProperty("messageText", "Second presentment with supporting evidence");
        }

        JsonObject fileAttachment = new JsonObject();
        fileAttachment.addProperty("filename", zipFilename);
        fileAttachment.addProperty("file", base64Zip);
        requestBody.add("fileAttachment", fileAttachment);

        log.info("Submitting second presentment to Mastercard: claimId={}, disputeChargebackID={}, " +
                        "reasonCode={}, amount={} {}",
                claimId, disputeChargebackId, reasonCode, amount, currency);

        String apiResponse;
        try {
            apiResponse = mastercardApiClient.post(
                    BASE + "/claims/" + claimId + "/chargebacks", gson.toJson(requestBody));
        } catch (Exception e) {
            log.error("Mastercard API call failed for second presentment: claimId={}", claimId, e);
            throw new RuntimeException("Failed to submit second presentment to Mastercard: " + e.getMessage(), e);
        }

        dispute.setStatus("SECOND_PRESENTMENT_SUBMITTED");
        dispute.setAction("Second presentment raised with " + textSources.size() + " text + " +
                mediaSources.size() + " media evidence files");
        disputeRepository.save(dispute);

        log.info("Second presentment submitted successfully: claimId={}, response={}",
                claimId, apiResponse);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("disputeId", disputeId);
        result.put("claimId", claimId);
        result.put("disputeChargebackID", disputeChargebackId);
        result.put("chargebackType", "SECOND_PRESENTMENT");
        result.put("reasonCode", reasonCode);
        result.put("amount", amount);
        result.put("currency", currency);
        result.put("evidenceFiles", textSources.size() + mediaSources.size());
        result.put("zipFilename", zipFilename);
        result.put("zipSizeBytes", zipBytes.length);
        result.put("mastercardResponse", apiResponse);
        result.put("status", "SUBMITTED");

        return result;
    }

    private byte[] buildEvidenceZip(Map<String, String> textSources, List<MediaFile> mediaSources) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, String> entry : textSources.entrySet()) {
                String path = entry.getKey().endsWith(".json") ? entry.getKey() : entry.getKey() + ".json";
                zos.putNextEntry(new ZipEntry(path));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            for (MediaFile mf : mediaSources) {
                zos.putNextEntry(new ZipEntry(mf.name()));
                zos.write(mf.data());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
