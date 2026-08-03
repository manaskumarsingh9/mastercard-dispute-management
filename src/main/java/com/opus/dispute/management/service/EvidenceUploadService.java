package com.opus.dispute.management.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.entity.EvidenceMap;
import com.opus.dispute.management.repository.DisputeRepository;
import com.opus.dispute.management.repository.EvidenceMapRepository;
import com.opus.dispute.management.service.agent.ChallengeAdvisorAgent;
import com.opus.dispute.management.service.agent.EvidenceStrategistAgent;
import com.opus.dispute.management.service.agent.MerchantSummaryAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

@Slf4j
@Service
public class EvidenceUploadService {

    private static final Path ACQUIRER_ROOT = Paths.get("src/data/sources/acquirer");
    private static final Set<String> VALID_CATEGORIES = Set.of(
            "customer-comms", "device", "fraud-tools", "identity", "merchant", "psp", "shipping"
    );
    private static final Set<String> BINARY_MEDIA_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "pdf"
    );

    private final DisputeRepository disputeRepository;
    private final EvidenceMapRepository evidenceMapRepository;
    private final DataSourceService dataSourceService;
    private final ChallengeAdvisorAgent challengeAdvisorAgent;
    private final EvidenceStrategistAgent evidenceStrategistAgent;
    private final MerchantSummaryAgent merchantSummaryAgent;
    private final Gson gson = new Gson();

    public EvidenceUploadService(DisputeRepository disputeRepository,
                                  EvidenceMapRepository evidenceMapRepository,
                                  DataSourceService dataSourceService,
                                  ChallengeAdvisorAgent challengeAdvisorAgent,
                                  EvidenceStrategistAgent evidenceStrategistAgent,
                                  MerchantSummaryAgent merchantSummaryAgent) {
        this.disputeRepository = disputeRepository;
        this.evidenceMapRepository = evidenceMapRepository;
        this.dataSourceService = dataSourceService;
        this.challengeAdvisorAgent = challengeAdvisorAgent;
        this.evidenceStrategistAgent = evidenceStrategistAgent;
        this.merchantSummaryAgent = merchantSummaryAgent;
    }

    public Map<String, Object> getEvidenceGaps(Long disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found: " + disputeId));

        EvidenceMap evidenceMap = evidenceMapRepository.findByDisputeId(disputeId).orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("disputeId", disputeId);
        result.put("claimId", dispute.getClaimId());

        List<Map<String, Object>> gaps = new ArrayList<>();

        if (evidenceMap != null && evidenceMap.getAnnotatedMap() != null) {
            try {
                JsonObject annotated = JsonParser.parseString(evidenceMap.getAnnotatedMap()).getAsJsonObject();

                if (annotated.has("evidenceItems")) {
                    JsonArray items = annotated.getAsJsonArray("evidenceItems");
                    for (JsonElement el : items) {
                        JsonObject item = el.getAsJsonObject();
                        String status = item.has("status") ? item.get("status").getAsString() : "";
                        if ("MISSING".equalsIgnoreCase(status)) {
                            Map<String, Object> gap = new LinkedHashMap<>();
                            gap.put("type", getStr(item, "type"));
                            gap.put("source", getStr(item, "source"));
                            gap.put("priority", getStr(item, "priority"));
                            gap.put("impact", getStr(item, "impact"));
                            gap.put("summary", getStr(item, "summary"));
                            gaps.add(gap);
                        }
                    }
                }

                if (annotated.has("criticalGaps")) {
                    List<String> criticalGaps = new ArrayList<>();
                    JsonArray cg = annotated.getAsJsonArray("criticalGaps");
                    for (JsonElement el : cg) {
                        criticalGaps.add(el.getAsString());
                    }
                    result.put("criticalGaps", criticalGaps);
                }

                if (annotated.has("manualUploadSuggestions")) {
                    List<String> suggestions = new ArrayList<>();
                    JsonArray su = annotated.getAsJsonArray("manualUploadSuggestions");
                    for (JsonElement el : su) {
                        suggestions.add(el.getAsString());
                    }
                    result.put("manualUploadSuggestions", suggestions);
                }

                if (annotated.has("completenessScore")) {
                    result.put("completenessScore", annotated.get("completenessScore").getAsInt());
                }

            } catch (Exception e) {
                log.warn("Failed to parse annotated map for dispute {}: {}", disputeId, e.getMessage());
            }
        }

        if (evidenceMap != null && evidenceMap.getFetchPlan() != null) {
            try {
                JsonObject fetchPlan = JsonParser.parseString(evidenceMap.getFetchPlan()).getAsJsonObject();

                addMissingFromPlan(fetchPlan, "requiredSources", gaps);
                addMissingFromPlan(fetchPlan, "conditionalSources", gaps);

            } catch (Exception e) {
                log.warn("Failed to parse fetch plan for dispute {}: {}", disputeId, e.getMessage());
            }
        }

        result.put("missingEvidence", gaps);
        result.put("totalMissing", gaps.size());
        result.put("validUploadCategories", VALID_CATEGORIES);
        result.put("enrichmentStatus", evidenceMap != null ? evidenceMap.getEnrichmentStatus() : "NOT_RUN");

        return result;
    }

    private void addMissingFromPlan(JsonObject fetchPlan, String field, List<Map<String, Object>> gaps) {
        if (!fetchPlan.has(field)) return;
        JsonArray arr = fetchPlan.getAsJsonArray(field);
        for (JsonElement el : arr) {
            JsonObject src = el.getAsJsonObject();
            boolean found = src.has("found") && src.get("found").getAsBoolean();
            if (!found) {
                String sourceKey = getStr(src, "source") + "/" + getStr(src, "file").replace(".json", "");
                String normalizedKey = sourceKey.replace(".json", "");
                boolean alreadyListed = gaps.stream()
                        .anyMatch(g -> {
                            String existingSource = String.valueOf(g.get("source"));
                            String normalizedExisting = existingSource.replace(".json", "");
                            return normalizedExisting.equals(normalizedKey) || normalizedExisting.contains(normalizedKey)
                                    || normalizedKey.contains(normalizedExisting);
                        });
                if (!alreadyListed) {
                    Map<String, Object> gap = new LinkedHashMap<>();
                    gap.put("type", getStr(src, "label"));
                    gap.put("source", sourceKey);
                    gap.put("priority", getStr(src, "priority"));
                    gap.put("impact", "Required by reason code rules but not found in evidence data");
                    gap.put("summary", "This evidence file was expected based on the reason code but was not available");
                    gaps.add(gap);
                }
            }
        }
    }

    public Map<String, Object> uploadEvidence(Long disputeId, String category, String evidenceName,
                                                MultipartFile file, boolean reRunAgents) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found: " + disputeId));

        if (!VALID_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Invalid category '" + category +
                    "'. Valid categories: " + VALID_CATEGORIES);
        }

        int caseNumber = dataSourceService.extractCaseNumber(dispute.getId(), dispute.getClaimId(), dispute.getCaseNumber());
        if (caseNumber < 0) {
            caseNumber = dispute.getId().intValue();
        }

        String sanitizedName = evidenceName.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
        Path targetDir = ACQUIRER_ROOT.resolve(category);

        String originalFilename = file.getOriginalFilename();
        String fileExtension = extractExtension(originalFilename);
        boolean isBinaryMedia = fileExtension != null && BINARY_MEDIA_EXTENSIONS.contains(fileExtension.toLowerCase());

        String fileName;
        if (isBinaryMedia) {
            fileName = sanitizedName + "_case_" + caseNumber + "." + fileExtension.toLowerCase();
        } else {
            fileName = sanitizedName + "_case_" + caseNumber + ".json";
        }
        Path targetFile = targetDir.resolve(fileName);

        try {
            Files.createDirectories(targetDir);

            if (isBinaryMedia) {
                Files.write(targetFile, file.getBytes());
                log.info("Binary media file uploaded: {} for dispute {} (case {}, {} bytes)",
                        targetFile, disputeId, caseNumber, file.getSize());
            } else {
                String content = new String(file.getBytes());

                try {
                    JsonParser.parseString(content);
                } catch (Exception e) {
                    JsonObject wrapper = new JsonObject();
                    wrapper.addProperty("source", "Manual Upload");
                    wrapper.addProperty("dataType", evidenceName);
                    wrapper.addProperty("uploadedAt", LocalDateTime.now().toString());
                    wrapper.addProperty("category", category);
                    wrapper.addProperty("content", content);
                    content = gson.toJson(wrapper);
                }

                Files.writeString(targetFile, content);
                log.info("Text evidence file uploaded: {} for dispute {} (case {})", targetFile, disputeId, caseNumber);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to save evidence file: " + e.getMessage(), e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("disputeId", disputeId);
        result.put("claimId", dispute.getClaimId());
        result.put("category", category);
        result.put("evidenceName", sanitizedName);
        result.put("savedAs", targetFile.toString());
        result.put("caseNumber", caseNumber);
        result.put("uploadedAt", LocalDateTime.now().toString());

        if (reRunAgents) {
            try {
                log.info("Re-running pipeline Agent 2→3a→4 after evidence upload for dispute {}", disputeId);

                log.info("Pipeline [{}]: Agent 2 (EvidenceStrategist) starting", disputeId);
                EvidenceMap updatedMap = evidenceStrategistAgent.enrichEvidence(disputeId);
                result.put("reEnrichmentStatus", "COMPLETED");
                result.put("newCompletenessScore", extractCompletenessScore(updatedMap));

                log.info("Pipeline [{}]: Agent 3a (MerchantSummary) starting", disputeId);
                String merchantSummary = merchantSummaryAgent.generateSummary(dispute, updatedMap);
                result.put("reMerchantSummaryStatus", "COMPLETED");

                log.info("Pipeline [{}]: Agent 4 (ChallengeAdvisor) starting", disputeId);
                String recommendation = challengeAdvisorAgent.recommend(dispute, updatedMap);
                result.put("reRecommendationStatus", "COMPLETED");
                result.put("updatedRecommendation", recommendation);
                result.put("updatedRecommendationStrength", updatedMap.getRecommendationStrength());

                log.info("Pipeline 2→3a→4 complete for dispute {}", disputeId);

            } catch (Exception e) {
                log.error("Agent pipeline re-run failed after evidence upload for dispute {}", disputeId, e);
                result.put("reRunStatus", "FAILED");
                result.put("reRunError", e.getMessage());
            }
        }

        return result;
    }

    public Map<String, Object> uploadMultipleEvidence(Long disputeId, List<UploadItem> items, boolean reRunAgents) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("disputeId", disputeId);

        List<Map<String, Object>> uploadResults = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (UploadItem item : items) {
            try {
                Map<String, Object> uploadResult = uploadEvidence(disputeId, item.category, item.evidenceName,
                        item.file, false);
                uploadResult.put("status", "SUCCESS");
                uploadResults.add(uploadResult);
                successCount++;
            } catch (Exception e) {
                Map<String, Object> errorResult = new LinkedHashMap<>();
                errorResult.put("category", item.category);
                errorResult.put("evidenceName", item.evidenceName);
                errorResult.put("status", "FAILED");
                errorResult.put("error", e.getMessage());
                uploadResults.add(errorResult);
                failCount++;
            }
        }

        result.put("uploads", uploadResults);
        result.put("totalUploaded", successCount);
        result.put("totalFailed", failCount);

        if (reRunAgents && successCount > 0) {
            try {
                Dispute dispute = disputeRepository.findById(disputeId)
                        .orElseThrow(() -> new RuntimeException("Dispute not found: " + disputeId));

                log.info("Re-running pipeline Agent 2→3a→4 after batch upload for dispute {}", disputeId);

                log.info("Pipeline [{}]: Agent 2 (EvidenceStrategist) starting", disputeId);
                EvidenceMap updatedMap = evidenceStrategistAgent.enrichEvidence(disputeId);
                result.put("reEnrichmentStatus", "COMPLETED");
                result.put("newCompletenessScore", extractCompletenessScore(updatedMap));

                log.info("Pipeline [{}]: Agent 3a (MerchantSummary) starting", disputeId);
                merchantSummaryAgent.generateSummary(dispute, updatedMap);
                result.put("reMerchantSummaryStatus", "COMPLETED");

                log.info("Pipeline [{}]: Agent 4 (ChallengeAdvisor) starting", disputeId);
                String recommendation = challengeAdvisorAgent.recommend(dispute, updatedMap);
                result.put("reRecommendationStatus", "COMPLETED");
                result.put("updatedRecommendation", recommendation);
                result.put("updatedRecommendationStrength", updatedMap.getRecommendationStrength());

                log.info("Pipeline 2→3a→4 complete for dispute {}", disputeId);

            } catch (Exception e) {
                log.error("Agent re-run failed after batch upload for dispute {}", disputeId, e);
                result.put("reRunStatus", "FAILED");
                result.put("reRunError", e.getMessage());
            }
        }

        return result;
    }

    private Integer extractCompletenessScore(EvidenceMap evidenceMap) {
        if (evidenceMap.getAnnotatedMap() == null) return null;
        try {
            JsonObject annotated = JsonParser.parseString(evidenceMap.getAnnotatedMap()).getAsJsonObject();
            if (annotated.has("completenessScore")) {
                return annotated.get("completenessScore").getAsInt();
            }
        } catch (Exception e) {
            log.warn("Could not extract completeness score: {}", e.getMessage());
        }
        return null;
    }

    private String getStr(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isEmpty()) return null;
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) return null;
        return filename.substring(lastDot + 1);
    }

    public record UploadItem(String category, String evidenceName, MultipartFile file) {}
}
