# Evidence Enrichment — Real API Research

This document maps real-world APIs (Stripe, DHL, FedEx, UPS) to the evidence categories
used by the EvidenceStrategistAgent during the enrichment phase.

---

## Current Evidence Categories → API Mapping

| Evidence Category | Current Source | Real API Replacement |
|---|---|---|
| `psp/auth_log` | Local JSON | **Stripe Charges API** + **Stripe PaymentIntents API** |
| `psp/settlement_record` | Local JSON | **Stripe Balance Transactions API** |
| `shipping/delivery_confirmation` | Local JSON | **DHL Tracking API** / **FedEx Track API** / **UPS Track API** |
| `shipping/tracking_data` | Local JSON | Same carrier APIs (event history) |
| `merchant/order_details` | Local JSON | **Stripe Checkout Sessions API** or merchant DB |
| `device/3ds_authentication` | Local JSON | **Stripe 3D Secure data** (nested in Charge object) |
| `identity/avs_cvv_results` | Local JSON | **Stripe Charge** `payment_method_details.card.checks` |
| `customer-comms/email_logs` | Local JSON | Merchant system (not API-enrichable) |
| `fraud-tools/ip_risk_report` | Local JSON | **Stripe Radar** data (nested in Charge object) |

---

## 1. Stripe APIs

### 1a. Retrieve Dispute (primary entry point)

```
GET https://api.stripe.com/v1/disputes/{dispute_id}
Authorization: Bearer sk_test_...
```

**Key response fields for evidence:**
- `id` — Dispute ID (dp_xxx)
- `amount` — Disputed amount in cents
- `currency` — ISO currency code
- `reason` — Cardholder's stated reason (fraudulent, product_not_received, etc.)
- `status` — needs_response, under_review, won, lost
- `charge` — Charge ID (ch_xxx) — use to fetch full charge details
- `payment_intent` — PaymentIntent ID (pi_xxx)
- `evidence` — Any previously submitted evidence
- `evidence_details.due_by` — Submission deadline (Unix timestamp)
- `metadata` — Custom key-value data

**Maps to:** Dispute metadata enrichment, deadline tracking

### 1b. Retrieve Charge (PSP authorization data)

```
GET https://api.stripe.com/v1/charges/{charge_id}
Authorization: Bearer sk_test_...
```

**Key response fields for evidence:**

| Response Path | Maps to Evidence Field | Category |
|---|---|---|
| `billing_details.name` | Cardholder name | `identity` |
| `billing_details.email` | Customer email | `customer-comms` |
| `billing_details.address` | Billing address (for AVS) | `identity` |
| `payment_method_details.card.checks.address_line1_check` | AVS line1 result (pass/fail/unavailable) | `identity/avs_cvv` |
| `payment_method_details.card.checks.address_postal_code_check` | AVS postal code result | `identity/avs_cvv` |
| `payment_method_details.card.checks.cvc_check` | CVV verification result | `identity/avs_cvv` |
| `payment_method_details.card.brand` | Card brand (visa, mastercard) | `psp/auth_log` |
| `payment_method_details.card.last4` | Last 4 digits | `psp/auth_log` |
| `payment_method_details.card.network` | Card network | `psp/auth_log` |
| `payment_method_details.card.three_d_secure.result` | 3DS authentication result | `device/3ds` |
| `payment_method_details.card.three_d_secure.version` | 3DS version (1.0 or 2.0) | `device/3ds` |
| `payment_method_details.card.three_d_secure.authentication_flow` | challenge or frictionless | `device/3ds` |
| `amount` | Authorization amount (cents) | `psp/auth_log` |
| `amount_captured` | Captured amount | `psp/auth_log` |
| `captured` | Whether charge was captured | `psp/auth_log` |
| `receipt_url` | Receipt URL | `merchant/order` |
| `shipping.name` | Shipping recipient | `shipping` |
| `shipping.carrier` | Shipping carrier name | `shipping` |
| `shipping.tracking_number` | Tracking number | `shipping` |
| `shipping.address` | Delivery address | `shipping` |
| `metadata` | Merchant-attached data (order ID, etc.) | `merchant/order` |
| `outcome.risk_level` | Radar risk level (normal, elevated, highest) | `fraud-tools` |
| `outcome.risk_score` | Radar risk score (0-100) | `fraud-tools` |
| `outcome.seller_message` | Radar assessment text | `fraud-tools` |
| `outcome.type` | authorized, blocked, etc. | `fraud-tools` |

### 1c. Retrieve PaymentIntent (shipping + customer context)

```
GET https://api.stripe.com/v1/payment_intents/{pi_id}
Authorization: Bearer sk_test_...
```

**Additional fields not on Charge:**
- `shipping.name`, `shipping.phone`, `shipping.carrier`, `shipping.tracking_number`, `shipping.address`
- `customer` — Stripe Customer ID (can expand to get full customer record)
- `metadata` — Order-level metadata (order_id, fulfillment_status, etc.)
- `description` — Order description

### 1d. Retrieve Balance Transaction (settlement record)

```
GET https://api.stripe.com/v1/balance_transactions/{txn_id}
Authorization: Bearer sk_test_...
```

**Key fields:**
- `amount` — Net amount after fees
- `fee` — Stripe fee amount
- `fee_details` — Breakdown of fees
- `available_on` — When funds become available (settlement date)
- `status` — available, pending
- `type` — charge, refund, dispute, etc.

**Maps to:** `psp/settlement_record`

---

## 2. DHL Tracking API (Unified)

### Endpoint

```
GET https://api-eu.dhl.com/track/shipments?trackingNumber={number}&recipientPostalCode={zip}
DHL-API-Key: {subscription_key}
```

### Authentication
- API key passed in `DHL-API-Key` header
- Get key from developer.dhl.com (free tier available)
- Demo key available for testing: returns mock responses

### Key Response Fields for Evidence

| Response Path | Maps to Evidence Field | Category |
|---|---|---|
| `shipments[0].status.statusCode` | Delivery status (delivered, in-transit) | `shipping/delivery_confirmation` |
| `shipments[0].status.timestamp` | Delivery timestamp | `shipping/delivery_confirmation` |
| `shipments[0].status.description` | "Delivered - Signed for by: JOHN DOE" | `shipping/delivery_confirmation` |
| `shipments[0].status.location.address` | Delivery location | `shipping/delivery_confirmation` |
| `shipments[0].proofOfDelivery.signedBy` | Recipient name who signed | `shipping/delivery_confirmation` |
| `shipments[0].proofOfDelivery.timestamp` | POD timestamp | `shipping/delivery_confirmation` |
| `shipments[0].proofOfDelivery.documentUrl` | POD document URL (signature image) | `shipping/delivery_confirmation` |
| `shipments[0].events[]` | Full tracking event history | `shipping/tracking_data` |
| `shipments[0].origin` | Origin address | `shipping/tracking_data` |
| `shipments[0].destination` | Destination address | `shipping/tracking_data` |
| `shipments[0].estimatedTimeOfDelivery` | Estimated delivery date | `shipping/tracking_data` |

### Rate Limits
- Free tier: 250 requests/day
- Standard: based on subscription plan

---

## 3. FedEx Track API

### Authentication (OAuth 2.0)

```
POST https://apis.fedex.com/oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials&client_id={api_key}&client_secret={secret_key}
```

Returns: `{ "access_token": "...", "token_type": "bearer", "expires_in": 3600 }`

### Track by Tracking Number

```
POST https://apis.fedex.com/track/v1/trackingnumbers
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "trackingInfo": [
    {
      "trackingNumberInfo": {
        "trackingNumber": "794644790138"
      }
    }
  ],
  "includeDetailedScans": true
}
```

### Key Response Fields for Evidence

| Response Path | Maps to Evidence Field | Category |
|---|---|---|
| `output.completeTrackResults[0].trackResults[0].latestStatusDetail.statusByLocale` | Delivery status text | `shipping/delivery_confirmation` |
| `output.completeTrackResults[0].trackResults[0].latestStatusDetail.code` | Status code (DL=delivered) | `shipping/delivery_confirmation` |
| `output.completeTrackResults[0].trackResults[0].dateAndTimes[].dateTime` | Actual delivery datetime | `shipping/delivery_confirmation` |
| `output.completeTrackResults[0].trackResults[0].deliveryDetails.receivedByName` | Recipient who signed | `shipping/delivery_confirmation` |
| `output.completeTrackResults[0].trackResults[0].deliveryDetails.deliveryAttempts` | Number of delivery attempts | `shipping/delivery_confirmation` |
| `output.completeTrackResults[0].trackResults[0].scanEvents[]` | Full scan/tracking history | `shipping/tracking_data` |
| `output.completeTrackResults[0].trackResults[0].shipperInformation` | Shipper details | `shipping/tracking_data` |
| `output.completeTrackResults[0].trackResults[0].recipientInformation` | Recipient details | `shipping/tracking_data` |

### Signature Proof of Delivery (SPOD)
- Available for Express and Ground shipments up to 16 months from ship date
- Request must include shipper's billing account number
- Returns base64-encoded signature image (PDF or PNG)
- Not available for Ground Economy shipments

---

## 4. UPS Track API

### Authentication (OAuth 2.0)

```
POST https://onlinetools.ups.com/security/v1/oauth/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic {base64(client_id:client_secret)}

grant_type=client_credentials
```

Test environment: `https://wwwcie.ups.com/security/v1/oauth/token`

### Track Shipment

```
GET https://onlinetools.ups.com/api/track/v1/details/{trackingNumber}?returnSignature=true
Authorization: Bearer {access_token}
transId: {uuid}
transactionSrc: {app_name}
```

### Key Response Fields for Evidence

| Response Path | Maps to Evidence Field | Category |
|---|---|---|
| `trackResponse.shipment[0].package[0].deliveryDate[0].date` | Delivery date (YYYYMMDD) | `shipping/delivery_confirmation` |
| `trackResponse.shipment[0].package[0].deliveryTime.endTime` | Delivery time | `shipping/delivery_confirmation` |
| `trackResponse.shipment[0].deliveryInformation.receivedBy` | Recipient who signed | `shipping/delivery_confirmation` |
| `trackResponse.shipment[0].deliveryInformation.signature.image` | Base64 signature image | `shipping/delivery_confirmation` |
| `trackResponse.shipment[0].deliveryInformation.location` | Delivery location description | `shipping/delivery_confirmation` |
| `trackResponse.shipment[0].package[0].activity[]` | Full tracking event history | `shipping/tracking_data` |
| `trackResponse.shipment[0].package[0].currentStatus.description` | Current status text | `shipping/tracking_data` |
| `trackResponse.shipment[0].shipperAddress` | Shipper address | `shipping/tracking_data` |
| `trackResponse.shipment[0].shipToAddress` | Delivery address | `shipping/tracking_data` |

### Proof of Delivery options
- Set `returnSignature=true` in query params to include signature image
- Signature returned as base64-encoded image in `deliveryInformation.signature.image`

---

## 5. Implementation Plan — Evidence Enrichment Pipeline

### Phase 1: Stripe Integration (PSP + Identity + Fraud-Tools)

One Stripe `GET /v1/charges/{id}` call provides data for 4 evidence categories:

```
Stripe Charge → psp/auth_log       (authorization details, amounts, response codes)
             → identity/avs_cvv    (AVS + CVV check results from card.checks)
             → device/3ds          (3D Secure result, version, flow)
             → fraud-tools/risk    (Radar risk_level, risk_score, seller_message)
```

Plus `GET /v1/disputes/{id}` provides:
- Dispute reason, status, deadline
- Previously submitted evidence
- Charge and PaymentIntent IDs for cross-referencing

And `GET /v1/balance_transactions/{id}` provides:
- Settlement date, net amount, fee breakdown → `psp/settlement_record`

**Required:** Stripe API key (secret key `sk_test_...` or `sk_live_...`)

### Phase 2: Carrier Tracking (Shipping)

Based on the carrier identified from Stripe's `shipping.carrier` field:

```
if carrier == "dhl":
    GET https://api-eu.dhl.com/track/shipments?trackingNumber={num}
    → shipping/delivery_confirmation (POD, signature, timestamp)
    → shipping/tracking_data (event history)

elif carrier == "fedex":
    POST https://apis.fedex.com/track/v1/trackingnumbers
    → shipping/delivery_confirmation (status, receivedByName, attempts)
    → shipping/tracking_data (scan events)

elif carrier == "ups":
    GET https://onlinetools.ups.com/api/track/v1/details/{num}?returnSignature=true
    → shipping/delivery_confirmation (receivedBy, signature image)
    → shipping/tracking_data (activity history)
```

**Required per carrier:**
- DHL: API key from developer.dhl.com
- FedEx: Client ID + secret from developer.fedex.com
- UPS: Client ID + secret from developer.ups.com

### Phase 3: Transform to Evidence Format

All API responses should be transformed into the standard evidence JSON format
used by the EvidenceStrategistAgent:

```json
{
  "source": "Stripe Charges API",
  "dataType": "PSP Authorization Log",
  "retrievedAt": "2026-04-03T10:00:00Z",
  "provider": "Stripe",
  "caseReference": "CASE-37",
  "records": [
    {
      "transactionId": "ch_3MtwBwLkdIwHu7ix",
      "authorizationCode": "...",
      "amount": 49.99,
      "currency": "USD",
      "responseCode": "approved",
      ...
    }
  ]
}
```

This ensures compatibility with the existing `DataSourceService` and
`EvidenceStrategistAgent` without modifying their logic.

---

## 6. API Key Requirements Summary

| API | Key Type | Environment | Endpoint Prefix |
|---|---|---|---|
| Stripe | Secret key (`sk_test_` / `sk_live_`) | Test/Live | `https://api.stripe.com/v1` |
| DHL | API subscription key | Sandbox/Prod | `https://api-eu.dhl.com/track` |
| FedEx | Client ID + Secret (OAuth) | Sandbox/Prod | `https://apis.fedex.com` |
| UPS | Client ID + Secret (OAuth) | CIE Test/Prod | `https://onlinetools.ups.com` |

---

## 7. Data Flow Summary

```
Dispute Ingested from Mastercard
        │
        ▼
  getClaimDetail (reason code, chargeback details)
        │
        ▼
  Evidence Enrichment Triggered (POST /api/agents/enrich/{id})
        │
        ├── Stripe Dispute API ──→ dispute context, deadlines
        ├── Stripe Charge API ───→ auth log, AVS/CVV, 3DS, Radar risk
        ├── Stripe Balance Tx ───→ settlement record
        │
        ├── Carrier identified from Stripe shipping.carrier
        │   ├── DHL API ──→ delivery confirmation + tracking history
        │   ├── FedEx API ─→ delivery confirmation + scan events
        │   └── UPS API ──→ delivery confirmation + activity history
        │
        ├── Local files (existing cases 19-36) ──→ all 7 categories
        │
        ▼
  Transform all responses → standard evidence JSON format
        │
        ▼
  PII Scrubbing
        │
        ▼
  Gemini Analysis → Annotated Evidence Map
        │
        ▼
  EvidenceMap saved to DB
```
