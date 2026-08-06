package com.opus.dispute.management.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.entity.IngestionState;
import com.opus.dispute.management.repository.DisputeRepository;
import com.opus.dispute.management.repository.IngestionStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ClaimIngestionService {

    private static final String BASE = "/mastercom/v6";
    private static final String INGESTION_STATE_ID = "queue-ingestion";
    private static final DateTimeFormatter MC_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final String[] QUEUES_TO_POLL = {"Pending", "Rejects", "Unworked"};

    @Value("${ingestion.max-new-claims:3}")
    private int maxNewClaims;

    private final MastercardApiClient mastercardApiClient;
    private final DisputeRepository disputeRepository;
    private final IngestionStateRepository ingestionStateRepository;
    private final ClaimDetailService claimDetailService;
    private final ReasonCodeRulesService reasonCodeRulesService;
    private final Gson gson = new Gson();

    public ClaimIngestionService(MastercardApiClient mastercardApiClient,
                                  DisputeRepository disputeRepository,
                                  IngestionStateRepository ingestionStateRepository,
                                  ClaimDetailService claimDetailService,
                                  ReasonCodeRulesService reasonCodeRulesService) {
        this.mastercardApiClient = mastercardApiClient;
        this.disputeRepository = disputeRepository;
        this.ingestionStateRepository = ingestionStateRepository;
        this.claimDetailService = claimDetailService;
        this.reasonCodeRulesService = reasonCodeRulesService;
    }

    public IngestionResult ingestFromQueues(String dateFrom, String dateTo) {
        int totalNew = 0;
        int totalUpdated = 0;
        int totalSkipped = 0;
        int totalCapped = 0;
        List<String> errors = new ArrayList<>();
        List<Long> allNewDisputeIds = new ArrayList<>();
        int remainingNewSlots = maxNewClaims;

        log.info("Ingestion cap: max {} new claims per run", maxNewClaims);

        for (String queueName : QUEUES_TO_POLL) {
            if (remainingNewSlots <= 0) {
                log.info("Skipping queue '{}': new-claim cap ({}) already reached", queueName, maxNewClaims);
                continue;
            }
            try {
                QueueResult result = ingestQueue(queueName, dateFrom, dateTo, remainingNewSlots);
                totalNew += result.newClaims;
                totalUpdated += result.updatedClaims;
                totalSkipped += result.skipped;
                totalCapped += result.capped;
                remainingNewSlots -= result.newClaims;
                allNewDisputeIds.addAll(result.newDisputeIds);
                log.info("Queue '{}': {} new, {} updated, {} skipped, {} capped (ignored)",
                        queueName, result.newClaims, result.updatedClaims, result.skipped, result.capped);
            } catch (Exception e) {
                String errorMsg = "Failed to ingest queue '" + queueName + "': " + e.getMessage();
                log.error(errorMsg, e);
                errors.add(errorMsg);
            }
        }

        if (totalCapped > 0) {
            log.info("Ingestion complete: {} new claims accepted, {} claims ignored due to cap of {}",
                    totalNew, totalCapped, maxNewClaims);
        }

        updateIngestionState(dateFrom, dateTo, totalNew + totalUpdated);

        return new IngestionResult(totalNew, totalUpdated, totalSkipped, totalCapped, errors, allNewDisputeIds);
    }

    public IngestionResult ingestScheduled() {
        IngestionState state = ingestionStateRepository.findById(INGESTION_STATE_ID).orElse(null);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from;

        if (state != null && state.getLastPolledTo() != null) {
            from = state.getLastPolledTo();
        } else {
            from = now.minusDays(5);
        }

        if (from.isBefore(now.minusDays(5))) {
            from = now.minusDays(5);
        }

        String dateFrom = from.format(MC_DATE_FORMAT);
        String dateTo = now.format(MC_DATE_FORMAT);

        log.info("Scheduled ingestion: {} to {}", dateFrom, dateTo);
        return ingestFromQueues(dateFrom, dateTo);
    }

    public IngestionResult ingestOnDemand(String queueName, String dateFrom, String dateTo, String pageNb) {
        int totalNew = 0;
        int totalUpdated = 0;
        int totalSkipped = 0;
        int totalCapped = 0;
        List<String> errors = new ArrayList<>();
        List<Long> newDisputeIds = new ArrayList<>();

        if (queueName != null && !queueName.isEmpty()) {
            try {
                QueueResult result = ingestQueue(queueName, dateFrom, dateTo, maxNewClaims);
                totalNew = result.newClaims;
                totalUpdated = result.updatedClaims;
                totalSkipped = result.skipped;
                totalCapped = result.capped;
                newDisputeIds.addAll(result.newDisputeIds);
            } catch (Exception e) {
                errors.add("Failed to ingest queue '" + queueName + "': " + e.getMessage());
            }
        } else {
            return ingestFromQueues(dateFrom, dateTo);
        }

        return new IngestionResult(totalNew, totalUpdated, totalSkipped, totalCapped, errors, newDisputeIds);
    }

    private QueueResult ingestQueue(String queueName, String dateFrom, String dateTo, int newClaimSlots) throws Exception {
        int newClaims = 0;
        int updatedClaims = 0;
        int skipped = 0;
        int capped = 0;
        int currentPage = 1;
        int totalPages = 1;
        boolean capReached = false;
        List<Long> newDisputeIds = new ArrayList<>();

        while (currentPage <= totalPages && !capReached) {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("queueName", queueName);
            if (dateFrom != null && dateTo != null) {
                requestBody.addProperty("lastModifiedDateFrom", dateFrom);
                requestBody.addProperty("lastModifiedDateTo", dateTo);
            }
            requestBody.addProperty("pageNb", String.valueOf(currentPage));

            String response = mastercardApiClient.post(BASE + "/queues", gson.toJson(requestBody));
            JsonObject responseObj = JsonParser.parseString(response).getAsJsonObject();

            if (responseObj.has("pageCount")) {
                totalPages = Integer.parseInt(responseObj.get("pageCount").getAsString());
            }

            if (responseObj.has("claimList")) {
                JsonArray claimList = responseObj.getAsJsonArray("claimList");
                int totalInResponse = claimList.size();
                log.info("Queue '{}' page {}/{}: received {} claims from Mastercard", queueName, currentPage, totalPages, totalInResponse);

                for (JsonElement element : claimList) {
                    JsonObject claim = element.getAsJsonObject();
                    String claimId = getStringField(claim, "claimId");

                    if (claimId == null || claimId.isEmpty()) {
                        skipped++;
                        continue;
                    }

                    Dispute existing = disputeRepository.findByClaimId(claimId).orElse(null);
                    if (existing != null) {
                        updateDisputeFromClaim(existing, claim, queueName);
                        disputeRepository.save(existing);
                        updatedClaims++;
                    } else {
                        if (newClaims >= newClaimSlots) {
                            capped++;
                            continue;
                        }
                        Dispute dispute = createDisputeFromClaim(claim, queueName);
                        Dispute saved = disputeRepository.save(dispute);
                        log.info("New dispute saved (id={}), now fetching claim details from Mastercard...", saved.getId());
                        try {
                            claimDetailService.fetchAndStoreClaimDetail(saved.getId());
                            log.info("Claim details fetched and stored for dispute {} (claimId={}, reasonCode={})",
                                    saved.getId(), saved.getClaimId(),
                                    disputeRepository.findById(saved.getId()).map(Dispute::getReasonCode).orElse("-"));
                        } catch (Exception detailEx) {
                            log.warn("Could not fetch claim details for dispute {} (claimId={}): {}. Dispute record is saved, details can be fetched later.",
                                    saved.getId(), saved.getClaimId(), detailEx.getMessage());
                        }
                        Dispute refreshed = disputeRepository.findById(saved.getId()).orElse(saved);
                        String rc = refreshed.getReasonCode();
                        if (rc == null || rc.isBlank() || !reasonCodeRulesService.getSupportedReasonCodes().contains(rc)) {
                            log.info("Dispute {} (claimId={}) has unsupported reason code '{}' — deleting",
                                    saved.getId(), saved.getClaimId(), rc);
                            disputeRepository.deleteById(saved.getId());
                            skipped++;
                            continue;
                        }
                        newDisputeIds.add(saved.getId());
                        newClaims++;
                        if (newClaims >= newClaimSlots) {
                            capReached = true;
                            log.info("New-claim cap reached ({}) for queue '{}'; remaining claims on this and subsequent pages will be ignored",
                                    newClaimSlots, queueName);
                        }
                    }
                }
            }

            currentPage++;
        }

        return new QueueResult(newClaims, updatedClaims, skipped, capped, newDisputeIds);
    }

    private Dispute createDisputeFromClaim(JsonObject claim, String queueName) {
        Dispute dispute = new Dispute();
        dispute.setClaimId(getStringField(claim, "claimId"));
        populateDisputeFields(dispute, claim, queueName);
        dispute.setIngestedAt(LocalDateTime.now());
        dispute.setStatus("NEW");
        dispute.setDetailsFetched(false);
        return dispute;
    }

    private void updateDisputeFromClaim(Dispute dispute, JsonObject claim, String queueName) {
        populateDisputeFields(dispute, claim, queueName);
        dispute.setLastUpdatedDate(LocalDateTime.now());
    }

    private void populateDisputeFields(Dispute dispute, JsonObject claim, String queueName) {
        dispute.setClaimType(getStringField(claim, "claimType"));
        dispute.setClaimValue(getStringField(claim, "claimValue"));
        dispute.setPrimaryAccountNum(getStringField(claim, "primaryAccountNum"));
        dispute.setAcquirerId(getStringField(claim, "acquirerId"));
        dispute.setAcquirerRefNum(getStringField(claim, "acquirerRefNum"));
        dispute.setIssuerId(getStringField(claim, "issuerId"));
        dispute.setMerchantId(getStringField(claim, "merchantId"));
        dispute.setTransactionId(getStringField(claim, "transactionId"));
        String incomingReasonCode = getStringField(claim, "reasonCode");
        if (incomingReasonCode != null && !incomingReasonCode.isBlank()) {
            dispute.setReasonCode(incomingReasonCode);
        } else if (dispute.getReasonCode() == null) {
            String progressState = getStringField(claim, "progressState");
            if (progressState != null) {
                String extracted = extractReasonCodeFromProgressState(progressState);
                if (extracted != null) {
                    dispute.setReasonCode(extracted);
                }
            }
        }
        dispute.setProgressState(getStringField(claim, "progressState"));
        dispute.setQueueName(queueName);
        dispute.setClearingNetwork(getStringField(claim, "clearingNetwork"));
        dispute.setClearingDueDate(getStringField(claim, "clearingDueDate"));
        dispute.setDueDate(getStringField(claim, "dueDate"));
        dispute.setCreateDate(getStringField(claim, "createDate"));
        dispute.setLastModifiedBy(getStringField(claim, "lastModifiedBy"));
        dispute.setLastModifiedMcDate(getStringField(claim, "lastModifiedDate"));

        if (claim.has("isOpen")) {
            dispute.setIsOpen(claim.get("isOpen").getAsBoolean());
        }
        if (claim.has("isIssuer")) {
            dispute.setIsIssuer(claim.get("isIssuer").getAsBoolean());
        }
        if (claim.has("isAcquirer")) {
            dispute.setIsAcquirer(claim.get("isAcquirer").getAsBoolean());
        }
        if (claim.has("isAccurate")) {
            dispute.setIsAccurate(claim.get("isAccurate").getAsBoolean());
        }

        String claimValue = getStringField(claim, "claimValue");
        if (claimValue != null && dispute.getAmount() == null) {
            try {
                String[] parts = claimValue.split(" ");
                dispute.setAmount(Double.parseDouble(parts[0]));
                if (parts.length > 1) {
                    dispute.setCurrency(parts[1]);
                }
            } catch (Exception ignored) {}
        }
    }

    private String getStringField(JsonObject obj, String field) {
        if (obj.has(field) && !obj.get(field).isJsonNull()) {
            return obj.get(field).getAsString();
        }
        return null;
    }

    private void updateIngestionState(String dateFrom, String dateTo, int claimsIngested) {
        IngestionState state = ingestionStateRepository.findById(INGESTION_STATE_ID)
                .orElse(new IngestionState(INGESTION_STATE_ID, null, null, null, 0, 0));

        try {
            state.setLastPolledFrom(LocalDateTime.parse(dateFrom, MC_DATE_FORMAT));
            state.setLastPolledTo(LocalDateTime.parse(dateTo, MC_DATE_FORMAT));
        } catch (Exception e) {
            log.warn("Could not parse ingestion dates: {} / {}", dateFrom, dateTo);
        }
        state.setLastRunAt(LocalDateTime.now());
        state.setClaimsIngested(state.getClaimsIngested() + claimsIngested);
        state.setTotalRuns(state.getTotalRuns() + 1);

        ingestionStateRepository.save(state);
    }

    public IngestionState getIngestionState() {
        return ingestionStateRepository.findById(INGESTION_STATE_ID).orElse(null);
    }

    public List<String> getAvailableQueues() {
        try {
            String response = mastercardApiClient.get(BASE + "/queues/names");
            JsonArray queues = JsonParser.parseString(response).getAsJsonArray();
            List<String> queueNames = new ArrayList<>();
            for (JsonElement element : queues) {
                JsonObject q = element.getAsJsonObject();
                if (q.has("queueName")) {
                    queueNames.add(q.get("queueName").getAsString());
                }
            }
            return queueNames;
        } catch (Exception e) {
            log.error("Failed to get queue names", e);
            throw new RuntimeException("Failed to get queue names: " + e.getMessage());
        }
    }

    public static String extractReasonCodeFromProgressState(String progressState) {
        if (progressState == null) return null;
        String[] parts = progressState.split("-");
        for (String part : parts) {
            if (part.matches("\\d{4}")) {
                return part;
            }
        }
        return null;
    }

    public static class IngestionResult {
        public final int newClaims;
        public final int updatedClaims;
        public final int skipped;
        public final int capped;
        public final List<String> errors;
        public final List<Long> newDisputeIds;

        public IngestionResult(int newClaims, int updatedClaims, int skipped, int capped, List<String> errors, List<Long> newDisputeIds) {
            this.newClaims = newClaims;
            this.updatedClaims = updatedClaims;
            this.skipped = skipped;
            this.capped = capped;
            this.errors = errors;
            this.newDisputeIds = newDisputeIds;
        }
    }

    private static class QueueResult {
        public final int newClaims;
        public final int updatedClaims;
        public final int skipped;
        public final int capped;
        public final List<Long> newDisputeIds;

        public QueueResult(int newClaims, int updatedClaims, int skipped, int capped, List<Long> newDisputeIds) {
            this.newClaims = newClaims;
            this.updatedClaims = updatedClaims;
            this.skipped = skipped;
            this.capped = capped;
            this.newDisputeIds = newDisputeIds;
        }
    }
}
