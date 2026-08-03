package com.opus.dispute.management.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class EvidenceEngineService {

    private final Gson gson = new Gson();

    public JsonObject fetchEvidence(String claimId, List<String> requiredEvidence, JsonObject claimData) {
        JsonObject results = new JsonObject();
        JsonArray fetched = new JsonArray();
        JsonArray missing = new JsonArray();

        for (String evidenceType : requiredEvidence) {
            JsonObject item = new JsonObject();
            item.addProperty("evidenceType", evidenceType);
            item.addProperty("requestedAt", LocalDateTime.now().toString());

            JsonObject fetchResult = fetchFromAdapter(evidenceType, claimData);

            if (fetchResult != null && fetchResult.has("data")) {
                item.addProperty("status", "RETRIEVED");
                item.add("data", fetchResult.get("data"));
                item.addProperty("source", fetchResult.has("source") ? fetchResult.get("source").getAsString() : "MOCK_ADAPTER");
                item.addProperty("retrievedAt", LocalDateTime.now().toString());
                fetched.add(item);
            } else {
                item.addProperty("status", "UNAVAILABLE");
                item.addProperty("reason", fetchResult != null && fetchResult.has("reason")
                        ? fetchResult.get("reason").getAsString()
                        : "No adapter configured for this evidence type");
                missing.add(item);
            }
        }

        results.add("fetched", fetched);
        results.add("missing", missing);
        results.addProperty("totalRequested", requiredEvidence.size());
        results.addProperty("totalFetched", fetched.size());
        results.addProperty("totalMissing", missing.size());
        results.addProperty("fetchedAt", LocalDateTime.now().toString());

        return results;
    }

    private JsonObject fetchFromAdapter(String evidenceType, JsonObject claimData) {
        String type = evidenceType.toLowerCase();

        if (type.contains("authorization") || type.contains("auth_record")) {
            return mockAuthorizationData(claimData);
        }
        if (type.contains("3ds") || type.contains("3d_secure") || type.contains("authentication")) {
            return mock3dsData(claimData);
        }
        if (type.contains("avs") || type.contains("address_verification")) {
            return mockAvsData(claimData);
        }
        if (type.contains("cvv") || type.contains("card_verification")) {
            return mockCvvData(claimData);
        }
        if (type.contains("delivery") || type.contains("shipping") || type.contains("tracking")) {
            return mockDeliveryData(claimData);
        }
        if (type.contains("refund") || type.contains("credit")) {
            return mockRefundData(claimData);
        }
        if (type.contains("customer_communication") || type.contains("correspondence")) {
            return mockCommunicationData(claimData);
        }
        if (type.contains("transaction") || type.contains("payment")) {
            return mockTransactionData(claimData);
        }
        if (type.contains("ip") || type.contains("device") || type.contains("fingerprint")) {
            return mockDeviceData(claimData);
        }
        if (type.contains("risk_score") || type.contains("fraud_score")) {
            return mockRiskScoreData(claimData);
        }

        JsonObject result = new JsonObject();
        result.addProperty("reason", "No adapter available for evidence type: " + evidenceType + ". Merchant may need to upload manually.");
        return result;
    }

    private JsonObject mockAuthorizationData(JsonObject claimData) {
        JsonObject result = new JsonObject();
        JsonObject data = new JsonObject();
        data.addProperty("authorizationCode", "AUTH" + System.currentTimeMillis() % 100000);
        data.addProperty("authorizationDate", "2026-03-15T14:30:00");
        data.addProperty("responseCode", "00");
        data.addProperty("responseDescription", "Approved");
        data.addProperty("amount", claimData.has("claimValue") ? claimData.get("claimValue").getAsString() : "0.00");
        data.addProperty("merchantName", claimData.has("merchantId") ? claimData.get("merchantId").getAsString() : "Unknown");
        result.add("data", data);
        result.addProperty("source", "PSP_ADAPTER");
        return result;
    }

    private JsonObject mock3dsData(JsonObject claimData) {
        JsonObject result = new JsonObject();
        JsonObject data = new JsonObject();
        data.addProperty("threeDsVersion", "2.2.0");
        data.addProperty("authenticationStatus", "Y");
        data.addProperty("eci", "05");
        data.addProperty("cavv", "AAABB" + System.currentTimeMillis() % 10000);
        data.addProperty("dsTransactionId", "DS-" + System.currentTimeMillis());
        data.addProperty("liabilityShift", true);
        result.add("data", data);
        result.addProperty("source", "PSP_ADAPTER");
        return result;
    }

    private JsonObject mockAvsData(JsonObject claimData) {
        JsonObject result = new JsonObject();
        JsonObject data = new JsonObject();
        data.addProperty("avsResponseCode", "Y");
        data.addProperty("avsDescription", "Address and ZIP match");
        data.addProperty("streetMatch", true);
        data.addProperty("zipMatch", true);
        result.add("data", data);
        result.addProperty("source", "PSP_ADAPTER");
        return result;
    }

    private JsonObject mockCvvData(JsonObject claimData) {
        JsonObject result = new JsonObject();
        JsonObject data = new JsonObject();
        data.addProperty("cvvResponseCode", "M");
        data.addProperty("cvvDescription", "CVV Match");
        data.addProperty("cvvPresent", true);
        result.add("data", data);
        result.addProperty("source", "PSP_ADAPTER");
        return result;
    }

    private JsonObject mockDeliveryData(JsonObject claimData) {
        JsonObject result = new JsonObject();
        JsonObject data = new JsonObject();
        data.addProperty("trackingNumber", "TRK" + System.currentTimeMillis() % 1000000);
        data.addProperty("carrier", "FedEx");
        data.addProperty("deliveryStatus", "DELIVERED");
        data.addProperty("deliveryDate", "2026-03-18T10:45:00");
        data.addProperty("signedBy", "J. SMITH");
        data.addProperty("deliveryAddress", "[ADDRESS_ON_FILE]");
        result.add("data", data);
        result.addProperty("source", "DELIVERY_PARTNER_ADAPTER");
        return result;
    }

    private JsonObject mockRefundData(JsonObject claimData) {
        JsonObject result = new JsonObject();
        JsonObject data = new JsonObject();
        data.addProperty("refundIssued", false);
        data.addProperty("previousRefunds", 0);
        data.addProperty("refundPolicy", "30-day return policy");
        result.add("data", data);
        result.addProperty("source", "PSP_ADAPTER");
        return result;
    }

    private JsonObject mockCommunicationData(JsonObject claimData) {
        JsonObject result = new JsonObject();
        JsonObject data = new JsonObject();
        JsonArray communications = new JsonArray();
        JsonObject comm1 = new JsonObject();
        comm1.addProperty("date", "2026-03-16T09:00:00");
        comm1.addProperty("channel", "email");
        comm1.addProperty("direction", "outbound");
        comm1.addProperty("subject", "Order Confirmation");
        comm1.addProperty("summary", "Order confirmation sent to customer with tracking details.");
        communications.add(comm1);
        data.add("communications", communications);
        data.addProperty("totalCommunications", 1);
        result.add("data", data);
        result.addProperty("source", "CRM_ADAPTER");
        return result;
    }

    private JsonObject mockTransactionData(JsonObject claimData) {
        JsonObject result = new JsonObject();
        JsonObject data = new JsonObject();
        data.addProperty("transactionId", claimData.has("transactionId") ? claimData.get("transactionId").getAsString() : "TXN-UNKNOWN");
        data.addProperty("transactionDate", "2026-03-15T14:30:00");
        data.addProperty("amount", claimData.has("claimValue") ? claimData.get("claimValue").getAsString() : "0.00");
        data.addProperty("currency", "USD");
        data.addProperty("paymentMethod", "CREDIT_CARD");
        data.addProperty("status", "COMPLETED");
        result.add("data", data);
        result.addProperty("source", "PSP_ADAPTER");
        return result;
    }

    private JsonObject mockDeviceData(JsonObject claimData) {
        JsonObject result = new JsonObject();
        JsonObject data = new JsonObject();
        data.addProperty("deviceFingerprint", "DFP-" + System.currentTimeMillis() % 100000);
        data.addProperty("ipAddress", "[IP_REDACTED]");
        data.addProperty("deviceType", "Desktop");
        data.addProperty("browser", "Chrome 120");
        data.addProperty("os", "Windows 11");
        data.addProperty("knownDevice", true);
        result.add("data", data);
        result.addProperty("source", "RISK_ENGINE_ADAPTER");
        return result;
    }

    private JsonObject mockRiskScoreData(JsonObject claimData) {
        JsonObject result = new JsonObject();
        JsonObject data = new JsonObject();
        data.addProperty("riskScore", 15);
        data.addProperty("riskLevel", "LOW");
        data.addProperty("riskFactors", "Known customer, verified device, matching billing address");
        result.add("data", data);
        result.addProperty("source", "RISK_ENGINE_ADAPTER");
        return result;
    }
}
