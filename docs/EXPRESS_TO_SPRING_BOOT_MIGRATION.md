# Express API Server → Spring Boot Migration Guide

This document maps every Express API server capability to its Spring Boot equivalent.
The frontend team can use this to update their API calls and remove the Express middleware.

## Quick Summary

| Express Capability | Spring Boot Status | Notes |
|---|---|---|
| Dispute CRUD proxy | **Ready** | Direct endpoints, no proxy needed |
| Agent call proxy | **Ready** | Direct endpoints at `/api/agents/` |
| Mastercom API proxy | **Ready** | Direct endpoints at `/api/mastercard/` |
| Ethoca API proxy | **Ready** | Direct endpoints at `/api/ethoca/` |
| Enrichment pipeline | **Ready** | `POST /api/agents/enrich/{id}` runs full 6-step flow |
| Recommendation engine | **Ready** | `POST /api/agents/recommend/{id}` with confidence scores |
| Supplement store | **Ready** | `PATCH /api/disputes/{id}` for field updates |
| Evidence file serving | **Ready** | `GET /api/disputes/{id}/sources` |
| Reason code rules | **Ready** | `GET /api/agents/reason-codes` and `GET /api/agents/reason-codes/{code}` |
| CORS | **Ready** | Configured for all `/api/**` paths |

---

## Frontend Configuration Change

Update Vite proxy to point directly at Spring Boot:

```javascript
// vite.config.ts — BEFORE (through Express)
server: {
  proxy: {
    '/api': 'http://localhost:3001'  // Express server
  }
}

// vite.config.ts — AFTER (direct to Spring Boot)
server: {
  proxy: {
    '/api': 'http://localhost:5000'  // Spring Boot
  }
}
```

---

## Endpoint Mapping

### 1. Dispute CRUD

| Operation | Express Route | Spring Boot Route | Method |
|---|---|---|---|
| List all | `GET /api/disputes` | `GET /api/disputes` | Same |
| Get one | `GET /api/disputes/:id` | `GET /api/disputes/{id}` | Same |
| Create | `POST /api/disputes` | `POST /api/disputes` | Same |
| Update (partial) | `PATCH /api/disputes/:id` | `PATCH /api/disputes/{id}` | **New** |
| Delete | `DELETE /api/disputes/:id` | `DELETE /api/disputes/{id}` | **New** |
| Get with sub-details | — | `GET /api/disputes/{id}/details` | **New** |
| Fetch from Mastercard | — | `POST /api/disputes/{id}/fetch-details` | **New** |

**PATCH /api/disputes/{id}** — Replaces the Express supplement store. Accepts any of these fields:
```json
{
  "merchantCategory": "Electronics",
  "customerEmail": "user@example.com",
  "cardNumber": "****3456",
  "evidenceFileId": "EV-001",
  "merchantName": "...",
  "cardholderName": "...",
  "itemDescription": "...",
  "disputeType": "...",
  "action": "...",
  "status": "...",
  "amount": 99.99,
  "currency": "USD"
}
```

### 2. AI Agents

| Agent | Express Route | Spring Boot Route | Method |
|---|---|---|---|
| Issuer Summary | `POST /api/agents/summarize/:id` | `POST /api/agents/summarize/{disputeId}` | Same |
| Batch Summary | `POST /api/agents/summarize/batch` | `POST /api/agents/summarize/batch` | Same |
| Summary Status | `GET /api/agents/summarize/status/:id` | `GET /api/agents/summarize/status/{disputeId}` | Same |
| Queue Status | `GET /api/agents/summarize/queue` | `GET /api/agents/summarize/queue` | Same |
| Evidence Enrichment | `POST /api/agents/enrich/:id` | `POST /api/agents/enrich/{disputeId}` | Same |
| Merchant Summary | `POST /api/agents/merchant-summary/:id` | `POST /api/agents/merchant-summary/{disputeId}` | Same |
| Recommendation | `POST /api/agents/recommend/:id` | `POST /api/agents/recommend/{disputeId}` | Same |
| Rebuttal | `POST /api/agents/rebuttal/:id` | `POST /api/agents/rebuttal/{disputeId}` | Same |
| Full Pipeline | `POST /api/agents/full-pipeline/:id` | `POST /api/agents/full-pipeline/{disputeId}` | Same |
| Evidence Map | `GET /api/agents/evidence-map/:id` | `GET /api/agents/evidence-map/{disputeId}` | Same |
| Agent Status | `GET /api/agents/status` | `GET /api/agents/status` | Same |
| Policy Diff | `POST /api/agents/policy/diff` | `POST /api/agents/policy/diff` | Same |
| Policy Analyze | `POST /api/agents/policy/analyze` | `POST /api/agents/policy/analyze` | Same |
| Analytics Insights | `GET /api/agents/analytics/insights` | `GET /api/agents/analytics/insights` | Same |
| Analytics Stats | `GET /api/agents/analytics/statistics` | `GET /api/agents/analytics/statistics` | Same |

### 3. Enrichment Pipeline (formerly Express-only)

The Express enrichment pipeline (reason code analysis, evidence retrieval, 6-step flow) is fully implemented in Spring Boot's `EvidenceStrategistAgent`. One call does everything:

```
POST /api/agents/enrich/{disputeId}
```

This runs:
1. Identifies the case number and loads reason code rules
2. Scans all 7 evidence source categories (merchant, shipping, PSP, identity, device, fraud-tools, customer-comms)
3. Generates a fetch plan (required vs available vs missing)
4. Sends all evidence content through PII scrubbing
5. Calls Gemini AI for deep content analysis (strength ratings, key findings)
6. Produces an annotated evidence map with completeness score and winning strategy

Response includes `fetchPlan` and `annotatedMap` JSON strings.

### 4. Recommendation Engine (formerly Express-only)

The Express recommendation engine is fully implemented in Spring Boot's `ChallengeAdvisorAgent`:

```
POST /api/agents/recommend/{disputeId}
```

Returns a structured JSON recommendation with:
- `recommendation`: STRONG_CHALLENGE / MODERATE_CHALLENGE / WEAK_CHALLENGE / ACCEPT
- `confidence`: numeric score (0-100)
- `estimatedWinProbability`: percentage
- `strengthFactors` and `weaknessFactors`: lists of evidence points
- `reasoning`: detailed explanation
- `suggestedActions`: steps to improve the case

### 5. Evidence Source Data (formerly Express-only)

Spring Boot now serves local evidence JSON files directly:

| Operation | Spring Boot Route |
|---|---|
| All sources for a dispute | `GET /api/disputes/{id}/sources` |
| Sources by category | `GET /api/disputes/{id}/sources/{category}` |

Categories: `merchant`, `shipping`, `psp`, `identity`, `device`, `fraud-tools`, `customer-comms`

### 6. Reason Code Rules (formerly Express-only)

| Operation | Spring Boot Route |
|---|---|
| All reason codes | `GET /api/agents/reason-codes` |
| Specific reason code | `GET /api/agents/reason-codes/{code}` |

### 7. Mastercom v6 API

All 26 Mastercard API proxy endpoints are under `/api/mastercard/`. No changes needed — same routes.

### 8. Ethoca API

All 5 Ethoca endpoints are under `/api/ethoca/`. No changes needed — same routes.

### 9. Ingestion

| Operation | Spring Boot Route |
|---|---|
| Trigger ingestion | `POST /api/ingestion/ingest` |
| Ingestion status | `GET /api/ingestion/status` |
| Paginated disputes | `GET /api/ingestion/disputes?page=0&size=50` |
| Available queues | `GET /api/ingestion/queues` |

---

## Supplement Store Fields

The Express server maintained a separate JSON store for fields the backend didn't have.
These fields are now first-class columns on the `Dispute` entity and can be read/written via the standard dispute endpoints:

| Field | Read via | Write via |
|---|---|---|
| `merchantCategory` | `GET /api/disputes/{id}` | `PATCH /api/disputes/{id}` |
| `customerEmail` | `GET /api/disputes/{id}` | `PATCH /api/disputes/{id}` |
| `cardNumber` | `GET /api/disputes/{id}` | `PATCH /api/disputes/{id}` |
| `evidenceFileId` | `GET /api/disputes/{id}` | `PATCH /api/disputes/{id}` |
| `merchantName` | `GET /api/disputes/{id}` | `PATCH /api/disputes/{id}` |
| `cardholderName` | `GET /api/disputes/{id}` | `PATCH /api/disputes/{id}` |
| `itemDescription` | `GET /api/disputes/{id}` | `PATCH /api/disputes/{id}` |

---

## Steps to Remove Express

1. Update Vite proxy target from Express port to `http://localhost:5000`
2. Remove any Express-specific response transformations in frontend code (if any)
3. Delete the Express artifact (`artifacts/api-server` or equivalent)
4. Remove the Express workflow
5. Test all frontend flows end-to-end against Spring Boot directly

---

## Spring Boot Server Details

- **Port**: 5000
- **CORS**: All origins allowed on `/api/**`
- **Content-Type**: `application/json`
- **Error format**: `{ "error": "message" }` or `{ "status": "ERROR", "error": "message" }`
