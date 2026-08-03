package com.opus.dispute.management.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class StripeEvidenceService {

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.base-url:https://api.stripe.com/v1}")
    private String baseUrl;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();

    public boolean isConfigured() {
        return stripeSecretKey != null && !stripeSecretKey.isBlank();
    }

    public JsonObject fetchDispute(String disputeId) {
        return callStripeApi("/disputes/" + disputeId);
    }

    public JsonObject fetchCharge(String chargeId) {
        return callStripeApi("/charges/" + chargeId);
    }

    public JsonObject fetchPaymentIntent(String paymentIntentId) {
        return callStripeApi("/payment_intents/" + paymentIntentId);
    }

    public JsonObject fetchBalanceTransaction(String balanceTransactionId) {
        return callStripeApi("/balance_transactions/" + balanceTransactionId);
    }

    public Map<String, String> buildEvidenceFromDispute(String stripeDisputeId, int caseNumber) {
        Map<String, String> evidence = new LinkedHashMap<>();
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
        String caseRef = "CASE-" + caseNumber;

        JsonObject dispute = fetchDispute(stripeDisputeId);
        if (dispute == null) {
            log.warn("Could not fetch Stripe dispute {}", stripeDisputeId);
            return evidence;
        }

        String chargeId = getStr(dispute, "charge");
        String paymentIntentId = getStr(dispute, "payment_intent");

        JsonObject charge = null;
        if (chargeId != null) {
            charge = fetchCharge(chargeId);
        }

        JsonObject paymentIntent = null;
        if (paymentIntentId != null) {
            paymentIntent = fetchPaymentIntent(paymentIntentId);
        }

        if (charge != null) {
            evidence.put("psp/auth_log", buildAuthLog(charge, caseRef, now));
            evidence.put("identity/avs_cvv_check", buildAvsCvvResults(charge, caseRef, now));
            evidence.put("device/3ds_authentication", build3dsAuthentication(charge, caseRef, now));
            evidence.put("fraud-tools/ip_risk_report", buildFraudToolsReport(charge, caseRef, now));

            String balanceTxnId = getStr(charge, "balance_transaction");
            if (balanceTxnId != null) {
                JsonObject balanceTxn = fetchBalanceTransaction(balanceTxnId);
                if (balanceTxn != null) {
                    evidence.put("psp/settlement_record", buildSettlementRecord(balanceTxn, charge, caseRef, now));
                }
            }

            evidence.put("shipping/delivery_confirmation", buildShippingEvidence(charge, paymentIntent, caseRef, now));
        }

        log.info("Built {} evidence entries from Stripe dispute {} (charge={})", evidence.size(), stripeDisputeId, chargeId);
        return evidence;
    }

    public Map<String, String> buildEvidenceFromPaymentIntent(String paymentIntentId, int caseNumber) {
        Map<String, String> evidence = new LinkedHashMap<>();
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
        String caseRef = "CASE-" + caseNumber;

        JsonObject pi = fetchPaymentIntent(paymentIntentId);
        if (pi == null) {
            log.warn("Could not fetch Stripe payment intent {}", paymentIntentId);
            return evidence;
        }

        String chargeId = null;
        JsonObject chargesData = getObj(pi, "latest_charge");
        if (chargesData == null) {
            String latestCharge = getStr(pi, "latest_charge");
            if (latestCharge != null) {
                chargeId = latestCharge;
            }
        }

        if (chargeId == null) {
            JsonObject charges = getObj(pi, "charges");
            if (charges != null) {
                com.google.gson.JsonArray data = charges.has("data") ? charges.getAsJsonArray("data") : null;
                if (data != null && data.size() > 0) {
                    chargeId = getStr(data.get(0).getAsJsonObject(), "id");
                }
            }
        }

        if (chargeId != null) {
            log.info("Resolved charge {} from payment intent {}", chargeId, paymentIntentId);
            return buildEvidenceFromCharge(chargeId, caseNumber);
        }

        log.warn("No charge found on payment intent {} — limited evidence available", paymentIntentId);
        evidence.put("shipping/delivery_confirmation", buildShippingEvidence(new JsonObject(), pi, caseRef, now));
        return evidence;
    }

    public Map<String, String> buildEvidenceFromCharge(String chargeId, int caseNumber) {
        Map<String, String> evidence = new LinkedHashMap<>();
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
        String caseRef = "CASE-" + caseNumber;

        JsonObject charge = fetchCharge(chargeId);
        if (charge == null) {
            log.warn("Could not fetch Stripe charge {}", chargeId);
            return evidence;
        }

        evidence.put("psp/auth_log", buildAuthLog(charge, caseRef, now));
        evidence.put("identity/avs_cvv_results", buildAvsCvvResults(charge, caseRef, now));
        evidence.put("device/3ds_authentication", build3dsAuthentication(charge, caseRef, now));
        evidence.put("fraud-tools/ip_risk_report", buildFraudToolsReport(charge, caseRef, now));

        String balanceTxnId = getStr(charge, "balance_transaction");
        if (balanceTxnId != null) {
            JsonObject balanceTxn = fetchBalanceTransaction(balanceTxnId);
            if (balanceTxn != null) {
                evidence.put("psp/settlement_record", buildSettlementRecord(balanceTxn, charge, caseRef, now));
            }
        }

        String piId = getStr(charge, "payment_intent");
        JsonObject paymentIntent = piId != null ? fetchPaymentIntent(piId) : null;
        evidence.put("shipping/delivery_confirmation", buildShippingEvidence(charge, paymentIntent, caseRef, now));

        log.info("Built {} evidence entries from Stripe charge {}", evidence.size(), chargeId);
        return evidence;
    }

    private String buildAuthLog(JsonObject charge, String caseRef, String now) {
        JsonObject record = new JsonObject();

        record.addProperty("transactionId", getStr(charge, "id"));
        record.addProperty("authorizationCode", getStr(charge, "authorization_code"));

        long createdEpoch = charge.has("created") ? charge.get("created").getAsLong() : 0;
        if (createdEpoch > 0) {
            record.addProperty("authorizationDate", java.time.Instant.ofEpochSecond(createdEpoch).toString());
        }

        int amount = charge.has("amount") ? charge.get("amount").getAsInt() : 0;
        record.addProperty("amount", amount / 100.0);
        record.addProperty("currency", getStr(charge, "currency") != null ? getStr(charge, "currency").toUpperCase() : null);
        record.addProperty("responseCode", charge.has("paid") && charge.get("paid").getAsBoolean() ? "00" : "05");
        record.addProperty("responseDescription", charge.has("paid") && charge.get("paid").getAsBoolean() ? "Approved" : "Declined");
        record.addProperty("captured", charge.has("captured") ? charge.get("captured").getAsBoolean() : false);

        JsonObject pmd = getObj(charge, "payment_method_details");
        if (pmd != null) {
            JsonObject card = getObj(pmd, "card");
            if (card != null) {
                record.addProperty("cardNetwork", getStr(card, "network"));
                record.addProperty("cardBrand", getStr(card, "brand"));
                record.addProperty("cardLast4", getStr(card, "last4"));
                record.addProperty("cardFunding", getStr(card, "funding"));
                record.addProperty("cardCountry", getStr(card, "country"));
            }
        }

        record.addProperty("cardPresent", false);
        record.addProperty("ecommerceIndicator", "07");
        record.addProperty("posEntryMode", "81");
        record.addProperty("merchantName", getStr(charge, "description"));
        record.addProperty("receiptUrl", getStr(charge, "receipt_url"));

        return wrapInStandardFormat("Stripe Charges API", "PSP Authorization Log", "Stripe", caseRef, now, record);
    }

    private String buildAvsCvvResults(JsonObject charge, String caseRef, String now) {
        JsonObject record = new JsonObject();

        JsonObject pmd = getObj(charge, "payment_method_details");
        if (pmd != null) {
            JsonObject card = getObj(pmd, "card");
            if (card != null) {
                JsonObject checks = getObj(card, "checks");
                if (checks != null) {
                    String avsLine1 = getStr(checks, "address_line1_check");
                    String avsPostal = getStr(checks, "address_postal_code_check");
                    String cvcCheck = getStr(checks, "cvc_check");

                    record.addProperty("avsLine1Result", avsLine1);
                    record.addProperty("avsPostalCodeResult", avsPostal);
                    record.addProperty("cvvResult", cvcCheck);
                    record.addProperty("avsResponseCode", mapStripeCheckToCode(avsLine1));
                    record.addProperty("cvvResponseCode", mapStripeCheckToCode(cvcCheck));
                }
            }
        }

        JsonObject billing = getObj(charge, "billing_details");
        if (billing != null) {
            record.addProperty("cardholderName", getStr(billing, "name"));
            record.addProperty("billingEmail", getStr(billing, "email"));
            JsonObject addr = getObj(billing, "address");
            if (addr != null) {
                record.addProperty("billingLine1", getStr(addr, "line1"));
                record.addProperty("billingCity", getStr(addr, "city"));
                record.addProperty("billingState", getStr(addr, "state"));
                record.addProperty("billingPostalCode", getStr(addr, "postal_code"));
                record.addProperty("billingCountry", getStr(addr, "country"));
            }
        }

        record.addProperty("transactionId", getStr(charge, "id"));

        return wrapInStandardFormat("Stripe Charges API", "AVS and CVV Verification Results", "Stripe", caseRef, now, record);
    }

    private String build3dsAuthentication(JsonObject charge, String caseRef, String now) {
        JsonObject record = new JsonObject();

        JsonObject pmd = getObj(charge, "payment_method_details");
        if (pmd != null) {
            JsonObject card = getObj(pmd, "card");
            if (card != null) {
                JsonObject tds = getObj(card, "three_d_secure");
                if (tds != null) {
                    record.addProperty("authenticationResult", getStr(tds, "result"));
                    record.addProperty("version", getStr(tds, "version"));
                    record.addProperty("authenticationFlow", getStr(tds, "authentication_flow"));
                    record.addProperty("electronicCommerceIndicator", getStr(tds, "electronic_commerce_indicator"));
                    record.addProperty("transactionId", getStr(tds, "transaction_id"));
                    record.addProperty("exemptionRequested", getStr(tds, "result_reason"));
                    record.addProperty("authenticated", "authenticated".equals(getStr(tds, "result")));
                } else {
                    record.addProperty("authenticationResult", "not_performed");
                    record.addProperty("authenticated", false);
                    record.addProperty("reason", "3D Secure was not used for this transaction");
                }
            }
        }

        record.addProperty("transactionId", getStr(charge, "id"));

        return wrapInStandardFormat("Stripe Charges API", "3D Secure Authentication Result", "Stripe", caseRef, now, record);
    }

    private String buildFraudToolsReport(JsonObject charge, String caseRef, String now) {
        JsonObject record = new JsonObject();

        JsonObject outcome = getObj(charge, "outcome");
        if (outcome != null) {
            record.addProperty("riskLevel", getStr(outcome, "risk_level"));
            record.addProperty("riskScore", outcome.has("risk_score") ? outcome.get("risk_score").getAsInt() : -1);
            record.addProperty("sellerMessage", getStr(outcome, "seller_message"));
            record.addProperty("outcomeType", getStr(outcome, "type"));
            record.addProperty("networkStatus", getStr(outcome, "network_status"));
            record.addProperty("reason", getStr(outcome, "reason"));
        }

        record.addProperty("transactionId", getStr(charge, "id"));
        record.addProperty("provider", "Stripe Radar");

        return wrapInStandardFormat("Stripe Radar", "Fraud Risk Assessment Report", "Stripe", caseRef, now, record);
    }

    private String buildSettlementRecord(JsonObject balanceTxn, JsonObject charge, String caseRef, String now) {
        JsonObject record = new JsonObject();

        record.addProperty("transactionId", getStr(balanceTxn, "id"));
        record.addProperty("sourceChargeId", getStr(charge, "id"));
        record.addProperty("type", getStr(balanceTxn, "type"));
        record.addProperty("status", getStr(balanceTxn, "status"));

        int amount = balanceTxn.has("amount") ? balanceTxn.get("amount").getAsInt() : 0;
        int fee = balanceTxn.has("fee") ? balanceTxn.get("fee").getAsInt() : 0;
        int net = balanceTxn.has("net") ? balanceTxn.get("net").getAsInt() : 0;
        record.addProperty("amount", amount / 100.0);
        record.addProperty("fee", fee / 100.0);
        record.addProperty("netAmount", net / 100.0);
        record.addProperty("currency", getStr(balanceTxn, "currency") != null ? getStr(balanceTxn, "currency").toUpperCase() : null);

        if (balanceTxn.has("available_on")) {
            long availableOn = balanceTxn.get("available_on").getAsLong();
            record.addProperty("settlementDate", java.time.Instant.ofEpochSecond(availableOn).toString());
        }

        long created = balanceTxn.has("created") ? balanceTxn.get("created").getAsLong() : 0;
        if (created > 0) {
            record.addProperty("transactionDate", java.time.Instant.ofEpochSecond(created).toString());
        }

        return wrapInStandardFormat("Stripe Balance Transactions API", "PSP Settlement Record", "Stripe", caseRef, now, record);
    }

    private String buildShippingEvidence(JsonObject charge, JsonObject paymentIntent, String caseRef, String now) {
        JsonObject record = new JsonObject();

        JsonObject shipping = getObj(charge, "shipping");
        if (shipping == null && paymentIntent != null) {
            shipping = getObj(paymentIntent, "shipping");
        }

        if (shipping != null) {
            record.addProperty("recipientName", getStr(shipping, "name"));
            record.addProperty("carrier", getStr(shipping, "carrier"));
            record.addProperty("trackingNumber", getStr(shipping, "tracking_number"));
            record.addProperty("phone", getStr(shipping, "phone"));

            JsonObject addr = getObj(shipping, "address");
            if (addr != null) {
                record.addProperty("deliveryLine1", getStr(addr, "line1"));
                record.addProperty("deliveryLine2", getStr(addr, "line2"));
                record.addProperty("deliveryCity", getStr(addr, "city"));
                record.addProperty("deliveryState", getStr(addr, "state"));
                record.addProperty("deliveryPostalCode", getStr(addr, "postal_code"));
                record.addProperty("deliveryCountry", getStr(addr, "country"));
            }
            record.addProperty("dataSource", "stripe_payment_metadata");
        } else {
            record.addProperty("dataSource", "stripe_payment_metadata");
            record.addProperty("shippingAvailable", false);
            record.addProperty("note", "No shipping information was attached to this Stripe charge or payment intent");
        }

        record.addProperty("transactionId", getStr(charge, "id"));

        return wrapInStandardFormat("Stripe Payment Metadata", "Shipping and Delivery Information", "Stripe", caseRef, now, record);
    }

    private String wrapInStandardFormat(String source, String dataType, String provider,
                                         String caseRef, String retrievedAt, JsonObject record) {
        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("source", source);
        wrapper.addProperty("dataType", dataType);
        wrapper.addProperty("retrievedAt", retrievedAt);
        wrapper.addProperty("provider", provider);
        wrapper.addProperty("caseReference", caseRef);

        com.google.gson.JsonArray records = new com.google.gson.JsonArray();
        records.add(record);
        wrapper.add("records", records);

        return gson.toJson(wrapper);
    }

    private JsonObject callStripeApi(String path) {
        if (!isConfigured()) {
            log.warn("Stripe API key not configured — skipping call to {}", path);
            return null;
        }

        String url = baseUrl + path;
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + stripeSecretKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.warn("Stripe API call to {} returned {}: {}", path, response.code(), body.length() > 200 ? body.substring(0, 200) : body);
                return null;
            }
            return JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            log.error("Stripe API call to {} failed: {}", path, e.getMessage());
            return null;
        }
    }

    private String mapStripeCheckToCode(String check) {
        if (check == null) return "U";
        return switch (check) {
            case "pass" -> "M";
            case "fail" -> "N";
            case "unavailable" -> "U";
            case "unchecked" -> "P";
            default -> "U";
        };
    }

    private String getStr(JsonObject obj, String field) {
        if (obj != null && obj.has(field) && !obj.get(field).isJsonNull()) {
            JsonElement el = obj.get(field);
            return el.isJsonPrimitive() ? el.getAsString() : null;
        }
        return null;
    }

    private JsonObject getObj(JsonObject obj, String field) {
        if (obj != null && obj.has(field) && obj.get(field).isJsonObject()) {
            return obj.getAsJsonObject(field);
        }
        return null;
    }
}
