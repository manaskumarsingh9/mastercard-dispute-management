# Mastercom API v6 - Complete Reference Document

**Title:** MasterCom
**Version:** v6
**Description:** MasterCom
**Artifact ID:** mastercom
**Test Environment:** SANDBOX

## Base URL

`https://api.mastercard.com/mastercom`

---

## Table of Contents

1. [Case Filing](#case-filing)
2. [Chargebacks](#chargebacks)
3. [Chargebacks (Debit MasterCard and Europe Dual Acquirer)](#chargebacks-debit-mastercard-and-europe-dual-acquirer)
4. [Claims](#claims)
5. [Fees](#fees)
6. [Fees (Debit MasterCard and Europe Dual Acquirer)](#fees-debit-mastercard-and-europe-dual-acquirer)
7. [Fraud](#fraud)
8. [Health Check](#health-check)
9. [Queues](#queues)
10. [Reconciliation](#reconciliation)
11. [Retrievals](#retrievals)
12. [Retrievals (Debit MasterCard and Europe Dual Acquirer)](#retrievals-debit-mastercard-and-europe-dual-acquirer)
13. [Transactions](#transactions)
14. [Transactions (Debit MasterCard and Europe Dual Acquirer)](#transactions-debit-mastercard-and-europe-dual-acquirer)
15. [Data Models (Schemas)](#data-models-schemas)

---

## Case Filing

### 1. Create a new case

**Endpoint:** `POST /v6/cases`

**Operation ID:** `createCaseFiling`

**Description:**

Issuer or acquirers use this endpoint to file a pre-arbitration, arbitration, pre-compliance, or compliance case and optionally upload documents. Issuers and acquirers also have the option to use the endpoint to create an Expedited Billing Dispute Form (EBDF) and attach it to the claim.

 Note: issuers or acquirers should not attach documents while requesting the automatic generation of the EBDF. Therefore, if issuers or acquirers need to submit the EBDF with additional documentation, they must complete the EBDF and include it in a ZIP file with any additional documentation.

#### Request Body

Create Case Filing information

**Content-Type:** `application/json`

**Schema:** [CreateCaseRequest](#createcaserequest)

**Examples:**

- **Create Case Pre-arbitration** → See examples section: `CreateCaseType1`
- **Create Case Arbitration** → See examples section: `CreateCaseType2`
- **Create Case Pre-compliance** → See examples section: `CreateCaseType3`
- **Create Case Compliance** → See examples section: `CreateCaseType4`
- **Create Case EBDF Doc** → See examples section: `CreateCaseEBDFDocRequest`
- **Create Case File Attachment** → See examples section: `CreateCaseFileAttachmentRequest`

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [CaseFilingResponse](#casefilingresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "caseType": "4",
  "chargebackRefNum": {
    "0": "1111423456",
    "1": "2222123456"
  },
  "customerFilingNumber": "5482",
  "violationCode": "D.2",
  "violationDate": "2017-11-13",
  "disputeAmount": "200.00",
  "currencyCode": "USD",
  "fileAttachment": {
    "filename": "test.tif",
    "file": "sample file"
  },
  "filedAgainstIca": "004321",
  "filingAs": "A",
  "filingIca": "001234",
  "memo": "This is a test memo",
  "messageText": "This is a test message",
  "changeReasonCodeFlag": "Y",
  "updatedChargebackReasonCode": "4863",
  "changeReasonCodeReason": "This is a test reason"
}
```
**Response:**
```json
{
  "caseId": 536092
}
```

---

### 2. Update or respond to case

**Endpoint:** `PUT /v6/cases/{case-id}`

**Operation ID:** `updateCaseFiling`

**Description:**

Issuers or acquirers use this endpoint to take action (accept,
reject, rebut, escalate, withdraw) on a case filing. Issuers and acquirers
may take action with or without attaching documents.

NOTE: Senders and receivers cannot provide REJECT for arbitration cases.

NOTE: Senders and receivers cannot provide REBUT for arbitration cases.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `case-id` | path | string | Yes | Case filing Id.   Length: 1-19   Valid Values/Format: Numeric Example: `536092` |

#### Request Body

Update Case Filing information

**Content-Type:** `application/json`

**Schema:** [UpdateCaseRequest](#updatecaserequest)

**Examples:**

- **Update Case actions with no doc** → See examples section: `UpdateCaseNoFile`
- **Update Case actions with doc** → See examples section: `UpdateCaseFileAttachment`

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [CaseFilingResponse](#casefilingresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "case-id": "536092",
  "fileAttachment": {
    "filename": "test.tif",
    "file": "sample file"
  },
  "action": "REJECT",
  "memo": "This is a test memo"
}
```
**Response:**
```json
{
  "caseId": 536092
}
```

---

### 3. Retrieve case documents

**Endpoint:** `GET /v6/cases/{case-id}/documents`

**Operation ID:** `getCaseFilingDoc`

**Description:**

Issuers and acquirers use this endpoint to retrieve all documents in a desired format associated with a case.

 Note: this endpoint returns all documents that either party has submitted for the case.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `case-id` | path | string | Yes | The case filing id.   Length: 0-19   Valid Values/Format: Numeric Example: `536092` |
| `format` | query | string [ORIGINAL, MERGED_TIFF, MERGED_PDF] | Yes | File Format.   Length: 8-11   Valid Values/Format: ORIGINAL, MERGED_TIFF, MERGED_PDF Example: `ORIGINAL` |
| `memo` | query | string | No | Adding field for future use. Please leave blank at this time.   Length: N/A    Valid Values/Format: Alphanumeric Example: `Memo` |

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [DocumentResponseStructure](#documentresponsestructure) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "format": "ORIGINAL",
  "case-id": "536092"
}
```
**Response:**
```json
{
  "fileAttachment": {
    "filename": "CS_536092.zip",
    "file": "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"
  }
}
```

---

### 4. Check case document status

**Endpoint:** `PUT /v6/cases/status`

**Operation ID:** `retrieveCaseFilingStatus`

**Description:**

Issuers and acquirers use this endpoint to search for the status of documents of a specific list of cases.

 Note: issuers and acquirers may send a maximum of 2,000 case IDs within a single request.

#### Request Body

Case Filing information

**Content-Type:** `application/json`

**Schema:** [CaseFilingStatusRequest](#casefilingstatusrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [CaseFilingStatusResponse](#casefilingstatusresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "caseFilingList": {
    "0": {
      "caseId": "536092"
    }
  }
}
```
**Response:**
```json
{
  "caseFilingResponseList": [
    {
      "caseId": "536092",
      "status": "COMPLETED_SND_10/10/2019 8:43:21 AM"
    }
  ]
}
```

---

### 5. Search case document by status

**Endpoint:** `PUT /v6/cases/imagestatus`

**Operation ID:** `retrieveCaseFilingImageStatus`

**Description:**

Issuers and acquirers use this endpoint to search documents that have a specific status (completed, pending, failed, unavailable, and document not applicable) for case filings.

 Note: For customers with high volumes of chargebacks or large numbers of BINs, Mastercom recommends using the Case Filing Status endpoint rather than the Case Filing Image Status endpoint.

#### Request Body

Case Filing information

**Content-Type:** `application/json`

**Schema:** [CaseFilingImageStatusRequest](#casefilingimagestatusrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [CaseFilingImageStatusResponse](#casefilingimagestatusresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "status": "COMPLETED",
  "startDate": "2018-10-01",
  "endDate": "2018-10-30"
}
```
**Response:**
```json
{
  "caseFilingImageStatusList": [
    {
      "caseId": "536092",
      "status": "COMPLETED"
    }
  ]
}
```

---

### 6. Retrieve claims by case

**Endpoint:** `PUT /v6/cases/retrieve/claims`

**Operation ID:** `retrieveClaims`

**Description:**

Issuers or acquirers use this endpoint to retrieve a list of claims associated with existing cases.

#### Request Body

Case Filing Retrieve Claims request information

**Content-Type:** `application/json`

**Schema:** [CaseFilingClaimsRequest](#casefilingclaimsrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [CaseFilingClaimsResponse](#casefilingclaimsresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "caseFilingList": {
    "0": {
      "caseId": "542691",
      "isIssuer": false
    },
    "1": {
      "caseId": "542692",
      "isIssuer": true
    }
  }
}
```
**Response:**
```json
{
  "caseFilingResponseList": [
    {
      "caseId": "542691",
      "claimId": "200000000001"
    },
    {
      "caseId": "542692",
      "claimId": "200000000002"
    }
  ]
}
```

---

## Chargebacks

### 7. Retrieve chargeback data

**Endpoint:** `POST /v6/claims/{claim-id}/chargebacks/loaddataforchargebacks`

**Operation ID:** `getDataForCreateChargeback`

**Description:**

Issuers and acquirers use this endpoint to obtain information about a potential first chargeback or second presentment prior to creating the chargeback. If the issuer or acquirer decides to create the chargeback, the issuer or acquirer should use the Create endpoint for chargebacks.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the chargeback to be created.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |

#### Request Body

The type of chargeback.  The following values are valid...CHARGEBACK, SECOND_PRESENTMENT.  The default value is CHARGEBACK.

**Content-Type:** `application/json`

**Schema:** [LoadDataForChargebacksRequest](#loaddataforchargebacksrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [LoadDataForChargebackResponse](#loaddataforchargebackresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654",
  "chargebackType": "CHARGEBACK",
  "reasonCode": "4853",
  "currency": "USD"
}
```
**Response:**
```json
{
  "currencies": [
    {
      "name": "USD",
      "value": "USD"
    }
  ],
  "docIndicators": [
    {
      "name": "1",
      "value": "1 - Supporting documentation will follow"
    }
  ],
  "messageTexts": [
    {
      "name": "CARD NOT VALID OR EXPIRED",
      "value": "CARD NOT VALID OR EXPIRED"
    }
  ],
  "reasonCodes": [
    {
      "name": "4831",
      "value": "4831 - Transaction Amount Differs"
    }
  ],
  "amount": {
    "name": "USD",
    "value": "64.13"
  }
}
```

---

### 8. Create chargeback or second presentment

**Endpoint:** `POST /v6/claims/{claim-id}/chargebacks`

**Operation ID:** `createChargeback`

**Description:**

Issuers and acquirers use this endpoint to create chargebacks and second presentments and optionally to upload supporting documents. Issuers and acquirers may use the parameters in the request to automatically generate the Expedited Billing Dispute Form (EBDF) and attach it to the claim.

 Note: issuers should not attach documents while requesting the automatic generation of the EBDF. Therefore, if issuers need to submit the EBDF with additional documentation, they must complete the EBDF and include it in a ZIP file with any additional documentation.

 Note: If the API call to this endpoint times out, customers should use the Chargeback Status or Retrieve Claim endpoint to determine the success or failure of document processing.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id where the chargeback will be added   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |

#### Request Body

Create Chargeback information

**Content-Type:** `application/json`

**Schema:** [CreateChargebackRequest](#createchargebackrequest)

**Examples:**

- **Create First Chargeback** → See examples section: `CreateCBChargeback`
- **Create Second Presentment** → See examples section: `CreateCBSecondPresentment`
- **Create with EBDF Docs** → See examples section: `CreateCBEbdfDocs`
- **Create with fileAttachment** → See examples section: `CreateCBFileAttachment`

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [ChargebackResponse](#chargebackresponse) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "12344",
  "currency": "USD",
  "documentIndicator": "true",
  "messageText": "test message",
  "amount": "100.00",
  "fileAttachment": {
    "filename": "test.tif",
    "file": "sample file"
  },
  "reasonCode": "4853",
  "chargebackType": "CHARGEBACK",
  "editExclusionCode": "BO",
  "refundNotReceivedIndicator": "false",
  "includeCurrencyConversionAssessmentCCA": "false"
}
```
**Response:**
```json
{
  "chargebackId": "300002063556"
}
```

---

### 9. Reverse chargeback

**Endpoint:** `POST /v6/claims/{claim-id}/chargebacks/{chargeback-id}/reversal`

**Operation ID:** `createChargebackReversal`

**Description:**

Issuers and acquirers use this endpoint to reverse an existing chargeback when they create a chargeback in error.

 Note: issuers and acquirers may only create reversals on chargebacks after the chargebacks are processed by the Global Clearing Management System (GCMS).

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the chargeback.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |
| `chargeback-id` | path | string | Yes | Chargeback Id.   Length: 1-19   Valid Values/Format: Numeric Example: `300018439680` |

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [ChargebackResponse](#chargebackresponse) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "12344",
  "chargeback-id": "300002063556"
}
```
**Response:**
```json
{
  "chargebackId": "300002063556"
}
```

---

### 10. Attach document to chargeback

**Endpoint:** `PUT /v6/claims/{claim-id}/chargebacks/{chargeback-id}`

**Operation ID:** `updateChargeback`

**Description:**

Issuers and acquirers use this endpoint to update an existing chargeback with memos or documents if they did not attach memos or documents when creating the chargeback.

 Note: If the API call to this endpoint times out, customers should use the Chargeback Status or Retrieve Claim endpoint to determine the success or failure of document processing.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the chargeback.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |
| `chargeback-id` | path | string | Yes | Chargeback Id.   Length: 1-19   Valid Values/Format: Numeric Example: `300018439680` |

#### Request Body

Update Chargeback information

**Content-Type:** `application/json`

**Schema:** [UpdateChargebackRequest](#updatechargebackrequest)

**Examples:**

- **Update with File Attachment** → See examples section: `UpdateChargebackFile`
- **Update with Credit Voucher** → See examples section: `UpdateChargebackVoucher`

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [ChargebackResponse](#chargebackresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "12344",
  "chargeback-id": "300002063556",
  "memo": "This is a test memo",
  "fileAttachment": {
    "filename": "test.tif",
    "file": "sample file"
  }
}
```
**Response:**
```json
{
  "chargebackId": "300002063556"
}
```

---

### 11. Retrieve chargeback documents

**Endpoint:** `GET /v6/claims/{claim-id}/chargebacks/{chargeback-id}/documents`

**Operation ID:** `getChargebackDoc`

**Description:**

Issuers and acquirers use this endpoint to retrieve documents in a desired format associated with any type of chargeback.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the chargeback.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |
| `chargeback-id` | path | string | Yes | Chargeback Id.   Length: 1-19   Valid Values/Format: Numeric Example: `300018439680` |
| `format` | query | string [ORIGINAL, MERGED_TIFF, MERGED_PDF] | Yes | File Format.   Length: 8-11   Valid Values/Format: ORIGINAL, MERGED_TIFF, MERGED_PDF Example: `ORIGINAL` |

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [DocumentResponseStructure](#documentresponsestructure) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "format": "ORIGINAL",
  "claim-id": "1234",
  "chargeback-id": "92344"
}
```
**Response:**
```json
{
  "fileAttachment": {
    "filename": "CB_92344.zip",
    "file": "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"
  }
}
```

---

### 12. Acknowledge chargeback or representment

**Endpoint:** `PUT /v6/chargebacks/acknowledge`

**Operation ID:** `acknowledgeChargebacks`

**Description:**

Issuers and acquirers use this endpoint to acknowledge a chargeback or second presentment. Acknowledging a chargeback or second presentment moves the claim from the Unworked queue to the Worked queue. Acknowledging the chargeback or second presentment does not close the claim. Issuers and acquirers may take further actions on acknowledged claims.

 Note: for efficient processing, issuers and acquirers should send 100 acknowledgments or fewer in a single request.

#### Request Body

Chargeback Receiver information

**Content-Type:** `application/json`

**Schema:** [ChargebackMarkProcessedRequest](#chargebackmarkprocessedrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [ChargebackMarkProcessedResponse](#chargebackmarkprocessedresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "chargebackList": {
    "0": {
      "claimId": "200002020654",
      "chargebackId": "300002063556"
    }
  }
}
```
**Response:**
```json
{
  "chargebackResponseList": [
    {
      "chargebackId": "300002063556",
      "status": "PROCESSED",
      "failureReason": null
    }
  ]
}
```

---

### 13. Retrieve document status by chargeback

**Endpoint:** `PUT /v6/chargebacks/status`

**Operation ID:** `retrieveChargebackStatus`

**Description:**

Issuers and acquirers use this endpoint to search for the status of documents of a specific list of claim IDs and chargeback IDs.

 Note: issuers and acquirers may send a maximum of 2,000 chargeback IDs within a single request.

#### Request Body

Chargeback information

**Content-Type:** `application/json`

**Schema:** [ChargebackStatusRequest](#chargebackstatusrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [ChargebackStatusResponse](#chargebackstatusresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "chargebackList": {
    "0": {
      "claimId": "200002020654",
      "chargebackId": "12344"
    }
  }
}
```
**Response:**
```json
{
  "chargebackResponseList": [
    {
      "claimId": "200002020654",
      "chargebackId": "12344",
      "status": "COMPLETED"
    }
  ]
}
```

---

## Chargebacks (Debit MasterCard and Europe Dual Acquirer)

### 14. Create chargeback or second presentment for mastercard debit or bridged debit

**Endpoint:** `POST /v6/claims/{claim-id}/chargebacks/debitmc`

**Operation ID:** `createChargebackDebitMC`

**Description:**

Issuers use this endpoint to create chargebacks for Debit Mastercard or Europe Dual Acquirer transactions and optionally to upload supporting documents. Issuers should only use this endpoint when the transaction has a single-message issuer, dual-message acquirer, and a transaction without a PIN.

 Note: If the API call to this endpoint times out, customers should use the Chargeback Status or Retrieve Claim endpoint to determine the success or failure of document processing.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id where the chargeback will be added.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |

#### Request Body

Create Chargeback information

**Content-Type:** `application/json`

**Schema:** [CreateChargebackSingleRequest](#createchargebacksinglerequest)

**Examples:**

- **Create Chargeback** → See examples section: `CreateChargebackSingle`
- **Create Chargeback with Type** → See examples section: `CreateChargebackSingleType`

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [ChargebackResponse](#chargebackresponse) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "12344",
  "replacementAmount": "100.00",
  "controlNumber": "99999",
  "usageCode": "1",
  "reversalReasonCode": "07",
  "brand": "PV",
  "refundNotReceivedIndicator": "true"
}
```
**Response:**
```json
{
  "chargebackId": "30077331352"
}
```

---

### 15. Reverse chargeback for mastercard debit or bridged debit

**Endpoint:** `POST /v6/claims/{claim-id}/chargebacks/debitmc/{chargeback-id}/reversal`

**Operation ID:** `createChargebackReversalDebitMC`

**Description:**

Issuers use this endpoint to reverse an existing Debit Mastercard or Europe Dual Acquirer chargeback when they create a chargeback in error.

 Note: issuers may only create reversals on chargebacks after the chargebacks are processed by Mastercard Debit Switch (MDS).

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the chargeback.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |
| `chargeback-id` | path | string | Yes | Chargeback Id.   Length: 1-19   Valid Values/Format: Numeric Example: `300018439680` |

#### Request Body

Create Chargeback Reversal information

**Content-Type:** `application/json`

**Schema:** [CreateChargebackSingleReversalRequest](#createchargebacksinglereversalrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [ChargebackResponse](#chargebackresponse) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "12344",
  "chargeback-id": "300002063556",
  "reversalReasonCode": "82",
  "replacementAmount": "100.00",
  "controlNumber": "99999"
}
```
**Response:**
```json
{
  "chargebackId": "30077331352"
}
```

---

### 16. Attach document to chargeback for mastercard debit or bridged debit

**Endpoint:** `PUT /v6/claims/{claim-id}/chargebacks/debitmc/{chargeback-id}`

**Operation ID:** `updateChargebackDebitMC`

**Description:**

Issuers use this endpoint to update an existing Debit Mastercard or Europe Dual Acquirer chargeback with memos or documents if they did not attach memos or documents when creating the chargeback.

 Note: If the API call to this endpoint times out, customers should use the Chargeback Status or Retrieve Claim endpoint to determine the success or failure of document processing.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the chargeback.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |
| `chargeback-id` | path | string | Yes | Chargeback Id.   Length: 1-19   Valid Values/Format: Numeric Example: `300018439680` |

#### Request Body

Update Chargeback information

**Content-Type:** `application/json`

**Schema:** [UpdateChargebackRequest](#updatechargebackrequest)

**Examples:**

- **Update with File Attachment** → See examples section: `UpdateChargebackFile`
- **Update with Credit Voucher** → See examples section: `UpdateChargebackVoucher`

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [ChargebackResponse](#chargebackresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "12344",
  "chargeback-id": "300002063556",
  "memo": "This is a test memo",
  "fileAttachment": {
    "filename": "test.tif",
    "file": "sample file"
  }
}
```
**Response:**
```json
{
  "chargebackId": "300002063556"
}
```

---

### 17. Retrieve chargeback documents for mastercard debit or bridged debit

**Endpoint:** `GET /v6/claims/{claim-id}/chargebacks/debitmc/{chargeback-id}/documents`

**Operation ID:** `getChargebackDocDebitMC`

**Description:**

Issuers and acquirers use this endpoint to retrieve documents in a desired format associated with any type of chargeback.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | The Claim Id.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |
| `chargeback-id` | path | string | Yes | Chargeback Id.   Length: 1-19   Valid Values/Format: Numeric Example: `300002063556` |
| `format` | query | string [ORIGINAL, MERGED_TIFF, MERGED_PDF] | Yes | File format.   Length: 8-11   Valid Values/Format: ORIGINAL, MERGED_TIFF, MERGED_PDF Example: `ORIGINAL` |

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [DocumentResponseStructure](#documentresponsestructure) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "format": "ORIGINAL",
  "claim-id": "1234",
  "chargeback-id": "92344"
}
```
**Response:**
```json
{
  "fileAttachment": {
    "filename": "CB_92344.zip",
    "file": "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"
  }
}
```

---

### 18. Acknowledge chargeback or representment for mastercard debit or bridged debit

**Endpoint:** `PUT /v6/chargebacks/debitmc/acknowledge`

**Operation ID:** `acknowledgeChargebacksDebitMC`

**Description:**

Issuers and acquirers use this endpoint to acknowledge a chargeback or second presentment. Acknowledging a chargeback or second presentment moves the claim from the Unworked queue to the Worked queue. Acknowledging the chargeback or second presentment does not close the claim. Issuers and acquirers may take further actions on acknowledged claims.

 Note: for efficient processing, issuers and acquirers should send 100 acknowledgments or fewer in a single request.

#### Request Body

Chargeback Receiver information

**Content-Type:** `application/json`

**Schema:** [ChargebackMarkProcessedRequest](#chargebackmarkprocessedrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [ChargebackMarkProcessedResponse](#chargebackmarkprocessedresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "chargebackList": {
    "0": {
      "claimId": "200002020654",
      "chargebackId": "300002063556"
    }
  }
}
```
**Response:**
```json
{
  "chargebackResponseList": [
    {
      "chargebackId": "300002063556",
      "status": "PROCESSED",
      "failureReason": null
    }
  ]
}
```

---

### 19. Retrieve document status by chargeback for mastercard debit or bridged debit

**Endpoint:** `PUT /v6/chargebacks/debitmc/status`

**Operation ID:** `retrieveChargebackStatusDebitMC`

**Description:**

Issuers and acquirers use this endpoint to search for the status of documents of a specific list of claim IDs and chargeback IDs.

 Note: issuers and acquirers may send a maximum of 2,000 chargeback IDs within a single request.

#### Request Body

Chargeback information

**Content-Type:** `application/json`

**Schema:** [ChargebackStatusRequest](#chargebackstatusrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [ChargebackStatusResponse](#chargebackstatusresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "chargebackList": {
    "0": {
      "claimId": "200002020654",
      "chargebackId": "12344"
    }
  }
}
```
**Response:**
```json
{
  "chargebackResponseList": [
    {
      "claimId": "200002020654",
      "chargebackId": "12344",
      "status": "COMPLETED"
    }
  ]
}
```

---

## Claims

### 20. Create new claim

**Endpoint:** `POST /v6/claims`

**Operation ID:** `createClaim`

**Description:**

Issuers use this endpoint to create a new claim, which is required before creating a retrieval request or a first chargeback. If an issuer attempts to create a duplicate claim on an original transaction, the issuer receives a error message with the claim ID of the existing claim. Acquirers are not able to create claims. They receive claims from disputes that issuers initiate.

 Note: issuers must have a first presentment in order to create a claim.

 Note: issuers cannot create claims on first presentments that acquirers have reversed.

#### Request Body

Create Claim Request

**Content-Type:** `application/json`

**Schema:** [CreateClaimRequest](#createclaimrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [ClaimResponse](#claimresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "disputedAmount": "100.00",
  "disputedCurrency": "USD",
  "claimType": "Standard",
  "clearingTransactionId": "hqCnaMDqmto4wnL+BSUKSdzROqGJ7YELoKhEvluycwKNg3XTzSfaIJhFDkl9hW081B5tTqFFiAwCpcocPdB2My4t7DtSTk63VXDl1CySA8Y="
}
```
**Response:**
```json
{
  "claimId": "200002020654"
}
```

---

### 21. Retrieve claim details

**Endpoint:** `GET /v6/claims/{claim-id}`

**Operation ID:** `getClaimDetail`

**Description:**

Issuers and acquirers use this endpoint to retrieve details for an existing claim, including any cases with which the claim is associated.

 Note: the Retrieve endpoint for claims contains document statuses (completed, pending, failed, unavailable, and document not applicable) for all documents attached to dispute events within a claim. Issuers and acquirers do not have to make a separate call to obtain document statuses, making the use of this endpoint efficient.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [ClaimDetail](#claimdetail) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654"
}
```
**Response:**
```json
{
  "acquirerId": "002222",
  "acquirerRefNum": "55306608112341123451234",
  "primaryAccountNum": "52751494691484000",
  "claimId": "200002000191",
  "claimType": "Standard",
  "claimValue": "123.28 USD",
  "standardClaims": null,
  "clearingDueDate": "2017-12-14",
  "clearingNetwork": "GCMS",
  "createDate": "2017-10-27",
  "dueDate": "2017-12-11",
  "transactionId": "g1f4kmlMcfQaLHtRX+WWB2TfOiyAIO0ZrxZ2zJ226sQuH6EsoypShLUwzD95i2QeIxoHYh7qrAqy9qMdbmDgw==",
  "isAccurate": "true",
  "isAcquirer": "true",
  "isIssuer": "false",
  "isOpen": "true",
  "issuerId": "001111",
  "lastModifiedBy": "user1234",
  "lastModifiedDate": "2017-11-13",
  "merchantId": "0024038000200",
  "queueName": "Pending",
  "switchSerialNumber": null,
  "auditControlNumber ": "123111111000025",
  "retrievalDetails": {
    "acquirerRefNum": "55306608112341123451234",
    "acquirerResponseCd": "D",
    "acquirerMemo": "This is an example memo RF1.",
    "acquirerResponseDt": "2018-01-29",
    "amount": "196.42",
    "currency": "PLN",
    "claimId": "200002000191",
    "issuerResponseCd": "REJECT_DOCUMENTATION_NOT_AS_REQUIRED",
    "issuerRejectRsnCd": "02",
    "issuerMemo": "This is an example memo RFJ.",
    "issuerResponseDt": "2018-01-29",
    "imageReviewDecision": "I",
    "imageReviewDt": "2018-01-29",
    "primaryAcctNum": "52751494691484000",
    "requestId": "200002000151",
    "retrievalRequestReason": "6305",
    "docNeeded": "1",
    "createDate": "2017-10-30",
    "chargebackRefNum": "2000000000",
    "cancelDate": null,
    "rejectDate": null,
    "reverseDate": null,
    "acquirerResponseNotificationStatus": "Processed",
    "rejectReason": "Code1=0142(00):D0063/002;DE072=D0063\\8000000808\\\\",
    "instructionsForHealthcare": "Instructions for Healthcare",
    "flexCode": "003",
    "collaborationExpirationDateTime": "2023-05-11T19:22:41"
  },
  "chargebackDetails": [
    {
      "currency": "USD",
      "documentIndicator": "false",
      "messageText": "AUTHORIZATION DECLINED MMDDYY",
      "amount": "196.43",
      "reasonCode": "4808",
      "isPartialChargeback": false,
      "chargebackType": "CHARGEBACK",
      "chargebackId": "200002000151",
      "claimId": "200002000191",
      "reversed": false,
      "reversal": false,
      "createDate": "2017-10-27",
      "chargebackRefNum": "2000000000",
      "documentStatus": "DOC_NOT_APPLICABLE",
      "rejectReason": "Code1=5000(00):D0025/000;DE072=D0025\\0000\\\\",
      "reconciliationAmount": "196.43",
      "reconciliationCurrency": "PLN",
      "editExclusionCode": "BO",
      "refundNotReceivedIndicator": "true",
      "creditVoucherStatus": "Credit Voucher Accepted",
      "currencyConversionAssessmentCCAIncluded": "true",
      "currencyConversionAssessmentCCAAmount": "20.00 USD",
      "japanCommonMerchantCode": "0410",
      "mexicoDomesticTaxAmount": "002901985000000000121985000000000124",
      "mexicoDomesticTransactionFeeAmount": "002901985000000000121985000000000125",
      "mexicoDomesticSettlementFeesAndVat": "002901985000000000121985000000000126",
      "installmentData": "1261610E81023498764532103",
      "flexCode": "003"
    },
    {
      "currency": "USD",
      "documentIndicator": "false",
      "messageText": null,
      "amount": "196.43",
      "reasonCode": "2001",
      "isPartialChargeback": false,
      "chargebackType": "SECOND_PRESENTMENT",
      "chargebackId": "200002000151",
      "claimId": "200002000191",
      "reversed": false,
      "reversal": false,
      "createDate": "2017-11-08",
      "chargebackRefNum": "2000000000",
      "documentStatus": "DOC_NOT_APPLICABLE",
      "rejectReason": "Code1=5000(00):D0025/000;DE072=D0025\\0000\\\\",
      "reconciliationAmount": null,
      "reconciliationCurrency": null,
      "editExclusionCode": "BO",
      "currencyConversionAssessmentCCAIncluded": "true",
      "currencyConversionAssessmentCCAAmount": "20.00 USD",
      "japanCommonMerchantCode": "0410",
      "installmentData": "1261610E81023498764532103",
      "flexCode": "003"
    },
    {
      "currency": "USD",
      "documentIndicator": "false",
      "messageText": null,
      "amount": "61.64",
      "reasonCode": "4807",
      "isPartialChargeback": false,
      "chargebackType": "CHARGEBACK",
      "chargebackId": "200002000151",
      "claimId": "200002000191",
      "reversed": false,
      "reversal": false,
      "createDate": "2017-10-30",
      "chargebackRefNum": "9000000006",
      "documentStatus": "DOC_NOT_APPLICABLE",
      "rejectReason": "Code1=5000(00):D0025/000;DE072=D0025\\0000\\\\",
      "reconciliationAmount": "61.64",
      "reconciliationCurrency": "USD",
      "editExclusionCode": "BO",
      "currencyConversionAssessmentCCAIncluded": "true",
      "currencyConversionAssessmentCCAAmount": "20.00 USD",
      "japanCommonMerchantCode": "0410",
      "mexicoDomesticTaxAmount": "002901985000000000121985000000000124",
      "mexicoDomesticTransactionFeeAmount": "002901985000000000121985000000000125",
      "mexicoDomesticSettlementFeesAndVat": "002901985000000000121985000000000126",
      "installmentData": "1261610E81023498764532103",
      "flexCode": "003"
    }
  ],
  "feeDetails": [
    {
      "cardAcceptorIdCode": "Test ID",
      "cardNumber": "52751494691484000",
      "countryCode": "USA",
      "currency": "USD",
      "feeDate": "2018-02-07",
      "destinationMember": "001527",
      "feeAmount": "0.24",
      "creditSender": "true",
      "creditReceiver": "false",
      "message": "00000013502000000135020626065946717713065946",
      "reason": "7623",
      "feeId": "300002002247",
      "chargebackRefNum": null,
      "reconciliationAmount": "0.21",
      "reconciliationCurrency": "USD",
      "rejectReason": "Code1=0142(00):D0063/002;DE072=D0063\\8000000808\\\\",
      "japanCommonMerchantCode": "0410",
      "mexicoDomesticTaxAmount": "002901985000000000121985000000000124",
      "mexicoDomesticTransactionFeeAmount": "002901985000000000121985000000000125",
      "mexicoDomesticSettlementFeesAndVat": "002901985000000000121985000000000126",
      "installmentData": "1261610E81023498764532103",
      "flexCode": "003"
    }
  ],
  "caseFilingDetails": {
    "caseFilingStatus": "Closed",
    "caseFilingDetails": {
      "claimId": "200002000151",
      "claimType": "CaseFiling",
      "caseId": "9000000012",
      "caseType": "4",
      "chargebackRefNum": [
        "1111423456,2222123456"
      ],
      "currencyCode": "USD",
      "customerFilingNumber": "5482",
      "disputeAmount": "50.00",
      "dueDate": "2017-12-11",
      "filingAgaintstIca": "5482",
      "filingAs": "A",
      "filingIca": "6000",
      "merchantName": "test name",
      "primaryAccountNum": "52751494691484000",
      "violationCode": "D.2",
      "violationDate": "2017-12-18",
      "rulingDate": "2018-01-24",
      "rulingStatus": "Favor Sender",
      "creditDate": "2018-11-01",
      "chargebackDate": "2018-11-01",
      "reasonCode": "4853",
      "virtualAccountNum": "5123123424999876"
    },
    "caseFilingRespHistory": [
      {
        "memo": "Sample Memo",
        "action": "FAVOR SENDER",
        "responseDate": "2018-01-24"
      },
      {
        "memo": "Sample Memo",
        "action": "REBUT",
        "responseDate": "2017-12-18"
      },
      {
        "memo": "Sample Memo",
        "action": "ESCALATE",
        "responseDate": "2017-12-18"
      },
      {
        "memo": "Sample Memo",
        "action": "REJECT",
        "responseDate": "2017-12-18"
      }
    ]
  },
  "retrievalDetailsList": [
    {
      "acquirerRefNum": "55306608112341123451234",
      "acquirerResponseCd": null,
      "acquirerMemo": null,
      "acquirerResponseDt": null,
      "amount": "196.42",
      "currency": "PLN",
      "claimId": "200002000191",
      "issuerResponseCd": null,
      "issuerRejectRsnCd": null,
      "issuerMemo": null,
      "issuerResponseDt": null,
      "imageReviewDecision": null,
      "imageReviewDt": null,
      "primaryAcctNum": "52751494691484000",
      "requestId": "200002000151",
      "retrievalRequestReason": "6305",
      "docNeeded": "2",
      "createDate": "2017-10-30",
      "chargebackRefNum": "2000000000",
      "cancelDate": null,
      "rejectDate": null,
      "reverseDate": null,
      "acquirerResponseNotificationStatus": "Pending",
      "rejectReason": "Code1=0142(00):D0063/002;DE072=D0063\\8000000808\\\\",
      "instructionsForHealthcare": "Instructions for Healthcare",
      "collaborationExpirationDateTime": "2023-05-11T19:22:41"
    },
    {
      "acquirerRefNum": null,
      "acquirerResponseCd": "D",
      "acquirerMemo": "This is an example memo RF1.",
      "acquirerResponseDt": "2018-01-29",
      "amount": null,
      "currency": null,
      "claimId": null,
      "issuerResponseCd": "REJECT_DOCUMENTATION_NOT_AS_REQUIRED",
      "issuerRejectRsnCd": "02",
      "issuerMemo": "This is an example memo RFJ.",
      "issuerResponseDt": "2018-01-29",
      "imageReviewDecision": null,
      "imageReviewDt": null,
      "primaryAcctNum": null,
      "requestId": null,
      "retrievalRequestReason": null,
      "docNeeded": null,
      "createDate": null,
      "chargebackRefNum": null,
      "cancelDate": null,
      "rejectDate": null,
      "reverseDate": null,
      "acquirerResponseNotificationStatus": "Processed",
      "rejectReason": "Code1=0142(00):D0063/002;DE072=D0063\\8000000808\\\\",
      "instructionsForHealthcare": "Instructions for Healthcare",
      "collaborationExpirationDateTime": "2023-05-11T19:22:41"
    }
  ]
}
```

---

### 22. Take action on claim

**Endpoint:** `PUT /v6/claims/{claim-id}`

**Operation ID:** `Update Claim`

**Description:**

Issuers use this endpoint to take an action (reopen or close) on an existing claim.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |

#### Request Body

Update Claim Request

**Content-Type:** `application/json`

**Schema:** [UpdateClaimRequest](#updateclaimrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [ClaimResponse](#claimresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654",
  "action": "CLOSE",
  "closeClaimReasonCode": "10"
}
```
**Response:**
```json
{
  "claimId": "200002020654"
}
```

---

## Fees

### 23. Retrieve fee

**Endpoint:** `POST /v6/claims/{claim-id}/fees/loaddataforfees`

**Operation ID:** `getDataForCreateFee`

**Description:**

Senders (issuers or acquirers) use this endpoint to obtain information about a potential fee collection associated with a claim.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the Fee.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |

#### Request Body

Load Data For Fee Request

**Content-Type:** `application/json`

**Schema:** [LoadDataForFeesRequest](#loaddataforfeesrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [LoadDataForFeeResponse](#loaddataforfeeresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654",
  "reasonCode": "4853"
}
```
**Response:**
```json
{
  "currencies": [
    {
      "name": "USD",
      "value": "USD"
    }
  ],
  "reasonCodes": [
    {
      "name": "7604",
      "value": "7604 - Emergency card replacement fee"
    }
  ],
  "countryCodes": [
    {
      "name": "UNITED STATES",
      "value": "UNITED STATES"
    }
  ],
  "messageTexts": [
    {
      "name": "LOST/STOLEN CARD TRANSACTION FEE",
      "value": "LOST/STOLEN CARD TRANSACTION FEE"
    }
  ]
}
```

---

### 24. Create fee collection event

**Endpoint:** `POST /v6/claims/{claim-id}/fee`

**Operation ID:** `createFee`

**Description:**

Senders (issuers or acquirers) use this endpoint to create a fee collection for receivers (issuers or acquirers). Receivers use this endpoint to reply to a fee collection.

 Note: Senders should create fee collections using this endpoint only (a) when permitted by chargeback rules and (b) when the fee collections are related to disputes.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the Fee item.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |

#### Request Body

Create Fee Request

**Content-Type:** `application/json`

**Schema:** [CreateFeeRequest](#createfeerequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [FeeResponse](#feeresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654",
  "cardAcceptorIdCode": "1",
  "cardNumber": "500000000001234",
  "countryCode": "USA",
  "currency": "USD",
  "feeDate": "2017-02-11",
  "destinationMember": "002083",
  "feeAmount": "100.00",
  "creditSender": "true",
  "creditReceiver": "false",
  "message": "This is a test message",
  "reason": "7604",
  "replyFeeId": "300009520876",
  "mastercomControlNumber": "1589457"
}
```
**Response:**
```json
{
  "feeId": "20002052146"
}
```

---

## Fees (Debit MasterCard and Europe Dual Acquirer)

### 25. Create fee collection event for mastercard debit or bridged debit

**Endpoint:** `POST /v6/claims/{claim-id}/fee/debitmc`

**Operation ID:** `createFeeDebitMC`

**Description:**

Senders (issuers) use this endpoint to create a fee collection for receivers (acquirers) for Mastercard Debit or Europe Dual Acquirer. Receivers use this endpoint to reply to a fee collection.

 Note: Senders should create fee collections using this endpoint only (a) when permitted by chargeback rules and (b) when the fee collections are related to disputes.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the fee item.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |

#### Request Body

Create Fee Request

**Content-Type:** `application/json`

**Schema:** [CreateFeeRequestSingle](#createfeerequestsingle)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [FeeSingleResponse](#feesingleresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654",
  "acquirerCustomerId": "003501",
  "conditionIndicator": "A",
  "controlNumber": "12345678901234567890",
  "declineDate": "013019",
  "functionCode": "700",
  "handlingFee": "20",
  "issuerCustomerID": "123456",
  "reasonCode": "22"
}
```
**Response:**
```json
{
  "status": "Success"
}
```

---

## Fraud

### 26. Retrieve fraud related information

**Endpoint:** `GET /v6/claims/{claim-id}/fraud/loaddataforfraud`

**Operation ID:** `getDataForCreateFraud`

**Description:**

Issuers use this endpoint to obtain fraud-related information associated with a claim before creating a fraud item for the claim.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the fraud item.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [LoadDataForFraudResponse](#loaddataforfraudresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654"
}
```
**Response:**
```json
{
  "acctDeviceTypes": [
    {
      "name": "1",
      "value": "1 - Chip with PIN"
    }
  ],
  "acctStatuses": [
    {
      "name": "N",
      "value": "N - Account has not been closed"
    }
  ],
  "cardValidCodes": [
    {
      "name": "M",
      "value": "M - CVC 2 Valid"
    }
  ],
  "subTypes": [
    {
      "name": "N",
      "value": "N - PIN not used"
    }
  ]
}
```

---

### 27. Create fraud event

**Endpoint:** `POST /v6/claims/{claim-id}/fraud/mastercard`

**Operation ID:** `createFraudMasterCard`

**Description:**

An issuer uses this endpoint to create a new fraud item when the issuer determines that a transaction was fraudulent.

 Note: Mastercom allows issuers to create fraud items in the Fraud and Loss database. However, issuers must use the Fraud and Loss application to make further updates to fraud items.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the fraud item.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |

#### Request Body

Create Fraud MasterCard Request

**Content-Type:** `application/json`

**Schema:** [CreateFraudMasterCardRequest](#createfraudmastercardrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [FraudResponse](#fraudresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654",
  "deviceType": "1",
  "acctStatus": "ACCT_IS_OPEN",
  "reportDate": "2017-02-11",
  "fraudType": "00",
  "subType": "K",
  "cvcInvalidIndicator": "Y",
  "chgbkIndicator": "1"
}
```
**Response:**
```json
{
  "fraudId": "300002292548"
}
```

---

## Health Check

### 28. Retrieve mastercom API status

**Endpoint:** `GET /v6/healthcheck`

**Operation ID:** `healthcheck`

**Description:**

This resource retrieves the status of the Mastercom API suite.

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [HealthCheckResponse](#healthcheckresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{}
```
**Response:**
```json
{
  "0": {
    "status": "true"
  }
}
```

---

## Queues

### 29. Retrieve list of queue names

**Endpoint:** `GET /v6/queues/names`

**Operation ID:** `getQueues`

**Description:**

An issuer or acquirer uses this endpoint to retrieve a list of queues.

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | array |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{}
```
**Response:**
```json
{
  "0": {
    "queueName": "Closed",
    "queueDescription": "Closed"
  }
}
```

---

### 30. Retrieve claims by queue name

**Endpoint:** `GET /v6/queues`

**Operation ID:** `getQueueSummary`

**Description:**

An issuer or acquirer uses this endpoint to retrieve a list of claims from a queue. Claims are sorted by last modified date in descending order.

 Note: The maximum amount of claims returned by the Retrieve Claims from Queue endpoint varies by payload size and request time. Issuers and acquirers should use the Retrieve Claims from Queue With Date Interval endpoint if they expect more than 10,000 claims.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `queue-name` | query | string | Yes | The queue to be queried for a list of claims.   Length: 1-30   Valid Values/Format: Alpha Example: `Rejects` |

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | array |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "queue-name": "Closed"
}
```
**Response:**
```json
{
  "0": {
    "acquirerId": "00000005195",
    "acquirerRefNum": "05103246259000000000126",
    "primaryAccountNum": "52751494691484000",
    "claimId": "200002020654",
    "clearingDueDate": "2017-11-13",
    "clearingNetwork": "GCMS",
    "dueDate": "2017-11-13",
    "isAccurate": true,
    "isAcquirer": true,
    "isIssuer": false,
    "isOpen": true,
    "issuerId": "5258",
    "lastModifiedBy": "user1234",
    "lastModifiedDate": "2017-11-08T13:01:30",
    "merchantId": "0024038000200",
    "progressState": "CB1-4807-O-A-NEW",
    "claimValue": "25.00 USD",
    "queueName": "Closed",
    "createDate": "2017-11-13",
    "transactionId": "118411681",
    "claimType": "Standard"
  }
}
```

---

### 31. Retrieve claims by queue name and date range

**Endpoint:** `POST /v6/queues`

**Operation ID:** `getQueueSummaryPost`

**Description:**

An issuer or acquirer uses this endpoint to retrieve a list of claims from a queue within a date interval. Claims are sorted by last modified date in descending order.

Note: The response is paginated with up to 2,000 claims per page. The total number of pages is included in the response.

Note: To get exact pageCount the lastModifiedDateFrom and lastModifiedDateTo are required and the date range should be less than or equal to 5 days in the request.

#### Request Body

Get queue content request

**Content-Type:** `application/json`

**Schema:** [getQueueContentRequest](#getqueuecontentrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [QueueContentSummary](#queuecontentsummary) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "queueName": "Rejects",
  "lastModifiedDateFrom": "2017-11-08T12:01",
  "lastModifiedDateTo": "2017-11-09T12:01",
  "pageNb": "2"
}
```
**Response:**
```json
{
  "pageCount": "1",
  "claimList": [
    {
      "acquirerId": "00000005195",
      "acquirerRefNum": "05103246259000000000126",
      "primaryAccountNum": "52751494691484000",
      "claimId": "200002020654",
      "clearingDueDate": "2017-11-13",
      "clearingNetwork": "GCMS",
      "dueDate": "2017-11-13",
      "isAccurate": true,
      "isAcquirer": true,
      "isIssuer": false,
      "isOpen": true,
      "issuerId": "5258",
      "lastModifiedBy": "user1234",
      "lastModifiedDate": "2017-11-08T13:01:30",
      "merchantId": "0024038000200",
      "progressState": "CB1-4807-O-A-NEW",
      "claimValue": "25.00 USD",
      "queueName": "Rejects",
      "creditVoucherStatus": "Credit Voucher Accepted",
      "createDate": "2017-11-13",
      "transactionId": "118411681",
      "claimType": "Standard"
    }
  ]
}
```

---

## Reconciliation

### 32. Create recon report

**Endpoint:** `POST /v6/reconreport/data/request`

**Operation ID:** `reconReportDataAcknowledge`

**Description:**

An issuer or acquirer uses this endpoint to request that Mastercom generate a reconciliation report or enhanced reconciliation report for a specified date range.

 Note: The issuer or acquirer receives a unique report identifier in the response. Using the unique report identifier from this endpoint, the issuer or acquirer then retrieves the report from the Retrieve Reconciliation Report endpoint.

#### Request Body

Reconciliation data request

**Content-Type:** `application/json`

**Schema:** [reconReportDataAcknowledgeRequest](#reconreportdataacknowledgerequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [reconReportDataAcknowledgeResponse](#reconreportdataacknowledgeresponse) |
| 400 | Bad Request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "ica": {
    "0": "000001"
  },
  "startDate": "2017-07-21",
  "endDate": "2017-07-21",
  "cycles": {
    "0": 1
  }
}
```
**Response:**
```json
{
  "reportIdentifier": "123e4567-e89b-42d3-a456-556642440000"
}
```

---

### 33. Retrieve recon report

**Endpoint:** `POST /v6/reconreport/data/retrieval/{reportIdentifier}`

**Operation ID:** `reconReportDataRetrieval`

**Description:**

An issuer or acquirer uses this endpoint to retrieve a reconciliation report or enhanced reconciliation report.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `reportIdentifier` | path | string | Yes | A reconciliation id that identifies the report to be retrieved.   Length: 36   Valid Values/Format: Alphanumeric Example: `123e4567-e89b-42d3-a456-556642440000` |

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [reconReportDataRetrivalResponse](#reconreportdataretrivalresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "reportIdentifier": "123e4567-e89b-42d3-a456-556642440000"
}
```
**Response:**
```json
{
  "status": "Available",
  "data": "RmlsZSBJRCAsTWVzc2FnZSBOdW1iZXIsQ2xhaW1DYmtJZCxJdGVtQ2JrSWQsQ2FyZCBJc3N1ZXIgUmVmZXJlbmNlIERhdGEvQ2hh...[truncated]"
}
```

---

## Retrievals

### 34. Retrieve retrieval related information

**Endpoint:** `GET /v6/claims/{claim-id}/retrievalrequests/loaddataforretrievalrequests`

**Operation ID:** `getDataForCreateRetrievalRequest`

**Description:**

An issuer uses this endpoint to retrieve values associated with a claim in preparation for creating a retrieval request.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the retrieval request.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [LoadDataForRetrievalResponse](#loaddataforretrievalresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654"
}
```
**Response:**
```json
{
  "docNeeded": [
    {
      "name": "2",
      "value": "2 - Copy or image (photocopy, microfilm, fax) of original document"
    }
  ],
  "reasonCodes": [
    {
      "name": "6305",
      "value": "6305 - Cardholder does not agree with amount billed"
    }
  ]
}
```

---

### 35. Create retrieval request

**Endpoint:** `POST /v6/claims/{claim-id}/retrievalrequests`

**Operation ID:** `createRetrievalRequest`

**Description:**

Prior to creating a chargeback, an issuer uses this endpoint to create a retrieval request for the acquirer to fulfill by providing a copy of the transaction information document (TID). The TID is used to satisfy a cardholder's inquiry or to satisfy an issuer's investigation of an original transaction.

 NOTE: an issuer is not required to create a retrieval request in order to create a first chargeback on a claim.

 NOTE: Below retrievalRequestReason codes will be accepted until October 23th, 2021.<br>
 6305 - Cardholder does not agree with amount billed<br>
 6321 - Cardholder does not recognize transaction<br>
 6322 - Request Transaction Certificate for a chip transaction<br>
 6323 - Cardholder needs information for personal records<br>
 6341 - Fraud investigation<br>
 6342 - Potential chargeback or compliance documentation is required<br>
 6343 - IIAS Audit (for healthcare transactions only)<br>
 6390 - Identifies a syntax error return

 NOTE: Below retrievalRequestReason code will be accepted for creation of retrieval request starting on October 24th, 2021.<br>
 6343 - IIAS Audit (for healthcare transactions only)

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the retrieval request.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |

#### Request Body

Create Retrieval Request

**Content-Type:** `application/json`

**Schema:** [CreateRetrievalRequest](#createretrievalrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [CreateRetrievalResponse](#createretrievalresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654",
  "retrievalRequestReason": "6343",
  "docNeeded": "2"
}
```
**Response:**
```json
{
  "requestId": "300002296235"
}
```

---

### 36. Add transaction information document

**Endpoint:** `POST /v6/claims/{claim-id}/retrievalrequests/{request-id}/fulfillments`

**Operation ID:** `acqFulfillRetrievalRequest`

**Description:**

After receiving a retrieval request from an issuer, an acquirer uses this endpoint to either provide a copy or substitute draft of the transaction information document (TID) or provide a reason for failing to fulfill the retrieval request.

 NOTE: Below acquirerResponseCd codes will be accepted starting on February 26th, 2023.<br>
 A - Funds Movement Request<br>
 B - Refunded<br>
 C - Initiating Refund<br>
 E - Reject Collaboration<br>
 F - IIAS Unfulfillable<br>
 G - IIAS Invalid request information<br>
 H - IIAS Fulfilled outside MasterCom system

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the Retrieval Request.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |
| `request-id` | path | string | Yes | Request Id for the retrieval request.   Length: 1-19   Valid Values/Format: Numeric Example: `300002296235` |

#### Request Body

Acquirer Retrieval Fulfillment information

**Content-Type:** `application/json`

**Schema:** [AcquirerFulfillmentRequest](#acquirerfulfillmentrequest)

**Examples:**

- **Collaboration Request to Move Funds - A** → See examples section: `AcquirerFulfillmentCollabFundsMvmtReq`
- **Collaboration Provide Refund/Reversal - B** → See examples section: `AcquirerFulfillmentCollabRefund`
- **Collaboration Provide Credit Voucher - B** → See examples section: `AcquirerFulfillmentCollabVoucher`
- **Collaboration Provide Intent to Refund - C** → See examples section: `AcquirerFulfillmentCollabIntentToRefund`
- **Collaboration Update with Refund Details - C** → See examples section: `AcquirerFulfillmentCollabRefundDetailsC`
- **Reject Collaboration - E** → See examples section: `AcquirerFulfillmentCollabReject`
- **Respond to U.S. Healthcare** → See examples section: `AcquirerFulfillmentCollabHealthcare`

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [AcquirerFulfillmentResponse](#acquirerfulfillmentresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654",
  "request-id": "300002296235",
  "acquirerResponseCd": "D",
  "docTypeIndicator": "2",
  "memo": "This is an example of what a memo could contain",
  "fileAttachment": {
    "filename": "test.tif",
    "file": "sample file"
  }
}
```
**Response:**
```json
{
  "requestId": "300002296235"
}
```

---

### 37. Approve or reject retrieval

**Endpoint:** `POST /v6/claims/{claim-id}/retrievalrequests/{request-id}/fulfillments/response`

**Operation ID:** `issuerResponseRetrievalRequest`

**Description:**

After an acquirer fulfills a retrieval request, an issuer uses this endpoint to approve or reject a retrieval request fulfillment. The issuer approves the fulfillment or rejects the fulfillment if the documentation does not meet requirements.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the Retrieval Request.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |
| `request-id` | path | string | Yes | Request Id for the Retrieval Request.   Length: 1-19   Valid Values/Format: Numeric Example: `300002296235` |

#### Request Body

Issuer Fulfillment Response

**Content-Type:** `application/json`

**Schema:** [IssuerFulfillmentRequest](#issuerfulfillmentrequest)

**Examples:**

- **Respond with APPROVE** → See examples section: `IssuerFulfillmentApprove`
- **Respond with REJECT** → See examples section: `IssuerFulfillmentReject`

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [RetrievalResponse](#retrievalresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654",
  "request-id": "300002296235",
  "issuerResponseCd": "APPROVE",
  "memo": "This is an example of what a memo could contain",
  "rejectReasonCd": null
}
```
**Response:**
```json
{
  "requestId": "300002296235"
}
```

---

### 38. Document status for retrieval requests

**Endpoint:** `GET /v6/claims/{claim-id}/retrievalrequests/{request-id}/documents`

**Operation ID:** `getRetrievalDoc`

**Description:**

Issuers and acquirers use this endpoint to retrieve all documents from acquirers associated with retrieval request fulfillment.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | The Claim Id.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |
| `request-id` | path | string | Yes | The Request Id.   Length: 1-19   Valid Values/Format: Numeric Example: `300002296235` |
| `format` | query | string [ORIGINAL, MERGED_TIFF, MERGED_PDF] | Yes | File Format.   Length: 8-11   Valid Values/Format: ORIGINAL, MERGED_TIFF, MERGED_PDF Example: `ORIGINAL` |

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [DocumentResponseStructure](#documentresponsestructure) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654",
  "request-id": "300002296235",
  "format": "ORIGINAL"
}
```
**Response:**
```json
{
  "fileAttachment": {
    "filename": "RT_300002296235.zip",
    "file": "UEsDBBQAAAAIAFqJLExsuevPiCkAAPjiAAASAAAAdGVzdEltYWdlX2dyZXkudGlm7Fx5WFPXtt8hzIQhjI4YI4hcCQEkDJFBSBCpBJFRvILNcAjRJCckJ4DU2VLUSkWcW6m2Sq2t2mqdJ5xtVUSccEJFxRnUOl1H3j4nCbM+8t6773vf986P7+SsvfZa67f2Pnuvc/7ZxMX9A1gCAFzAB2AKKEAPCpWQX1pBUS/fpgFgopdxD6peNoOXuV7GLzrFFPSHd2vYcGyjd2kju+rlbHi56e2/gYpeFDNC72gCQG+93AvKg/T2UAT+FBPAhHcmbARA2RnKfCiHt7HZY0IBkiAAxsBG3FcUMBPmvicEAFmxbownbwKgKbYApVQAImJ0Qz41Av4IYMzRAEhSAViSAcCfmQC8EQLgAxNVqQGgTYf2X0J5NgA/zgPgXCkc+2IAQpYDMKEcgBU/wDgVMM4vMM5GGGczjLMNxtkF4+yDcQ4BkHUMgNITABysBuD5WQAGXoR5XgVg7g3oY/rxiwfHsMJWJ/+7rig4X/uPsoCDV+s9SoKKEEZiDoqhmhxUxeDxGAF+/oGMQekypQTN13gD2Azh+vlz/QMY/kO4/n5cjh8IiyxQCcUTEYwhQqQyZTjz8e5KJkMmCWemcwR+AhUPyZGNKFQjyYUJKeLCieJQCTMywjqsgFugUCkQTMgoUMiVGm5BOFOI83OhjKvZTAZhgk0MZ+oSGyNIZPBQNcLg+AaxxHhiwaG+/pyg4NBgHyJRtl8IO8CP5RfK5Qzh+gUw9GBGWMPfMLUkm5vEH66ng61wZg6Gqbhsdn5+vm/+EF9ULWX7h4aGsv0C2AEBLGjB0kxSYsICllIzQBfEEIePaMRqmQqToUoG3haKUC0WzmRaM9pAPy6FqoVIqfElxugrRhXsAqGK7e/rx+7KSSJu8VFp1XIiNYmYjcgRBaLENNDPn/0RMoHg03QKRZeeGiwmD/u0pyZlkgphJyEaVKsWIzF5MJMBXYVSGRZQ1+FaunWjj2iNEAbT5/LUiBBD1SkoKo/4TxdkGLujS1fRED68IvC1y/LzZ/kHpBjWLssviOvnF8buYNkhhgCuRokQE3YnSjvbjnFQiSx7UreitFq2iSERc7NRtUKIRcgUQinCxmTZ2WHsVm17OoGAG6fUYEKlGInjR0CFr0wmgQyIeEgAImLBdR7MCggNzGaJOJxAVqgoJChI6B8oGhLoH8bu5N4pNB8Va/GlqA8tMTJ0G/dOoUepZbCMCOX/TYouwnSiGiHTwHUzKaK1w7DBk5Hc9lpDh1xGbHiVUK1B8P0QzjRsCGYnB9yH2FdcoRgvFRFiYo1JwtjttB93k/1XH2An949z5Ocgyk+txzZWHw+iQbOxfKEaiZLCme7Otu3KrdN8s3UT3uHxsDs/nzD2x55nWEu14fJQOaqGOwuJgBPUlbpLrzgeL1GNZsvkSAQfxRixQpkSDsYzjN2lieEdwe7wktC9gdj6VxB8+7FbXn9E178dJIlRIEmMAkliFEgSo0CSGAWSxCiQJEaBJDEKJIlRIEmMAkliFEgSo0CSGAWSxCiQJEbh/zxJt0GSGAWSxCiQJEaBJDEKJIlRIEmMAkliFEgSo0CSGAWSxCiQJEaBJDEKJIlRIEmMAkliFEgSo0CSGAWSxCiQJEaBJDEKJIlRIEmMAkliFP6fkFi3HihDlJJwZj4zMiIkOk5g6kkc3nMAHUD0cQmxQd+vO9xoo1LLlNgoLabSYvrTiyBRgyWLUFROWMQpMQRRahUGGb/z5Gq8bU/4JssKcItoGYb7tMZE1AlCBZISMyalhUznkKhG0exkBNOqRokmiKGaBhKBGqDwLxswQDJAAAa0QEW4WKtarA1houWYUp8RTaSVyTGZkggJ21aENU+QMVI34qG4vQmr3Ygd24x4FHHwQIMfuyTGpcKUhkHAQYrULY0kqUbQ2qNW8lobSqy1ES+Sa1oaCVIsr6URo5DzWxpwHltDR4snSvUToT9vmhQbzcNPlBKyhMGQoFrRMPQGMCBWreyki5Z3totWS1JSldjwAUlyDLRBtFzC6EqfpJFjhD6xQD5Mvd+gts5DxBiq5gsxYcuqSJQmavTPgJD1dzxtEI9kY12FT0FVXdImi+U6faJaPCyjRe0gVqOq9BxEmYg/L5lSapgxO7wjCeYUjWIYqpCjSqnehWbowVNoo7c16JNk0py2HTaGDphbixpfOdRGXQ4UH+KxUAx3YlUNJPrsWkcQOV2/w2yJpim8xhLtni1tdyLqM6JtZfCjEF5eut1qbXj8Ohbqc0J2BBzCEF/D5m16XxFyHwCauwT1nj4jS904dIvLZJju0ve5t2Sna/fCJSqnhYkA7TZM+GVLwp3RRmf1Umf/UYMWtG5HoNTK5fpzyuYiVKuUaDrsBjHmb0gTX0JtHh/o8JxBdMt60KWR1PK4iSGYa+QyMaJJk8fjC5bSjseM6CNmHAALohHHbxPbQqpGtap2KnOUOD5nqEUxybiT7kgdvuiEWgyNRZSIGj/ORmQ/SWUopdY6Y1yD98QppIz/hfFTtWp5u4JMTH57jUAjbV+0zYVyLEUobaezEyPQDynA4jQjUgTxhrJgaVC3M7bKQdWFUXKZ1DBT9rrBjzCo8dmVINlCLVEXrPIQNdaFeZpB3d7cRiQlTqe1mVy6ziE6tqUDTyMBVeJ3KwxVweKvQdpOnLUcTmQnLU1EFJhOehs1XkY6qIkdNEjnhy/nyCegVe8MgP4RmhBtGtHM1k/RZHitgiIczj1YSUw/Az0ApflicyOgEacEx/MEsN0E7IgWkEzH/ZqvgC8BzdLS0sqSZmVFo9tY29Bd7Gk0e5ceTk4uTk496DQCdNonQLG1sbG1s3Wws3NwtrOzc8Z/7Jx1LvTuBGjeD+iWMPnJVAoTmNApVDqluQ4O1LL5MCUSZmlGIaCfOCqgmJiamVtYWlnbUDp2UoAJ1dDpACimFKqJqYmZhbmlGZU2BHbSqab9Hf3NokYLnZi5MwLMnRf8sCl6gIdL0gHRkED1zBqehWdZ8pP6p2INx/XHzbMG8hemSGIOrsaC3M6k3kT+/uPLQ2e1t54N91q0pmjL4sPnbj+v2HrkfMOLtOy8r5b8tO3ohTsvg2PTpfnFS9duP1Z79xUdmJjAbE2JnCzMzThECv39HU1hBrlMJ7OAGQuc8QwOJNU8GeIhqlfPLOMlu4g1gU89zfEELAZyDp6BSax2k8SkBmHIzZYUPp6BV2sKzZcBjUpw0kEkeBHkv3jT1gunj/qZb93gAHpxNuQkxXksWlMW5+ntURb3WZxHWVJZB0Vy8+kXKZ8yIBTNoLmm1Yy378297Lde8q/ejs1c5xcxXc0UURz48wMX1fblmV54GJ4n/9zuizXD7N745jetyygvHq2R79zg4/c6MvbywynNYOejjYd5Ai9s01pVcQ7H3W7E2bg6zpamkjPmvweVXL8UN6IyR+jwt69Qkbt3/71gN86+x6uf/1qIXs4Mswp1mdF7ji/d3bNHauIyMf/B57baxW63xBd5K5P7RJk6WjBCV3/4+mpZGafqC95l3rf1rAGJUU3lfsCyF2V/QtkiOJQKOADv5jPdG/PZ7pmd657Z+e6ZXeieWW33zLr5dC8RFac/sccy8X2Gbzh7EAUk8INbBD+2GfDzOwfKGLw0hKQiPog/bcEAPPjHAAHAD/iDQH0po5ZG8aNjTBzw/w+CqWOTojLGZIxlWFTDLwqqbpsLxRpVVGJiPC4rdSW5LWCQlxd0L46zLDwWMA5mYkgM7/jXa4AE0eAvUvxLSpqPqaCeMg7KzqKJhIwP03liShL8aqV8CUszDYOvLjwID1VNImo8A/9XFwzdUe3kSRoMUWgYcUoxqlahhre6ngOHbdvDz+B/GPg84ve289Oqa4VYq84jBJ2RA3xEfmAYfIBCMA0W6iPgEcWNwqdoKT9Trpm4miSZlJqcpjpRM6g/UB+ZhprONr1s5mtWZHbNPNR8iflLi3SL3ZZMy7mWz62EVqeto6y32HjbrKL1oi22dbQts3OyW2Lfx361g4/DDnoM/Zwj4vgvpxJnD+e9Lukuz13L3ALczvUo6One82gvtHfv3n/2yevr1fey+zf9+P3eMbb2z2X6MhsHbPBQeQZ6vh54wGvuoFRvpveTf+wfXOaDsLi+dN8H7MN+q/ynBIwbEh7Yj0Pl3A86HbwzZE1oKXfqUEVYVvjoiJjIocOGRPlED+Qx+YyYfsMZsQNGeMWxPgscGRYfK0hO+HwUmjht9IKkiuRdKTWp99NNxrhncMeO+WfBuOWZe7JufG4uZInSxNMl65FLUsucEJlsQvnEGoWFMhItUG3ObdIMxiZof8l7VMCehBXumkyZ8tnUxdPqZ/jOnDyrqsj9q9ziI3N6z1V/faLE85sZ8+sWhJV9t/D94vFLDi0btHz+t69WfF5+YmXwqoofnVcXrXn5k2zttXUpv5xYz9+w77fQ37dtDvxjy9bAbdt3cHdW7o7ZU7UvrfLGAfnB14eLj7od+/mvocdPnRRXvakuqxl85sg54fkPtSsuhV2uuzrt2oDrf9Yrb7nerrwju+d8/8BDtLFf0+knM//mPHvwYtWr9NcOb46/K/oQ1dw8PXK/Q4iHq63Fl9HCxB2SoL72s/mi0SFr0lhu84aLR++4nRfer3SEJCnUY4nAa9FnSNJOybZM9rL47OTQNRcUwSsSpMk7b7+cErkqMSeF6+laErtq/zrnXUhQ+aifUyakcivSNqSvT5uYuqshb+/438fI04Z6LqlKq8hoTNuNbKtDd4xTpg+tuNCYvycLTd/d8PL9pC3Xn4wJG+hmW3xYlDtmT3Zw3/l/StQZoRcsvJeezNZk7LmTH77ydA42NnzgUoHtCdmLsXuzt2f+dlGe98/w"
  }
}
```

---

### 39. Document status for retrieval events

**Endpoint:** `PUT /v6/retrievalrequests/status`

**Operation ID:** `retrieveFulfillmentStatus`

**Description:**

Issuers and acquirers use this endpoint to search for the status of documents that are associated with a specific list of retrieval requests.

 Note: issuers and acquirers may send a maximum of 2,000 retrieval fulfillment IDs within a single request.

#### Request Body

Retrieval information

**Content-Type:** `application/json`

**Schema:** [RetrievalStatusRequest](#retrievalstatusrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [RetrievalStatusResponse](#retrievalstatusresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "retrievalList": {
    "0": {
      "claimId": "200002020654",
      "requestId": "12344"
    }
  }
}
```
**Response:**
```json
{
  "retrievalResponseList": [
    {
      "claimId": "200002020654",
      "requestId": "12344",
      "status": "COMPLETED"
    }
  ]
}
```

---

## Retrievals (Debit MasterCard and Europe Dual Acquirer)

### 40. Create a retrieval request for mastercard debit or bridged debit

**Endpoint:** `POST /v6/claims/{claim-id}/retrievalrequests/debitmc`

**Operation ID:** `createRetrievalRequestDebitMC`

**Description:**

Prior to creating a chargeback, an issuer uses this endpoint to create a retrieval request for a Debit Mastercard or Europe Dual Acquirer transaction for the acquirer to fulfill by providing a copy of the transaction information document (TID). The TID is used to satisfy a cardholder's inquiry or to satisfy an issuer's investigation of an original transaction.

 NOTE: An issuer is not required to create a retrieval request in order to create a first chargeback on a claim.

 NOTE: Creation of retrieval request is allowed only for reversalReasonCode (healthcare reason code (43)) starting on October 24th, 2021.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the Retrieval Request.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |

#### Request Body

Create Retrieval Request

**Content-Type:** `application/json`

**Schema:** [CreateRetrievalRequestSingle](#createretrievalrequestsingle)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [CreateRetrievalResponse](#createretrievalresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654",
  "replacementAmount": "200.00",
  "reversalReasonCode": "04",
  "controlNumber": "12354",
  "usageCode": "1",
  "documentType": "1",
  "additionalInformation": "SMTM Manual"
}
```
**Response:**
```json
{
  "requestId": "25859113"
}
```

---

### 41. Approve or reject retrieval for mastercard debit or bridged debit

**Endpoint:** `POST /v6/claims/{claim-id}/retrievalrequests/debitmc/{request-id}/fulfillments/response`

**Operation ID:** `issuerResponseRetrievalDebitMCRequest`

**Description:**

After an acquirer fulfills a retrieval request, an issuer uses this endpoint to approve or reject a retrieval request fulfillment. The issuer approves the fulfillment or rejects the fulfillment if the documentation does not meet requirements.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id for the Retrieval Request.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |
| `request-id` | path | string | Yes | Request Id for the Retrieval Request.   Length: 1-19   Valid Values/Format: Numeric Example: `300002296235` |

#### Request Body

Issuer Fulfillment Response

**Content-Type:** `application/json`

**Schema:** [IssuerFulfillmentRequest](#issuerfulfillmentrequest)

**Examples:**

- **Respond with APPROVE** → See examples section: `IssuerFulfillmentApprove`
- **Respond with REJECT** → See examples section: `IssuerFulfillmentReject`

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [RetrievalResponse](#retrievalresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654",
  "request-id": "300002296235",
  "issuerResponseCd": "APPROVE",
  "memo": "This is an example of what a memo could contain",
  "rejectReasonCd": null
}
```
**Response:**
```json
{
  "requestId": "300002296235"
}
```

---

### 42. Retrieve documents for retrieval events for mastercard debit or bridged debit

**Endpoint:** `GET /v6/claims/{claim-id}/retrievalrequests/debitmc/{request-id}/documents`

**Operation ID:** `getRetrievalDocDebitMC`

**Description:**

Issuers and acquirers use this endpoint to retrieve all documents from acquirers associated with retrieval request fulfillment

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | The Claim Id.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |
| `request-id` | path | string | Yes | The Request Id.   Length: 1-19   Valid Values/Format: Numeric Example: `300002296235` |
| `format` | query | string [ORIGINAL, MERGED_TIFF, MERGED_PDF] | Yes | File Format.   Length: 8-11   Valid Values/Format: ORIGINAL, MERGED_TIFF, MERGED_PDF Example: `ORIGINAL` |

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [DocumentResponseStructure](#documentresponsestructure) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654",
  "request-id": "300002296235",
  "format": "ORIGINAL"
}
```
**Response:**
```json
{
  "fileAttachment": {
    "filename": "RT_300002296235.zip",
    "file": "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"
  }
}
```

---

### 43. Document status for retrieval requests for mastercard debit or bridged debit

**Endpoint:** `PUT /v6/retrievalrequests/debitmc/status`

**Operation ID:** `retrieveFulfillmentDebitMCStatus`

**Description:**

Issuers and acquirers use this endpoint to search for the status of documents that are associated with a specific list of retrieval requests.

 Note: Issuers and acquirers may send a maximum of 2,000 retrieval fulfillment IDs within a single request.

#### Request Body

Retrieval information

**Content-Type:** `application/json`

**Schema:** [RetrievalStatusRequest](#retrievalstatusrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [RetrievalStatusResponse](#retrievalstatusresponse) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "retrievalList": {
    "0": {
      "claimId": "200002020654",
      "requestId": "12344"
    }
  }
}
```
**Response:**
```json
{
  "retrievalResponseList": [
    {
      "claimId": "200002020654",
      "requestId": "12344",
      "status": "COMPLETED"
    }
  ]
}
```

---

## Transactions

### 44. Search for transactions

**Endpoint:** `POST /v6/transactions/search`

**Operation ID:** `transactionSearch`

**Description:**

An issuer uses this endpoint to search for information about an original transaction. An issuer may use this information to take an action on the original transaction, such as creating a claim.

 Note: Mastercom retrieves transactions normally when the clearing occurs within 150 days of authorization. To find late presentments and Brazilian installment transactions, provide the acquirer reference number (ARN) and the clearing date associated with the ARN of the first presentment in the request.

#### Request Body

Transaction Search Request

**Content-Type:** `application/json`

**Schema:** [TransactionSearchRequest](#transactionsearchrequest)

**Examples:**

- **Txn Search ARN+PAN** → See examples section: `TxnSearchArnPan`
- **Txn Search BNR+PAN** → See examples section: `TxnSearchBnrPan`
- **Txn Search ARN** → See examples section: `TxnSearchArn`
- **Txn Search PAN** → See examples section: `TxnSearchPan`

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [TransactionSummary](#transactionsummary) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "acquirerRefNumber": "05436847276000293995738",
  "primaryAccountNum": "5488888888887192",
  "transAmountFrom": "10000",
  "transAmountTo": "20050",
  "tranStartDate": "2021-01-01",
  "tranEndDate": "2021-01-30"
}
```
**Response:**
```json
{
  "authorizationSummaryCount": "1",
  "message": "Search returned 1 records",
  "authorizationSummary": [
    {
      "originalMessageTypeIdentifier": "0110",
      "banknetDate": "210127",
      "transactionAmountUsd": "401.17",
      "primaryAccountNumber": "5488888888887192",
      "processingCode": "00",
      "transactionAmountLocal": "000000010000",
      "authorizationDateAndTime": "0127125633",
      "track2": "Y101",
      "authenticationId": "418443",
      "cardAcceptorName": "Amazon",
      "cardAcceptorCity": "SAINT LOUIS",
      "cardAcceptorState": "MO",
      "track1": "N",
      "currencyCode": "840",
      "chipPresent": "N",
      "transactionId": "hqCnaMDqmto4wnL+BSUKSdzROqGJ7YELoKhEvluycwKNg3XTzSfaIJhFDkl9hW081B5tTqFFiAwCpcocPdB2My4t7DtSTk63VXDl1CySA8Y=",
      "clearingSummary": [
        {
          "primaryAccountNumber": "5488888888887192",
          "transactionAmountLocal": "2500",
          "dateAndTimeLocal": "210127160100",
          "cardDataInputCapability": "5",
          "cardholderAuthenticationCapability": "9",
          "cardPresent": "1",
          "acquirerReferenceNumber": "05413364365000000000667",
          "cardAcceptorName": "Amazon",
          "currencyCode": "840",
          "transactionId": "U7dImb1ncs24LKNU9dDpl+FHlPzyfYOOvS5PijTlO6wHH09l7aiVJ1KJHp3sWPUHH0l90J1U82DGrE3hq72A=",
          "installmentPaymentDataBrazil": "4070000000479500302000000015983000000000000000000000000",
          "settlementIndicator": "C"
        }
      ]
    }
  ]
}
```

---

### 45. Retrieve clearing details

**Endpoint:** `GET /v6/claims/{claim-id}/transactions/clearing/{transaction-id}`

**Operation ID:** `getTransactionClearingDetail`

**Description:**

After an issuer creates a claim, the issuer uses this endpoint to retrieve clearing details for the original transaction involved in the claim. Acquirers may also use this endpoint to retrieve clearing details associated with an original transaction.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |
| `transaction-id` | path | string | Yes | Clearing transaction id.   Length: N/A   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Example: `FIEaEgnM3bwPijwZgjc3Te+Y0ieLbN9ijUugqNSvJmVbO1xs6Jh5iIlmpOpkbax79L8Yj1rBOWBACx+Vj17rzvOepWobpgWNJNdsgHB4ag` |

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [ClearingDetail](#clearingdetail) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654",
  "transaction-id": "FIEaEgnM3bwPijwZgjc3Te+Y0ieLbN9ijUugqNSvJmVbO1xs6Jh5iIlmpOpkbax79L8Yj1rBOWBACx+Vj17rzvOepWobpgWNJNdsgHB4ag"
}
```
**Response:**
```json
{
  "accountLevelManagementAccountCategoryCode": "N",
  "acquirerReferenceData": "25131304365000000033393",
  "acquiringInstitutionIdCode": "999663",
  "approvalCode": "97574B",
  "businessCycle": "01",
  "businessServiceArrangementTypeCode": "2",
  "businessServiceIdCode": "10001",
  "cardAcceptorBusinessCode": "5411",
  "cardAcceptorCity": "SAINT LOUIS",
  "cardAcceptorClassificationOverrideIndicator": "N",
  "cardAcceptorCountry": "USA",
  "cardAcceptorIdCode": "375555569895",
  "cardAcceptorName": "Amazon",
  "cardAcceptorPostalCode": "63102",
  "cardAcceptorState": "MO",
  "cardAcceptorStreetAddress": "Gateway Arch Trail",
  "cardAcceptorTerminalId": "73429189",
  "cardAcceptorUrl": "www.amazon.com",
  "cardCaptureCapability": "9",
  "cardDataInputCapability": "5",
  "cardDataInputMode": "R",
  "cardDataOutputCapability": "0",
  "cardholderAuthenticationCapability": "9",
  "cardholderAuthenticationEntity": "9",
  "cardholderAuthenticationMethod": "9",
  "mexicoDomesticTaxAmount": "002901985000000000121985000000000124",
  "mexicoDomesticTransactionFeeAmount": "002901985000000000121985000000000125",
  "mexicoDomesticSettlementFeesAndVat": "002901985000000000121985000000000126",
  "cardholderBillingAmount": "2500",
  "cardholderBillingCurrencyCode": "840",
  "cardholderFromAccountCode": "00",
  "cardholderPresentData": "0",
  "cardholderToAccountCode": "00",
  "cardIssuerReferenceData": "9000000959",
  "cardPresentData": "1",
  "cardProgramIdentifier": "MCC",
  "centralSiteBusinessDate": "210127",
  "centralSiteProcessingDateOriginalMessage": "210127",
  "currencyCodeCardholderBilling": "000",
  "currencyCodeReconciliation": "840",
  "currencyCodeTransaction": "840",
  "currencyExponentCardholderBilling": "2",
  "currencyExponentReconciliation": "2",
  "currencyExponentTransaction": "2",
  "dataRecord": "1",
  "electronicCommerceCardAuth": "0",
  "electronicCommerceSecurityLevelIndicator": "0",
  "electronicCommerceUcafCollectionIndicator": "2",
  "forwardingInstitutionIdCode": "5258",
  "functionCode": "200",
  "gcmsProductIndentifier": "MPL",
  "installmentAmount": null,
  "installmentNumber": null,
  "installmentFee": null,
  "installmentParameters": null,
  "installmentPaymentData": "20",
  "installmentPaymentDataAnnualPercentageRate": "0",
  "installmentPaymentDataFirstInstallmentAmount": "24",
  "installmentPaymentDataInstallmentFee": "0",
  "installmentPaymentDataInterestRate": "23",
  "installmentPaymentDataNumberInstallments": "2",
  "installmentPaymentDataSubsequentInstallmentAmount": "20",
  "installmentPlanType": null,
  "integratedCircuitCardRelatedData": "100",
  "interchangeRateDesignator": "79",
  "licensedProductIndentifier": "MPL",
  "legalCorporateName": "Amazon",
  "localMessageReasonCode": null,
  "localTax1IVA": null,
  "localTransactionDateTime": "210127010100",
  "mastercardAssignedId": "PDS176",
  "mastercardAssignedIdOverrideIndicator": "N",
  "mastercardMappingServiceAccountNumber": "5154676300000001",
  "masterPassIncentiveIndicator": "N",
  "messageReasonCode": "1401",
  "messageReversalIndicator": "R",
  "numberOfInstallments": null,
  "originalInformationInstallments": null,
  "originatingMessageFormat": "2",
  "partnerIdCode": "PDS190",
  "pinCaptureCapability": "1",
  "primaryAccountNumber": "5154676300000001",
  "processingCode": "00",
  "productOverrideIndicator": "Y",
  "programRegistrationId": "C57",
  "qpsPaypassEligibilityIndicator": "I",
  "rateIndicator": "N",
  "receivingInstitutionIdCode": "2202",
  "reconciliationAmount": "2500",
  "reconciliationCurrencyCode": "840",
  "remotePaymentsProgramData": "1",
  "serviceCode": "200",
  "settlementData": "1",
  "settlementIndicator": "M",
  "specialConditionsIndicator": "N",
  "terminalDataOutputCapability": "0",
  "terminalOperatingEnvironment": "2",
  "terminalType": "CT6",
  "tokenRequestorId": "4GFCYTH29Z",
  "totalTransactionAmount": null,
  "transactionAmountLocal": "2500",
  "transactionCategoryIndicator": "02",
  "transactionCurrencyCode": "840",
  "transactionDestinationInstitutionIdCode": "2705",
  "transactionLifeCycleId": "MPL2OSTCV0728",
  "transactionOriginatorInstitutionIdCode": "5258",
  "transactionType": "Clearing",
  "transitProgramCode": "05",
  "walletIdentifierMdes": "101",
  "installmentPaymentDataBrazil": "4070000000479500302000000015983000000000000000000000000",
  "additionalAmountDataCode": "0040710D100003466878;2929985C876543456687;1929985D876543456687;0040710D100003466878",
  "currencyConversionAssessmentAmount": "000000021250",
  "japanCommonMerchantCode": "0410",
  "installmentData": "1261610E81023498764532103",
  "flexCode": "003"
}
```

---

### 46. Retrieve authorization details

**Endpoint:** `GET /v6/claims/{claim-id}/transactions/authorization/{transaction-id}`

**Operation ID:** `retrieveAuthorizationDetail`

**Description:**

After an issuer creates a claim, the issuer uses this endpoint to retrieve authorization details for the original transaction involved in the claim.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |
| `transaction-id` | path | string | Yes | The Authorization Transaction Identifier from Authorization Summary Results.   Length: N/A   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Example: `FIEaEgnM3bwPijwZgjc3Te+Y0ieLbN9ijUugqNSvJmVbO1xs6Jh5iIlmpOpkbax79L8Yj1rBOWBACx+Vj17rzvOepWobpgWNJNdsgHB4ag` |

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [AuthorizationDetail](#authorizationdetail) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654",
  "transaction-id": "FIEaEgnM3bwPijwZgjc3Te+Y0ieLbN9ijUugqNSvJmVbO1xs6Jh5iIlmpOpkbax79L8Yj1rBOWBACx+Vj17rzvOepWobpgWNJNdsgHB4ag"
}
```
**Response:**
```json
{
  "accountNumber": "5154676300000001",
  "accountNumberIndicator": "I",
  "acquirer": "N",
  "acquiringInstitutionCountryCode": "USA",
  "acquiringInstitutionId": "2705",
  "addressVerificationServiceResponse": "S",
  "adviceReasonCode": "160",
  "atcDiscrepancyIndicator": "G",
  "atcDiscrepancyValue": "00005",
  "atcValue": "00053",
  "authenticationIndicator": "1",
  "authorizationIdResponse": "418443",
  "banknetDate": "210127",
  "banknetReferenceNumber": "U68FRG",
  "billingCurrencyCode": "840",
  "cardAcceptorCity": "SAINT LOUIS",
  "cardAcceptorId": "0024038000200",
  "cardAcceptorName": "Amazon",
  "cardAcceptorState": "MO",
  "cardAcceptorTerminalId": "TERM-041",
  "cardholderActivatedTerminalLevel": "6",
  "cardholderBillingActualAmount": "000000010000",
  "cardholderBillingAmount": "000000010000",
  "cardAuthenticationMethodValidationCode": "N",
  "conversionDate": "0127",
  "conversionRate": "61000000",
  "electronicCommerceIndicators": "215",
  "electronicCommerceSecurityLevelIndicatorAndUcafCollectionIndicator": "10",
  "expirationDatePresenceInd": "N",
  "finalAuthorizationIndicator": "0",
  "financialNetworkCode": "MPL",
  "forwardingInstitutionId": "5258",
  "infData": "4814653169024340",
  "integratedCircuitCardRelatedData": "100",
  "issuer": "N",
  "mastercardPromotionCode": "HGMINS",
  "mccMessageId": "MCW",
  "merchantAdviceCode": "03",
  "merchantCategoryCode": "3370",
  "mobilePhoneNumber": null,
  "mobilePhoneServiceProviderName": null,
  "originalAcquiringInstitutionIdCode": "2705",
  "originalElectronicCommerceSecurityLevelIndicatorAndUcafCollectionIndicator": "0",
  "originalIssuerForwardingInstitutionIdCode": "5258",
  "originalMessageTypeIdentifier": "0110",
  "pinServiceCode": "TV",
  "posCardDataTerminalInputCapability": "0",
  "posCardholderPresence": "0",
  "posCardPresence": "0",
  "posEntryModePan": "05",
  "posEntryModePin": "1",
  "posTerminalAttendance": "0",
  "posTerminalLocation": "0",
  "posTransactionStatus": "0",
  "primaryAccountNumber": "510001000000134",
  "primaryAccountNumberAccountRange": "510001000",
  "privateData": "38038405002UU90220107ACQREG10207ISSREG17104C2C 102101920CM04020CM0402S1I1353041100000000000050100604...[truncated]",
  "processingCode": "00",
  "realTimeSubstantiationIndicator": "0",
  "reasonForUcafCollectionIndicatorDowngrade": "210",
  "recordDataPresenceIndicator": "N",
  "responseCode": "00",
  "retrievalReferenceNumber": "730607628081",
  "settlementActualAmount": "000000010000",
  "settlementDate": "0127",
  "stan": "002511",
  "storageTechnology": "01",
  "systemsTraceAuditNumber": "002511",
  "tokenAssuranceLevel": "99",
  "tokenRequestorId": "12345678936",
  "track1": "N",
  "track2": "Y101",
  "transactionActualAmount": "000000010000",
  "transactionAmountLocal": "10000",
  "transactionCategoryCode": "R",
  "transactionCurrencyCode": "840",
  "transactionType": "Authorization",
  "transmissionDateAndTime": "0127075837",
  "universalCardholderAuthenticationFieldUcaf": "PARTIALSHIPMENT0000000000000 ALrP9TrnbuMCAANkrglrAoABFA== ACFa0knOekU7AAnwugwJAoABFA== ICQk7mTHQKqwx9tKqqY= hgeiVCYsZLM8YwAAAFcqCVkAAAA= hgeiVCYsZLM8YwAAAIFuCEYAAAA= hmJA2XBYTaCdCAEAABneAAAAAAA=",
  "vcnProductCode": "MCO",
  "walletIdentifier": "100"
}
```

---

## Transactions (Debit MasterCard and Europe Dual Acquirer)

### 47. Search for transactions for mastercard debit or bridged debit

**Endpoint:** `POST /v6/transactions/debitmc/search`

**Operation ID:** `transactionMessageSearchDebitMC`

**Description:**

An issuer uses this endpoint to search for information about an Mastercard Debit or Europe Dual Acquirer original transaction. An issuer may use this information to take an action on the original transaction, such as creating a claim.

#### Request Body

Transaction DebitMC Message Search Request

**Content-Type:** `application/json`

**Schema:** [TransactionSingleSearchRequest](#transactionsinglesearchrequest)

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [TransactionSingleMessageSummaryList](#transactionsinglemessagesummarylist) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "primaryAccountNumber": "5488888888887192",
  "settlementFromDate": "2019-05-01",
  "settlementToDate": "2019-05-03"
}
```
**Response:**
```json
{
  "transactionSummaryList": [
    {
      "authTransactionId": "b50oo1RrbahBE83Z4ISk6U1hAV1e8lENtCJKVCqk3goL9bTycP_XhR1oA9qn_Q45-Atz6z1Q7JPPo8vlrnE1yQ==",
      "clearingTransactionId": "uucmqifQnKxn3nAWvUdrsFAK3Fzta4COnX6IAfHG2EwSRpJBpxlkBQAIIAbousIFu_vq1iUq00M2hPpzK2Ed_cZv6lauVwLQ6Fv5NIt1kQ8=",
      "singleMessageSummaryDetails": {
        "authTransaction": {
          "acquirerReferenceNumber": "614659480514   000032  ",
          "adviceReasonCode": "290",
          "brand": "MD",
          "localCurrencyCode": "826",
          "localRequestedAmount": "22.15",
          "merchantName": "R-5311-USA",
          "merchantType": "5311",
          "primaryAccountNumber": "5488888888887192",
          "processingCode": "000000",
          "responseCode": "00",
          "responseSource": "Member",
          "settlementDate": "051519",
          "switchSerialNumber": "301863212",
          "switchDateTime": "0514141945",
          "trace": "000038",
          "tranType": "A"
        },
        "clearingTransaction": {
          "acquirerReferenceNumber": "75110445229000000000001",
          "adviceReasonCode": "290",
          "brand": "MD",
          "localCurrencyCode": "840",
          "localRequestedAmount": "22.15",
          "merchantName": "1234771   ",
          "merchantType": "5411",
          "primaryAccountNumber": "5488888888887192",
          "processingCode": "000000",
          "responseCode": "00",
          "responseSource": "Single Message System",
          "settlementDate": "051719",
          "switchSerialNumber": "441055449",
          "switchDateTime": "0516145325",
          "trace": "999999",
          "tranType": "C"
        }
      }
    },
    {
      "authTransactionId": "b50oo1RrbahBE83Z4ISk6U1hAV1e8lENtCJKVCqk3goL9bTycP_XhR1oA9qn_Q45-Atz6z1Q7JPPo8vlrnE1yQ==",
      "clearingTransactionId": "uucmqifQnKxn3nAWvUdrsFAK3Fzta4COnX6IAfHG2ExOnkASKwhtibyYdcwwtCOyhyuBRSYx432oM6YiR-cWivrpwDxVnBewtqybxMBD3Ek=",
      "singleMessageSummaryDetails": {
        "authTransaction": {
          "acquirerReferenceNumber": "614659480514   000032  ",
          "adviceReasonCode": "290",
          "brand": "MD",
          "localCurrencyCode": "826",
          "localRequestedAmount": "22.15",
          "merchantName": "R-5311-USA",
          "merchantType": "5311",
          "primaryAccountNumber": "5488888888887192",
          "processingCode": "000000",
          "responseCode": "00",
          "responseSource": "Member",
          "settlementDate": "051519",
          "switchSerialNumber": "301863212",
          "switchDateTime": "0514141945",
          "trace": "000038",
          "tranType": "A"
        },
        "clearingTransaction": {
          "acquirerReferenceNumber": "75110445229000000000001",
          "adviceReasonCode": "290",
          "brand": "MD",
          "localCurrencyCode": "840",
          "localRequestedAmount": "22.15",
          "merchantName": "1234771   ",
          "merchantType": "5411",
          "primaryAccountNumber": "5488888888887192",
          "processingCode": "000000",
          "responseCode": "00",
          "responseSource": "Single Message System",
          "settlementDate": "051719",
          "switchSerialNumber": "441055449",
          "switchDateTime": "0516145325",
          "trace": "999999",
          "tranType": "C"
        }
      }
    }
  ]
}
```

---

### 48. Retrieve clearing details for mastercard debit or bridged debit

**Endpoint:** `GET /v6/{claim-id}/transactions/debitmc/detail`

**Operation ID:** `transactionDebitMCMessageDetail`

**Description:**

After an issuer creates a claim, the issuer uses this endpoint to retrieve details for the Mastercard Debit or Europe Dual Acquirer original transaction involved in the claim.

#### Parameters

| Name | Location | Type | Required | Description |
|------|----------|------|----------|-------------|
| `claim-id` | path | string | Yes | Claim Id.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |

#### Responses

| Status | Description | Schema |
|--------|-------------|--------|
| 200 | OK | [TransactionSingleMessageDetail](#transactionsinglemessagedetail) |
| 400 | Bad request | [Errors](#errors) |
| 401 | Unauthorized | [Errors](#errors) |
| 403 | Forbidden | [Errors](#errors) |
| 404 | Resource not found | [Errors](#errors) |
| 500 | Internal Server Error | [Errors](#errors) |

#### Sample Request/Response

**Request:**
```json
{
  "claim-id": "200002020654"
}
```
**Response:**
```json
{
  "authTransaction": {
    "acqLocCntry": "US",
    "acquirerAddress": "Maryland,MO",
    "acquirerAdjustmentSettlementAmount": "100",
    "acquirerAdjustmentSettlementAmountIndicator": "A",
    "acquirerAdjustmentSettlementCompletionAmount": "200",
    "acquirerAdjustmentSettlementCurrency": "840",
    "acquirerAdviceReason": "Close",
    "acquirerBridgedICA": "2565874",
    "acquirerCity": "Maryland",
    "acquirerCurrencyConversionRate": "5653",
    "acquirerInstitutionNumber": "123654",
    "acquirerInterchangeAmount": "200",
    "acquirerInterchangeCurrency": "840",
    "acquirerName": "AcqName",
    "acquirerProcessorID": "524431",
    "acquirerReferenceNumber": "05413364365000000000667",
    "acquirerSettlementCompletionAmount": "200",
    "acquirerSettlementCompletionCurrency": "840",
    "acquirerSettlementConversionRate": "200",
    "additionalAdvice": "DONE",
    "additionalPOSData": "12312312",
    "additionalResponse": "Closed",
    "adjustmentAdviceReason": "Closed",
    "adjustmentCashbackAmount": "200",
    "adjustmentDate": "051018",
    "adjustmentPurchaseAmount": "100",
    "afaMember": "A",
    "alternatePrimaryAccountNumber": "532423424322",
    "amountICCR": "234234",
    "atmPosFlag": "Y",
    "banknetReferenceNumber": "U68FRGDFA",
    "bridgingICA": "2234234",
    "businessActivity": "Approved",
    "cashBackAmount": "100",
    "cashBackCurrency": "840",
    "catLevel": "A",
    "cccaIssuerBankName": "TestBank",
    "chipFlag": "Y",
    "conditionCode": "UED",
    "corporateCardIndicator": "A",
    "creditLineUsageFee": "usagefee",
    "crossBorderIndicator": "A",
    "currencyConversionAssesementAmount": "100",
    "currencyConversionAssesementCurrency": "840",
    "currencyConversionIndicator": "U",
    "cvc2ProgramValidationCode": "234",
    "documentIndicator": "0",
    "feesInterChgAcqLoc": "Mo",
    "financialInstitutionID": "Test",
    "fraudDate": "032019",
    "fraudDeviceType": "A",
    "fraudType": "FT",
    "gcmsAdviceCode": "Close",
    "gcmsSettlementDate": "101019",
    "issuerAdjustmentSettlementAmount": "200",
    "issuerAdjustmentSettlementAmountIndicator": "A",
    "issuerAdjustmentSettlementCompletionAmount": "200",
    "issuerAdjustmentSettlementCurrency": "840",
    "issuerAdviceReason": "Pending",
    "issuerCurrencyConversionRate": "3111",
    "issuerICA": "123654",
    "issuerInstitutionNumber": "123123",
    "issuerInterchangeAmount": "100",
    "issuerInterchangeCurrency": "840",
    "issuerProcessorID": "12331",
    "issuerSettlementCompletionAmount": "100",
    "issuerSettlementCompletionCurrency": "840",
    "issuerSettlementConversionRate": "100",
    "localCompletionAmount": "2600",
    "localCurrencyCode": "840",
    "localRequestedAmount": "2500",
    "mcElectronicIndicator": "A",
    "mcResponseValue": "AP",
    "merchantCategoryCodeInfo": "4567",
    "merchantType": "1253",
    "originalCardHolderBillingAmount": "100",
    "originalCardHolderBillingCurrency": "840",
    "originalCashbackAmount": "300",
    "originalCashbackCurrency": "840",
    "originalPurchaseAmount": "200",
    "ownerID": "s060972",
    "pointOfServiceAmount": "150",
    "pointOfServiceCurrency": "840",
    "posData": "PosData",
    "posEntry": "354",
    "primaryAccountNumber": "510001000000134",
    "primaryAccountNumberSequenceNumber": "12121225663355",
    "processingCode": "125422",
    "productIdentifierCode": "PDI",
    "programIndicator": "A",
    "qpsPayPassChargebackElgibility": "A",
    "referenceNumber": "123123123",
    "responseCode": "12",
    "responseSource": "A",
    "serviceCode": "125",
    "serviceLevelIndicator": "120",
    "settlementDate": "051018",
    "settlementDatePosition": "102018",
    "settlementServiceConfiguration": "123",
    "surchargeFreeIndicator": "E",
    "switchDateTime": "101019",
    "switchSerialNumber": "123654",
    "switchSerialNumberPosition": "125444",
    "switchTime": "102019",
    "terminalID": "Test",
    "trace": "2545",
    "transactionCategoryCode": "P",
    "transactionClass": "transaction",
    "transactionDateTime": "101019",
    "transitData": "Data",
    "tranType": "AU",
    "universalCardAuthenticationFee": "A",
    "usageCode": "2"
  },
  "clearingTransaction": {
    "acqLocCntry": "US",
    "acquirerAddress": "Maryland,MO",
    "acquirerAdjustmentSettlementAmount": "100",
    "acquirerAdjustmentSettlementAmountIndicator": "A",
    "acquirerAdjustmentSettlementCompletionAmount": "200",
    "acquirerAdjustmentSettlementCurrency": "840",
    "acquirerAdviceReason": "Close",
    "acquirerBridgedICA": "2565874",
    "acquirerCity": "Maryland",
    "acquirerCurrencyConversionRate": "5653",
    "acquirerInstitutionNumber": "123654",
    "acquirerInterchangeAmount": "200",
    "acquirerInterchangeCurrency": "840",
    "acquirerName": "AcqName",
    "acquirerProcessorID": "524431",
    "acquirerReferenceNumber": "05413364365000000000667",
    "acquirerSettlementCompletionAmount": "200",
    "acquirerSettlementCompletionCurrency": "840",
    "acquirerSettlementConversionRate": "200",
    "additionalAdvice": "DONE",
    "additionalPOSData": "12312312",
    "additionalResponse": "Closed",
    "adjustmentAdviceReason": "Closed",
    "adjustmentCashbackAmount": "200",
    "adjustmentDate": "061018",
    "adjustmentPurchaseAmount": "100",
    "afaMember": "A",
    "alternatePrimaryAccountNumber": "532423424322",
    "amountICCR": "234234",
    "atmPosFlag": "Y",
    "banknetReferenceNumber": "U68FRGDFA",
    "bridgingICA": "2234234",
    "businessActivity": "Approved",
    "cashBackAmount": "100",
    "cashBackCurrency": "840",
    "catLevel": "A",
    "cccaIssuerBankName": "TestBank",
    "chipFlag": "Y",
    "conditionCode": "UED",
    "corporateCardIndicator": "A",
    "creditLineUsageFee": "usagefee",
    "crossBorderIndicator": "A",
    "currencyConversionAssesementAmount": "100",
    "currencyConversionAssesementCurrency": "840",
    "currencyConversionIndicator": "U",
    "cvc2ProgramValidationCode": "234",
    "documentIndicator": "0",
    "feesInterChgAcqLoc": "Mo",
    "financialInstitutionID": "Test",
    "fraudDate": "062019",
    "fraudDeviceType": "A",
    "fraudType": "FT",
    "gcmsAdviceCode": "Close",
    "gcmsSettlementDate": "101019",
    "issuerAdjustmentSettlementAmount": "200",
    "issuerAdjustmentSettlementAmountIndicator": "A",
    "issuerAdjustmentSettlementCompletionAmount": "200",
    "issuerAdjustmentSettlementCurrency": "840",
    "issuerAdviceReason": "Pending",
    "issuerCurrencyConversionRate": "3111",
    "issuerICA": "123654",
    "issuerInstitutionNumber": "123123",
    "issuerInterchangeAmount": "100",
    "issuerInterchangeCurrency": "840",
    "issuerProcessorID": "12331",
    "issuerSettlementCompletionAmount": "100",
    "issuerSettlementCompletionCurrency": "840",
    "issuerSettlementConversionRate": "100",
    "localCompletionAmount": "2600",
    "localCurrencyCode": "840",
    "localRequestedAmount": "2500",
    "mcElectronicIndicator": "A",
    "mcResponseValue": "AP",
    "merchantCategoryCodeInfo": "4567",
    "merchantType": "1253",
    "originalCardHolderBillingAmount": "100",
    "originalCardHolderBillingCurrency": "840",
    "originalCashbackAmount": "300",
    "originalCashbackCurrency": "840",
    "originalPurchaseAmount": "200",
    "ownerID": "s060972",
    "pointOfServiceAmount": "150",
    "pointOfServiceCurrency": "840",
    "posData": "PosData",
    "posEntry": "354",
    "primaryAccountNumber": "510001000000134",
    "primaryAccountNumberSequenceNumber": "12121225663355",
    "processingCode": "125422",
    "productIdentifierCode": "PDI",
    "programIndicator": "A",
    "qpsPayPassChargebackElgibility": "A",
    "referenceNumber": "123123123",
    "responseCode": "12",
    "responseSource": "A",
    "serviceCode": "125",
    "serviceLevelIndicator": "120",
    "settlementDate": "061018",
    "settlementDatePosition": "102018",
    "settlementServiceConfiguration": "123",
    "surchargeFreeIndicator": "E",
    "switchDateTime": "111019",
    "switchSerialNumber": "123654",
    "switchSerialNumberPosition": "125444",
    "switchTime": "102019",
    "terminalID": "Test",
    "trace": "2545",
    "transactionCategoryCode": "P",
    "transactionClass": "transaction",
    "transactionDateTime": "111019",
    "transitData": "Data",
    "tranType": "AU",
    "universalCardAuthenticationFee": "A",
    "usageCode": "2"
  }
}
```

---

## Data Models (Schemas)

### ErrorDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `Name` | string | No | Type of information provided by the element |
| `Value` | string | No | The value of the element |

### Error

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `RequestId` | string | No | Request id for the error Example: `f43f5b78-bc99-1f2a-8742-972238eac271` |
| `Source` | string | No | Source for the error Example: `SYSTEM` |
| `ReasonCode` | string | No | Reason code for error Example: `INVALID_REQUEST` |
| `Description` | string | No | Brief description of error Example: `Invalid request.` |
| `Recoverable` | boolean | No | Indicates whether the client can make changes to resolve this issue Example: `false` |
| `Details` | array of [ErrorDetail](#errordetail) | No | Detail structure containing error detail code |

### Errors

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `Errors` | array of [Error](#error) | No | List of Errors returned to service |

### RetrievalDocNeededNameValueDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | string | No | The name of the element Example: `2` |
| `value` | string | No | The value of the element Example: `2 - Copy or image (photocopy, microfilm, fax) of original document` |

### RetrievalReasonCodesNameValueDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | string | No | The name of the element Example: `6305` |
| `value` | string | No | The value of the element Example: `6305 - Cardholder does not agree with amount billed` |

### CurrenciesNameValueDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | string | No | The name of the element Example: `USD` |
| `value` | string | No | The value of the element Example: `USD` |

### ChargebackDocIndicatorsNameValueDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | string | No | The name of the element Example: `1` |
| `value` | string | No | The value of the element Example: `1 - Supporting documentation will follow` |

### ChargebackMessageTextsNameValueDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | string | No | The name of the element Example: `CARD NOT VALID OR EXPIRED` |
| `value` | string | No | The value of the element Example: `CARD NOT VALID OR EXPIRED` |

### ChargebackReasonCodesNameValueDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | string | No | The name of the element Example: `4831` |
| `value` | string | No | The value of the element Example: `4831 - Transaction Amount Differs` |

### ChargebackAmountNameValueDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | string | No | The name of the element Example: `USD` |
| `value` | string | No | The value of the element  Note: In some edge cases, when the amount value is very high, the API may return amount value with scientific E notation. <br>       In these cases, the actual amount value can be translated as below: <br>       For example: 9.7978499566E8 = 9.7978499566 * 10^8 = 979784995.66 <br><br> Example: `100.00` |

### FraudAcctDeviceTypesNameValueDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | string | No | The name of the element Example: `1` |
| `value` | string | No | The value of the element Example: `1 - Chip with PIN` |

### FraudAcctStatusesNameValueDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | string | No | The name of the element Example: `N` |
| `value` | string | No | The value of the element Example: `N - Account has not been closed` |

### FraudCardValidCodesNameValueDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | string | No | The name of the element Example: `M` |
| `value` | string | No | The value of the element Example: `M - CVC 2 Valid` |

### FraudSubTypesNameValueDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | string | No | The name of the element Example: `N` |
| `value` | string | No | The value of the element Example: `N - PIN not used` |

### FeeReasonCodesNameValueDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | string | No | The name of the element Example: `7604` |
| `value` | string | No | The value of the element Example: `7604 - Emergency card replacement fee` |

### FeeCountryCodesNameValueDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | string | No | The name of the element Example: `UNITED STATES` |
| `value` | string | No | The value of the element Example: `UNITED STATES` |

### FeeMessageTextsNameValueDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | string | No | The name of the element Example: `LOST/STOLEN CARD TRANSACTION FEE` |
| `value` | string | No | The value of the element Example: `LOST/STOLEN CARD TRANSACTION FEE` |

### Queue

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `queueName` | string | No | Queue Name.  This is used as input in other apis Example: `Closed` |
| `queueDescription` | string | No | This describes the contents of the queue Example: `Closed Disputes` |

### ClaimSummary

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `acquirerId` | string | No | Acquirer Inst Id Example: `002222` |
| `acquirerRefNum` | string | No | Acquirer Reference Number Example: `05103246259000000000126` |
| `primaryAccountNum` | string | No | Card Number for which the Claim is opened Example: `5123432112344321` |
| `claimId` | string | No | Claim Id Example: `200002020654` |
| `claimType` | string | No | Claim Type Example: `Standard` |
| `claimValue` | string | No | The value of the claim Example: `25.00 USD` |
| `clearingDueDate` | string | No | The clearing due date of the claim Example: `2021-02-20` |
| `clearingNetwork` | string | No | Clearing Network Example: `GCMS` |
| `createDate` | string | No | This is the date of the Claim creation Example: `2021-02-10` |
| `dueDate` | string | No | The due date of the claim Example: `2021-02-20` |
| `transactionId` | string | No | A 9 digit numeric identifier used by mastercom internal processes and it is not equivalent to clearing or authorization transaction id. Example: `123456789` |
| `isAccurate` | boolean | No | True if the claim value is accurate Example: `true` |
| `isAcquirer` | boolean | No | True if the claim is acquirer Example: `true` |
| `isIssuer` | boolean | No | True if the claim is issuer Example: `false` |
| `isOpen` | boolean | No | True if the claim is open Example: `true` |
| `issuerId` | string | No | The issuer institution identifier Example: `00000006195` |
| `lastModifiedBy` | string | No | User who signed this event Example: `system` |
| `lastModifiedDate` | string | No | The date of the last claim modification Example: `2021-02-15T13:01:30` |
| `merchantId` | string | No | Returns the related merchant identifier Example: `0024038000200` |
| `progressState` | string | No | The progress state of the claim Example: `CB1-4807-O-A-NEW` |
| `queueName` | string | No | The queue name to which the claim has been allocated Example: `Rejects` |
| `creditVoucherStatus` | string | No | The actual status of the credit voucher Example: `Credit Voucher Accepted` |
| `collaborationExpirationDateTime` | string | No | Date and time by which the acquirer can respond to a Collaboration request. Example: `2023-05-11T19:22:41` |

### ClaimDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `acquirerId` | string | No | Acquirer Inst Id Example: `002222` |
| `acquirerRefNum` | string | No | Acquirer Reference Number Example: `05131054165000000048149` |
| `primaryAccountNum` | string | No | Card Number for which the Claim is opened Example: `5123432112344321` |
| `claimId` | string | No | Claim Id Example: `200002020654` |
| `claimType` | string | No | Claim Type Example: `Standard` |
| `claimValue` | string | No | The value of the claim Example: `25.00 USD` |
| `standardClaims` | string | No | Contain all Standard Claim Ids associated with the claimType of CaseFiling.  This field will contain a comma delimited list. Example: `200002020654, 200002020654` |
| `clearingDueDate` | string | No | The clearing due date of the claim Example: `2021-02-20` |
| `clearingNetwork` | string | No | Clearing Network Example: `GCMS` |
| `createDate` | string | No | This is the date of the Claim creation Example: `2021-02-10` |
| `dueDate` | string | No | The due date of the claim Example: `2021-02-20` |
| `transactionId` | string | No | An alphanumeric identifier that ties the clearingTransactionId and authTransactionId to the Claim. The format is TI:<ClearingSummary.transactionId>#<AuthorizationSummary.transactionId> Example: `TI:FIEaEgnM3bwPijwZgjc3Te+Y0ieLbN9ijUugqNSvJmVbO1xs6Jh5iIlmpOpkbax79L8Yj1rBOWBAC` |
| `isAccurate` | string | No | True if the claim value is accurate Example: `true` |
| `isAcquirer` | string | No | True if the claim is acquirer Example: `true` |
| `isIssuer` | string | No | True if the claim is issuer Example: `false` |
| `isOpen` | string | No | True if the claim is open Example: `true` |
| `issuerId` | string | No | The issuer institution identifier Example: `00000006195` |
| `lastModifiedBy` | string | No | User who signed this event Example: `system` |
| `lastModifiedDate` | string | No | The date of the last claim modification Example: `2021-02-15` |
| `merchantId` | string | No | Returns the related merchant identifier Example: `0024038000200` |
| `queueName` | string | No | The queue name to which the claim has been allocated Example: `Pending` |
| `switchSerialNumber` | string | No | The Switch Serial Number is a unique transaction identification number generated (or assigned) by the Single Message System Example: `140859012` |
| `auditControlNumber` | string | No | Identifier assigned by Mastercom to the fraud reporting event reported through Mastercom. The auditControNumber is used as a reference in the FLD request API to subsequently modify, delete or convert a suspended to a confirmed fraud record and is echoed back. Example: `123111111000025` |
| `caseFilingDetails` | [CaseFilingLifeCycle](#casefilinglifecycle) | No |  |
| `retrievalDetails` | [RetrievalSummary](#retrievalsummary) | No |  |
| `chargebackDetails` | array of [ChargebackDetails](#chargebackdetails) | No |  |
| `feeDetails` | array of [FeeDetails](#feedetails) | No |  |
| `retrievalDetailsList` | array of [RetrievalSummary](#retrievalsummary) | No |  |

### ClaimResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `claimId` | string | No | Identifier assigned to the Claim Example: `200002020654` |

### ChargebackResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `chargebackId` | string | No | Identifier assigned to the Chargeback Example: `300018439680` |

### LoadDataForRetrievalResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `docNeeded` | array of [RetrievalDocNeededNameValueDetail](#retrievaldocneedednamevaluedetail) | No | List of valid docNeeded fields |
| `reasonCodes` | array of [RetrievalReasonCodesNameValueDetail](#retrievalreasoncodesnamevaluedetail) | No | List of valid reason codes |

### LoadDataForChargebacksRequest

**Type:** object

**Required fields:** `chargebackType`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `chargebackType` | string enum: [CHARGEBACK, SECOND_PRESENTMENT] | Yes | The type of chargeback.The default value is CHARGEBACK   Length: 10-18   Valid Values/Format: CHARGEBACK, SECOND_PRESENTMENT Example: `CHARGEBACK` |
| `reasonCode` | string | No | Reason Code    Length: 4   Valid Values/Format: Numeric Example: `4853` |
| `currency` | string | No | The currency in with the chargeback will be created.   Length: 3   Valid Values/Format: A-Z (uppercase alphabetic letter) Example: `USD` |

### LoadDataForChargebackResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `currencies` | array of [CurrenciesNameValueDetail](#currenciesnamevaluedetail) | No | List of valid currencies |
| `docIndicators` | array of [ChargebackDocIndicatorsNameValueDetail](#chargebackdocindicatorsnamevaluedetail) | No | List of valid doc indicators |
| `messageTexts` | array of [ChargebackMessageTextsNameValueDetail](#chargebackmessagetextsnamevaluedetail) | No | List of valid message texts |
| `reasonCodes` | array of [ChargebackReasonCodesNameValueDetail](#chargebackreasoncodesnamevaluedetail) | No | List of valid reason codes |
| `amount` | [ChargebackAmountNameValueDetail](#chargebackamountnamevaluedetail) | No |  |

### LoadDataForFraudResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `acctDeviceTypes` | array of [FraudAcctDeviceTypesNameValueDetail](#fraudacctdevicetypesnamevaluedetail) | No | List of valid account device types |
| `acctStatuses` | array of [FraudAcctStatusesNameValueDetail](#fraudacctstatusesnamevaluedetail) | No | List of valid account statuses |
| `cardValidCodes` | array of [FraudCardValidCodesNameValueDetail](#fraudcardvalidcodesnamevaluedetail) | No | List of valid card valid codes |
| `subTypes` | array of [FraudSubTypesNameValueDetail](#fraudsubtypesnamevaluedetail) | No | List of valid sub types |

### LoadDataForFeesRequest

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `reasonCode` | string | No | Reason Code.   Length: 1-4   Valid Values/Format: Numeric Example: `4853` |

### LoadDataForFeeResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `currencies` | array of [CurrenciesNameValueDetail](#currenciesnamevaluedetail) | No | List of valid currencies |
| `reasonCodes` | array of [FeeReasonCodesNameValueDetail](#feereasoncodesnamevaluedetail) | No | List of valid reason codes |
| `countryCodes` | array of [FeeCountryCodesNameValueDetail](#feecountrycodesnamevaluedetail) | No | List of valid country codes |
| `messageTexts` | array of [FeeMessageTextsNameValueDetail](#feemessagetextsnamevaluedetail) | No | List of valid message texts |

### RetrievalResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `requestId` | string | No | Identifier assigned to the fulfillment. Example: `300002296235` |

### CreateRetrievalResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `requestId` | string | No | Identifier assigned to the retrieval request. Example: `300002296235` |

### AcquirerFulfillmentResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `requestId` | string | No | Same identifier assigned as to the retrieval request. A fulfillment request id will be generated in the background and can be retrieved on a retrieve claim API call. Example: `300002296235` |

### FeeSingleResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `status` | string | No | Status of fee creation. Example: `Success` |

### FeeResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `feeId` | string | No | Identifier assigned to the fee Example: `300018439680` |

### FraudResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `fraudId` | string | No | Identifier assigned to the fraud item Example: `300018014812` |

### RetrievalSummary

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `acquirerRefNum` | string | No | Acquirer Reference Number is a unique number that tags a credit card transaction when it goes from the merchants bank (The Acquiring Bank) through the card scheme to the cardholders bank (The Issuer) Example: `05131054165000000048149` |
| `acquirerResponseCd` | string | No | Predetermined response code chosen by the Acquirer Example: `A` |
| `acquirerMemo` | string | No | The memo the acquirer created when fulfilling the retrieval request Example: `This is a test memo` |
| `acquirerResponseDt` | string | No | Date the acquirer responded to the fulfillment request Example: `2021-02-11` |
| `amount` | string | No | Amount of the claim Example: `100.00` |
| `currency` | string | No | The retrieval currency.  The data should be standard currency alpha code Max: 3 Example: `USD` |
| `claimId` | string | No | Claim identifier Example: `200002020654` |
| `createDate` | string | No | This is the date of the Retrieval Request creation Example: `2021-02-11` |
| `cancelDate` | string | No | This is the date of the Retrieval Request cancelation Example: `2021-02-11` |
| `reverseDate` | string | No | This is the date of the Retrieval Request reversion Example: `2021-02-11` |
| `rejectDate` | string | No | This is the date of the Retrieval Request rejection by GCMS Example: `2021-02-11` |
| `docNeeded` | string | No | Documentation Needed Indicator. Possible values are    - NONE: Present when acquirer has not yet responded to a Collaboration request or present for issuers when the acquirer has responded with response code A (funds movement request)    - Refund: Present when acquirer has responded to a Collaboration request with response code B (Refunded) and provided refund details or when acquirer has responded with response code C (Initiating refund)    - Credit Voucher: Present when acquirer has responded to a Collaboration request with response code B (Refunded) and provided a credit voucher    - null: Present when no other values are applicable    **Note:** The following apply only to U.S. healthcare transactions.    - 2 (Copy or image such as a photocopy, microfilm, fax of original document)    - 4 (Substitute draft) Example: `NONE` |
| `issuerResponseCd` | string | No | Predetermined response code chosen by the Issuer Example: `APPROVE` |
| `issuerRejectRsnCd` | string | No | Predetermined reject reason codes Example: `A` |
| `issuerMemo` | string | No | Memo pertaining to the case Example: `This is a test memo` |
| `issuerResponseDt` | string | No | Date the issuer responded to the fulfillment Example: `2021-02-11` |
| `imageReviewDecision` | string | No | The image review decision Example: `A` |
| `imageReviewDt` | string | No | Date the image review decision occurred Example: `2021-02-11` |
| `primaryAcctNum` | string | No | Primary Account Number Example: `5488888888887192` |
| `rejectReason` | string | No | Retrieval Request reject reason. Example: `Code1=0142(00):D0063/002;DE072=D0063\\8000000808\\\\` |
| `requestId` | string | No | Identifier assigned to the fulfillment Example: `25859113` |
| `retrievalRequestReason` | string | No | Retrieval Request Reason Example: `6343` |
| `chargebackRefNum` | string | No | Contains card issuer reference data for a specific cardholder transaction. This number must be unique within BIN. It is used to track the chargeback throughout its life cycle Example: `9000000006` |
| `acquirerResponseNotificationStatus` | string | No | The field would show Processed or Rejected or Pending or Cancelled status depending on the GCMS processing of the retrieval request. Pending- Item created but NOT yet sent to GCMS Cancelled- Item discarded without being sent to GCMS Processed- Item is being sent for clearing (via ipmClearingOutput batch job) Rejected- If item being rejected from GCMS (No update on Issuer side claim). Example: `PROCESSED` |
| `instructionsForHealthcare` | string | No | Instructions for healthcare Example: `Instructions for Healthcare` |
| `refundReversalDate` | string | No | Refund/Reversal Date Example: `2021-02-11` |
| `refundReversalAmount` | string | No | Refund/Reversal Amount Example: `100.00` |
| `refundReversalCurrency` | string | No | Refund/Reversal Currency Example: `USD` |
| `refundReversalType` | string | No | Refund/Reversal Type Example: `REFUND` |
| `refundReversalReferenceId` | string | No | Refund/Reversal reference ID Example: `123458111` |
| `memo` | string | No | Memo Example: `This is a test memo` |
| `flexCode` | string | No | Specific to Brazil Flex Card transactions to communicate the product code used for clearing. Example: `003` |
| `collaborationExpirationDateTime` | string | No | Date and time by which the acquirer can respond to a Collaboration request. Example: `2023-05-11T19:22:41` |

### ClearingSummary

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `primaryAccountNumber` | string | No | Primary Account Number Example: `5488888888887192` |
| `transactionAmountLocal` | string | No | Transaction amount in local currency Example: `2500` |
| `dateAndTimeLocal` | string | No | Local date and time of transaction.The seconds in time will always be set to 00 Example: `210127160100` |
| `cardDataInputCapability` | string | No | Indicates the conditions that exist at the point of service at the time of the transaction Example: `5` |
| `cardholderAuthenticationCapability` | string | No | Describes the capability of the terminal device to support/accept authentication data Example: `9` |
| `cardPresent` | string | No | Indicates if the card was present or not Example: `1` |
| `acquirerReferenceNumber` | string | No | Acquirer reference number Example: `05413364365000000000667` |
| `cardAcceptorName` | string | No | Name the card acceptor that defines the point of interaction in both local and interchange environments (excluding ATM and Card-Activated Public Phones) Example: `Amazon` |
| `currencyCode` | string | No | Currency code the issuer will be charging the cardholder for repayment Example: `840` |
| `installmentPaymentDataBrazil` | string | No | The field will contain installment payment data for Brazilian intracountry transactions Example: `4070000000479500302000000015983000000000000000000000000` |
| `transactionId` | string | No | The host's identifier Example: `U7dImb1ncs24LKNU9dDpl+FHlPzyfYOOvS5PijTlO6wHH09l7aiVJ1KJHp3sWPUHH0l90J1U82DGrE3h` |
| `settlementIndicator` | string | No | Field to identify GCO and NON-GCO transactions Example: `C` |
| `messageReversalIndicator` | string | No | Identifies a message as a reversal of a previous message Example: `R` |

### AuthorizationSummary

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `originalMessageTypeIdentifier` | string | No | Indicates the Message Type Identifier (MTI) of the original message Example: `0110` |
| `banknetDate` | string | No | The date/time when the SAFE record is matched to the Authorization transaction Example: `210127` |
| `transactionAmountUsd` | string | No | Transaction amount in USD Example: `401.17` |
| `primaryAccountNumber` | string | No | Primary account number Example: `5488888888887192` |
| `processingCode` | string | No | A series of digits used to describe the effect of a transaction on the customer account and the type of accounts affected Example: `00` |
| `transactionAmountLocal` | string | No | Transaction amount in local currency Example: `000000010000` |
| `authorizationDateAndTime` | string | No | The date and time that a message is entered into the Mastercard Network Example: `0127125633` |
| `authenticationId` | string | No | Defined by the Authorization Platform and is passed to the issuer to indicate that the transaction qualified for Authentication service Example: `418443` |
| `cardAcceptorName` | string | No | Name the card acceptor that defines the point of interaction in both local and interchange environments (excluding ATM and Card-Activated Public Phones) Example: `Amazon` |
| `cardAcceptorCity` | string | No | City of the card acceptor that defines the point of interaction in both local and interchange environments (excluding ATM and Card-Activated Public Phones) Example: `SAINT LOUIS` |
| `cardAcceptorState` | string | No | State of the card acceptor that defines the point of interaction in both local and interchange environments (excluding ATM and Card-Activated Public Phones) Example: `MO` |
| `currencyCode` | string | No | Currency code the issuer will be charging the cardholder for repayment Example: `840` |
| `chipPresent` | string | No | Indicates if chip was present or not Example: `N` |
| `transactionId` | string | No | The host's identifier Example: `hqCnaMDqmto4wnL+BSUKSdzROqGJ7YELoKhEvluycwKNg3XTzSfaIJhFDkl9hW081B5tTqFFiAwCpcoc` |
| `track1` | string | No | The information encoded on track 1 of the card's magnetic stripe as defined in the ISO 7813 specification, including data element separators but excluding beginning and ending sentinels and LRC characters as defined in this data element definition Example: `N` |
| `track2` | string | No | The information encoded on track 2 of the card magnetic stripe as defined in the ISO 7813 specification, including data element separators but excluding beginning and ending sentinels and longitudinal redundancy check (LRC) characters as defined therein Example: `Y101` |
| `clearingSummary` | array of [ClearingSummary](#clearingsummary) | No |  |

### QueueContentSummary

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `pageCount` | string | No | The number of pages queue results can be returned Example: `1` |
| `claimList` | array of [ClaimSummary](#claimsummary) | No |  |

### TransactionSummary

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `authorizationSummaryCount` | string | No | The number of records returned in the response Example: `1` |
| `message` | string | No | Provides the message receiver with the reason for sending the message Example: `Search returned 1 records` |
| `authorizationSummary` | array of [AuthorizationSummary](#authorizationsummary) | No |  |

### MexicoDomesticTaxAmount

The Value Added Tax (VAT) associated with a Mexico domestic transaction.

**Type:** string

### MexicoDomesticTransactionFeeAmount

The fee amounts associated with a Mexico domestic transaction.

**Type:** string

### MexicoDomesticSettlementFeesAndVat

Identifies the reconciliation and settlement information for mexicoDomesticTaxAmount and mexicoDomesticTransactionFeeAmount in each Mexico Domestic Dual Message Clearing System transaction with settlement impact.

**Type:** string

### ClearingDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `accountLevelManagementAccountCategoryCode` | string | No | Describes the category code of Account Level Management assigned Example: `N` |
| `acquirerReferenceData` | string | No | Data an acquirer supplies in an acquirer-originated message that may be required for an issuer to return to the acquirer in a subsequent message Example: `25131304365000000033393` |
| `acquiringInstitutionIdCode` | string | No | Identifies the acquiring institution (for example, merchant bank) or its agent. Example: `999663` |
| `approvalCode` | string | No | A code the authorizing institution assigns indicating approval Example: `97574B` |
| `businessCycle` | string | No | A two-digit numeric subfield (such as 01, 02, or 03) that identifies a reconciliation period in a Business Date Example: `01` |
| `businessServiceArrangementTypeCode` | string | No | A one position numeric value that identifies the business relationship between the transaction's participants Example: `2` |
| `businessServiceIdCode` | string | No | A six-position value that identifies the business service to which the transaction is assigned for reconciliation and rules basis Example: `10001` |
| `cardAcceptorBusinessCode` | string | No | Classifies the type of business applicable to the card acceptor Example: `5411` |
| `cardAcceptorCity` | string | No | Contains the card acceptor city of the merchant or, if a payment facilitator is involved in the transaction, the sub-merchant Example: `SAINT LOUIS` |
| `cardAcceptorClassificationOverrideIndicator` | string | No | Indicate whether Card Acceptor Business Code (MCC) override rates, Card Acceptor Business (CAB) Type override rates, or neither, were used in the interchange fee amount Example: `N` |
| `cardAcceptorCountry` | string | No | Contains the card acceptor county of the merchant or, if a payment facilitator is involved in the transaction, the sub-merchant Example: `USA` |
| `cardAcceptorIdCode` | string | No | Identifies the card acceptor ID assigned by the acquirer Example: `375555569895` |
| `cardAcceptorName` | string | No | Contains the card acceptor's name Example: `Amazon` |
| `cardAcceptorPostalCode` | string | No | Contains the card acceptor's postal code Example: `63102` |
| `cardAcceptorState` | string | No | Contains the card acceptor's state Example: `MO` |
| `cardAcceptorStreetAddress` | string | No | Contains the card acceptor's street address Example: `Gateway Arch Trail` |
| `cardAcceptorTerminalId` | string | No | A unique code identifying a terminal at the card acceptor location Example: `73429189` |
| `cardAcceptorUrl` | string | No | Contains the card website URL address Example: `www.amazon.com` |
| `cardCaptureCapability` | string | No | Indicates whether the terminal has card capture capabilities Example: `9` |
| `cardDataInputCapability` | string | No | Indicates the terminal capabilities for transferring the data on the card into the terminal Example: `5` |
| `cardDataInputMode` | string | No | Indicates how the card data was entered at the terminal Example: `R` |
| `cardDataOutputCapability` | string | No | Indicates the ability of the terminal to write or output data to a card Example: `0` |
| `cardholderAuthenticationCapability` | string | No | Describes the capability of the terminal device to support/accept authentication data Example: `9` |
| `cardholderAuthenticationEntity` | string | No | Indicates the entity through which the cardholder's identity was verified at the point of service Example: `9` |
| `cardholderAuthenticationMethod` | string | No | Indicates the method by which the cardholder's identity was verified at the point of service Example: `9` |
| `mexicoDomesticTaxAmount` | [MexicoDomesticTaxAmount](#mexicodomestictaxamount) | No |  |
| `mexicoDomesticTransactionFeeAmount` | [MexicoDomesticTransactionFeeAmount](#mexicodomestictransactionfeeamount) | No |  |
| `mexicoDomesticSettlementFeesAndVat` | [MexicoDomesticSettlementFeesAndVat](#mexicodomesticsettlementfeesandvat) | No |  |
| `cardholderBillingAmount` | string | No | The transaction amount in the issuers currency Example: `2500` |
| `cardholderBillingCurrencyCode` | string | No | Identifies the currency code of the cardholder billing amount Example: `840` |
| `cardholderFromAccountCode` | string | No | Describes the cardholder from account type Example: `00` |
| `cardholderPresentData` | string | No | Indicates whether the cardholder is present at the point of service and explains the condition if the cardholder is not present Example: `0` |
| `cardholderToAccountCode` | string | No | Describes the cardholder to account type Example: `00` |
| `cardIssuerReferenceData` | string | No | Issuer provided when processing retrieval or chargeback messages Example: `9000000959` |
| `cardPresentData` | string | No | Indicates if the card is present at the point of service Example: `1` |
| `cardProgramIdentifier` | string | No | A three-character code that identifies the card program or financial network to which a transaction belongs Example: `MCC` |
| `centralSiteBusinessDate` | string | No | Identifies the official business processing date when the business service reconciles a transaction Example: `210127` |
| `centralSiteProcessingDateOriginalMessage` | string | No | Central site processing date of original message Example: `210127` |
| `currencyCodeCardholderBilling` | string | No | Defines the cardholder billing amount currency Example: `840` |
| `currencyCodeReconciliation` | string | No | Defines the reconciliation amount currency Example: `840` |
| `currencyCodeTransaction` | string | No | Defines the local transaction amount currency Example: `840` |
| `currencyExponentCardholderBilling` | string | No | Explicitly identifies the implicit decimal point locations associated with cardholder billing amount Example: `2` |
| `currencyExponentReconciliation` | string | No | Explicitly identifies the implicit decimal point locations associated with reconciliation amount Example: `2` |
| `currencyExponentTransaction` | string | No | Explicitly identifies the implicit decimal point locations associated with transaction amount Example: `2` |
| `dataRecord` | string | No | Contains message text data, file update data, or other information as specified in individual IPM messages Example: `1` |
| `electronicCommerceCardAuth` | string | No | Indicates the type of Cardholder Authentication used in the authorization process Example: `0` |
| `electronicCommerceSecurityLevelIndicator` | string | No | Indicates the presence and type of security protocol present in the authorization process Example: `0` |
| `electronicCommerceUcafCollectionIndicator` | string | No | Identifies the level of UCAF™ supported in the authorization process Example: `2` |
| `forwardingInstitutionIdCode` | string | No | Identifies a message's forwarding institution Example: `5258` |
| `installmentAmount` | string | No | Contains total installment amount Example: `50.00` |
| `installmentFee` | string | No | Installment fee contains any fee paid to or by the issuer in an installment transaction. Numeric value where the last 2 are decimal Example: `1200` |
| `installmentNumber` | string | No | Indicates which installment number is being submitted Example: `3` |
| `installmentPaymentDataBrazil` | string | No | The field will contain installment payment data for Brazilian intracountry transactions Example: `4070000000479500302000000015983000000000000000000000000` |
| `additionalAmountDataCode` | string | No | The field will contain the Additional Amount Data Example: `0040710D100003466878;2929985C876543456687;1929985D876543456687` |
| `installmentParameters` | [InstallmentParameters](#installmentparameters) | No |  |
| `originalInformationInstallments` | [OriginalInformationInstallments](#originalinformationinstallments) | No |  |
| `installmentPlanType` | string | No | Indicates the specific installment plan types ... 21,22,23 Example: `21` |
| `functionCode` | string | No | Indicates a message's specific purpose Example: `200` |
| `gcmsProductIndentifier` | string | No | Identifies the product recognized by GCMS for the combination of issuer account range and Card Program Identifier Example: `MPL` |
| `installmentPaymentData` | string | No | Contains the type of financing applicable for the installment Example: `20` |
| `installmentPaymentDataAnnualPercentageRate` | string | No | Contains the annual percentage rate (XXX.XX%) an issuer will charge the cardholder for the installment payments. For installment payments, the default is all spaces Example: `0` |
| `installmentPaymentDataFirstInstallmentAmount` | string | No | Identifies the amount of the first installment payment Example: `24` |
| `installmentPaymentDataInstallmentFee` | string | No | Contains the fee amount an issuer will charge the cardholder for the installment payments in cardholder billing currency. The default is all spaces. Example: `0` |
| `installmentPaymentDataInterestRate` | string | No | Identifies the interest rate of installment payments Example: `23` |
| `installmentPaymentDataNumberInstallments` | string | No | Identifies the number of installment payments Example: `2` |
| `installmentPaymentDataSubsequentInstallmentAmount` | string | No | Contains the fee amount an issuer will charge the cardholder for the installment payments in cardholder billing currency. The default is all spaces. Example: `20` |
| `integratedCircuitCardRelatedData` | string | No | Contains data related to ICC systems Example: `100` |
| `interchangeRateDesignator` | string | No | Indicates the interchange rate and editing rules applied to the transaction Example: `79` |
| `licensedProductIndentifier` | string | No | Identifies the actual product code assigned by Mastercard when licensing the combination of issuer account range and Card Program Identifier Example: `MPL` |
| `legalCorporateName` | string | No | Provides the card acceptor's legal corporation name Example: `Amazon` |
| `localMessageReasonCode` | string | No | Indicate a particular chargeback domestic reason code(only for Chargebacks) Example: `3 positions = 130 [First Chargeback]` |
| `localTax1IVA` | string | No | Contains the VAT amount for the installment fee. Numeric value where the last 2 are decimal. Example: `006` |
| `localTransactionDateTime` | string | No | The local year, month, day, and time at which the transaction takes place at the card acceptor location.The seconds in time will always be set to 00 Example: `210127010100` |
| `mastercardAssignedId` | string | No | Identifier assigned by Mastercard Example: `PDS176` |
| `mastercardAssignedIdOverrideIndicator` | string | No | Indicates whether override rates were used in the interchange fee amount Example: `N` |
| `mastercardMappingServiceAccountNumber` | string | No | Provides either the virtual account number/token or the primary account number in a transaction for which Mastercard performed the mapping service Example: `5154676300000001` |
| `masterPassIncentiveIndicator` | string | No | Populated by GCMS and identifies the transaction as having received the Masterpass-Enabled Merchant Incentive Example: `N` |
| `messageReasonCode` | string | No | Contains the initial transaction's Message Reason Code Example: `1401` |
| `messageReversalIndicator` | string | No | Identifies a message as a reversal of a previous message Example: `R` |
| `numberOfInstallments` | string | No | Total number of installments to be submitted Example: `15` |
| `originatingMessageFormat` | string | No | Provides the message format in which the clearing system received a message Example: `2` |
| `partnerIdCode` | string | No | Identifies a specific partnership agreement, generally between specific card acceptors and issuers. Example: `PDS190` |
| `pinCaptureCapability` | string | No | Indicates the maximum number of PIN characters that the POS terminal can capture Example: `1` |
| `primaryAccountNumber` | string | No | Account number associated with transaction in question Example: `5154676300000001` |
| `processingCode` | string | No | Identifies the entity to be debited and the entity to be credited for the fee amount Example: `00` |
| `productOverrideIndicator` | string | No | Product Class value to indicate whether product class override interchange rates were used in the interchange fee amount Example: `Y` |
| `programRegistrationId` | string | No | Monitors and tracks a participant's activity in special promotion programs (primarily U.S.-defined). Example: `C57` |
| `qpsPaypassEligibilityIndicator` | string | No | Indicates whether or not the Quick Payment Service (QPS) or PayPass transaction qualifies for chargeback protection Example: `I` |
| `rateIndicator` | string | No | Indicates the rate applied by GCMS to the transaction Example: `N` |
| `receivingInstitutionIdCode` | string | No | Identifies the receiving institution Example: `2202` |
| `reconciliationAmount` | string | No | The transaction amount value converted to the customer's reconciliation (that is, the customer's payment or settlement) currency Example: `2500` |
| `reconciliationCurrencyCode` | string | No | Identifies the currency code of the reconciliation amount Example: `840` |
| `remotePaymentsProgramData` | string | No | Identifies the domain in which the Service Manager (program originator) of the Mobile Remote Payments Program is operating Example: `1` |
| `serviceCode` | string | No | Provides codes that increase issuers' flexibility in defining card acceptance parameters. Example: `200` |
| `settlementData` | string | No | Provides reconciliation and settlement information for each clearing system transaction that has settlement impact Example: `1` |
| `settlementIndicator` | string | No | Indicates the settlement impact of amounts in an IPM message Example: `M` |
| `specialConditionsIndicator` | string | No | Indicates whether an adjustment was included in the Fee amount Example: `N` |
| `terminalDataOutputCapability` | string | No | Indicates the print and display capabilities of the terminal Example: `0` |
| `terminalOperatingEnvironment` | string | No | Indicates whether the card acceptor is attending the terminal and the location of the terminal Example: `2` |
| `terminalType` | string | No | Identifies the type of terminal used at the point of interaction Example: `CT6` |
| `tokenRequestorId` | string | No | Contains the ID assigned by the token service provider to the token requestor Example: `4GFCYTH29Z` |
| `totalTransactionAmount` | string | No | Contains total transaction amount Example: `250.00` |
| `transactionAmountLocal` | string | No | Transaction amount in local currency Example: `2500` |
| `transactionCategoryIndicator` | string | No | Provides information about the transaction that cannot be determined from other transaction information, such as Primary Account Number or Point of Service Data Code Example: `02` |
| `transactionCurrencyCode` | string | No | Defines the currency code of the transaction Example: `840` |
| `transactionDestinationInstitutionIdCode` | string | No | Identifies the transaction destination institution Example: `2705` |
| `transactionLifeCycleId` | string | No | Contains all life cycle messages such as Authorizations, Financials, Reversals, Retrievals, Fulfillments and Chargebacks Example: `MPL2OSTCV0728` |
| `transactionOriginatorInstitutionIdCode` | string | No | Identifies the transaction originator institution Example: `5258` |
| `transactionType` | string | No | Describes the specific cardholder transaction type Example: `Clearing` |
| `transitProgramCode` | string | No | Provides data related to the transit transaction type and the transaction mode Example: `05` |
| `walletIdentifierMdes` | string | No | Provides information about transactions initiated through the use of a digital wallet Example: `101` |
| `currencyConversionAssessmentAmount` | string | No | DE 111 (Amount, Currency Conversion Assessment) is the amount based on the result of applying the multi-currency conversion rate (Currency Conversion Assessment) adjustment to the currency conversion rate used to convert DE 4 (Amount, Transaction) to DE 6 (Amount, Cardholder Billing) for qualified transactions Example: `000000021250` |
| `japanCommonMerchantCode` | string | No | Identifies the merchant's category in Japan referred to as the Common Merchant Category Code (CMC). Mastercard uses this value to identify a link to a corresponding Mastercard Assigned ID. Example: `0410` |
| `installmentData` | string | No | Provides information about the installment payment option selected by the cardholder at the point of interaction. Example: `1261610E81023498764532103` |
| `flexCode` | string | No | Specific to Brazil Flex Card transactions to communicate the product code used for clearing. Example: `003` |

### AuthorizationDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `accountNumber` | string | No | Account number associated with transaction in question Example: `5154676300000001` |
| `accountNumberIndicator` | string | No | Indicates the type of PAN mapping account Example: `I` |
| `acquirer` | string | No | The acquiring institution (for example, merchant bank) or its agent Example: `N` |
| `acquiringInstitutionCountryCode` | string | No | The code of the country where the acquirer is located Example: `USA` |
| `acquiringInstitutionId` | string | No | Identifies the acquiring institution (for example, merchant bank) or its agent Example: `2705` |
| `addressVerificationServiceResponse` | string | No | Indicates that verification of the cardholder billing address is requested in the authorization message Example: `S` |
| `adviceReasonCode` | string | No | Indicates the specific purpose of an advice message Example: `160` |
| `atcDiscrepancyIndicator` | string | No | Indicates if the ATC Discrepancy Value is above, below or within the maximum values allowed by the issuer. Example: `G` |
| `atcDiscrepancyValue` | string | No | The differential between the transaction ATC and the maximum value allowed by the issuer when the transaction ATC is above the previous ATC, or the differential between the transaction ATC and the minimum value allowed by the issuer when the transaction ATC is below the previous ATC. ATC Discrepancy Value will be zero when the transaction ATC is within the issuer-defined limits Example: `00005` |
| `atcValue` | string | No | Contains the derived full ATC Value used in the validation Example: `00053` |
| `authenticationIndicator` | string | No | Defined by the Authorization Platform and is passed to the issuer to indicate that the transaction qualified for Authentication service Example: `1` |
| `authorizationIdResponse` | string | No | A transaction response ID code that the authorizing institution assigns. DE 38 is used to transmit a card issuer's authorization code for Authorization transactions Example: `418443` |
| `banknetDate` | string | No | Date the transaction hit Mastercard network Example: `210127` |
| `banknetReferenceNumber` | string | No | Generated by the Authorization Platform for each originating message it routes Example: `U68FRG` |
| `billingCurrencyCode` | string | No | Currency code for the billing amount Example: `840` |
| `cardAcceptorCity` | string | No | Contains the card acceptor city of the merchant or, if a payment facilitator is involved in the transaction, the sub-merchant Example: `SAINT LOUIS` |
| `cardAcceptorId` | string | No | Identifies the card acceptor ID assigned by the acquirer Example: `0024038000200` |
| `cardAcceptorName` | string | No | Contains the card acceptor's name Example: `Amazon` |
| `cardAcceptorState` | string | No | Contains the card acceptor's state Example: `MO` |
| `cardAcceptorTerminalId` | string | No | A unique code identifying a terminal at the card acceptor location Example: `TERM-041` |
| `cardholderActivatedTerminalLevel` | string | No | Indicates whether the cardholder activated the terminal with the use of the card and the CAT security level Example: `6` |
| `cardholderBillingActualAmount` | string | No | The actual amount in the issuer currency Example: `000000010000` |
| `cardholderBillingAmount` | string | No | The transaction amount in the issuers currency Example: `000000010000` |
| `cardAuthenticationMethodValidationCode` | string | No | Indicates the method by which the cardholder's identity was verified at the point of service Example: `N` |
| `conversionDate` | string | No | The month and day that the conversion rate is effective to convert the transaction amount from the transaction currency into the currency of settlement or to convert the transaction amount from the original currency into the cardholder billing currency Example: `0127` |
| `conversionRate` | string | No | Indicates the conversion rate Example: `61000000` |
| `electronicCommerceIndicators` | string | No | Electronic commerce code Example: `215` |
| `electronicCommerceSecurityLevelIndicatorAndUcafCollectionIndicator` | string | No | Indicates the electronic commerce security level and UCAF collection Example: `10` |
| `expirationDatePresenceInd` | string | No | Indicates if expiration Date present on card Example: `N` |
| `finalAuthorizationIndicator` | string | No | Designates whether the authorization is a final authorization Example: `0` |
| `financialNetworkCode` | string | No | Identifies the specific program or service (for example, the financial network, financial program, or card program) with which the transaction is associated. DE 63 will contain the graduated product when the issuer's cardholder account participates in the Product Graduation Account Level Management service Example: `MPL` |
| `forwardingInstitutionId` | string | No | Identifies the institution forwarding a Request or Advice message in an interchange system if it is not the same institution as specified in Acquiring Institution ID Code Example: `5258` |
| `infData` | string | No | Contains acquiring network trace information that INFs may require to quickly and accurately route Administrative Advice/0620 messages back to the original acquiring institution Example: `4814653169024340` |
| `integratedCircuitCardRelatedData` | string | No | Contains chip data formatted in accordance with the Europay MasterCard Visa (EMV) specifications Example: `100` |
| `issuer` | string | No | The issuing institution Example: `N` |
| `mastercardPromotionCode` | string | No | Contains the promotion code to identify unique use of a Mastercard product for a specific program or service established between issuers and merchants Example: `HGMINS` |
| `mccMessageId` | string | No | The classification (card acceptor business code/merchant category code [MCC]) of the merchant's type of business or service Example: `3370` |
| `merchantAdviceCode` | string | No | Contains the merchant advice code Example: `03` |
| `merchantCategoryCode` | string | No | Contains the Merchant Category Code Example: `MCW` |
| `mobilePhoneNumber` | string | No | Contains the phone number of the wireless phone for which the customer is purchasing extra service Example: `1235551234` |
| `mobilePhoneServiceProviderName` | string | No | Contains the name or other identifier of the mobile phone service provider Example: `AT&T` |
| `originalAcquiringInstitutionIdCode` | string | No | Identifies the acquiring institution (for example, merchant bank) or its agent Example: `2705` |
| `originalElectronicCommerceSecurityLevelIndicatorAndUcafCollectionIndicator` | string | No | Identifies the level of UCAF supported in the authorization process. Example: `0` |
| `originalIssuerForwardingInstitutionIdCode` | string | No | Identifies a message's forwarding institution Example: `2705` |
| `originalMessageTypeIdentifier` | string | No | Contains the message type identifier Example: `0110` |
| `pinServiceCode` | string | No | Indicates the results of PIN processing by the Authorization Platform Example: `TV` |
| `realTimeSubstantiationIndicator` | string | No | Indicates if the merchant terminal verified the purchased items against the Inventory Information Approval System (IIAS) Example: `0` |
| `reasonForUcafCollectionIndicatorDowngrade` | string | No | Describes the reason why the Authorization Request/0100 message was downgraded Example: `210` |
| `posCardDataTerminalInputCapability` | string | No | Indicates the terminal capabilities for transferring the data on the card into the terminal Example: `0` |
| `posCardholderPresence` | string | No | Indicates whether the cardholder is present at the point of service and explains the condition if the cardholder is not present Example: `0` |
| `posCardPresence` | string | No | Indicates if the card is present at the point of service Example: `0` |
| `posEntryModePan` | string | No | Describes the capability of the terminal device to support/accept PAN entry Example: `05` |
| `posEntryModePin` | string | No | Describes the capability of the terminal device to support/accept PIN entry Example: `1` |
| `posTerminalAttendance` | string | No | Indicates if the card acceptor is attending the terminal Example: `0` |
| `posTerminalLocation` | string | No | Indicates the terminal location Example: `0` |
| `posTransactionStatus` | string | No | Indicates the purpose or status of the request Example: `0` |
| `primaryAccountNumber` | string | No | Account number associated with transaction in question Example: `510001000000134` |
| `primaryAccountNumberAccountRange` | string | No | Carries either the first nine digits of the cardholder PAN, or the full Visa cardholder PAN in the authorization response for a transaction initiated with a Visa token Example: `510001000` |
| `privateData` | string | No | Contain any private-use data that the customer may want to include in a message Example: `38038405002UU90220107ACQREG10207ISSREG17104C2C 102101920CM04020CM0402S1I13530411` |
| `processingCode` | string | No | A series of digits used to describe the effect of a transaction on the customer account and the type of accounts affected Example: `00` |
| `recordDataPresenceIndicator` | string | No | A variable-length data element used for transmitting file record data or textual character string data in various message types Example: `N` |
| `responseCode` | string | No | Defines the disposition of a previous message or an action taken as a result of receipt of a previous message. Response codes also are used to indicate approval or decline of a transaction. In the event an authorization is declined, the response code indicates the reason for rejection and may indicate an action to be taken at the card acceptor (for example, capture card) Example: `00` |
| `retrievalReferenceNumber` | string | No | A document reference number supplied by the system retaining the original source document of the transaction and assists in locating that source document or a copy thereof Example: `730607628081` |
| `settlementActualAmount` | string | No | Indicates the actual settlement amount in the settlement currency Example: `000000010000` |
| `settlementDate` | string | No | The date (month and day) that funds will be transferred between an acquirer and an issuer or an appropriate intermediate network facility (INF) Example: `0127` |
| `stan` | string | No | Indicates the Systems Trace Audit Number (STAN) Example: `002511` |
| `storageTechnology` | string | No | Describes the Storage Technology of a requested or created token Example: `01` |
| `systemsTraceAuditNumber` | string | No | The unique identifier assigned to each transaction by the originator of the message Example: `002511` |
| `tokenAssuranceLevel` | string | No | Contains a value indicating the confidence level of the token to PAN/cardholder binding Example: `99` |
| `tokenRequestorId` | string | No | Contains the ID assigned by the token service provider to the token requestor Example: `12345678936` |
| `track1` | string | No | The information encoded on track 1 of the card's magnetic stripe as defined in the ISO 7813 specification, including data element separators but excluding beginning and ending sentinels and LRC characters as defined in this data element definition Example: `N` |
| `track2` | string | No | The information encoded on track 2 of the card magnetic stripe as defined in the ISO 7813 specification, including data element separators but excluding beginning and ending sentinels and longitudinal redundancy check (LRC) characters as defined therein Example: `Y101` |
| `transactionActualAmount` | string | No | Indicates the actual transaction amount Example: `000000010000` |
| `transactionAmountLocal` | string | No | Transaction in the currency of transaction Example: `10000` |
| `transactionCategoryCode` | string | No | The format is LLLt, where t is the transaction category code (TCC). The message must contain an appropriate TCC as the first byte of data after the length within DE 48. The TCC classifies major categories of transactions, such as Retail Sale, Cash Disbursement, and Mail Order Example: `R` |
| `transactionCurrencyCode` | string | No | The local currency of the acquirer or source location of the transaction Example: `840` |
| `transactionType` | string | No | Contains the type of ATM credit card cash advance installment transaction Example: `Authorization` |
| `transmissionDateAndTime` | string | No | The date and time a message was transmitted by a processing entity, to be expressed in Coordinated Universal Time (UTC) Example: `0127075837` |
| `universalCardholderAuthenticationFieldUcaf` | string | No | Contains the encoded MasterCard SecureCode  issuer or cardholder-generated authentication data (collected by the merchant) resulting from all SecureCode fully authenticated transactions, data for Visa or American Express transactions associated with the 3-D Secure Electronic Commerce Verification Service (if collected), or the static AAV assigned by MasterCard for Maestro or MasterCard Advance Registration Program, Maestro Recurring Payments Program, or MasterCard Utility Payment Program Example: `PARTIALSHIPMENT0000000000000ALrP9TrnbuMCAANkrglrAoABFA==ACFa0knOekU7AAnwugwJAoAB` |
| `vcnProductCode` | string | No | The product code associated with the virtual card number VCN Example: `MCO` |
| `walletIdentifier` | string | No | Provides information about transactions initiated through the use of a digital wallet Example: `100` |

### CaseFilingResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `caseId` | string | No | The case filing id Example: `536092` |

### CaseFilingRespHistory

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `memo` | string | No | Memo pertaining to the case Example: `This is a test memo` |
| `action` | string | No | Action taken by party.   Valid Values/Format: ACCEPT, REJECT, REBUT, ESCALATE, WITHDRAW, FAVORSENDER, FAVORRECEIVER, DECLINED, MODIFYCASE, CREATECASE, EXPIRED Example: `REJECT` |
| `responseDate` | string | No | The date and the response was provided Example: `2021-02-14` |

### CaseFilingDetails

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `claimId` | string | No | Claim identifier associated with the standard claimId Example: `200002020654` |
| `claimType` | string | No | Claim Type Example: `Standard` |
| `caseId` | string | No | Identifier assigned to the case filing Example: `536092` |
| `caseType` | string | No | Type of Case Filing. The following number values represent each case type.  1-Pre-arbitration  2-Arbitration  3-Pre-compliance 4-Compliance Example: `1` |
| `chargebackRefNum` | array of string | No | A list of Chargeback Reference numbers Example: `1234123456` |
| `currencyCode` | string | No | The case currency. Value should be standard alpha currency code.   For domestic transactions: domestic currency or USD   For cross-border transactions: USD or EUR, per Global Clearing Management System rules   Length: 3   Valid values/format: USD, EUR, GBP, MXN etc and A-Z (uppercase alphabetic letter) Max: 3 Example: `USD` |
| `customerFilingNumber` | string | No | Customer filing number which is the filing party's internal number Example: `5482` |
| `creditDate` | string | No | Credit date when the violationCode is 1.4 in the case of pre-compliance or compliance case filing. The format should be yyyy-MM-dd Example: `2021-02-14` |
| `chargebackDate` | string | No | Chargeback date when the violationCode is 1.4 in the case of pre-compliance or compliance case filing. The format should be yyyy-MM-dd Example: `2021-02-12` |
| `reasonCode` | string | No | Reason code is returned when the case type is pre-arbitration or arbitration. Example: `4853` |
| `disputeAmount` | string | No | Dispute amount.  The currency will be determined by the ICA region entered in the Filed ICA and Filed Against ICA Example: `100.00` |
| `dueDate` | string | No | Due date when the response is required.  The format should be yyyy-MM-dd Example: `2021-02-14` |
| `filingAgaintstIca` | string | No | Filed Against ICA Example: `001111` |
| `filingAs` | string | No | Filing case as Issuer or Acquirer. Following values represents each type I-ISSUER  A-ACQUIRER Example: `A` |
| `filingIca` | string | No | Filing ICA Example: `002222` |
| `merchantName` | string | No | Merchant name for filing pre-arbitration and arbitration case Max: 22 Example: `test name` |
| `primaryAccountNum` | string | No | The primary account number Example: `5123123412341234` |
| `violationCode` | string | No | Violation code Example: `D.2` |
| `violationDate` | string | No | Violation Date Example: `2021-02-14` |
| `rulingDate` | string | No | Ruling Date Example: `2021-02-14` |
| `rulingStatus` | string | No | Ruling Status.  Valid values are Reviewed, Filed In Error, Declined, Expired, Favor Sender, Favor Receiver Example: `Reviewed` |
| `virtualAccountNum` | string | No | The virtual account number Example: `5123123424999876` |

### CaseFilingLifeCycle

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `caseFilingStatus` | string | No | Case Filing Status  *Valid Values:* New, Won, Lost, Withdrawn, Rejected, Accepted, Escalated, Unescalated, Ruled, Reviewed, Filed in Error, Declined, Expired, Favor Sender, Favor Receiver Example: `New` |
| `caseFilingDetails` | [CaseFilingDetails](#casefilingdetails) | No |  |
| `caseFilingRespHistory` | array of [CaseFilingRespHistory](#casefilingresphistory) | No |  |

### DocumentResponseStructure

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `fileAttachment` | [DocumentStructureResp](#documentstructureresp) | No |  |

### DocumentStructure

CONDITIONAL: Unless specified as REQUIRED, fileAttachment object is OPTIONAL. When fileAttachment is provided, then fileName and file parameters are required. The base64 encoded string must represent a ZIP, JPG, TIFF, or PDF file. Please note: ZIP files may contain JPG, TIFF or PDF files.

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `filename` | string | No | File name of image.   Valid Values/Format: Alphanumeric Max: 100 Example: `testimage111111.tif` |
| `file` | string | No | File converted to a base64 encoded string.   Length: 1-22000000   Valid Values/Format: Alphanumeric Example: `This is an image file stored in a base64 encoded string` |

### DocumentStructureResp

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `filename` | string | No | File name of image.  The filename will have an extension of .zip. Example: `testimage.zip` |
| `file` | string | No | File converted to a base64 encoded string.  File Format is ZIP  Note: ZIP file may contain these formats...JPG, TIFF, PDF Example: `This is an image file stored in a base64 encoded string` |

### CaseFilingEbdfStructure

When CaseFilingEbdfDocuments is used for automatic EBDF document generation of expeditedBillingDrfDocument  (form name of Dispute Resolution Form - Pre-Compliance/Compliance)  or smsLinkedCaseFilingDrfDocument no other documents should be attached on the call. Attaching documents will lead to call failure.

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `expeditedBillingDrfDocument` | [ExpeditedBillingDrfDocumentStructure](#expeditedbillingdrfdocumentstructure) | No |  |
| `smsLinkedCaseFilingDrfDocument` | [SmsLinkedCaseFilingDrfDocumentStructure](#smslinkedcasefilingdrfdocumentstructure) | No |  |

### ExpeditedBillingDrfDocumentStructure

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `cardholderName` | string | No | CONDITIONAL: The cardholder's name is optional on Dispute Resolution Form - Pre-Compliance/Compliance form.   Length: 1-30   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 75 Example: `Test Cardholder name` |
| `acquirerRefData` | string | No | The Acquirer’s Reference Data.   Length: 1-23   Valid Values/Format: Numeric Max: 23 Example: `05158764165000000084682` |
| `transactionDate` | string | No | The transaction date.   Length: 11   Valid Values/Format: Date (dd-MMM-yyyy) Max: 11 Example: `16-Dec-2020` |
| `transactionAmount` | string | No | The total transaction amount.   Length: 4-12   Valid Values/Format: Numeric Max: 12 Example: `100.00` |
| `disputeDescription` | string | No | Give a reasonably specific description of the dispute.   Length: 1-1500   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 1500 Example: `Test Dispute Description` |
| `certification` | string | No | Who is the company representative or government agency representative on behalf of the corporate card.   Length: 1-24   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 24 Example: `Test Certification` |
| `chargebackRepresentative` | string | No | Customer Service/Chargeback Representative.   Length: 1-25   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 25 Example: `Test Representative` |

### SmsLinkedCaseFilingDrfDocumentStructure

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `contactName` | string | No | The contact name.   Length: 1-30   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 30 Example: `Test Contact Name` |
| `companyOrInstitution` | string | No | The company or institution name.   Length: 1-70   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 70 Example: `Test Company Name` |
| `contactEmail` | string | No | The contact email.   Length: 1-70   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 70 Example: `reply@acme.com` |
| `reasonForFilingCase` | string | No | The reason for filing case.   Length: 1-490   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 490 Example: `Test Reason for Filing Case` |
| `processorId` | string | No | The processor Id.   Length: 1-11   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 11 Example: `999696` |
| `acquirerSwitchSerialNum` | string | No | The Acquirer Reference Data or Switch Serial Number.   Length: 1-23   Valid Values/Format: Numeric Max: 23 Example: `05131054165000000048149` |
| `transactionOrSettlementDate` | string | No | The Transaction or Settlement Date.   Length: 11   Valid Values/Format: Date (dd-MMM-yyyy) Max: 11 Example: `16-Dec-2020` |
| `numberOfTransactions` | string | No | A numeric count of number of transactions being disputed.   Length: 1-20   Valid Values/Format: (VALUES)/ (Numeric, Alphanumeric, Special Char) Max: 20 Example: `1` |

### ChargebackEbdfStructure

NOTE: When chargebackEbdfDocuments is used for automatic EBDF document generation for transactionInformation (or) fraudDrfDocument (or) cardholderDisputeChargebackDrfDocument (or) pointOfInteractionErrorsDrfDocument, no other documents should be attached on the call and documentIndicator must be set to true. Attaching documents or setting documentIndicator to false will lead to call failure.

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `transactionInformation` | [TransactionInformationEbdfStructure](#transactioninformationebdfstructure) | No |  |
| `fraudDrfDocument` | [FraudDrfDocumentStructure](#frauddrfdocumentstructure) | No |  |
| `cardholderDisputeChargebackDrfDocument` | [CardholderDisputeChargebackDrfDocumentStructure](#cardholderdisputechargebackdrfdocumentstructure) | No |  |
| `pointOfInteractionErrorsDrfDocument` | [PointOfInteractionErrorsDrfDocumentStructure](#pointofinteractionerrorsdrfdocumentstructure) | No |  |

### TransactionInformationEbdfStructure

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `acquirerRefDataOrSwitchSerialNum` | string | No | Acquirers Reference Data or Switch Serial Number.   Length: 0-23   Valid Values/Format: Alphanumeric / Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 23 Example: `05131054165000000048149` |
| `merchantName` | string | No | The Merchant Name   Length: 0-22   Valid Values/Format: Alphanumeric / Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 22 Example: `Test Merchant Name` |
| `transactionOrSettlementDate` | string | No | Transaction or Settlement Date   Length: 11   Valid Values/Format: Date (dd-MMM-yyyy) Max: 11 Example: `16-Feb-2018` |
| `disputedAmount` | string | No | The Disputed Amount.   Length: 0-12   Valid Values/Format: Numeric Max: 11 Example: `100.00` |

### FraudDrfDocumentStructure

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `numberOfItems` | string | No | Numeric value of number of transactions being disputed.   Length: 0-19   Valid Values/Format: Numeric Example: `1` |
| `types` | string | No | Enter any of the valid values comma separated.   Length: 7-12   Valid Values/Format: CARD_CLOSED, REPORT_SAFE, CAPTURE_CARD, LOST_STOLEN, COUNTERFEIT, RC_4837, RC_4840 Example: `CARD_CLOSED,REPORT_SAFE` |
| `additionalInformation` | string | No | Additional information, if needed   Length: 0-1000   Valid Values/Format: Alphanumeric / Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 1000 Example: `Test additional information` |
| `chargebackRepresentative` | string | No | Customer Service/Chargeback Representative   Length: 0-25   Valid Values/Format: Alphanumeric / Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 25 Example: `Test chargeback representative` |
| `cardIssuerRegion` | string enum: [AP, Europe, NAM, LAC, MEA] | No | The card issuer region.   Length: 2-6   Valid Values/Format: AP, Europe, NAM, LAC, MEA Example: `Europe` |
| `cardholderVerificationMethod` | [CardholderVerificationMethodStructure](#cardholderverificationmethodstructure) | No |  |

### CardholderVerificationMethodStructure

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `onlinePIN` | string | No | Online PIN Preferring.   Length: 1   Valid Values/Format: 1 Example: `1` |
| `offlinePIN` | string | No | Offline PIN.   Length: 1   Valid Values/Format: 2 Example: `2` |
| `signature` | string | No | Signature   Length: 1   Valid Values/Format: 3 Example: `3` |
| `none` | string | No | None (No CVM)   Length: 1   Valid Values/Format: 4 Example: `4` |

### CardholderDisputeChargebackDrfDocumentStructure

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `transactionAmount` | string | No | The transaction amount.   Length: 0-12   Valid Values/Format: Numeric Example: `100.00` |
| `type` | string enum: [PRODUCT_DISPUTE, NOT_PROVIDED, DIGITAL_GOODS, CREDIT_NOT_PROCESSED, COUNTERFEIT, RECURRING_CANCELLED, RECURRING_DISCLOSURE, ADDENDUM_DISPUTE, HOTEL_NO_SHOW, PURCHASE_INCOMPLETE, CANCELLATION_AGREEMENT, POSTED_CREDIT, FAILED_TRAVEL] | No | Type of Cardholder Dispute.   Length: 11-22   Valid Values/Format: PRODUCT_DISPUTE,NOT_PROVIDED,DIGITAL_GOODS,CREDIT_NOT_PROCESSED,COUNTERFEIT,RECURRING_CANCELLED,RECURRING_DISCLOSURE,ADDENDUM_DISPUTE,HOTEL_NO_SHOW,PURCHASE_INCOMPLETE,CANCELLATION_AGREEMENT,POSTED_CREDIT,FAILED_TRAVEL Example: `PRODUCT_DISPUTE` |
| `deliveryDateOfGoodsOrServices` | string | No | CONDITIONAL: In case of PRODUCT_DISPUTE, delivery date of the goods or services.   Length: 11   Valid Values/Format: Date (dd-MMM-yyyy) Max: 11 Example: `18-FEB-2018` |
| `expectedDeliveryDateOfGoodOrServices` | string | No | CONDITIONAL: In case of NOT_PROVIDED, expected delivery date of the goods or services.   Length: 11   Valid Values/Format: Date (dd-MMM-yyyy) Max: 11 Example: `18-FEB-2018` |
| `returnDate` | string | No | CONDITIONAL: In case of DIGITAL_GOODS, return or cancellation of the goods or services.   Length: 11   Valid Values/Format: Date (dd-MMM-yyyy) Max: 11 Example: `18-FEB-2018` |
| `cancellationDate` | string | No | CONDITIONAL: In case of RECURRING_CANCELLED, cancellation of the goods or services.   Length: 11   Valid Values/Format: Date (dd-MMM-yyyy) Max: 11 Example: `18-FEB-2018` |
| `cardholderParticipation` | boolean | No | Did the cardholder participate in the transaction?   Length: 4-5   Valid Values/Format: true / false Example: `false` |
| `disputeDetails` | string | No | Describe the cardholder’s compliant in sufficient detail to meet the requirements for the chargeback as described in the Chargeback Guide and to enable all parties to understand the dispute.   Length: 0-3000   Valid Values/Format: Alphanumeric / Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 3000 Example: `Test dispute details` |
| `chargebackRepresentative` | string | No | Customer Service/Chargeback Representative.   Length: 0-25   Valid Values/Format: Alphanumeric / Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 25 Example: `Test chargeback representative` |

### PointOfInteractionErrorsDrfDocumentStructure

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `transactionAmount` | string | No | The transaction amount.   Length: 0-12   Valid Values/Format: Numeric Example: `20000` |
| `type` | string enum: [DUPLICATE_DEBIT, INCORRECT_AMOUNT, LTD_TRANSACTION, CURRENCY_DISPUTE, UNREASONABLE_AMOUNT, DUPLICATE_TRANSACTION, MERCHANT_CREDIT, IMPROPER_MERCHANT_SURCHARGE] | No | The type of the error.   Length: 15-27   Valid Values/Format: DUPLICATE_DEBIT, INCORRECT_AMOUNT, LTD_TRANSACTION, CURRENCY_DISPUTE, UNREASONABLE_AMOUNT, DUPLICATE_TRANSACTION, MERCHANT_CREDIT, IMPROPER_MERCHANT_SURCHARGE Example: `DUPLICATE_DEBIT` |
| `alternateMeansOfPaymentDetails` | string | No | CONDITIONAL: in case of DUPLICATE_DEBIT, Alternate means of payment details.   Length: 0-55   Valid Values/Format: Alphanumeric / Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 55 Example: `Test alternate means of payment details` |
| `disputeDetails` | string | No | Describe the cardholder’s compliant in sufficient detail to meet the requirements for the chargeback as described in the Chargeback Guide and to enable all parties to understand the dispute.   Length: 0-4800   Valid Values/Format: Alphanumeric / Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 4800 Example: `Test dispute details` |
| `chargebackRepresentative` | string | No | Customer Service/Chargeback Representative.   Length: 0-25   Valid Values/Format: Alphanumeric / Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 25 Example: `Test Chargeback representative` |

### CreateClaimRequest

**Type:** object

**Required fields:** `claimType`, `clearingTransactionId`, `disputedAmount`, `disputedCurrency`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `disputedAmount` | string | Yes | The total amount of the original transaction in the dispute process. The disputedAmount should be equal to the original transaction amount (DE4) from the clearing record.   Length: 4-12   Valid Values/Format: Numeric Example: `100.00` |
| `disputedCurrency` | string | Yes | Currency of amount disputed in the claim. disputedCurrency can be provided as standard alpha code or numeric code   Length: 3   Valid Values/Format: A-Z (Uppercase Alphabetic Letter) / Numeric Example: `USD` |
| `claimType` | string enum: [Standard] | Yes | Type of claim to be created.   Length: 8   Valid Values/Format: Standard Example: `Standard` |
| `clearingTransactionId` | string | Yes | The Clearing Transaction Identifier from Clearing Summary Results.   Length: N/A   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Example: `ccCnaMDqmto4wnL+BSUKSdzROqGJ7YELoKhEvluycwKNg3XTzSfaIJhFDkl9hW081B5tTqFFiAwCpcoc` |
| `authTransactionId` | string | No | The Authorization Transaction Identifier from Authorization Summary Results.   Length: N/A   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Example: `aaCnaMDqmto4wnL+BSUKSdzROqGJ7YELoKhEvluycwKNg3XTzSfaIJhFDkl9hW081B5tTqFFiAwCpcoc` |

### UpdateClaimRequest

**Type:** object

**Required fields:** `action`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `openClaimDueDate` | string | No | The due date for opening the claim.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Example: `2024-02-20` |
| `action` | string enum: [REOPEN, CLOSE] | Yes | Action to perform on claim.   Length: 5-6   Valid Values/Format: REOPEN, CLOSE Example: `CLOSE` |
| `closeClaimReasonCode` | string enum: [10, 20, 30, 40] | No | Reason code for closing the claim.   Length: 2   Valid Values/Format: 10, 20, 30, 40 Example: `10` |

### CreateChargebackSingleRequest

**Type:** object

**Required fields:** `brand`, `replacementAmount`, `reversalReasonCode`, `usageCode`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `brand` | string enum: [MC, CI, MS, MD, PL, PV, VI] | Yes | The brand of service or program.   Length: 2   Valid Values/Format: MC - Mastercard, CI - Cirrus®, MS - Maestro®, MD - Debit Mastercard ®card, PL - Plus®, PV(L) - Private Label, VI - VISA Example: `MD` |
| `replacementAmount` | string | Yes | Replacement Amount.  The replacement amount should reflect the final amount of the transaction value that should remain applied to the cardholder balance.   Length: 3-12   Valid Values/Format: Numeric Example: `100.00` |
| `reversalReasonCode` | string | Yes | MDS dispute reason code for creating the dispute item   Length: 1-2   Valid Values/Format: Alphanumeric Max: 12 Example: `71` |
| `usageCode` | string enum: [1, 2] | Yes | The usage code indicates the type of chargeback record processed.   Length: 1   Valid Values/Format: 1 = first chargeback, 2 = second presentment Example: `1` |
| `chargebackType` | string enum: [S, D] | No | If 0.00 is entered in the Replacement Amount local field, you will be requested to define the Chargeback as Single or Double. Select chargebackType, either Single Chargeback or Double Chargeback. A double chargeback is used to reverse a credit posted as a debit.   Length: 1   Valid Values/Format: S or D Example: `S` |
| `acquirerFirstReferenceNumber` | string | No | CONDITIONAL: Mandatory when brand is MD and reason code is 34 (Duplicate processing). A unique identifier assigned by the acquirer of Debit Mastercard transactions.   Length: 23   Valid Values/Format: Numeric Example: `05103246259000000000126` |
| `additionalInformation` | string | No | Any additional information or note may be entered in this field.   Length: 1-38   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Example: `SMTM Manual` |
| `adjustmentContactFax` | string | No | CONDITIONAL: The chargeback contact fax number. Required when brand not MD and reversalReasonCode in one of the following...12, 30, 69, 70, 71, 73, 75, 79, 80, 95, 96, 97, 98.   Length: 1-15   Valid Values/Format: Numeric, Special Char(-) Example: `555-555-5555` |
| `adjustmentContactName` | string | No | CONDITIONAL: The chargeback contact name. Required when brand not MD and reversalReasonCode in one of the following...12, 30, 69, 70, 71, 73, 75, 79, 80, 95, 96, 97, 98.   Length: 1-24   Valid Values/Format: Character and Space Example: `John Smith` |
| `adjustmentContactPhone` | string | No | CONDITIONAL: The chargeback contact phone number.  Required when brand not MD and reversalReasonCode in one of the following...12, 30, 69, 70, 71, 73, 75, 79, 80, 95, 96, 97, 98   Length: 1-15   Valid Values/Format: Numeric Example: `5555555555` |
| `controlNumber` | string | No | Control Number.  Used to identify the specific transaction for internal auditing and tracing purposes.   Length: 1-5   Valid Values/Format: Numeric Example: `99999` |
| `dataRecordText` | string enum: [R3, RS7] | No | Data Record Text.   Length: 2-3   Valid Values/Format: R3,RS7   Note: RS5 (Guarantee No Show) is not supported, customers must use field reversalReasonCode 53 instead Example: `R3` |
| `documentIndicator` | string enum: [0, 1] | No | CONDITIONAL: Required when brand is MD and for the following reversalReasonCodes when brand is not MD...70, 71.   Length: 1   Valid Values/Format: 0-No Documentation, 1-Document will follow Example: `1` |
| `documentType` | string enum: [1, 2, 4] | No | CONDITIONAL: This field is mandatory when reversalReasonCode is 02.   Length: 1   Valid Values/Format: 1, 2, 4 Example: `1` |
| `illegibleItemCd` | string enum: [1, 2, 3, 4, 5, 6] | No | CONDITIONAL: The supporting documentation was illegible. This field is mandatory when reversalReasonCode is 02.   Length: 1   Valid Values/Format: 1, 2, 3, 4, 5, 6 Example: `1` |
| `program` | string enum: [QMAP, GMAP, INVAL] | No | CONDITIONAL: The type of card program bearing the account number. This field is mandatory when reversalReasonCode is 49.   Length: 4-5   Valid Values/Format: QMAP, GMAP, INVAL. Example: `INVAL` |
| `retrievalRequestDate` | string | No | CONDITIONAL: The date of the retrieval request.  This field is mandatory when brand is MD and reversalReasonCode is 02.   Length: 6   Valid Values/Format: Date Example: `010129` |
| `securityBulletinNumber` | string | No | OPTIONAL: This is three-digit Global Security Bulletin number using a YMM format (for example, bulletin number 6 June 2008 would be entered as 806).  Mandatory when brand is MD and reason code is 49. YMM is the format.   Length: 3   Valid Values/Format: Numeric Example: `122` |
| `fileAttachment` | [DocumentStructure](#documentstructure) | No |  |
| `refundNotReceivedIndicator` | string | No | Cardholder/Issuer did not receive refund when a first chargeback was rejected by Collaboration with reason code 5000 indicating refund provided. 20 days after rejection of CB through collaboration.   CONDITIONAL: this field is only applicable if chargebackType is CHARGEBACK.   Length: 4-5   Valid Values/Format: true, false Max: 5 Example: `true` |

### CreateChargebackSingleReversalRequest

**Type:** object

**Required fields:** `replacementAmount`, `reversalReasonCode`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `replacementAmount` | string | Yes | Replacement Amount.  The replacement amount should reflect the final amount of the transaction value that should remain applied to the cardholder balance.   Length: 3-12   Valid Values/Format: Numeric Example: `100.00` |
| `reversalReasonCode` | string enum: [03, 82] | Yes | Dispute reason code for reversing the chargeback item.   Length: 2   Valid Values/Format: 03 / 82 Example: `82` |
| `controlNumber` | string | No | Control Number.  Discretionary data input field used by issuers, acquirers, and  Mastercard.   Length: 1-5   Valid Values/Format: Numeric Example: `99999` |

### CreateChargebackRequest

**Type:** object

**Required fields:** `amount`, `chargebackType`, `currency`, `documentIndicator`, `reasonCode`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `amount` | string | Yes | Amount of CB should be OT amount (DE4). US Issuers should always submit in USD. For more details refer to the GCMS Reference Manual.   Length: 1-12   Valid Values/Format: Numeric Example: `100.00` |
| `chargebackType` | string enum: [CHARGEBACK, SECOND_PRESENTMENT] | Yes | Provide the chargeback.   Length: 10-18   Valid Values/Format: CHARGEBACK, SECOND_PRESENTMENT Example: `CHARGEBACK` |
| `currency` | string | Yes | The chargeback currency. The data should be standard currency alpha code or numeric code. Currency should correspond with the amount submitted for chargeback creation Length: 3 Valid Values/Format: A-Z (Uppercase Alphabetic Letter) OR Numeric Max: 3 Example: `USD` |
| `documentIndicator` | string | Yes | Document Indicator defines if a document is required for the dispute.   Length: 4-5   Valid Values/Format: true / false Example: `true` |
| `reasonCode` | string | Yes | Chargeback Reason Code provides the chargeback receiver with the reason for sending the chargeback.   Length: 1-4   Valid Values/Format: Numeric Max: 4 Example: `4853` |
| `credPostedAsPurchase` | boolean | No | Indicator to notify this is a credit posted as a purchase.   Only applicable to reason codes of 4853 and 4860.  Defaults to false.   Length: 4-5   Valid Values/Format: true / false Example: `false` |
| `isPartialChargeback` | boolean | No | Indicates a partial chargeback.  Defaults to false.   Length: 4-5   Valid Values/Format: true / false Example: `false` |
| `messageText` | string | No | Member message text to be used for the chargeback.   Length: 0-100   Valid Values/Format: Alphanumeric / Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) / Spaces Max: 100 Example: `This is a test message text` |
| `settlementDate` | string | No | CONDITIONAL: Required for Argentina and Uruguay's Settlement Service participation ID codes (LA00003201, LA00003202, LA00085801, LA00085802, LA00084011, LA00084012). The date may not be prior to the current date or beyond 90 days from the current date.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Example: `2025-10-26` |
| `disputeChargebackID` | string | No | Accepts a chargeback ID when 'chargebackType' is set to 'SECOND_PRESENTMENT'   Length: 1-19   Valid Values/Format: Numeric Example: `300045678811` |
| `localTax1IVA` | string | No | Applies only to LAC installments (Argentina and Uruguay). PDS 1015. Contains the VAT amount for the installment fee.   Length: 4-6   Valid Values/Format: Numeric (2 Decimals) Max: 6 Example: `0.60` |
| `installmentFee` | string | No | Applies only to LAC installments (Argentina and Uruguay). PDS 1028. Contains the VAT amount for the installment fee.   Length: 1-12   Valid Values/Format: Numeric Max: 12 Example: `1.2` |
| `editExclusionCode` | string | No | Edit exclusion code to bypass clearing system edits.   Length: 1-2   Valid Values/Format: Alphanumeric / Spaces. (Y,B0,B1,B2,B3,B4,B5,B6,B7,B8,B9,BA,BB,BC,BD,BE,BF,BG,BH,BI,BJ,BK,BL,BM,BN,BO,BP,BQ,BR,BS,BT,BU,BV,BW,BX,BY,BZ,SPACES). Max: 2 Example: `BO` |
| `refundNotReceivedIndicator` | string | No | Cardholder/Issuer did not receive refund when a first chargeback was rejected by Collaboration with reason code 5000 indicating refund provided 20 days after rejection of CB through collaboration.   CONDITIONAL: this field is only applicable if chargebackType is CHARGEBACK.   Length: 4-5   Valid Values/Format: true, false Max: 5 Example: `true` |
| `includeCurrencyConversionAssessmentCCA` | string | No | Currency Conversion Assessment amount applied for full first chargeback, to indicate, if Currency Conversion Assessment was included or not for qualified transactions. Example: `true` |
| `acknowledgeFirstPartyTrustEvidence` | boolean | No | Issuer has acknowledged First-Party Trust evidence and proceeded with the chargeback. Defaults to false. This field is not applicable to second presentments.  CONDITIONAL: This field is only applicable if chargebackType is CHARGEBACK.  Valid Values/Format: true, false Example: `true` |
| `fileAttachment` | [DocumentStructure](#documentstructure) | No |  |
| `chargebackEbdfDocuments` | [ChargebackEbdfStructure](#chargebackebdfstructure) | No |  |

### UpdateChargebackRequest

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `memo` | string | No | Memo.   Length: 1-100   Valid Values/Format: Alphanumeric / Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Example: `This is a test memo` |
| `creditVoucherAction` | string | No | Action to be performed on 1st chargeback.   CONDITIONAL: This field is required and only applicable if fileAttachment is not provided. Length: 6-7   Valid Values/Format: ACCEPT, DECLINE Example: `ACCEPT` |
| `fileAttachment` | [DocumentStructure](#documentstructure) | No |  |

### ChargebackMarkProcessedRequest

**Type:** object

**Required fields:** `chargebackList`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `chargebackList` | array of [ChargebackMarkProcessedRequestStructure](#chargebackmarkprocessedrequeststructure) | Yes | A list of Chargeback Ids to acknowledge, maximum list size is 500. |

### ChargebackMarkProcessedRequestStructure

**Type:** object

**Required fields:** `chargebackId`, `claimId`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `claimId` | string | Yes | Claim Id.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |
| `chargebackId` | string | Yes | Chargeback Id.   Length: 1-19   Valid Values/Format: Numeric Example: `300018439680` |

### ChargebackMarkProcessedResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `chargebackResponseList` | array of [ChargebackMarkProcessedResponseStructure](#chargebackmarkprocessedresponsestructure) | No | A list of Chargeback statuses |

### ChargebackMarkProcessedResponseStructure

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `chargebackId` | string | No | Chargeback Id marked as processed Example: `300018439680` |
| `status` | string | No | The status of the chargeback processing. Example: `FAILURE` |
| `failureReason` | string | No | The failure reason of the chargeback processing Example: `Failed to execute useCase: The item #300000000099 has already been processed.` |

### ChargebackStatusRequest

**Type:** object

**Required fields:** `chargebackList`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `chargebackList` | array of [ChargebackStatusRequestStructure](#chargebackstatusrequeststructure) | Yes | A list of Chargeback Ids to query, maximum list size is 2000 |

### ChargebackStatusRequestStructure

**Type:** object

**Required fields:** `chargebackId`, `claimId`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `claimId` | string | Yes | Claim Id.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |
| `chargebackId` | string | Yes | Chargeback Id.   Length: 1-19   Valid Values/Format: Numeric Example: `300018439680` |

### ChargebackStatusResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `chargebackResponseList` | array of [ChargebackStatusResponseStructure](#chargebackstatusresponsestructure) | No | A list of chargeback image statuses |

### ChargebackStatusResponseStructure

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `chargebackId` | string | No | Chargeback Id Example: `300018439680` |
| `claimId` | string | No | Claim Id Example: `200002020654` |
| `status` | string | No | Status of chargebacks, the valid values are: COMPLETED, FAILED, PENDING, UNAVAILABLE AND DOC_NOT_APPLICABLE. COMPLETED: Image was processed and no further action required. FAILED: Some failure happened during image process flow, i.e,The image could not be converted, The image is not imported, Image extension not supported etc. PENDING: The image is pending to be processed. DOC_NOT_APPLICABLE: The dispute does not require a document. UNAVAILABLE: The image is unavailable because it is not picked up by mastercom internal processes yet. Example: `COMPLETED` |

### CreateRetrievalRequest

**Type:** object

**Required fields:** `docNeeded`, `retrievalRequestReason`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `retrievalRequestReason` | string enum: [6343] | Yes | Retrieval Request Reason Codes.   NOTE: Below retrievalRequestReason code will be accepted for creation of retrieval request starting on October 24th, 2021.<br>  6343 - IIAS Audit (for healthcare transactions only)   Length: 1-4   Valid Values/Format: 6343 - IIAS Audit (for healthcare transactions only) Example: `6343` |
| `docNeeded` | string enum: [2, 4] | Yes | Documentation Needed Indicator.   Length: 1   Valid Values/Format: 2 - Copy or image (photocopy, microfilm, fax) of original document, 4 - Substitute draft Example: `2` |
| `instructionsForHealthcare` | string | No | Instructions for Healthcare.   CONDITIONAL: Required when retrievalRequestReason = 6343.   Length: 16-200   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Example: `Instructions to acquirer for fulfilling the retrieval request` |

### CreateRetrievalRequestSingle

**Type:** object

**Required fields:** `documentType`, `replacementAmount`, `reversalReasonCode`, `usageCode`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `documentType` | string enum: [2, 4] | Yes | The document type field indicates what type of documentation is sent supporting the retrieval.   Length: 1   Valid Values/Format: 2, 4 Example: `1` |
| `replacementAmount` | string | Yes | Replacement Amount.  The replacement amount should reflect the final amount of the transaction value that should remain applied to the cardholder balance.   Length: 4-12   Valid Values/Format: Numeric Example: `200.00` |
| `reversalReasonCode` | string | Yes | MDS dispute reason code for healthcare to create the dispute item.   NOTE: Creation of retrieval request is allowed only for reversalReasonCode (healthcare reason code (43)) starting on October 24th, 2021.   Length: 2   Valid Values/Format: 43 Example: `43` |
| `usageCode` | string enum: [1, 2, 3, 6, 7] | Yes | The usage code indicates the type of retrieval request record processed.   Length: 1   Valid Values/Format: 1, 2, 3, 6, 7 Example: `6` |
| `additionalInformation` | string | No | Any additional information or note may be entered in this field.   Length: 1-38   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Example: `SMTM Manual` |
| `controlNumber` | string | No | Control Number.  Used to identify the specific transaction for internal auditing and tracing purposes.   Length: 1-5   Valid Values/Format: Numeric Example: `12354` |

### AcquirerFulfillmentRequest

**Type:** object

**Required fields:** `acquirerResponseCd`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `acquirerResponseCd` | string enum: [A, B, C, E, F, G, H] | Yes | Acquirer Response Code.   NOTE: Acquirers can no longer respond to retrieval requests using the existing process in production, except for transactions related to U.S. healthcare.   NOTE: Below acquirerResponseCd codes will be accepted starting on February 26th, 2023.   A - Funds Movement Request, B - Refunded, C - Initiating Refund, E - Reject Collaboration, F - IIAS Unfulfillable, G - IIAS Invalid request information, H - IIAS Fulfilled outside MasterCom system   Length: 1   Valid Values/Format: A - Funds Movement Request, B - Refunded, C - Initiating Refund, E - Reject Collaboration, F - IIAS Unfulfillable, G - IIAS Invalid request information, H - IIAS Fulfilled outside MasterCom system Example: `F` |
| `refundReversalType` | string | No | Refund/Reversal Type.   CONDITIONAL: This field is not valid if acquirerResponseCd is A, E, F, G or H.   If acquirerResponseCd is C, then CREDIT VOUCHER is not a valid value.   Required if acquirerResponseCd is B.   Length: 6-14   Valid Values/Format: REFUND, CREDIT VOUCHER Example: `REFUND` |
| `refundReversalDate` | string | No | Refund/Reversal Date.   CONDITIONAL: This field is not valid if acquirerResponseCd is A, E, F, G or H.   Not valid if refundReversalType is not provided.   Required if acquirerResponseCd is B or C and refundReversalType is REFUND.   Length: 16   Valid Values/Format: Date (yyyy-MM-ddTHH:mm) Example: `2021-02-15T12:01` |
| `refundReversalAmount` | string | No | Refund/Reversal Amount.   CONDITIONAL: This field is not valid if acquirerResponseCd is A, E, F, G or H.   This field is only valid if acquirerResponseCd is B and refundReversalType is CREDIT VOUCHER.   Not valid if refundReversalType is not provided.   Valid Values/Format: Numeric Example: `100.00` |
| `refundReversalCurrency` | string | No | Refund/Reversal Currency.   CONDITIONAL: This field is not valid if acquirerResponseCd is A, E, F, G or H.   This field is only valid if acquirerResponseCd is B and refundReversalType is CREDIT VOUCHER.   Not valid if refundReversalType is not provided.   Length: 3   Valid Values/Format: A-Z (Uppercase Alphabetic Letter) Example: `USD` |
| `refundReversalReferenceId` | string | No | For Transaction type Authorized transactions:<br> First 12 positions of Data Element (DE) 63 (Network Data) from one of the following:<br> – Authorized refund message (Authorization Request Response/0110 or Financial Transaction Request Response/0210)<br> – Reversal message (Reversal Request Response/ 0410 or Acquirer Reversal Advice Response/ 0430)<br> – Automated fuel dispenser transaction (MCC = 5542 and CAT Level 2) (Authorization Advice Response/0130)   For Transaction type Cleared transactions:<br>  DE 63 (Network Data), subfield 2 (Trace ID) in the First Presentment/1240 message   For Transaction type Not authorized but cleared transactions:<br>  Either Private Data Subelement (PDS) 0105 (File ID) or PDS 158, subfield 5 (Central Site Business Date) and subfield 6 (Business Cycle) with no spaces   CONDITIONAL: This field is not valid if acquirerResponseCd is A, E, F, G or H.   Not valid if refundReversalType is not provided.   Required if acquirerResponseCd is B or C and refundReversalType is REFUND.   Length: 8-25   Valid Values/Format: Alphanumeric Example: `123458111` |
| `memo` | string | No | Memo.   Length: 1-100   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Example: `This is a test memo` |

### IssuerFulfillmentRequest

**Type:** object

**Required fields:** `issuerResponseCd`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `memo` | string | No | This is a test memo.   Length: 1-100   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Example: `This is a test memo` |
| `issuerResponseCd` | string enum: [APPROVE, REJECT_DOCUMENTATION_NOT_AS_REQUIRED, REJECT_ILLEGIBLE_OR_MISSING] | Yes | Issuer Response Code.   Length: 7-36   Valid Values/Format: APPROVE, REJECT_DOCUMENTATION_NOT_AS_REQUIRED, REJECT_ILLEGIBLE_OR_MISSING Example: `APPROVE` |
| `rejectReasonCd` | string enum: [A, M, P, D, O] | No | Reject Reason Code.   Length: 1   Valid Values/Format: A - TRANSACTION AMOUNT MISSING/ILLEGIBLE, M - MERCHANT NAME MISSING/ILLEGIBLE, P - PRIMARY ACCOUNT NUMBER MISSING/ILLEGIBLE, D - TRANSACTION DATE MISSING/ILLEGIBLE, O - OTHER (it can also be used for NOT A SUBSTITUTE DRAFT Example: `A` |

### RetrievalStatusRequest

**Type:** object

**Required fields:** `retrievalList`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `retrievalList` | array of [RetrievalStatusRequestStructure](#retrievalstatusrequeststructure) | Yes | A list of Request Ids to query, maximum list size is 2000 |

### RetrievalStatusRequestStructure

**Type:** object

**Required fields:** `claimId`, `requestId`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `claimId` | string | Yes | Claim Id for the Retrieval Request.   Length: 1-19   Valid Values/Format: Numeric Example: `200002020654` |
| `requestId` | string | Yes | Retrieval Request Id.   Length: 1-19   Valid Values/Format: Numeric Example: `300002296235` |

### RetrievalStatusResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `retrievalResponseList` | array of [RetrievalStatusResponseStructure](#retrievalstatusresponsestructure) | No | A list of retrieval image statuses |

### RetrievalStatusResponseStructure

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `claimId` | string | No | Claim Id Example: `200002020654` |
| `requestId` | string | No | Request Id Example: `300002296235` |
| `status` | string | No | When retrieving status of an image, the valid values are: COMPLETED, FAILED, PENDING, UNAVAILABLE AND DOC_NOT_APPLICABLE. COMPLETED: Image was processed and no further action required. FAILED: Some failure happened during image process flow, i.e,The image could not be converted, The image is not imported, Image extension not supported etc. PENDING: The image is pending to be processed. DOC_NOT_APPLICABLE: The dispute does not require a document. UNAVAILABLE: The image is unavailable because it is not picked up by mastercom internal processes yet. Example: `COMPLETED` |

### CaseFilingStatusRequest

**Type:** object

**Required fields:** `caseFilingList`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `caseFilingList` | array of [CaseFilingStatusRequestStructure](#casefilingstatusrequeststructure) | Yes | A list of case filing ids to query, maximum list size is 2000 |

### CaseFilingStatusRequestStructure

**Type:** object

**Required fields:** `caseId`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `caseId` | string | Yes | Case Id    Length: 1-19   Valid Values/Format: Numeric Example: `536092` |

### CaseFilingStatusResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `caseFilingResponseList` | array of [CaseFilingStatusResponseStructure](#casefilingstatusresponsestructure) | No | A list of case filing statuses |

### CaseFilingStatusResponseStructure

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `caseId` | string | No | Case Id Example: `536092` |
| `status` | string | No | Status of case filing images.   If neither party attached documentation, valid responses are in the format "UNAVAILABLE".   If either party attached documentation, valid responses are in the format "Status_Party_processDate_REBUT".  * Valid values for "Status" are COMPLETED, PENDING, FAILED, and UNAVAILABLE.  * Valid values for "Party" are SND (sender) and REC (receiver).  * "processDate" is formatted MM/DD/YYYY HH:MM:SS AM/PM.  * "REBUT" will be suffixed to the status when the case is rebutted. Example: `COMPLETED_SND_2/10/2021 8:43:21 AM` |

### CaseFilingImageStatusRequest

**Type:** object

**Required fields:** `endDate`, `startDate`, `status`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `status` | string | Yes | Case filing image status.   Length: 6-11   Valid Values/Format: COMPLETED, FAILED, UNPROCESSED Example: `COMPLETED` |
| `startDate` | string | Yes | Case filing image processing start date.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Example: `2025-10-11` |
| `endDate` | string | Yes | Case filing image processing end date.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Example: `2025-10-20` |

### CaseFilingImageStatusResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `caseFilingImageStatusList` | array of [CaseFilingImageStatusResponseStructure](#casefilingimagestatusresponsestructure) | No | A list of case filing image statuses |

### CaseFilingImageStatusResponseStructure

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `caseId` | string | No | Case Id Example: `536092` |
| `status` | string | No | Status of case filing images, the valid values are: COMPLETED, FAILED, UNPROCESSED.COMPLETED: Image was processed and no further action required. FAILED: Some failure happened during image process flow, i.e,The image could not be converted, The image is not imported, Image extension not supported etc.UNPROCESSED: The image is unavailable because it is not picked up by mastercom internal processes yet. Example: `COMPLETED` |

### CreateFraudMasterCardRequest

**Type:** object

**Required fields:** `chgbkIndicator`, `cvcInvalidIndicator`, `fraudType`, `reportDate`, `subType`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `acctStatus` | string enum: [ACCT_IS_OPEN, ACCT_HAS_BEEN_CLOSED] | No | Account status.   Length: 12-20   Valid Values/Format: ACCT_IS_OPEN, ACCT_HAS_BEEN_CLOSED Example: `ACCT_IS_OPEN` |
| `chgbkIndicator` | string enum: [0, 1] | Yes | Chargeback Indicator.   Length:    Valid Values/Format: (VALUES)/(Numeric, Alphanumeric, Special Char) Example: `1` |
| `cvcInvalidIndicator` | string enum: [Y, *, M, N, P, U, ?, E] | Yes | CVC Invalid Indicator.   Length: 1   Valid Values/Format: Y, *, M, N, P, U, ?, E Example: `Y` |
| `deviceType` | string enum: [1, 2, 3, 4, A, B, C, D, E, F, G, H, I, J] | No | Account Device Type.   Length: 1   Valid Values/Format: 1, 2, 3, 4, A, B, C, D, E, F, G, H, I, J Example: `1` |
| `fraudType` | string enum: [00, 01, 02, 03, 04, 05, 06, 07, 51, 55, 56, 57] | Yes | Fraud Type.   Length: 2   Valid Values/Format: 00, 01, 02, 03 ,04 ,05, 06, 07, 51, 55, 56, 57 Example: `00` |
| `reportDate` | string | Yes | Fraud Report Date.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Example: `2024-02-11` |
| `subType` | string enum: [K, N, P, U, A, I, V, H, R] | Yes | Fraud Sub Type.   Length: 1   Valid Values/Format: K, N, P, U, A, I, V, H, R Example: `K` |

### TransactionSearchRequest

**Type:** object

**Required fields:** `tranEndDate`, `tranStartDate`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `acquirerRefNumber` | string | No | CONDITIONAL: Acquirer Reference Number. If provided bankNetRefNumber may not be used. This field is required and applicable if primaryAccountNum is not provided.   Length: 23   Valid Values/Format: Numeric Example: `05436847276000293995738` |
| `bankNetRefNumber` | string | No | CONDITIONAL: This field is comprised of the Financial Network Code + Banknet Reference Number. If provided, must provide PAN in primaryAccountNum parameter. If provided, cannot use acquirerRefNumber parameter.  Length: 9  Valid Values/Format: Alphanumeric Example: `MPLU68FRG` |
| `primaryAccountNum` | string | No | CONDITIONAL: Primary Account Number. This field is required and applicable if acquirerRefNum is not provided.   Length: 11-19   Valid Values/Format: Numeric Example: `5488888888887192` |
| `transAmountFrom` | string | No | Transaction amount lower limit value to be searched.   Length: 0-12   Valid Values/Format: Numeric Example: `10000` |
| `transAmountTo` | string | No | Transaction amount upper limit value to be searched.   Length: 0-12   Valid Values/Format: Numeric Example: `20050` |
| `tranStartDate` | string | Yes | Transaction Date min search range.  The search range is a maximum of 30 days, and searches can be completed for up to 730 days of history.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Example: `2025-08-22` |
| `tranEndDate` | string | Yes | Transaction Date max search range.  The search range is a maximum of 30 days, and searches can be completed for up to 730 days of history.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Example: `2025-08-22` |

### TransactionSingleSearchRequest

**Type:** object

**Required fields:** `primaryAccountNumber`, `settlementFromDate`, `settlementToDate`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `primaryAccountNumber` | string | Yes | Primary Account Number [PAN] is a series of digits used to identify a customer account or relationship.   Length: 12-19   Valid Values/Format: Numeric Example: `5488888888887192` |
| `settlementFromDate` | string | Yes | From date of Settlement date range to search for a transaction. Mastercard uses Settlement Date to group the transactions for reporting and for subsequent settlement. The search range is a maximum of 30 days, and searches can be completed for up to 180 days of history.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Example: `2025-08-22` |
| `settlementToDate` | string | Yes | To date of Settlement date range to search for a transaction. Mastercard uses Settlement Date to group the transactions for reporting and for subsequent settlement. The search range is a maximum of 30 days, and searches can be completed for up to 180 days of history.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Example: `2025-08-22` |
| `acquirerRefNumber` | string | No | Acquirer Reference Data is data an acquirer supplies in an acquirer-originated message that may be required for an issuer to return to the acquirer in a subsequent message Example: `05436847276000293995738` |
| `switchSerialNumber` | string | No | The Switch Serial Number is a unique transaction identification number generated (or assigned) by the Single Message.   Length: 9   Valid Values/Format: Numeric Example: `142389095` |

### CreateCaseRequest

**Type:** object

**Required fields:** `caseType`, `currencyCode`, `disputeAmount`, `filedAgainstIca`, `filingAs`, `filingIca`, `memo`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `caseType` | string enum: [1, 2, 3, 4] | Yes | Type of Case Filing.   Length: 1   Valid Values/Format: 1-Pre-arbitration, 2-Arbitration, 3-Pre-compliance, 4-Compliance Example: `2` |
| `chargebackRefNum` | array of string | No | A list of Chargeback Reference numbers.   CONDITIONAL: This field is mandatory and applicable if the case type is pre-arbitration or arbitration or if the primary account number field is not populated.   Pre-Arbitration and Arbitration case will have one chargeback to one case filing. Pre-Compliance and Compliance case can have many chargeback to one case filing.   Length: 0-10   Valid Values/Format: Numeric |
| `customerFilingNumber` | string | No | Customer filing number which is the filing party's internal number.   Length: 0-15   Valid Values/Format: Numeric Max: 15 Example: `5482` |
| `disputeAmount` | string | Yes | Dispute amount. The currency will be determined by the ICA region entered in the Filed ICA and Filed Against ICA.   Length: 1-10 (Integer Part). 2 (Decimal Part)   Valid Values/Format: Decimal Max: 13 Example: `200.00` |
| `currencyCode` | string | Yes | The case currency. Value should be standard alpha currency code.   For domestic transactions: domestic currency or USD   For cross-border transactions: USD or EUR, per Global Clearing Management System rules   Length: 3   Valid values/format: USD, EUR, GBP, MXN etc and A-Z (uppercase alphabetic letter) Max: 3 Example: `USD` |
| `dueDate` | string | No | Due date when the response is required.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Max: 10 Example: `2021-02-20` |
| `fileAttachment` | [DocumentStructure](#documentstructure) | No |  |
| `filedAgainstIca` | string | Yes | Filed Against ICA.   Length: 1-11   Valid Values/Format: Numeric Max: 11 Example: `004321` |
| `filingAs` | string enum: [I, A] | Yes | Filing case as Issuer or Acquirer.   Length: 1   Valid Values/Format: I, A Example: `A` |
| `filingIca` | string | Yes | Filing ICA.   Length: 1-11   Valid Values/Format: Numeric Max: 11 Example: `001234` |
| `memo` | string | Yes | Enter a Memo pertaining to the case.   Length: 1-13000   Valid Values/Format: Alphanumeric and Special Characters (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 13000 Example: `This is a test memo` |
| `messageText` | string | No | Enter a MessageText pertaining to the case.   CONDITIONAL: Applicable only for filing pre-arbitration and arbitration case for sender while creating the case.   Length: 1-100   Valid Values/Format: Alphanumeric and Special Characters (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 100 Example: `This is a test message` |
| `changeReasonCodeFlag` | string | No | Change reason code Flag.   CONDITIONAL: Applicable only for filing pre-arbitration and arbitration case for sender while creating the case.   Length: 1   Valid Values/Format: Y,N Max: 1 Example: `Y` |
| `updatedChargebackReasonCode` | string enum: [4863, 4899, 2001, 2002, 2003, 2004, 2005, 2008, 2011, 2700, 2701, 2702, 2703, 2704, 2705, 2706, 2707, 2708, 2709, 2710, 2711, 2712, 2713, 4801, 4802, 4807, 4808, 4809, 4812, 4831, 4834, 4835, 4837, 4840, 4841, 4842, 4846, 4847, 4849, 4850, 4853, 4854, 4855, 4856, 4857, 4859, 4860, 4862, 4900, 4901, 4902, 4903, 4905, 4908, 2000, 4870, 4871, 03, 06, 17, 30, 69, 70, 71, 73, 74, 75, 79, 80, 82, 85, 95, 96, 97, 98, 13, 10, 20, 24, 25, 26, 27, 28, 29] | No | Updated Chargeback Reason Code.   CONDITIONAL: Required and applicable for filing pre-arbitration and arbitration case for sender while creating the case and if changeReasonCodeFlag is Y.   Length: 2-4   Valid Values/Format: 4863,4899,2001,2002,2003,2004,2005,2008,2011,2700,2701,2702,2703,2704,2705,2706,2707,2708,2709,2710,2711,2712,2713,4801,4802,4807,4808,4809,4812,4831,4834,4835,4837,4840,4841,4842,4846,4847,4849,4850,4853,4854,4855,4856,4857,4859,4860,4862,4900,4901,4902,4903,4905,4908,2000,4870,4871,03,06,17,30,69,70,71,73,74,75,79,80,82,85,95,96,97,98,13,10,20,24,25,26,27,28,29 Example: `4863` |
| `changeReasonCodeReason` | string | No | Change reason Code reason.   CONDITIONAL: Required and applicable for filing pre-arbitration and arbitration case for sender while creating the case and if changeReasonCodeFlag is Y.   Length: 1-1000   Valid Values/Format: Alphanumeric and Special Characters (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 1000 Example: `This is a test reason` |
| `primaryAccountNum` | string | No | The primary account number.   CONDITIONAL: This field is mandatory and applicable if the chargeback ref number field is not populated.   Length: 1-19   Valid Values/Format: Numeric Max: 19 Example: `5123123412341234` |
| `acquirerRefNum` | string | No | The acquirer reference number.   CONDITIONAL: This field is mandatory and applicable if case is created using primary account number.   Length: 1-23   Valid Values/Format: Numeric Max: 23 Example: `05131054165000000048149` |
| `chargebackReasonCode` | string enum: [4863, 4899, 2001, 2002, 2003, 2004, 2005, 2008, 2011, 2700, 2701, 2702, 2703, 2704, 2705, 2706, 2707, 2708, 2709, 2710, 2711, 2712, 2713, 4801, 4802, 4807, 4808, 4809, 4812, 4831, 4834, 4835, 4837, 4840, 4841, 4842, 4846, 4847, 4849, 4850, 4853, 4854, 4855, 4856, 4857, 4859, 4860, 4862, 4900, 4901, 4902, 4903, 4905, 4908, 2000, 4870, 4871, 03, 06, 17, 30, 69, 70, 71, 73, 74, 75, 79, 80, 82, 85, 95, 96, 97, 98, 13, 10, 20, 24, 25, 26, 27, 28, 29] | No | Chargeback Reason Code.   CONDITIONAL: Required and applicable for filing pre-arbitration and arbitration case.   Length: 2-4   Valid Values/Format: 4863,4899,2001,2002,2003,2004,2005,2008,2011,2700,2701,2702,2703,2704,2705,2706,2707,2708,2709,2710,2711,2712,2713,4801,4802,4807,4808,4809,4812,4831,4834,4835,4837,4840,4841,4842,4846,4847,4849,4850,4853,4854,4855,4856,4857,4859,4860,4862,4900,4901,4902,4903,4905,4908,2000,4870,4871,03,06,17,30,69,70,71,73,74,75,79,80,82,85,95,96,97,98,13,10,20,24,25,26,27,28,29 Example: `4863` |
| `merchantName` | string | No | Merchant name.   CONDITIONAL: This is required and applicable for filing pre-arbitration and arbitration case.   Length: 0-100   Valid Values/Format: Alphanumeric and Special Characters (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 100 Example: `test name` |
| `violationCode` | string | No | Violation code.   CONDITIONAL: This is only applicable and mandatory in case of pre-compliance and compliance types of cases   Length: 1-20   Valid Values/Format: Alphanumeric and Special Character (.) Max: 20 Example: `D.2` |
| `violationDate` | string | No | Violation Date.   CONDITIONAL: This is only applicable and mandatory in case of pre-compliance and compliance types of cases.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Max: 10 Example: `2021-01-15` |
| `chargebackDate` | string | No | Chargeback Date.   CONDITIONAL:  This is only applicable and mandatory in case of pre-compliance and compliance types of cases with a violation code of 1.4.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Max: 10 Example: `2021-02-15` |
| `creditDate` | string | No | Credit Date.   CONDITIONAL:  This is only applicable and mandatory in case of pre-compliance and compliance types of cases with a violation code of 1.4.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Max: 10 Example: `2021-02-15` |
| `caseFilingEbdfDocuments` | [CaseFilingEbdfStructure](#casefilingebdfstructure) | No |  |

### UpdateCaseRequest

**Type:** object

**Required fields:** `action`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `action` | string enum: [ACCEPT, REJECT, REBUT, ESCALATE, WITHDRAW, DOC_RETRY] | Yes | Action to be performed on case.   CONDITIONAL: ESCALATE is applicable on pre compliance and pre arbitration cases.  NOTE: Senders and receivers cannot provide REJECT for arbitration cases.  NOTE: Senders and receivers cannot provide REBUT for arbitration cases.  Length: 5-8   Valid Values/Format: ACCEPT, REJECT, REBUT, ESCALATE, WITHDRAW, DOC_RETRY. Example: `REJECT` |
| `fileAttachment` | [DocumentStructure](#documentstructure) | No |  |
| `memo` | string | No | Memo pertaining to the case.   CONDITIONAL:  This field is mandatory and applicable if the action code is ACCEPT, REJECT, REBUT or DOC_RETRY.   Length: 0-100   Valid Values/Format: Alphanumeric Max: 100 Example: `This is a test memo` |
| `rebuttedAs` | string enum: [SND, REC] | No | Rebutted as Sender or Receiver.   CONDITIONAL: This field is mandatory and applicable if the action code is REBUT.   Length: 3   Valid Values/Format: SND,REC Example: `SND` |
| `docRetryAs` | string enum: [SND, REC] | No | Uploading document as Sender or Receiver.   CONDITIONAL: This field is mandatory and applicable, if the action code is DOC_RETRY and not applicable for any Actions.   Length: 3   Valid Values/Format: SND,REC Example: `SND` |

### CreateFeeRequest

**Type:** object

**Required fields:** `creditReceiver`, `creditSender`, `currency`, `destinationMember`, `feeAmount`, `feeDate`, `reason`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `cardAcceptorIdCode` | string | No | Merchant Id associated with this fee collection..if any.   Length: 1-15   Valid Values/Format: Alphanumeric Example: `Test1234` |
| `cardNumber` | string | No | Card number when required by the reason code.   Length: 1-19   Valid Values/Format: Numeric Max: 19 Example: `510001000000134` |
| `countryCode` | string | No | Code identifying the country.   Length: 3   Valid Values/Format: A-Z (Uppercase alphabetic letters) Max: 3 Example: `USA` |
| `currency` | string | Yes | Currency of the fee.   Length: 3   Valid Values/Format: A-Z (Uppercase alphabetic letters) Max: 3 Example: `USD` |
| `feeDate` | string (YYYY-MM-DD) | Yes | Date the fee was attached to the claim.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Example: `2024-02-15` |
| `destinationMember` | string | Yes | Destination member for the fee collection.   Length: 1-6   Valid Values/Format: Numeric Example: `002083` |
| `feeAmount` | string | Yes | Amount of the fee.   Length: 4-9   Valid Values/Format: Numeric Example: `100.00` |
| `creditSender` | boolean | Yes | Credit the Sender   Length: 4-5   Valid Values/Format: true / false Example: `true` |
| `creditReceiver` | boolean | Yes | Credit the Receiver.   Length: 4-5   Valid Values/Format: true / false Example: `false` |
| `mastercomControlNumber` | string | No | Used in routing chargeback and retrieval documentation. It contain either a MasterCom endpoint suffix, in case of 2 characters, or a full MasterCom endpoint in case of 7 characters. When mastercomControlNumber is present, it cannot be all spaces or all zeros.   Length: 1-7   Valid Values/Format: Numeric Example: `1589457` |
| `message` | string | No | Message regarding fee..   Length: 1-100   Valid Values/Format: Alphanumeric, Special Char (~!@#$%^&*()_+{}\|:"<>?,./;'[]-=) Max: 100 Example: `This is a test message` |
| `reason` | string | Yes | Collection Reason Code.   Length: 1-4   Valid Values/Format: Numeric Max: 4 Example: `7604` |
| `settlementDate` | string | No | CONDITIONAL: settlementDate is a conditional field and is required for Argentina and Uruguay's Settlement Service participation ID codes (LA00003201, LA00003202, LA00085801, LA00085802, LA00084011, LA00084012). The date may not be prior to the current date or beyond 90 days from the current date.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Example: `2025-10-26` |
| `replyFeeId` | string | No | Fee identifier to be sent to reply to created Fee.   CONDITIONAL: This field is only applicable, if feeType is REPLY to an existing created feeId.   Length: 1-19   Valid Values/Format: Numeric Example: `300009520876` |
| `feeType` | string | No | The fee type.   The default value is CREATE   Valid Values/Format: CREATE, REPLY. Example: `REPLY` |

### CreateFeeRequestSingle

**Type:** object

**Required fields:** `acquirerCustomerId`, `conditionIndicator`, `controlNumber`, `functionCode`, `handlingFee`, `reasonCode`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `acquirerCustomerId` | string | Yes | Acquirer ICA number that is taken from the transaction and supplied by Single Message Transaction Manager.   Length: 1-6   Valid Values/Format: Numeric Example: `003501` |
| `conditionIndicator` | string enum: [A, B, C, D] | Yes | Condition Indicator identifies the Message Reason Code (MRC) for a chargeback..   Length: 1   Valid Values/Format: A, B, C, D Example: `A` |
| `controlNumber` | string | Yes | Allows the issuer to identify the progressive handling fee message being created. The control number must be 20 positions in length and right-justified, zero-filled if less.   Length: 1-20   Valid Values/Format: Alphanumeric Example: `12345678901234567890` |
| `functionCode` | string enum: [700] | Yes | Determines whether the progressive handling fee is being applied to a First Chargeback.   Length: 3   Valid Values/Format: 700 Example: `700` |
| `handlingFee` | string | Yes | Any monetary amount can be entered in this field. There is a USD 25 limit for the First Chargeback (Reason Code 22).   Length: 4-9   Valid Values/Format: Numeric Example: `25` |
| `reasonCode` | string enum: [22] | Yes | Reason code for applying handling fee.   Length: 2   Valid Values/Format: 22 - First Chargeback Handling Fee Example: `22` |
| `declineDate` | string | No | This is the date the authorization request was declined. This field is required when conditionIndicator is A.   Length: 6   Valid Values/Format: Date(MMDDYY) Example: `013019` |
| `issuerCustomerID` | string | No | Issuer ICA number that is taken from the transaction and supplied by Single Message Transaction Manager.   Length: 1-6   Valid Values/Format: Numeric Example: `123456` |

### ChargebackDetails

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `currency` | string | No | The chargeback currency.  The data should be standard currency alpha code Max: 3 Example: `USD` |
| `createDate` | string | No | This is the date of the chargeback creation Example: `2021-02-12` |
| `documentIndicator` | string | No | Document Indicator Example: `true` |
| `messageText` | string | No | Member message text to be used for the chargeback Max: 100 Example: `This is a test message text` |
| `amount` | string | No | Chargeback Amount Example: `100.00` |
| `reasonCode` | string | No | Chargeback Reason Code Max: 12 Example: `4853` |
| `isPartialChargeback` | boolean | No | Indicates a partial chargeback Example: `false` |
| `chargebackType` | string enum: [CHARGEBACK, SECOND_PRESENTMENT] | No | Provide the chargeback type.  The following values are valid - CHARGEBACK, SECOND_PRESENTMENT Example: `CHARGEBACK` |
| `chargebackId` | string | No | Identifier assigned to the Chargeback Example: `25859113` |
| `claimId` | string | No | Claim identifier Example: `200002020654` |
| `reversed` | boolean | No | Indicates this chargeback has been reversed Example: `false` |
| `reversal` | boolean | No | Indicates this chargeback is a reversal chargeback Example: `false` |
| `chargebackRefNum` | string | No | Contains card issuer reference data for a specific cardholder transaction. This number must be unique within BIN. It is used to track the chargeback throughout its life cycle Example: `9000000006` |
| `documentStatus` | string | No | The document status on chargebacks is helpful for customers to identify the chargebacks that need documents to be uploaded within the stipulated 8 days limit for uploading documentation after chargeback creation. Example: `COMPLETED` |
| `reconciliationAmount` | string | No | Reconciliation amount of the chargeback type. Amount will only be retrieved by the receiver side of the dispute cycle. Example: `61.64` |
| `reconciliationCurrency` | string | No | Reconciliation currency of the chargeback type. Currency will only be retrieved by the receiver side of the dispute cycle. Example: `USD` |
| `rejectReason` | string | No | Reason for the reject.  Mastercom specific rejects include:     __5000__ - Merchant providing refund/reversal or acquirer providing refund/reversal on behalf of merchant.     __5001__ - Merchant providing voucher or acquirer providing voucher on behalf of merchant. <br>   __5002__ - Rejected by First-Party Trust program due to compelling evidence.     __5050__ - Amount error has occurred due to an adjustment. Please review claim for edits.  This reject is generated by MDS only.  In the event that the issuer receives this reject reason code, the system automatically updates the claim value to the latest position.     __5051__ - Invalid action. Prior record processed or service not allowed. Generated by MDS only.   Switch rejects are as described in the IPM Clearing Format error numbers and messages manual. Max: 512 Example: `5002` |
| `editExclusionCode` | string | No | Edit exclusion code to bypass clearing system edits. Valid Values - Y ,B0,B1,B2,B3,B4,B5,B6,B7,B8,B9,BA,BB,BC,BD,BE,BF,BG,BH,BI,BJ,BK,BL,BM,BN,BO,BP,BQ,BR,BS,BT,BU,BV,BW,BX,BY,BZ,SPACES. Example: `BO` |
| `refundNotReceivedIndicator` | string | No | Cardholder/Issuer did not receive refund when a first chargeback was rejected by Collaboration with reason code 5000 indicating refund provided. 20 days after rejection of CB through collaboration. This field is only applicable if chargebackType is CHARGEBACK. Valid values are: true, false. Example: `true` |
| `creditVoucherStatus` | string | No | The actual status of the credit voucher Example: `Credit Voucher Accepted` |
| `currencyConversionAssessmentCCAIncluded` | string | No | Currency Conversion Assessment amount applied for full first chargeback, to indicate, if Currency Conversion Assessment was included or not for qualified transactions. Example: `true` |
| `currencyConversionAssessmentCCAAmount` | string | No | Currency Conversion Assessment amount Fee associated with full first chargeback. Example: `0.20 USD` |
| `japanCommonMerchantCode` | string | No | Identifies the merchant's category in Japan referred to as the Common Merchant Category Code (CMC). Mastercard uses this value to identify a link to a corresponding Mastercard Assigned ID. Example: `0410` |
| `mexicoDomesticTaxAmount` | [MexicoDomesticTaxAmount](#mexicodomestictaxamount) | No |  |
| `mexicoDomesticTransactionFeeAmount` | [MexicoDomesticTransactionFeeAmount](#mexicodomestictransactionfeeamount) | No |  |
| `mexicoDomesticSettlementFeesAndVat` | [MexicoDomesticSettlementFeesAndVat](#mexicodomesticsettlementfeesandvat) | No |  |
| `installmentData` | string | No | Provides information about the installment payment option selected by the cardholder at the point of interaction. Example: `1261610E81023498764532103` |
| `flexCode` | string | No | Specific to Brazil Flex Card transactions to communicate the product code used for clearing. Example: `003` |
| `acknowledgeFirstPartyTrustEvidence` | string | No | Issuer has acknowledged First-Party Trust evidence and proceeded with the chargeback. Example: `true` |

### FeeDetails

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `cardAcceptorIdCode` | string | No | Merchant Id associated with this fee collection..if any Example: `Test ID` |
| `cardNumber` | string | No | Card number when required by the reason code Max: 19 Example: `510001000000134` |
| `countryCode` | string | No | Code identifying the country Max: 3 Example: `USA` |
| `currency` | string | No | Currency of the fee Max: 3 Example: `USD` |
| `feeDate` | string (YYYY-MM-DD) | No | Date the fee was attached to the claim Example: `2021-02-13` |
| `destinationMember` | string | No | Destination member for the fee collection Example: `002083` |
| `feeId` | string | No | Identifier assigned to the fee Example: `20002052146` |
| `feeAmount` | string | No | Amount of the fee.    - When the amount is a credit to the sender of the fee collection, the value of feeAmount is positive.    - When the amount is a debit to the sender of the fee collection, the value of feeAmount is negative.    - When the amount is a credit to the receiver of the fee collection, the value of feeAmount is negative.    - When the amount is a debit to the receiver of the fee collection, the value of feeAmount is positive.    See also the creditSender and creditReceiver parameters. Example: `100.00` |
| `creditSender` | string | No | Credit the Sender Example: `true` |
| `creditReceiver` | string | No | Credit the Receiver Example: `false` |
| `message` | string | No | Message regarding fee Max: 95 Example: `This is a test message` |
| `reason` | string | No | Collection Reason Code Max: 95 Example: `7604` |
| `rejectReason` | string | No | Fee reject reason. Max: 512 Example: `Code1=0142(00):D0063/002;DE072=D0063\\8000000808\\\\` |
| `chargebackRefNum` | string | No | Contains card issuer reference data for a specific cardholder transaction. This number must be unique within BIN. It is used to track the chargeback throughout its life cycle Example: `9000000006` |
| `reconciliationAmount` | string | No | Reconciliation amount of the fee. Amount will only be retrieved by the receiver side of the fee Example: `20.25` |
| `reconciliationCurrency` | string | No | Reconciliation currency of the fee. Currency will only be retrieved by the receiver side of the fee Example: `EUR` |
| `japanCommonMerchantCode` | string | No | Identifies the merchant's category in Japan referred to as the Common Merchant Category Code (CMC). Mastercard uses this value to identify a link to a corresponding Mastercard Assigned ID. Example: `0410` |
| `mexicoDomesticTaxAmount` | [MexicoDomesticTaxAmount](#mexicodomestictaxamount) | No |  |
| `mexicoDomesticTransactionFeeAmount` | [MexicoDomesticTransactionFeeAmount](#mexicodomestictransactionfeeamount) | No |  |
| `mexicoDomesticSettlementFeesAndVat` | [MexicoDomesticSettlementFeesAndVat](#mexicodomesticsettlementfeesandvat) | No |  |
| `installmentData` | string | No | Provides information about the installment payment option selected by the cardholder at the point of interaction. Example: `1261610E81023498764532103` |
| `flexCode` | string | No | Specific to Brazil Flex Card transactions to communicate the product code used for clearing. Example: `003` |

### HealthCheckResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `status` | string | No |  Example: `true` |

### getQueueContentRequest

**Type:** object

**Required fields:** `queueName`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `queueName` | string | Yes | The queue to be queried for a list of claims.   Length: 1-30   Valid Values/Format: Alpha Example: `Rejects` |
| `lastModifiedDateFrom` | string | No | Start of claim’s last modified date range.   CONDITIONAL: If lastModifiedDateFrom is provided then lastModifiedDateTo is required.   Length: 16   Valid Values/Format: Date (yyyy-MM-ddTHH:mm) Example: `2025-08-22T12:01` |
| `lastModifiedDateTo` | string | No | End of claim’s last modified date range.   CONDITIONAL: If lastModifiedDateTo is provided then lastModifiedDateFrom is required.   Length: 16   Valid Values/Format: Date (yyyy-MM-ddTHH:mm) Example: `2025-08-22T12:01` |
| `pageNb` | string | No | The queue data will be retrieved in separate sets.  The pageNb field indicates which page should be returned.  The total page counts available in a date range will be returned in the pageCount field. Possible values are 1,2,3 etc. If page number is not provided, value will default to 1.   Length: 1-3   Valid Values/Format: Numeric Example: `1` |

### reconReportDataAcknowledgeRequest

**Type:** object

**Required fields:** `endDate`, `startDate`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `ica` | array of string | No | Interbank Card Association number used to identify the member in transaction.   Length: 1-11   Valid Values/Format: Numeric Example: `000001,000002` |
| `startDate` | string | Yes | Start date for the reconciliation report.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Example: `2021-02-15` |
| `endDate` | string | Yes | End date for the reconciliation report.   Length: 10   Valid Values/Format: Date (yyyy-MM-dd) Example: `2021-02-16` |
| `cycles` | array of [cycle](#cycle) | No | It represents the cycle(s) where the clearing data was exchanged.   Length: 1   Valid Values/Format: 1, 2, 3, 4, 5, 6, 7 |
| `enhancedReconciliationReportFlag` | boolean | No | It is set to "true" because the enhanced reconciliation report is available.   Length: 4-5   Valid Values/Format: true / false Example: `false` |

### reconReportDataAcknowledgeResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `reportIdentifier` | string | No | A 36 bit UUID identifier that identifies the current reconciliation report generation request. Max: 36 Example: `123e4567-e89b-42d3-a456-556642440000` |

### reconReportDataRetrivalResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `status` | string | No | Status of polling request. This can be Available, Unavailable or Failed. Failed status occurs if the data processing underwent some kind of error leading to unavailability of the report. Example: `Available` |
| `data` | string | No | Base64 encoded String containing binary data for the CSV document. Decode the field to get a byte array that can be converted into a CSV file or String/Stream Example: `This is an CSV file stored in a base64 encoded string` |

### cycle

Optional cycle values from 1 to 7.   Length: 1   Valid Values/Format: 1, 2, 3, 4, 5, 6, 7

**Type:** integer

**Allowed Values:** 1, 2, 3, 4, 5, 6, 7

### TransactionSingleMessageDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `authTransaction` | [SingleMessageDetail](#singlemessagedetail) | No |  |
| `clearingTransaction` | [SingleMessageDetail](#singlemessagedetail) | No |  |

### TransactionSingleMessageSummaryList

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `transactionSummaryList` | array of [TransactionSingleMessageSummary](#transactionsinglemessagesummary) | No |  |

### TransactionSingleMessageSummary

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `authTransactionId` | string | No | Authorization Transaction Identifier Example: `hqCnaMDqmto4wnL+BSUKSdzROqGJ7YELoKhEvluycwKNg3XTzSfaIJhFDkl9hW081B5tTqFFiAwCpcoc` |
| `clearingTransactionId` | string | No | Authorization Transaction Identifier Example: `ccCnaMDqmto4wnL+BSUKSdzROqGJ7YELoKhEvluycwKNg3XTzSfaIJhFDkl9hW081B5tTqFFiAwCpcoc` |
| `singleMessageSummaryDetails` | [SingleMessageSummaryDetails](#singlemessagesummarydetails) | No |  |

### SingleMessageSummaryDetails

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `authTransaction` | [SingleMessageSummary](#singlemessagesummary) | No |  |
| `clearingTransaction` | [SingleMessageSummary](#singlemessagesummary) | No |  |

### SingleMessageSummary

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `acquirerReferenceNumber` | string | No | Contains the acquirers reference number. Example: `05413364365000000000667` |
| `adviceReasonCode` | string | No | Used to inform processors an action has been taken on the advice messages. Example: `4890071` |
| `brand` | string | No | The brand of service or program.  Acceptable values...MC - Mastercard, CI - Cirrus®, MS - Maestro®, MD - Debit Mastercard ®card, PL - Plus®, PV(L) - Private Label, VI - VISA Example: `MD` |
| `localCurrencyCode` | string | No | The code defining the currency of the transaction as it was submitted to the Single Message System. The Single Message System uses it to specify the currency used in localRequestedAmount Example: `840` |
| `localRequestedAmount` | string | No | The amount of funds requested by the cardholder in the local currency of the acquirer or source location of the transaction. Example: `15.00` |
| `merchantName` | string | No | For POS acquirers this is the name of the merchant owning the POS terminal. For ATM acquirers this the ATM owning institution name. Example: `Test Merchant` |
| `merchantType` | string | No | Card Acceptor Business Code. Identifies the card acceptor’s primary business. Example: `6011` |
| `primaryAccountNumber` | string | No | Series of digits used to identify a customer account or relationship. Example: `52751494691484000` |
| `processingCode` | string | No | Series of digits used to describe the effect of a transaction on the customer account and the type of accounts affected. Example: `010000` |
| `responseCode` | string | No | This field displays the disposition of a message. Example: `91` |
| `responseSource` | string | No | M = The decline was initiated by the member/issuer, S = The decline was initiated by the Single Message System. Example: `M` |
| `settlementDate` | string | No | Date Mastercard uses to group the transactions for reporting and for subsequent settlement: Format: MMDDYY Example: `051018` |
| `switchSerialNumber` | string | No | Describes a unique transaction identification number generated (or assigned) by the Single Message System Example: `543532313` |
| `switchDateTime` | string | No | Date and time Mastercard routes the transaction to the issuer  (MMDDHHMMSS) Example: `062614093022` |
| `trace` | string | No | Unique identifier assigned to each transaction by the originator of the message Example: `999998` |
| `tranType` | string | No | Tran Type Example: `C` |

### SingleMessageDetail

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `acqLocCntry` | string | No | Acquirer Country Example: `USA` |
| `acquirerAddress` | string | No | Acquirer Address City Example: `NEWYORK` |
| `acquirerAdjustmentSettlementAmount` | string | No | Acquirer Adjustment Settlement Amount Example: `100.00` |
| `acquirerAdjustmentSettlementAmountIndicator` | string | No | Acquirer Adjustment Settlement Amount Indicator Example: `CR` |
| `acquirerAdjustmentSettlementCompletionAmount` | string | No | Acquirer Adjustment Settlement Completion Amount Example: `100.00` |
| `acquirerAdjustmentSettlementCurrency` | string | No | Acquirer Adjustment Settlement Currency Example: `840` |
| `acquirerAdviceReason` | string | No | Acquirer Advice Reason Code Example: `4000000` |
| `acquirerBridgedICA` | string | No | Acquirer Bridged ICA Example: `999663` |
| `acquirerCity` | string | No | Acquirer Address City Example: `NEWYORK` |
| `acquirerCurrencyConversionRate` | string | No | Acquirer Currency Conversion Rate Example: `10011` |
| `acquirerInstitutionNumber` | string | No | Displays the routing and transit number of the customer Example: `1900053380` |
| `acquirerInterchangeAmount` | string | No | Acquirer Interchange Amount Example: `0.00` |
| `acquirerInterchangeCurrency` | string | No | Acquirer Interchange Currency Example: `840` |
| `acquirerName` | string | No | Debit Mastercard field only. Acquirer Name is a card acceptor identification code assigned by the merchant. Example: `SampleName` |
| `acquirerProcessorID` | string | No | Acquirer processor number Example: `9000001607` |
| `acquirerReferenceNumber` | string | No | Contains the acquirers reference number. Example: `05413364365000000000667` |
| `acquirerSettlementCompletionAmount` | string | No | Acquirer Settlement Completion Amount Example: `100.00` |
| `acquirerSettlementCompletionCurrency` | string | No | Acquirer Settlement Completion Currency Example: `840` |
| `acquirerSettlementConversionRate` | string | No | Acquirer Settlement Conversion Rate Example: `11000000` |
| `additionalAdvice` | string | No | Additional Advice Example: `000` |
| `additionalPOSData` | string | No | Additional POS Data Example: `12452234` |
| `additionalResponse` | string | No | A code that provides additional supporting data pertaining to the response code associated with the transaction Example: `0111111` |
| `adjustmentAdviceReason` | string | No | Adjustment Advice Reason Example: `4000000` |
| `adjustmentCashbackAmount` | string | No | Adjustment Cashback Amount Example: `100.00` |
| `adjustmentDate` | string | No | Date the adjustment is performed. YYMMDD Example: `140626` |
| `adjustmentPurchaseAmount` | string | No | Adjustment Purchase Amount Example: `100.00` |
| `afaMember` | string | No | AFA Member Example: `Y` |
| `alternatePrimaryAccountNumber` | string | No | Alternate Primary Account Number Example: `5555555555555555` |
| `amountICCR` | string | No | Amount ICCR Example: `100.00` |
| `atmPosFlag` | string | No | ATM Pos Flag Example: `Y` |
| `banknetReferenceNumber` | string | No | Banknet Reference Number Example: `U68FRGDFA` |
| `bridgingICA` | string | No | Bridging ICA Example: `543267` |
| `businessActivity` | string | No | Business Activity Example: `ACT` |
| `cashBackAmount` | string | No | Cash Back Amount Example: `10.00` |
| `cashBackCurrency` | string | No | Cash Back Currency Example: `840` |
| `catLevel` | string | No | This field indicates the specific conditions present at the point of service (POS) at the time that a transaction takes place. Example: `2` |
| `cccaIssuerBankName` | string | No | For Mexico domestic credit card cash advance ATM transactions Example: `005020examplebank` |
| `chipFlag` | string | No | Indicates if chip was present or not. Example: `N` |
| `conditionCode` | string | No | Code that describes the PAN entry, PIN entry, and authorization mode of a transaction. Example: `9C0` |
| `corporateCardIndicator` | string | No | Corporate Card Indicator Example: `N` |
| `creditLineUsageFee` | string | No | For Mexico domestic credit card cash advance ATM transactions. Example: `0040110760000100` |
| `crossBorderIndicator` | string | No | Any transaction on a credit or debit card branded by Mastercard processed through the Single Message System in which the cardholder country code differs from the merchant country code Example: `N` |
| `currencyConversionAssesementAmount` | string | No | Currency Conversion Assessment amount Example: `0.00` |
| `currencyConversionAssesementCurrency` | string | No | Currency Conversion Assesement Currency Example: `840` |
| `currencyConversionIndicator` | string | No | Compares the currency of the cardholder with the merchant’s country currency when the cardholder performs a transaction in another country Example: `Y` |
| `cvc2ProgramValidationCode` | string | No | Debit Mastercard field only. A value of C indicates that the merchant participates in the Mastercard CVC 2 Validation Program Example: `C` |
| `documentIndicator` | string | No | The document Indicator field indicates whether documentation is sent supporting the chargeback Example: `1` |
| `feesInterChgAcqLoc` | string | No | Fees Interchange Acquirer Location Example: `123456789` |
| `financialInstitutionID` | string | No | Financial Institution ID Example: `AA` |
| `fraudDate` | string | No | Fraud Date Example: `010119` |
| `fraudDeviceType` | string | No | Fraud Device Type Example: `1` |
| `fraudType` | string | No | Fraud Type Example: `AA` |
| `gcmsAdviceCode` | string | No | Advice reason code that identifies the Global Clearing Management System (GCMS) message reason code when an exception item is processed Example: `7806` |
| `gcmsSettlementDate` | string | No | GCMS Settlement Date Example: `180624` |
| `issuerAdjustmentSettlementAmount` | string | No | Issuer Adjustment Settlement Amount Example: `100.00` |
| `issuerAdjustmentSettlementAmountIndicator` | string | No | Issuer Adjustment Settlement Amount Indicator Example: `DR` |
| `issuerAdjustmentSettlementCompletionAmount` | string | No | Issuer Adjustment Settlement Completion Amount Example: `100.00` |
| `issuerAdjustmentSettlementCurrency` | string | No | Issuer Adjustment Settlement Currency Example: `840` |
| `issuerAdviceReason` | string | No | Issuer Advice Reason Code. Example: `4900085` |
| `issuerCurrencyConversionRate` | string | No | Conversion rate used to convert amounts from the local transaction currency to the settlement currency. Example: `11000000` |
| `issuerICA` | string | No | Issuer ICA Example: `728345` |
| `issuerInstitutionNumber` | string | No | Issuer institution ID Displays the routing and transit number of the customer. If the customer has no routing and transit number, i.e., the institution is International, then the number displayed is the pseudo number assigned by Mastercard. Example: `1900053380` |
| `issuerInterchangeAmount` | string | No | Issuer Interchange Amount Example: `100.00` |
| `issuerInterchangeCurrency` | string | No | Issuer Interchange Currency Example: `840` |
| `issuerProcessorID` | string | No | Issuer processor number Example: `9000001608` |
| `issuerSettlementCompletionAmount` | string | No | The settlement amount is displayed in the issuers chosen settlement currency, as processed in the Single Message System Example: `10.00` |
| `issuerSettlementCompletionCurrency` | string | No | Current issuer settlement currency code. Example: `840` |
| `issuerSettlementConversionRate` | string | No | Issuer Settlement Conversion Rate Example: `11000000` |
| `localCompletionAmount` | string | No | The monetary value appearing in this field represents the original completed amount of the transaction expressed in local currency. Example: `10.00` |
| `localCurrencyCode` | string | No | The code defining the currency of the transaction as it was submitted to the Single Message System. The Single Message System uses it to specify the currency used in localRequestedAmount Example: `840` |
| `localRequestedAmount` | string | No | The amount of funds requested by the cardholder in the local currency of the acquirer or source location of the transaction. Example: `15.00` |
| `mcElectronicIndicator` | string | No | MC Electronic Indicator Example: `A` |
| `mcResponseValue` | string | No | MC Response Value Example: `AA` |
| `merchantCategoryCodeInfo` | string | No | Merchant Category Code Info Example: `ABCD` |
| `merchantType` | string | No | Card Acceptor Business Code. Identifies the card acceptor’s primary business. Example: `6011` |
| `originalCardHolderBillingAmount` | string | No | Amount the cardholder is billed by the issuing institution. The Cardholder Billing Amount is displayed with the local currency code and the decimal positioning of the local currency code. Example: `10.00` |
| `originalCardHolderBillingCurrency` | string | No | Original CardHolder Billing Currency Example: `840` |
| `originalCashbackAmount` | string | No | Original Cashback Amount Example: `100.00` |
| `originalCashbackCurrency` | string | No | Original Cashback Currency Example: `840` |
| `originalPurchaseAmount` | string | No | This field is displayed only for partial approval transactions. The monetary value appearing in this field represents the original transaction amount expressed in local currency. Example: `100.00` |
| `ownerID` | string | No | Owner ID Example: `s060972` |
| `pointOfServiceAmount` | string | No | Point Of Service Amount Example: `100.00` |
| `pointOfServiceCurrency` | string | No | Point Of Service Currency Example: `840` |
| `posData` | string | No | Displays the contents of Point of Service [POS] Data, which contain terminal and other Point of Service information Example: `101001000150084048111-1234` |
| `posEntry` | string | No | Indicates the method used to enter the PAN into the terminal device and the PIN entry capability of that device. Example: `901` |
| `primaryAccountNumber` | string | No | Series of digits used to identify a customer account or relationship. Example: `52751494691484000` |
| `primaryAccountNumberSequenceNumber` | string | No | Primary Account Number Sequence Number Example: `5555666677778888` |
| `processingCode` | string | No | Series of digits used to describe the effect of a transaction on the customer account and the type of accounts affected. Example: `010000` |
| `productIdentifierCode` | string | No | Provides issuers additional information about Product ID, also known as product code Example: `MCD` |
| `programIndicator` | string | No | A special Debit Mastercard promotion program code displayed from GCMS. Example: `Q` |
| `qpsPayPassChargebackElgibility` | string | No | Debit Mastercard field only. A value of I indicates that the transaction is not eligible for a chargeback with reason codes 0001, 0002 or 0037 Example: `I` |
| `referenceNumber` | string | No | The retrieval reference number from the original transaction is typically printed on the cardholders transaction receipt. This field is populated by the acquirer. Example: `DMC20100` |
| `responseCode` | string | No | This field displays the disposition of a message. Example: `91` |
| `responseSource` | string | No | M = The decline was initiated by the member/issuer, S = The decline was initiated by the Single Message System. Example: `M` |
| `serviceCode` | string | No | This field displays the extended service code of Track 2 data indicating the transaction acceptance parameters of a magnetic stripe card. When paired with the POS Entry Mode, acquirers will be able to validate whether the issuer is properly utilizing the Chip Liability Shift chargeback Example: `101` |
| `serviceLevelIndicator` | string | No | Service Level Indicator Example: `ABC` |
| `settlementDate` | string | No | Date (month and day) that Mastercard uses to group the transactions for reporting and for subsequent settlement: Format: MMDDYY Example: `051018` |
| `settlementDatePosition` | string | No | Settlement Date Position Example: `010119` |
| `settlementServiceConfiguration` | string | No | Settlement Service Configuration (SSC) records define the business day and time of settlement for a transaction. The Settlement Service Configuration Indicator (SSC ID) describes which Single Message System Settlement Service record was used for the transaction.�� The SSC ID is assigned to a transaction based on a combination of the following values: product, interchange type, ISIS agreement, and region. Example: `000` |
| `surchargeFreeIndicator` | string | No | This field will identify an eligible transaction with the a specific identifier of the related program.� Transactions that are not eligible will be identified with value N. Example: `N` |
| `switchDateTime` | string | No | Date and time Mastercard routes the transaction to the issuer  (MMDDHHMMSS) Example: `062614093022` |
| `switchSerialNumber` | string | No | Describes a unique transaction identification number generated (or assigned) by the Single Message System Example: `543532313` |
| `switchSerialNumberPosition` | string | No | Switch Serial Number Position Example: `123455` |
| `switchTime` | string | No | Time that Mastercard routes the transaction to the issuer Example: `093022` |
| `terminalID` | string | No | The Terminal ID is the unique identification number assigned by the acquirer to each terminal Example: `M1_I1 8` |
| `trace` | string | No | Unique identifier assigned to each transaction by the originator of the message Example: `999998` |
| `transactionCategoryCode` | string | No | Transaction Category Code Example: `Z` |
| `transactionClass` | string | No | Displays the Transaction Class. For transactions submitted to the Single Message System, this field consists of the following: the source product type, the entry device, and fee classification Example: `MS ATM DOM` |
| `transactionDateTime` | string | No | Date and time a message was transmitted by a processing entity, to be expressed in Coordinated Universal Time (UTC). Example: `010129` |
| `transitData` | string | No | Transit Data Example: `ABCDE` |
| `tranType` | string | No | Tran Type Example: `A` |
| `universalCardAuthenticationFee` | string | No | Universal Cardholder Authentication Field (UCAF) displays data systems used to communicate authentication information among cardholder, issuer, merchant, and acquirer communities Example: `0` |
| `usageCode` | string | No | Chargeback and retrieval request usage codes. The usage code indicates the type of chargeback record processed. Example: `1` |

### InstallmentParameters

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `deferredGracePeriodFlag` | string | No | Issuer and merchant grace period option Max: 1 Example: `1` |
| `deferredGracePeriodMonths` | string | No | Number of months of the deferred period selected by the cardholder Max: 1 Example: `0` |
| `issuerPromotion` | string | No | Indicates the Issuer/Acquirer agreed promotion installment plan was selected by the cardholder to complete the transaction Max: 1 Example: `1` |

### OriginalInformationInstallments

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `amountOfPurchase` | string | No | Amount of original purchase Max: 12 Example: `1234` |
| `transactionInterestRate` | string | No | Percent rate for transaction Max: 4 Example: `1.5` |
| `installmentAmount` | string | No | Monthly amount for transaction Max: 12 Example: `90.25` |

### CaseFilingClaimsRequest

**Type:** object

**Required fields:** `caseFilingList`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `caseFilingList` | array of [CaseFilingIdSourceRequest](#casefilingidsourcerequest) | Yes | A list of case filing ids, maximum list size is 2000 |

### CaseFilingIdSourceRequest

**Type:** object

**Required fields:** `caseId`, `isIssuer`

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `caseId` | string | Yes | The case filing id.   Length: 1-19   Valid Values/Format: Numeric Example: `536092` |
| `isIssuer` | boolean | Yes | In a case filling context, if 'true' the caller is on the sender side, if 'false' on the receiver side.   Length: 4-5   Valid Values/Format: true / false Example: `true` |

### CaseFilingClaimsResponse

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `caseFilingResponseList` | array of [CaseFilingClaim](#casefilingclaim) | No | A list of case ids and its associated claim ids. |

### CaseFilingClaim

**Type:** object

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `caseId` | string | No | The case filing id Example: `536092` |
| `claimId` | string | No | The associated claimId. Example: `200000001234` |

---

## Request/Response Examples

### CreateCaseType1

Create Case for caseType 1
```json
{
  "caseType": "1",
  "chargebackRefNum": [
    "1111423456"
  ],
  "customerFilingNumber": "5482",
  "disputeAmount": "200.00",
  "currencyCode": "USD",
  "dueDate": "2021-02-20",
  "filedAgainstIca": "004321",
  "filingAs": "A",
  "filingIca": "001234",
  "memo": "This is a test memo",
  "messageText": "This is a test message",
  "changeReasonCodeFlag": "Y",
  "updatedChargebackReasonCode": "4863",
  "changeReasonCodeReason": "This is a test reason",
  "primaryAccountNum": "5123123412341234",
  "acquirerRefNum": "05131054165000000048149",
  "chargebackReasonCode": "4863",
  "merchantName": "test name",
  "violationCode": "D.2",
  "violationDate": "2021-01-15",
  "chargebackDate": "2021-02-15",
  "creditDate": "2021-02-15"
}
```

### CreateCaseType2

Create Case for caseType 2
```json
{
  "caseType": "2",
  "chargebackRefNum": [
    "1111423456"
  ],
  "customerFilingNumber": "5482",
  "disputeAmount": "200.00",
  "currencyCode": "USD",
  "dueDate": "2021-02-20",
  "filedAgainstIca": "004321",
  "filingAs": "A",
  "filingIca": "001234",
  "memo": "This is a test memo",
  "messageText": "This is a test message",
  "changeReasonCodeFlag": "Y",
  "updatedChargebackReasonCode": "4863",
  "changeReasonCodeReason": "This is a test reason",
  "primaryAccountNum": "5123123412341234",
  "acquirerRefNum": "05131054165000000048149",
  "chargebackReasonCode": "4863",
  "merchantName": "test name",
  "violationCode": "D.2",
  "violationDate": "2021-01-15",
  "chargebackDate": "2021-02-15",
  "creditDate": "2021-02-15"
}
```

### CreateCaseType3

Create Case for caseType 3
```json
{
  "caseType": "3",
  "chargebackRefNum": [
    "1111423456",
    "2222123456"
  ],
  "customerFilingNumber": "5482",
  "disputeAmount": "200.00",
  "currencyCode": "USD",
  "dueDate": "2021-02-20",
  "filedAgainstIca": "004321",
  "filingAs": "A",
  "filingIca": "001234",
  "memo": "This is a test memo",
  "primaryAccountNum": "5123123412341234",
  "acquirerRefNum": "05131054165000000048149",
  "merchantName": "test name",
  "violationCode": "D.2",
  "violationDate": "2021-01-15",
  "chargebackDate": "2021-02-15",
  "creditDate": "2021-02-15"
}
```

### CreateCaseType4

Create Case for caseType 4
```json
{
  "caseType": "4",
  "chargebackRefNum": [
    "1111423456",
    "2222123456"
  ],
  "customerFilingNumber": "5482",
  "disputeAmount": "200.00",
  "currencyCode": "USD",
  "dueDate": "2021-02-20",
  "filedAgainstIca": "004321",
  "filingAs": "A",
  "filingIca": "001234",
  "memo": "This is a test memo",
  "primaryAccountNum": "5123123412341234",
  "acquirerRefNum": "05131054165000000048149",
  "merchantName": "test name",
  "violationCode": "D.2",
  "violationDate": "2021-01-15",
  "chargebackDate": "2021-02-15",
  "creditDate": "2021-02-15"
}
```

### CreateCaseEBDFDocRequest

Create Case with EBDF Document
```json
{
  "caseType": "2",
  "chargebackRefNum": [
    "1111423456"
  ],
  "customerFilingNumber": "5482",
  "disputeAmount": "200.00",
  "currencyCode": "USD",
  "dueDate": "2021-02-20",
  "filedAgainstIca": "004321",
  "filingAs": "A",
  "filingIca": "001234",
  "memo": "This is a test memo",
  "messageText": "This is a test message",
  "changeReasonCodeFlag": "Y",
  "updatedChargebackReasonCode": "4863",
  "changeReasonCodeReason": "This is a test reason",
  "primaryAccountNum": "5123123412341234",
  "acquirerRefNum": "05131054165000000048149",
  "chargebackReasonCode": "4863",
  "merchantName": "test name",
  "violationCode": "D.2",
  "violationDate": "2021-01-15",
  "chargebackDate": "2021-02-15",
  "creditDate": "2021-02-15",
  "caseFilingEbdfDocuments": {
    "expeditedBillingDrfDocument": {
      "cardholderName": "Test Cardholder name",
      "acquirerRefData": "05158764165000000084682",
      "transactionDate": "16-Dec-2020",
      "transactionAmount": "100.00",
      "disputeDescription": "Test Dispute Description",
      "certification": "Test Certification",
      "chargebackRepresentative": "Test Representative"
    },
    "smsLinkedCaseFilingDrfDocument": {
      "contactName": "Test Contact Name",
      "companyOrInstitution": "Test Company Name",
      "contactEmail": "reply@acme.com",
      "reasonForFilingCase": "Test Reason for Filing Case",
      "processorId": "999696",
      "acquirerSwitchSerialNum": "05131054165000000048149",
      "transactionOrSettlementDate": "16-Dec-2020",
      "numberOfTransactions": "1"
    }
  }
}
```

### CreateCaseFileAttachmentRequest

Create Case with fileAttachment
```json
{
  "caseType": "2",
  "chargebackRefNum": [
    "1111423456"
  ],
  "customerFilingNumber": "5482",
  "disputeAmount": "200.00",
  "currencyCode": "USD",
  "dueDate": "2021-02-20",
  "filedAgainstIca": "004321",
  "filingAs": "A",
  "filingIca": "001234",
  "memo": "This is a test memo",
  "messageText": "This is a test message",
  "changeReasonCodeFlag": "Y",
  "updatedChargebackReasonCode": "4863",
  "changeReasonCodeReason": "This is a test reason",
  "primaryAccountNum": "5123123412341234",
  "acquirerRefNum": "05131054165000000048149",
  "chargebackReasonCode": "4863",
  "merchantName": "test name",
  "violationCode": "D.2",
  "violationDate": "2021-01-15",
  "chargebackDate": "2021-02-15",
  "creditDate": "2021-02-15",
  "fileAttachment": {
    "filename": "testimage111111.tif",
    "file": "This is an image file stored in a base64 encoded string"
  }
}
```

### UpdateCaseNoFile

Update Case for action with no fileAttachment
```json
{
  "action": "ACCEPT",
  "memo": "This is a test memo",
  "rebuttedAs": "SND"
}
```

### UpdateCaseFileAttachment

Update Case for DOC_RETRY action with fileAttachment
```json
{
  "action": "DOC_RETRY",
  "fileAttachment": {
    "filename": "testimage111111.tif",
    "file": "This is an image file stored in a base64 encoded string"
  },
  "memo": "This is a test memo",
  "rebuttedAs": "SND",
  "docRetryAs": "SND"
}
```

### CreateCBChargeback

Create chargeback for chargebackType CHARGEBACK
```json
{
  "amount": "100.00",
  "chargebackType": "CHARGEBACK",
  "currency": "USD",
  "documentIndicator": "false",
  "reasonCode": "4853",
  "credPostedAsPurchase": false,
  "isPartialChargeback": false,
  "messageText": "This is a test message text",
  "settlementDate": "2025-10-26",
  "localTax1IVA": "0.60",
  "installmentFee": 1.2,
  "editExclusionCode": "BO",
  "refundNotReceivedIndicator": "true",
  "includeCurrencyConversionAssessmentCCA": "true",
  "acknowledgeFirstPartyTrustEvidence": "false"
}
```

### CreateCBSecondPresentment

Create chargeback for chargebackType SECOND_PRESENTMENT
```json
{
  "amount": "100.00",
  "chargebackType": "SECOND_PRESENTMENT",
  "currency": "USD",
  "documentIndicator": "false",
  "reasonCode": "4853",
  "credPostedAsPurchase": false,
  "isPartialChargeback": false,
  "messageText": "This is a test message text",
  "settlementDate": "2025-10-26",
  "disputeChargebackID": "300045678811",
  "localTax1IVA": "0.60",
  "installmentFee": 1.2,
  "editExclusionCode": "BO"
}
```

### CreateCBEbdfDocs

Create chargeback with EBDF doc
```json
{
  "amount": "100.00",
  "chargebackType": "CHARGEBACK",
  "currency": "USD",
  "documentIndicator": "true",
  "reasonCode": "4853",
  "credPostedAsPurchase": false,
  "isPartialChargeback": false,
  "messageText": "This is a test message text",
  "settlementDate": "2025-10-26",
  "localTax1IVA": "0.60",
  "installmentFee": 1.2,
  "editExclusionCode": "BO",
  "refundNotReceivedIndicator": "true",
  "includeCurrencyConversionAssessmentCCA": "true",
  "acknowledgeFirstPartyTrustEvidence": "false",
  "chargebackEbdfDocuments": {
    "transactionInformation": {
      "acquirerRefDataOrSwitchSerialNum": "05131054165000000048149",
      "merchantName": "Test Merchant Name",
      "transactionOrSettlementDate": "16-Feb-2018",
      "disputedAmount": "100.00"
    },
    "fraudDrfDocument": {
      "numberOfItems": "1",
      "types": "CARD_CLOSED,REPORT_SAFE",
      "additionalInformation": "Test additional information",
      "chargebackRepresentative": "Test representative",
      "cardIssuerRegion": "Europe",
      "cardholderVerificationMethod": {
        "onlinePIN": "1",
        "offlinePIN": "2",
        "signature": "3",
        "none": "4"
      }
    },
    "cardholderDisputeChargebackDrfDocument": {
      "transactionAmount": "100.00",
      "type": "PRODUCT_DISPUTE",
      "deliveryDateOfGoodsOrServices": "18-FEB-2018",
      "expectedDeliveryDateOfGoodOrServices": "18-FEB-2018",
      "returnDate": "18-FEB-2018",
      "cancellationDate": "18-FEB-2018",
      "cardholderParticipation": false,
      "disputeDetails": "Test dispute details",
      "chargebackRepresentative": "Test representative"
    },
    "pointOfInteractionErrorsDrfDocument": {
      "transactionAmount": "20000",
      "type": "DUPLICATE_DEBIT",
      "alternateMeansOfPaymentDetails": "Test alternate means of payment details",
      "disputeDetails": "Test dispute details",
      "chargebackRepresentative": "Test representative"
    }
  }
}
```

### CreateCBFileAttachment

Create chargeback with File Attachment
```json
{
  "amount": "100.00",
  "chargebackType": "CHARGEBACK",
  "currency": "USD",
  "documentIndicator": "true",
  "reasonCode": "4853",
  "credPostedAsPurchase": false,
  "isPartialChargeback": false,
  "messageText": "This is a test message text",
  "settlementDate": "2025-10-26",
  "localTax1IVA": "0.60",
  "installmentFee": 1.2,
  "editExclusionCode": "BO",
  "refundNotReceivedIndicator": "true",
  "includeCurrencyConversionAssessmentCCA": "true",
  "acknowledgeFirstPartyTrustEvidence": "false",
  "fileAttachment": {
    "filename": "testimage111111.tif",
    "file": "This is an image file stored in a base64 encoded string"
  }
}
```

### TxnSearchArnPan

Transaction Search with ARN and PAN
```json
{
  "acquirerRefNumber": "05436847276000293995738",
  "primaryAccountNum": "5488888888887192",
  "transAmountFrom": "10000",
  "transAmountTo": "20050",
  "tranStartDate": "2025-08-22",
  "tranEndDate": "2025-08-22"
}
```

### TxnSearchBnrPan

Transaction Search with BNR and PAN
```json
{
  "bankNetRefNumber": "MPLU68FRG",
  "primaryAccountNum": "5488888888887192",
  "transAmountFrom": "10000",
  "transAmountTo": "20050",
  "tranStartDate": "2025-08-22",
  "tranEndDate": "2025-08-22"
}
```

### TxnSearchArn

Transaction Search with ARN only and dates
```json
{
  "acquirerRefNumber": "05436847276000293995738",
  "transAmountFrom": "10000",
  "transAmountTo": "20050",
  "tranStartDate": "2025-08-22",
  "tranEndDate": "2025-08-22"
}
```

### TxnSearchPan

Transaction Search with PAN only and dates
```json
{
  "primaryAccountNum": "5488888888887192",
  "transAmountFrom": "10000",
  "transAmountTo": "20050",
  "tranStartDate": "2025-08-22",
  "tranEndDate": "2025-08-22"
}
```

### AcquirerFulfillmentCollabFundsMvmtReq

Acquirer Fulfillment after Collaboration start with FUNDS MOVEMENT REQUEST
```json
{
  "acquirerResponseCd": "A",
  "memo": "This is a test memo"
}
```

### AcquirerFulfillmentCollabRefund

Acquirer Fulfillment after Collaboration start with REFUND
```json
{
  "acquirerResponseCd": "B",
  "refundReversalType": "REFUND",
  "refundReversalDate": "2021-02-15T12:01",
  "refundReversalReferenceId": "123458111",
  "memo": "This is a test memo"
}
```

### AcquirerFulfillmentCollabVoucher

Acquirer Fulfillment after Collaboration start with CREDIT VOUCHER
```json
{
  "acquirerResponseCd": "B",
  "refundReversalType": "CREDIT VOUCHER",
  "refundReversalDate": "2021-02-15T12:01",
  "refundReversalAmount": "100.00",
  "refundReversalCurrency": "USD",
  "refundReversalReferenceId": "123458111",
  "memo": "This is a test memo"
}
```

### AcquirerFulfillmentCollabIntentToRefund

Acquirer Fulfillment after Collaboration initiating REFUND
```json
{
  "acquirerResponseCd": "C",
  "memo": "This is a test memo"
}
```

### AcquirerFulfillmentCollabRefundDetailsC

Acquirer Fulfillment after Collaboration respond with REFUND details
```json
{
  "acquirerResponseCd": "C",
  "refundReversalType": "REFUND",
  "refundReversalDate": "2021-02-15T12:01",
  "refundReversalReferenceId": "123458111",
  "memo": "This is a test memo"
}
```

### AcquirerFulfillmentCollabReject

Acquirer Fulfillment after Collaboration respond with REJECTION
```json
{
  "acquirerResponseCd": "E",
  "memo": "This is a test memo"
}
```

### AcquirerFulfillmentCollabHealthcare

Acquirer Fulfillment after Collaboration respond to Healthcare Retrieval Request
```json
{
  "acquirerResponseCd": "F",
  "memo": "This is a test memo"
}
```

### UpdateChargebackFile

Update Chargeback with fileAttachment
```json
{
  "memo": "This is a test memo",
  "fileAttachment": {
    "filename": "testimage111111.tif",
    "file": "This is an image file stored in a base64 encoded string"
  }
}
```

### UpdateChargebackVoucher

Update Chargeback with creditVoucherAction
```json
{
  "memo": "This is a test memo",
  "creditVoucherAction": "ACCEPT"
}
```

### CreateChargebackSingle

Create chargeback (Debit MasterCard and Europe Dual Acquirer)
```json
{
  "brand": "MD",
  "replacementAmount": "100.00",
  "reversalReasonCode": "71",
  "usageCode": "1",
  "acquirerFirstReferenceNumber": "05103246259000000000126",
  "additionalInformation": "SMTM Manual",
  "adjustmentContactFax": "5555555555",
  "adjustmentContactName": "John Smith",
  "adjustmentContactPhone": "5555555555",
  "controlNumber": "99999",
  "dataRecordText": "R3",
  "documentIndicator": "1",
  "documentType": "1",
  "illegibleItemCd": "1",
  "program": "INVAL",
  "retrievalRequestDate": "010129",
  "securityBulletinNumber": "122",
  "refundNotReceivedIndicator": "true",
  "fileAttachment": {
    "filename": "testimage111.tif",
    "file": "This is an image file stored in a base64 encoded string"
  }
}
```

### CreateChargebackSingleType

Create chargeback (Debit MasterCard and Europe Dual Acquirer) with chargebackType
```json
{
  "brand": "MD",
  "replacementAmount": "0.00",
  "reversalReasonCode": "53",
  "usageCode": "1",
  "chargebackType": "S",
  "acquirerFirstReferenceNumber": "05103246259000000000126",
  "additionalInformation": "SMTM Manual",
  "adjustmentContactFax": "5555555555",
  "adjustmentContactName": "John Smith",
  "adjustmentContactPhone": "5555555555",
  "controlNumber": "99999",
  "dataRecordText": "R3",
  "documentIndicator": "1",
  "documentType": "1",
  "illegibleItemCd": "1",
  "program": "INVAL",
  "retrievalRequestDate": "010129",
  "securityBulletinNumber": "122",
  "refundNotReceivedIndicator": "true",
  "fileAttachment": {
    "filename": "testimage111.tif",
    "file": "This is an image file stored in a base64 encoded string"
  }
}
```

### IssuerFulfillmentApprove

Issuer Respond Fulfillment for issuerResponseCd APPROVE
```json
{
  "issuerResponseCd": "APPROVE",
  "memo": "This is a test memo"
}
```

### IssuerFulfillmentReject

Issuer Respond Fulfillment for issuerResponseCd REJECT_DOCUMENTATION_NOT_AS_REQUIRED or REJECT_ILLEGIBLE_OR_MISSING
```json
{
  "issuerResponseCd": "REJECT_DOCUMENTATION_NOT_AS_REQUIRED",
  "memo": "This is a test memo",
  "rejectReasonCd": "A"
}
```

