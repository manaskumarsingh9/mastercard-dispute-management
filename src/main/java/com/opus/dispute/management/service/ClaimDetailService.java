package com.opus.dispute.management.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opus.dispute.management.entity.*;
import com.opus.dispute.management.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ClaimDetailService {

    private static final String BASE = "/mastercom/v6";

    @Value("${claim-detail.local-fallback.enabled:false}")
    private boolean localFallbackEnabled;

    private final MastercardApiClient mastercardApiClient;
    private final DisputeRepository disputeRepository;
    private final ChargebackDetailRepository chargebackDetailRepository;
    private final RetrievalDetailRepository retrievalDetailRepository;
    private final FeeDetailRepository feeDetailRepository;
    private final CaseFilingDetailRepository caseFilingDetailRepository;
    private final Gson gson = new Gson();

    public ClaimDetailService(MastercardApiClient mastercardApiClient,
                              DisputeRepository disputeRepository,
                              ChargebackDetailRepository chargebackDetailRepository,
                              RetrievalDetailRepository retrievalDetailRepository,
                              FeeDetailRepository feeDetailRepository,
                              CaseFilingDetailRepository caseFilingDetailRepository) {
        this.mastercardApiClient = mastercardApiClient;
        this.disputeRepository = disputeRepository;
        this.chargebackDetailRepository = chargebackDetailRepository;
        this.retrievalDetailRepository = retrievalDetailRepository;
        this.feeDetailRepository = feeDetailRepository;
        this.caseFilingDetailRepository = caseFilingDetailRepository;
    }

    @PostConstruct
    public void startupTasks() {
        long deleted = chargebackDetailRepository.deleteByChargebackType("SECOND_PRESENTMENT");
        if (deleted > 0) {
            log.info("Startup cleanup: deleted {} SECOND_PRESENTMENT chargeback detail records (sandbox artifact, not real acquirer actions)", deleted);
        }
        backfillDisputeFieldsFromDetails();
    }

    @Transactional
    public void backfillDisputeFieldsFromDetails() {
        List<Dispute> disputes = disputeRepository.findAll();
        int updated = 0;
        for (Dispute dispute : disputes) {
            boolean changed = false;

            if (dispute.getChargebackDisplayId() == null || dispute.getChargebackDisplayId().isBlank()
                    || !dispute.getChargebackDisplayId().equals(dispute.getClaimId())
                    || dispute.getDisputeType() == null || dispute.getDisputeType().isBlank()) {
                List<ChargebackDetail> cbDetails = chargebackDetailRepository.findByDisputeId(dispute.getId());
                if (!cbDetails.isEmpty()) {
                    if (dispute.getChargebackDisplayId() == null || dispute.getChargebackDisplayId().isBlank()
                            || !dispute.getChargebackDisplayId().equals(dispute.getClaimId())) {
                        dispute.setChargebackDisplayId(dispute.getClaimId());
                        changed = true;
                    }

                    ChargebackDetail primary = cbDetails.stream()
                            .filter(cb -> "CHARGEBACK".equals(cb.getChargebackType()))
                            .findFirst()
                            .orElse(cbDetails.get(0));

                    if ((dispute.getDisputeType() == null || dispute.getDisputeType().isBlank())
                            && primary.getChargebackType() != null && !primary.getChargebackType().isBlank()) {
                        dispute.setDisputeType(primary.getChargebackType());
                        changed = true;
                    }
                }
            }

            if (dispute.getMerchantName() == null || dispute.getMerchantName().isBlank()) {
                List<CaseFilingDetail> cfDetails = caseFilingDetailRepository.findByDisputeId(dispute.getId());
                for (CaseFilingDetail cf : cfDetails) {
                    if (cf.getMerchantName() != null && !cf.getMerchantName().isBlank()) {
                        dispute.setMerchantName(cf.getMerchantName());
                        changed = true;
                        break;
                    }
                }
            }

            if (changed) {
                disputeRepository.save(dispute);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("Backfilled common fields for {} disputes from existing detail records", updated);
        }
    }

    @Async
    public void fetchDetailsForNewDisputesAsync(List<Long> disputeIds) {
        log.info("Starting async claim detail fetch for {} disputes", disputeIds.size());
        int success = 0;
        int failed = 0;
        for (Long disputeId : disputeIds) {
            try {
                fetchAndStoreClaimDetail(disputeId);
                success++;
            } catch (Exception e) {
                log.error("Failed to fetch claim detail for dispute {}: {}", disputeId, e.getMessage());
                failed++;
            }
        }
        log.info("Async claim detail fetch complete: {} success, {} failed", success, failed);
    }

    @Transactional
    public void fetchAndStoreClaimDetail(Long disputeId) throws Exception {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new RuntimeException("Dispute not found: " + disputeId));

        String claimId = dispute.getClaimId();
        if (claimId == null || claimId.isEmpty()) {
            log.warn("Dispute {} has no claimId, skipping detail fetch", disputeId);
            return;
        }

        log.info("Fetching claim detail for claimId={} (disputeId={})", claimId, disputeId);

        try {
            String response = mastercardApiClient.get(BASE + "/claims/" + claimId);
            JsonObject detail = JsonParser.parseString(response).getAsJsonObject();

            overwriteDisputeFieldsFromDetail(dispute, detail);

            if (detail.has("chargebackDetails") && detail.get("chargebackDetails").isJsonArray()) {
                processChargebackDetails(dispute, detail.getAsJsonArray("chargebackDetails"));
            }

            if (detail.has("retrievalDetailsList") && detail.get("retrievalDetailsList").isJsonArray()) {
                processRetrievalDetails(dispute, detail.getAsJsonArray("retrievalDetailsList"));
            } else if (detail.has("retrievalDetails") && !detail.get("retrievalDetails").isJsonNull()) {
                JsonArray singleList = new JsonArray();
                singleList.add(detail.getAsJsonObject("retrievalDetails"));
                processRetrievalDetails(dispute, singleList);
            }

            if (detail.has("feeDetails") && detail.get("feeDetails").isJsonArray()) {
                processFeeDetails(dispute, detail.getAsJsonArray("feeDetails"));
            }

            if (detail.has("caseFilingDetails") && !detail.get("caseFilingDetails").isJsonNull()) {
                processCaseFilingDetails(dispute, detail.getAsJsonObject("caseFilingDetails"));
            }

            dispute.setDetailsFetched(true);
            dispute.setLastUpdatedDate(LocalDateTime.now());
            disputeRepository.save(dispute);

            log.info("Claim detail stored for claimId={}: {} chargebacks, reasonCode={}",
                    claimId,
                    chargebackDetailRepository.findByDisputeId(disputeId).size(),
                    dispute.getReasonCode());

        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            boolean isMastercardRejection = msg.contains("INVALID_INPUT_VALUE") || msg.contains("400 Bad Request");

            if (isMastercardRejection && localFallbackEnabled) {
                log.info("Mastercard rejected claimId={}, local-fallback is ON — populating from local dispute data (disputeId={})", claimId, disputeId);
                populateDetailsFromLocalData(dispute);
                return;
            }

            throw e;
        }
    }

    @Transactional
    protected void populateDetailsFromLocalData(Dispute dispute) {
        Long disputeId = dispute.getId();

        chargebackDetailRepository.deleteByDisputeId(disputeId);
        retrievalDetailRepository.deleteByDisputeId(disputeId);
        feeDetailRepository.deleteByDisputeId(disputeId);
        caseFilingDetailRepository.deleteByDisputeId(disputeId);

        ChargebackDetail cbd = new ChargebackDetail();
        cbd.setDispute(dispute);
        cbd.setChargebackId(dispute.getChargebackDisplayId());
        cbd.setClaimId(dispute.getClaimId());
        cbd.setReasonCode(dispute.getReasonCode());
        cbd.setChargebackType("CHARGEBACK");
        cbd.setAmount(dispute.getAmount() != null ? String.valueOf(dispute.getAmount()) : dispute.getClaimValue());
        cbd.setCurrency(dispute.getCurrency());
        cbd.setCreateDate(dispute.getIngestedAt() != null ? dispute.getIngestedAt().toString() : null);
        cbd.setIsPartialChargeback(false);
        cbd.setReversed(false);
        cbd.setReversal(false);
        cbd.setDocumentIndicator(false);
        cbd.setRefundNotReceivedIndicator(false);
        cbd.setMessageText("Detail populated from local dispute record (Mastercard API claim ID not recognized)");
        cbd.setIngestedAt(LocalDateTime.now());
        chargebackDetailRepository.save(cbd);

        dispute.setDetailsFetched(true);
        dispute.setLastUpdatedDate(LocalDateTime.now());
        disputeRepository.save(dispute);

        log.info("Local fallback complete for disputeId={}: created 1 chargeback detail from local data (reasonCode={}, amount={} {})",
                disputeId, dispute.getReasonCode(), cbd.getAmount(), cbd.getCurrency());
    }

    private void processChargebackDetails(Dispute dispute, JsonArray chargebackArray) {
        chargebackDetailRepository.deleteByDisputeId(dispute.getId());

        String primaryReasonCode = null;
        Double primaryAmount = null;

        for (JsonElement element : chargebackArray) {
            JsonObject cb = element.getAsJsonObject();

            String cbType = getStr(cb, "chargebackType");
            if ("SECOND_PRESENTMENT".equals(cbType)) {
                log.debug("Skipping SECOND_PRESENTMENT chargeback detail for claim {} — acquirer action, not ingested",
                        dispute.getClaimId());
                continue;
            }

            ChargebackDetail detail = new ChargebackDetail();
            detail.setDispute(dispute);
            detail.setChargebackId(getStr(cb, "chargebackId"));
            detail.setClaimId(getStr(cb, "claimId"));
            detail.setReasonCode(getStr(cb, "reasonCode"));
            detail.setChargebackType(cbType);
            detail.setAmount(getStr(cb, "amount"));
            detail.setCurrency(getStr(cb, "currency"));
            detail.setMessageText(getStr(cb, "messageText"));
            detail.setIsPartialChargeback(getBool(cb, "isPartialChargeback"));
            detail.setReversed(getBool(cb, "reversed"));
            detail.setReversal(getBool(cb, "reversal"));
            detail.setCreateDate(getStr(cb, "createDate"));
            detail.setChargebackRefNum(getStr(cb, "chargebackRefNum"));
            detail.setDocumentStatus(getStr(cb, "documentStatus"));
            detail.setDocumentIndicator(getBool(cb, "documentIndicator"));
            detail.setReconciliationAmount(getStr(cb, "reconciliationAmount"));
            detail.setReconciliationCurrency(getStr(cb, "reconciliationCurrency"));
            detail.setEditExclusionCode(getStr(cb, "editExclusionCode"));
            detail.setRejectReason(getStr(cb, "rejectReason"));
            detail.setRefundNotReceivedIndicator(getBool(cb, "refundNotReceivedIndicator"));
            detail.setCreditVoucherStatus(getStr(cb, "creditVoucherStatus"));
            detail.setCurrencyConversionAssessmentCCAIncluded(getBool(cb, "currencyConversionAssessmentCCAIncluded"));
            detail.setCurrencyConversionAssessmentCCAAmount(getStr(cb, "currencyConversionAssessmentCCAAmount"));
            detail.setFlexCode(getStr(cb, "flexCode"));
            detail.setIngestedAt(LocalDateTime.now());

            chargebackDetailRepository.save(detail);

            if (primaryReasonCode == null && "CHARGEBACK".equals(cbType)) {
                primaryReasonCode = getStr(cb, "reasonCode");
                try {
                    primaryAmount = Double.parseDouble(getStr(cb, "amount"));
                } catch (Exception ignored) {}
            }
        }

        if (primaryReasonCode == null && chargebackArray.size() > 0) {
            JsonObject first = chargebackArray.get(0).getAsJsonObject();
            primaryReasonCode = getStr(first, "reasonCode");
            try {
                primaryAmount = Double.parseDouble(getStr(first, "amount"));
            } catch (Exception ignored) {}
        }

        if (primaryReasonCode != null && (dispute.getReasonCode() == null || dispute.getReasonCode().isBlank())) {
            dispute.setReasonCode(primaryReasonCode);
        }
        if (primaryAmount != null && dispute.getAmount() == null) {
            dispute.setAmount(primaryAmount);
            String cbCurrency = getStr(chargebackArray.get(0).getAsJsonObject(), "currency");
            if (cbCurrency != null) {
                dispute.setCurrency(cbCurrency);
            }
        }

        copyChargebackFieldsToDispute(dispute, chargebackArray);
    }

    private void copyChargebackFieldsToDispute(Dispute dispute, JsonArray chargebackArray) {
        JsonObject primaryCb = null;
        for (JsonElement el : chargebackArray) {
            JsonObject cb = el.getAsJsonObject();
            String cbType = getStr(cb, "chargebackType");
            if ("SECOND_PRESENTMENT".equals(cbType)) continue;
            if ("CHARGEBACK".equals(cbType)) {
                primaryCb = cb;
                break;
            }
            if (primaryCb == null) {
                primaryCb = cb;
            }
        }
        if (primaryCb == null && chargebackArray.size() > 0) {
            primaryCb = chargebackArray.get(0).getAsJsonObject();
        }
        if (primaryCb == null) return;

        if (dispute.getChargebackDisplayId() == null || dispute.getChargebackDisplayId().isBlank()) {
            dispute.setChargebackDisplayId(dispute.getClaimId());
        }

        if (dispute.getDisputeType() == null || dispute.getDisputeType().isBlank()) {
            String cbType = getStr(primaryCb, "chargebackType");
            if (cbType != null && !cbType.isBlank()) {
                dispute.setDisputeType(cbType);
            }
        }

        log.debug("Copied chargeback fields to dispute {}: chargebackDisplayId={}, disputeType={}",
                dispute.getId(), dispute.getChargebackDisplayId(), dispute.getDisputeType());
    }

    private void processRetrievalDetails(Dispute dispute, JsonArray retrievalArray) {
        retrievalDetailRepository.deleteByDisputeId(dispute.getId());

        for (JsonElement element : retrievalArray) {
            JsonObject rd = element.getAsJsonObject();
            RetrievalDetail detail = new RetrievalDetail();
            detail.setDispute(dispute);
            detail.setRequestId(getStr(rd, "requestId"));
            detail.setClaimId(getStr(rd, "claimId"));
            detail.setAcquirerRefNum(getStr(rd, "acquirerRefNum"));
            detail.setAcquirerResponseCd(getStr(rd, "acquirerResponseCd"));
            detail.setAcquirerMemo(getStr(rd, "acquirerMemo"));
            detail.setAcquirerResponseDt(getStr(rd, "acquirerResponseDt"));
            detail.setIssuerResponseCd(getStr(rd, "issuerResponseCd"));
            detail.setIssuerRejectRsnCd(getStr(rd, "issuerRejectRsnCd"));
            detail.setIssuerMemo(getStr(rd, "issuerMemo"));
            detail.setIssuerResponseDt(getStr(rd, "issuerResponseDt"));
            detail.setAmount(getStr(rd, "amount"));
            detail.setCurrency(getStr(rd, "currency"));
            detail.setRetrievalRequestReason(getStr(rd, "retrievalRequestReason"));
            detail.setDocNeeded(getStr(rd, "docNeeded"));
            detail.setCreateDate(getStr(rd, "createDate"));
            detail.setChargebackRefNum(getStr(rd, "chargebackRefNum"));
            detail.setCancelDate(getStr(rd, "cancelDate"));
            detail.setRejectDate(getStr(rd, "rejectDate"));
            detail.setReverseDate(getStr(rd, "reverseDate"));
            detail.setAcquirerResponseNotificationStatus(getStr(rd, "acquirerResponseNotificationStatus"));
            detail.setRejectReason(getStr(rd, "rejectReason"));
            detail.setImageReviewDecision(getStr(rd, "imageReviewDecision"));
            detail.setImageReviewDt(getStr(rd, "imageReviewDt"));
            detail.setCollaborationExpirationDateTime(getStr(rd, "collaborationExpirationDateTime"));
            detail.setFlexCode(getStr(rd, "flexCode"));
            detail.setIngestedAt(LocalDateTime.now());

            retrievalDetailRepository.save(detail);
        }
    }

    private void processFeeDetails(Dispute dispute, JsonArray feeArray) {
        feeDetailRepository.deleteByDisputeId(dispute.getId());

        for (JsonElement element : feeArray) {
            JsonObject fd = element.getAsJsonObject();
            FeeDetail detail = new FeeDetail();
            detail.setDispute(dispute);
            detail.setFeeId(getStr(fd, "feeId"));
            detail.setCardAcceptorIdCode(getStr(fd, "cardAcceptorIdCode"));
            detail.setCardNumber(getStr(fd, "cardNumber"));
            detail.setCountryCode(getStr(fd, "countryCode"));
            detail.setCurrency(getStr(fd, "currency"));
            detail.setFeeDate(getStr(fd, "feeDate"));
            detail.setDestinationMember(getStr(fd, "destinationMember"));
            detail.setFeeAmount(getStr(fd, "feeAmount"));
            detail.setCreditSender(getBool(fd, "creditSender"));
            detail.setCreditReceiver(getBool(fd, "creditReceiver"));
            detail.setReason(getStr(fd, "reason"));
            detail.setChargebackRefNum(getStr(fd, "chargebackRefNum"));
            detail.setReconciliationAmount(getStr(fd, "reconciliationAmount"));
            detail.setReconciliationCurrency(getStr(fd, "reconciliationCurrency"));
            detail.setRejectReason(getStr(fd, "rejectReason"));
            detail.setMessage(getStr(fd, "message"));
            detail.setFlexCode(getStr(fd, "flexCode"));
            detail.setIngestedAt(LocalDateTime.now());

            feeDetailRepository.save(detail);
        }
    }

    private void processCaseFilingDetails(Dispute dispute, JsonObject caseFilingObj) {
        caseFilingDetailRepository.deleteByDisputeId(dispute.getId());

        String caseFilingStatus = getStr(caseFilingObj, "caseFilingStatus");

        if (caseFilingObj.has("caseFilingDetails") && !caseFilingObj.get("caseFilingDetails").isJsonNull()) {
            JsonObject cfd = caseFilingObj.getAsJsonObject("caseFilingDetails");
            CaseFilingDetail detail = new CaseFilingDetail();
            detail.setDispute(dispute);
            detail.setCaseFilingStatus(caseFilingStatus);
            detail.setCaseId(getStr(cfd, "caseId"));
            detail.setCaseType(getStr(cfd, "caseType"));
            detail.setClaimId(getStr(cfd, "claimId"));
            detail.setClaimType(getStr(cfd, "claimType"));
            detail.setCurrencyCode(getStr(cfd, "currencyCode"));
            detail.setCustomerFilingNumber(getStr(cfd, "customerFilingNumber"));
            detail.setDisputeAmount(getStr(cfd, "disputeAmount"));
            detail.setDueDate(getStr(cfd, "dueDate"));
            detail.setFilingAgainstIca(getStr(cfd, "filingAgaintstIca"));
            detail.setFilingAs(getStr(cfd, "filingAs"));
            detail.setFilingIca(getStr(cfd, "filingIca"));
            detail.setMerchantName(getStr(cfd, "merchantName"));
            detail.setPrimaryAccountNum(getStr(cfd, "primaryAccountNum"));
            detail.setViolationCode(getStr(cfd, "violationCode"));
            detail.setViolationDate(getStr(cfd, "violationDate"));
            detail.setRulingDate(getStr(cfd, "rulingDate"));
            detail.setRulingStatus(getStr(cfd, "rulingStatus"));
            detail.setCreditDate(getStr(cfd, "creditDate"));
            detail.setChargebackDate(getStr(cfd, "chargebackDate"));
            detail.setReasonCode(getStr(cfd, "reasonCode"));
            detail.setVirtualAccountNum(getStr(cfd, "virtualAccountNum"));

            if (caseFilingObj.has("caseFilingRespHistory") && caseFilingObj.get("caseFilingRespHistory").isJsonArray()) {
                detail.setResponseHistory(gson.toJson(caseFilingObj.getAsJsonArray("caseFilingRespHistory")));
            }

            detail.setIngestedAt(LocalDateTime.now());
            caseFilingDetailRepository.save(detail);

            copyCaseFilingFieldsToDispute(dispute, cfd);
        }
    }

    private void copyCaseFilingFieldsToDispute(Dispute dispute, JsonObject caseFilingDetail) {
        if (dispute.getMerchantName() == null || dispute.getMerchantName().isBlank()) {
            String merchantName = getStr(caseFilingDetail, "merchantName");
            if (merchantName != null && !merchantName.isBlank()) {
                dispute.setMerchantName(merchantName);
                log.debug("Copied merchantName '{}' from case filing to dispute {}", merchantName, dispute.getId());
            }
        }
    }

    public ClaimDetailResult getClaimDetailSummary(Long disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId).orElse(null);
        if (dispute == null) return null;

        List<ChargebackDetail> chargebacks = chargebackDetailRepository.findByDisputeId(disputeId);
        List<RetrievalDetail> retrievals = retrievalDetailRepository.findByDisputeId(disputeId);
        List<FeeDetail> fees = feeDetailRepository.findByDisputeId(disputeId);
        List<CaseFilingDetail> caseFilings = caseFilingDetailRepository.findByDisputeId(disputeId);

        return new ClaimDetailResult(dispute, chargebacks, retrievals, fees, caseFilings);
    }

    private void overwriteDisputeFieldsFromDetail(Dispute dispute, JsonObject detail) {
        setIfPresent(detail, "claimType", dispute::setClaimType);
        setIfPresent(detail, "claimValue", dispute::setClaimValue);
        setIfPresent(detail, "primaryAccountNum", dispute::setPrimaryAccountNum);
        setIfPresent(detail, "acquirerId", dispute::setAcquirerId);
        setIfPresent(detail, "acquirerRefNum", dispute::setAcquirerRefNum);
        setIfPresent(detail, "issuerId", dispute::setIssuerId);
        setIfPresent(detail, "merchantId", dispute::setMerchantId);
        setIfPresent(detail, "transactionId", dispute::setTransactionId);
        setIfPresent(detail, "progressState", dispute::setProgressState);
        setIfPresent(detail, "clearingNetwork", dispute::setClearingNetwork);
        setIfPresent(detail, "clearingDueDate", dispute::setClearingDueDate);
        setIfPresent(detail, "dueDate", dispute::setDueDate);
        setIfPresent(detail, "createDate", dispute::setCreateDate);
        setIfPresent(detail, "lastModifiedBy", dispute::setLastModifiedBy);
        setIfPresent(detail, "auditControlNumber", dispute::setAuditControlNumber);
        setIfPresent(detail, "switchSerialNumber", dispute::setSwitchSerialNumber);

        if (detail.has("lastModifiedDate") && !detail.get("lastModifiedDate").isJsonNull()) {
            dispute.setLastModifiedMcDate(detail.get("lastModifiedDate").getAsString());
        }

        if (detail.has("isOpen") && !detail.get("isOpen").isJsonNull()) {
            dispute.setIsOpen(detail.get("isOpen").getAsBoolean());
        }
        if (detail.has("isIssuer") && !detail.get("isIssuer").isJsonNull()) {
            dispute.setIsIssuer(detail.get("isIssuer").getAsBoolean());
        }
        if (detail.has("isAcquirer") && !detail.get("isAcquirer").isJsonNull()) {
            dispute.setIsAcquirer(detail.get("isAcquirer").getAsBoolean());
        }
        if (detail.has("isAccurate") && !detail.get("isAccurate").isJsonNull()) {
            dispute.setIsAccurate(detail.get("isAccurate").getAsBoolean());
        }

        String reasonCode = getStr(detail, "reasonCode");
        if (reasonCode != null && !reasonCode.isBlank()) {
            dispute.setReasonCode(reasonCode);
        } else if (dispute.getReasonCode() == null) {
            String ps = getStr(detail, "progressState");
            if (ps != null) {
                String extracted = ClaimIngestionService.extractReasonCodeFromProgressState(ps);
                if (extracted != null) {
                    dispute.setReasonCode(extracted);
                }
            }
        }

        String claimValue = getStr(detail, "claimValue");
        if (claimValue != null) {
            try {
                String[] parts = claimValue.split(" ");
                dispute.setAmount(Double.parseDouble(parts[0]));
                if (parts.length > 1) {
                    dispute.setCurrency(parts[1]);
                }
            } catch (Exception ignored) {}
        }

        log.info("Overwrote dispute {} fields from getClaimDetail response", dispute.getId());
    }

    private void setIfPresent(JsonObject obj, String field, java.util.function.Consumer<String> setter) {
        if (obj.has(field) && !obj.get(field).isJsonNull()) {
            setter.accept(obj.get(field).getAsString());
        }
    }

    private String getStr(JsonObject obj, String field) {
        if (obj.has(field) && !obj.get(field).isJsonNull()) {
            return obj.get(field).getAsString();
        }
        return null;
    }

    private Boolean getBool(JsonObject obj, String field) {
        if (obj.has(field) && !obj.get(field).isJsonNull()) {
            String val = obj.get(field).getAsString();
            return "true".equalsIgnoreCase(val);
        }
        return null;
    }

    public static class ClaimDetailResult {
        public final Dispute dispute;
        public final List<ChargebackDetail> chargebackDetails;
        public final List<RetrievalDetail> retrievalDetails;
        public final List<FeeDetail> feeDetails;
        public final List<CaseFilingDetail> caseFilingDetails;

        public ClaimDetailResult(Dispute dispute, List<ChargebackDetail> chargebackDetails,
                                  List<RetrievalDetail> retrievalDetails, List<FeeDetail> feeDetails,
                                  List<CaseFilingDetail> caseFilingDetails) {
            this.dispute = dispute;
            this.chargebackDetails = chargebackDetails;
            this.retrievalDetails = retrievalDetails;
            this.feeDetails = feeDetails;
            this.caseFilingDetails = caseFilingDetails;
        }
    }
}
