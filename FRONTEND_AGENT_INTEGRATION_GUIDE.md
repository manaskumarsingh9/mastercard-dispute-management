# Frontend Integration Guide — AI Agents

Base URL: `https://14c4d4fe-3a81-4b0e-a199-f1f4d5147f31-00-bhxfmiflhthj.pike.replit.dev`

All agent endpoints are under `/api/agents/`.

---

## Architecture Overview

There are **6 agents** total. They follow a sequential pipeline for each dispute:

```
Claim Arrives (getQueueSummaryPost)
       │
       ▼
  ┌─────────────────────┐
  │  AGENT 1             │  Auto-triggered when new claims appear in dashboard
  │  Case Summarizer     │  Generates an issuer-side plain-language summary
  └──────────┬──────────┘
             │ summary stored on dispute.issuerSummary
             ▼
  ┌─────────────────────┐
  │  AGENT 2             │  User clicks "Enrich Evidence" button
  │  Evidence Strategist │  Plans what evidence is needed → fetches it → annotates results
  └──────────┬──────────┘
             │ creates/updates EvidenceMap record
             ▼
  ┌─────────────────────┐
  │  AGENT 3a            │  Auto-runs after enrichment OR user clicks "Merchant Summary"
  │  Merchant Summary    │  Summarizes merchant-side evidence in readable format
  └──────────┬──────────┘
             │
             ▼
  ┌─────────────────────┐
  │  AGENT 4             │  Auto-runs after enrichment OR user clicks "Get Recommendation"
  │  Challenge Advisor   │  Recommends: STRONG_CHALLENGE / MODERATE / WEAK / ACCEPT
  └──────────┬──────────┘
             │ user reviews recommendation
             ▼
  ┌─────────────────────┐
  │  AGENT 3b            │  User clicks "Generate Rebuttal" (only after confirming challenge)
  │  Rebuttal Architect  │  Generates a formal rebuttal document for submission
  └──────────┬──────────┘
             │
             ▼
     Submit to Mastercard (existing Mastercom API endpoints)
```

**Agents 5 and 6 are standalone** (not part of the dispute pipeline):
- **Agent 5 (Policy Intelligence)** — Admin uploads policy docs for analysis
- **Agent 6 (Analytics Insight)** — Dashboard analytics and AI-powered insights

---

## Agent 1 — Case Summarizer

### Trigger
**Auto-trigger from the frontend** when the dashboard receives new claim records via the "Fetch Disputes" button (which calls `getQueueSummaryPost` under the hood).

### How It Works — Step by Step

**Step 1: User clicks "Fetch Disputes" button in the frontend.**

Frontend calls the ingestion endpoint:
```
POST /api/ingestion/ingest
```
```json
{
  "queueName": "Pending",
  "lastModifiedDateFrom": "2026-03-28T00:00",
  "lastModifiedDateTo": "2026-04-01T12:00"
}
```

**Step 2: Backend deduplicates automatically.**

The backend checks every claim by `claimId`:
- If a claim with that `claimId` already exists in the database → **update** it (no duplicate created)
- If the `claimId` is new → **insert** it and track its `disputeId`

This means the user can click the button as many times as they want — the same claims will never be duplicated.

**Step 3: Backend returns the response with `newDisputeIds`.**

```json
{
  "success": true,
  "newClaims": 3,
  "updatedClaims": 150,
  "skipped": 0,
  "errors": [],
  "newDisputeIds": [68733, 68734, 68735]
}
```

Key fields:
- `newClaims` — count of claims that were genuinely new (not seen before)
- `updatedClaims` — count of claims that already existed and were refreshed
- `newDisputeIds` — **the exact internal IDs of newly created disputes** (only new ones, never duplicates)

If the user clicks the button again with the same date range:
```json
{
  "success": true,
  "newClaims": 0,
  "updatedClaims": 153,
  "skipped": 0,
  "errors": [],
  "newDisputeIds": []
}
```
`newDisputeIds` is empty — nothing new to summarize.

**Step 4: Frontend queues Agent 1 for all new disputes at once.**

The backend uses an internal queue that processes summarizations **one at a time** to prevent overload. The frontend should send all IDs in a single batch call, then poll for results.

```javascript
// After ingestion response
const result = await fetch('/api/ingestion/ingest', { method: 'POST', body: ... });
const data = await result.json();

// Queue ALL new disputes in one batch call — backend handles sequencing
if (data.newDisputeIds.length > 0) {
  await fetch('/api/agents/summarize/batch', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ disputeIds: data.newDisputeIds })
  });
}
```

**Step 5: Frontend polls for individual results.**

```javascript
// Poll a specific dispute's summarization status
async function checkSummaryStatus(disputeId) {
  const res = await fetch(`/api/agents/summarize/status/${disputeId}`);
  const job = await res.json();
  // job.status is one of: "QUEUED", "PROCESSING", "COMPLETED", "FAILED"
  if (job.status === 'COMPLETED') {
    return job.summary; // Markdown text
  }
  return null; // Still processing — poll again in a few seconds
}

// Poll overall queue status
async function checkQueueStatus() {
  const res = await fetch('/api/agents/summarize/queue');
  return await res.json();
  // { queueDepth: 5, queued: 3, processing: 1, completed: 10, failed: 0, totalTracked: 14 }
}
```

### Endpoints

**Queue a single dispute:**
```
POST /api/agents/summarize/{disputeId}
```
No request body needed. `disputeId` is the internal database ID (the `id` field from the `disputes` table, NOT the Mastercard `claimId`).

**Queue multiple disputes at once (recommended):**
```
POST /api/agents/summarize/batch
```
```json
{
  "disputeIds": [68733, 68734, 68735, 68736]
}
```

**Check status of a single dispute:**
```
GET /api/agents/summarize/status/{disputeId}
```

**Check overall queue status:**
```
GET /api/agents/summarize/queue
```

### Response — Queue Request (202 Accepted)
Both single and batch endpoints return `202 Accepted` (not `200`):
```json
{
  "agent": "CaseSummarizer",
  "disputeId": 40816,
  "status": "QUEUED",
  "queuedAt": "2026-04-01T10:30:00Z",
  "message": "Summarization request queued. Poll GET /api/agents/summarize/status/40816 for result."
}
```

### Response — Batch Queue (202 Accepted)
```json
{
  "agent": "CaseSummarizer",
  "totalQueued": 4,
  "jobs": [
    { "disputeId": 68733, "status": "QUEUED", "queuedAt": "2026-04-01T10:30:00Z" },
    { "disputeId": 68734, "status": "QUEUED", "queuedAt": "2026-04-01T10:30:00Z" },
    { "disputeId": 68735, "status": "QUEUED", "queuedAt": "2026-04-01T10:30:00Z" },
    { "disputeId": 68736, "status": "QUEUED", "queuedAt": "2026-04-01T10:30:00Z" }
  ],
  "message": "All requests queued. Poll GET /api/agents/summarize/queue for overall status."
}
```

### Response — Job Status (200 OK)
```json
{
  "disputeId": 40816,
  "status": "COMPLETED",
  "queuedAt": "2026-04-01T10:30:00Z",
  "startedAt": "2026-04-01T10:30:05Z",
  "completedAt": "2026-04-01T10:30:18Z",
  "summary": "### Chargeback Claim Summary\n\n**Overview**\nA new chargeback has been raised for a transaction amounting to $616.34 USD...\n\n**Cardholder's Claim**\n...\n\n**Key Facts**\n* Claim ID: 200002038279\n* Claim Amount: $616.34 USD\n...\n\n**Deadlines & Important Dates**\n* Response Due Date: April 3, 2026\n\n**Notable Observations**\n..."
}
```
The `summary` field only appears when `status` is `"COMPLETED"`. When `status` is `"FAILED"`, an `error` field appears instead.

### Response — Queue Status (200 OK)
```json
{
  "queueDepth": 3,
  "queued": 3,
  "processing": 1,
  "completed": 10,
  "failed": 0,
  "totalTracked": 14
}
```

### Response (500 Error)
```json
{
  "error": "error message"
}
```

### Frontend Implementation Notes
- **IMPORTANT**: Summarization now uses a queue. The POST endpoint returns `202 Accepted` immediately — it does NOT wait for the summary to complete.
- The `summary` field (when completed) contains **Markdown-formatted** text. Render it with a Markdown renderer.
- Summary is saved to the dispute record automatically. On subsequent page loads, read it from `dispute.issuerSummary` (via `GET /api/disputes/{id}`) — no need to re-call Agent 1.
- If the summary is `null` or `"Summary generation unavailable"`, show a fallback message and allow manual re-trigger.
- **Recommended UX**: After queueing, show a progress bar or counter: "Summarizing 4 disputes... (1/4 complete)". Poll `GET /api/agents/summarize/queue` every 5 seconds.
- Duplicate requests for the same dispute ID are automatically ignored if already queued or processing.
- As a fallback, you can also detect unsummarized disputes by checking `issuerSummary === null` on dispute records from `GET /api/ingestion/disputes`.

---

## Agent 2 — Evidence Strategist

### Trigger
User clicks an **"Enrich Evidence"** button on the dispute detail page.

### Endpoint
```
POST /api/agents/enrich/{disputeId}
```

### Request
No request body needed. Uses the internal `disputeId`.

### Response (200 OK)
```json
{
  "agent": "EvidenceStrategist",
  "disputeId": 40816,
  "claimId": "200002038279",
  "enrichmentStatus": "COMPLETED",
  "fetchPlan": "{\"reasonCodeCategory\":\"other\",\"standardEvidence\":[\"authorization_record\",\"transaction_details\",...],\"dynamicEvidence\":[\"delivery_proof\",...],\"reasoning\":\"...\",\"priorityOrder\":[...],\"winningStrategy\":\"...\"}",
  "annotatedMap": "{\"evidenceItems\":[{\"type\":\"authorization_record\",\"status\":\"PRESENT\",\"strength\":\"STRONG\",\"source\":\"PSP_ADAPTER\",\"summary\":\"...\",\"impact\":\"...\"},...],\"completenessScore\":75,\"overallAssessment\":\"...\",\"manualUploadSuggestions\":[...],\"criticalGaps\":[...]}"
}
```

### Important: Parsing the Response
Both `fetchPlan` and `annotatedMap` are **JSON strings** (stringified JSON inside a JSON field). You need to `JSON.parse()` them on the frontend.

#### fetchPlan structure (after parsing):
```json
{
  "reasonCodeCategory": "fraud|product_not_received|product_not_as_described|duplicate|subscription|other",
  "standardEvidence": ["authorization_record", "transaction_details", ...],
  "dynamicEvidence": ["delivery_proof", "tracking_info", ...],
  "reasoning": "explanation text...",
  "priorityOrder": ["3ds_authentication", "authorization_record", ...],
  "winningStrategy": "brief strategy description"
}
```

#### annotatedMap structure (after parsing):
```json
{
  "evidenceItems": [
    {
      "type": "authorization_record",
      "status": "PRESENT",
      "strength": "STRONG",
      "source": "PSP_ADAPTER",
      "summary": "Authorization code 'AUTH5549' with 'Approved' response...",
      "impact": "Strongly validates the transaction's approval..."
    },
    {
      "type": "signature_confirmation",
      "status": "MISSING",
      "strength": "N_A",
      "source": "N/A",
      "summary": "No signature confirmation document available...",
      "impact": "Weakens the delivery defense..."
    }
  ],
  "completenessScore": 75,
  "overallAssessment": "The merchant has strong evidence for...",
  "manualUploadSuggestions": ["Upload signed delivery receipt", ...],
  "criticalGaps": ["signature_confirmation", "terms_of_service"]
}
```

### Frontend Implementation Notes
- **This is the slowest call** — can take 30-60 seconds because it makes 2 Gemini calls (plan + verify) plus evidence fetching. Show a progress indicator.
- Display evidence items as a table/cards:
  - Status: PRESENT (green) / MISSING (red)
  - Strength: STRONG (green) / MODERATE (yellow) / WEAK (red) / N_A (grey)
- Show `completenessScore` as a progress bar (0-100)
- Show `criticalGaps` prominently — these are what the merchant should try to provide manually
- Show `manualUploadSuggestions` as action items the merchant can address
- The dispute status changes to `"ENRICHED"` after this call
- **Re-calling** this endpoint will re-run enrichment (overwrites previous results)

---

## Agent 3a — Merchant Summary

### Trigger
- Auto-runs as part of the full pipeline, OR
- User clicks **"View Merchant Summary"** after enrichment is done

### Prerequisites
Agent 2 (enrichment) must be completed first. If not, returns an error.

### Endpoint
```
POST /api/agents/merchant-summary/{disputeId}
```

### Request
No request body needed.

### Response (200 OK)
```json
{
  "agent": "MerchantSummary",
  "disputeId": 40816,
  "merchantSummary": "**Merchant Summary: Claim ID 200002038279**\n\nThis summary outlines the evidence gathered...\n\n### Merchant's Position\n...\n\n### Evidence Supporting the Merchant\n...\n\n### Evidence Gaps\n...\n\n### Overall Evidence Strength\n..."
}
```

### Response (500 — Enrichment Not Done)
```json
{
  "error": "Evidence map not found for claim: 200002038279. Run enrichment first."
}
```

### Frontend Implementation Notes
- `merchantSummary` is **Markdown-formatted**. Render with a Markdown renderer.
- This is a read-friendly narrative for the merchant — no structured JSON to parse.
- Saved to the EvidenceMap automatically. Can be re-read via `GET /api/agents/evidence-map/{disputeId}`.

---

## Agent 4 — Challenge Advisor

### Trigger
- Auto-runs as part of the full pipeline, OR
- User clicks **"Get Recommendation"** after enrichment

### Prerequisites
Agent 2 (enrichment) must be completed first.

### Endpoint
```
POST /api/agents/recommend/{disputeId}
```

### Request
No request body needed.

### Response (200 OK)
```json
{
  "agent": "ChallengeAdvisor",
  "disputeId": 40816,
  "recommendation": "{\"recommendation\":\"MODERATE_CHALLENGE\",\"confidence\":75,\"reasoning\":\"...\",\"strengthFactors\":[\"...\",\"...\"],\"weaknessFactors\":[\"...\",\"...\"],\"missingEvidenceImpact\":\"...\",\"estimatedWinProbability\":70,\"suggestedActions\":[\"...\"]}"
}
```

### recommendation structure (after JSON.parse):
```json
{
  "recommendation": "MODERATE_CHALLENGE",
  "confidence": 75,
  "reasoning": "detailed explanation...",
  "strengthFactors": [
    "Successful 3D Secure authentication with liability shift",
    "Strong AVS and CVV verification matches",
    "Delivery proof with signature"
  ],
  "weaknessFactors": [
    "Missing chargeback reason code",
    "No terms of service documentation",
    "No customer service interaction logs"
  ],
  "missingEvidenceImpact": "The absence of the reason code means...",
  "estimatedWinProbability": 70,
  "suggestedActions": [
    "Obtain the specific chargeback reason code",
    "Provide terms of service documentation",
    "Upload any customer service interaction logs"
  ]
}
```

### Frontend Implementation Notes
- The `recommendation` field is a **JSON string** — parse it with `JSON.parse()`.
- Display the recommendation prominently with color coding:
  - `STRONG_CHALLENGE` → Green badge
  - `MODERATE_CHALLENGE` → Yellow/amber badge
  - `WEAK_CHALLENGE` → Orange badge
  - `ACCEPT` → Red badge
- Show `confidence` as a percentage
- Show `estimatedWinProbability` as a percentage
- Display `strengthFactors` and `weaknessFactors` as two-column lists
- Show `suggestedActions` as a checklist
- **This is advisory only** — always include a clear "This is a recommendation. You make the final decision." message.
- Based on the recommendation, show either:
  - A "Challenge — Generate Rebuttal" button (for STRONG/MODERATE/WEAK)
  - An "Accept Chargeback" button (for ACCEPT)

---

## Agent 3b — Rebuttal Architect

### Trigger
User clicks **"Generate Rebuttal"** — only after reviewing Agent 4's recommendation and confirming they want to challenge. **Human-in-the-loop**: never auto-trigger this.

### Prerequisites
Agent 2 (enrichment) must be completed first.

### Endpoint
```
POST /api/agents/rebuttal/{disputeId}
```

### Request
No request body needed.

### Response (200 OK)
```json
{
  "agent": "RebuttalArchitect",
  "disputeId": 40816,
  "rebuttalDocument": "## CHARGEBACK REBUTTAL DOCUMENT\n\n**Case Reference:**\n* Claim ID: 200002038279\n* Reason Code: Not Provided\n* Amount: 616.34 USD\n\n**Executive Summary:**\n...\n\n**Response to Cardholder's Claim:**\n...\n\n**Supporting Evidence:**\n1. Authorization Record...\n2. 3D Secure Authentication...\n...\n\n**Conclusion:**\n..."
}
```

### Frontend Implementation Notes
- `rebuttalDocument` is **Markdown-formatted**. Render with Markdown renderer.
- Provide a "Copy to Clipboard" button so merchants can copy the rebuttal text.
- Provide a "Download as PDF" option if possible.
- After generation, the dispute status changes to `"REBUTTAL_READY"`.
- Show a final confirmation: "Submit this rebuttal to Mastercard?" with a submit button that calls the existing Mastercom chargeback/case filing endpoints.
- **Do NOT auto-submit** the rebuttal to Mastercard. The merchant must explicitly confirm.

---

## Full Pipeline (One-Click Analyze)

Runs Agents 1 → 2 → 3a → 4 in sequence with a single API call. Useful for a "Quick Analysis" button.

### Endpoint
```
POST /api/agents/full-pipeline/{disputeId}
```

### Response (200 OK)
```json
{
  "disputeId": 40816,
  "claimId": "200002038279",
  "pipelineStatus": "COMPLETED",
  "step1_issuerSummary": "### Chargeback Claim Summary...",
  "step2_fetchPlan": "{...json string...}",
  "step2_annotatedMap": "{...json string...}",
  "step3a_merchantSummary": "**Merchant Summary...**",
  "step4_recommendation": "{...json string...}",
  "note": "Rebuttal generation (step 3b) requires merchant confirmation. Call POST /api/agents/rebuttal/40816 when ready."
}
```

### Frontend Implementation Notes
- This takes **60-120 seconds**. Show a multi-step progress indicator:
  - Step 1: "Generating case summary..." 
  - Step 2: "Enriching evidence..." 
  - Step 3: "Analyzing merchant evidence..."
  - Step 4: "Generating recommendation..."
- Parse `step2_fetchPlan`, `step2_annotatedMap`, and `step4_recommendation` with `JSON.parse()`.
- Rebuttal (step 3b) is intentionally excluded — requires merchant confirmation.

---

## Stored Evidence Map (Read-Only)

### Endpoint
```
GET /api/agents/evidence-map/{disputeId}
```

### Response (200 OK)
Returns the full EvidenceMap entity:
```json
{
  "id": 1,
  "claimId": "200002038279",
  "disputeId": 40816,
  "fetchPlan": "{...}",
  "annotatedMap": "{...}",
  "merchantSummary": "...",
  "challengeRecommendation": "{...}",
  "rebuttalDocument": "...",
  "enrichmentStatus": "COMPLETED",
  "recommendationStrength": "MODERATE_CHALLENGE",
  "enrichmentRequestedAt": "2026-04-01T10:30:00",
  "enrichmentCompletedAt": "2026-04-01T10:31:15",
  "recommendationGeneratedAt": "2026-04-01T10:31:45",
  "rebuttalGeneratedAt": "2026-04-01T10:32:30"
}
```

### Response (404)
```json
{
  "error": "No evidence map found for dispute 12345"
}
```

Use this to check if enrichment has already been done, and to display saved results without re-running agents.

---

## Agent 5 — Policy Intelligence (Admin Only)

### Trigger
Admin uploads or pastes policy document text.

### Endpoints

**Compare two policy versions:**
```
POST /api/agents/policy/diff
```
```json
{
  "previousPolicy": "full text of the old policy version...",
  "newPolicy": "full text of the new policy version..."
}
```
Response:
```json
{
  "agent": "PolicyIntelligence",
  "type": "diff",
  "analysis": "## Summary of Changes\n...\n## High-Impact Changes\n...\n## Suggested Configuration Updates\n..."
}
```

**Analyze a single policy document:**
```
POST /api/agents/policy/analyze
```
```json
{
  "policyDocument": "full text of the policy document...",
  "networkName": "Mastercard"
}
```
Response:
```json
{
  "agent": "PolicyIntelligence",
  "type": "analysis",
  "networkName": "Mastercard",
  "analysis": "## Key Deadlines and SLAs\n...\n## Required Evidence Types\n...\n## Common Pitfalls\n..."
}
```

### Frontend Implementation Notes
- Both `analysis` fields contain **Markdown-formatted** text.
- These are admin-level features — put them in a settings or admin section.
- Allow text paste or file upload (extract text from PDF on the frontend before sending).

---

## Agent 6 — Analytics Insight

### Trigger
User navigates to the analytics/dashboard page.

### Endpoints

**AI-generated insights:**
```
GET /api/agents/analytics/insights
```
Response:
```json
{
  "agent": "AnalyticsInsight",
  "insights": "## Key Metrics Overview\n...\n## Trend Analysis\n...\n## Reason Code Insights\n...\n## Risk Alerts\n...\n## Recommendations\n..."
}
```

**Raw statistics (for charts):**
```
GET /api/agents/analytics/statistics
```
Response:
```json
{
  "totalDisputes": 66232,
  "openDisputes": 1000,
  "closedDisputes": 0,
  "sampleSize": 1000,
  "byQueue": {
    "Pending": 699,
    "Rejects": 301
  },
  "byReasonCode": {},
  "byStatus": {
    "NEW": 1000
  },
  "byNetwork": {
    "GCMS": 1000
  }
}
```

### Frontend Implementation Notes
- `insights` is **Markdown-formatted** text.
- Use `statistics` data to build charts (pie charts for `byQueue`, `byReasonCode`; bar charts for `byStatus`, `byNetwork`; top-level metric cards for totals).
- The statistics endpoint is fast (no AI call). The insights endpoint takes 10-20 seconds (Gemini call).

---

## Agent Status Check

### Endpoint
```
GET /api/agents/status
```

### Response
```json
{
  "geminiAvailable": true,
  "agents": {
    "agent1_caseSummarizer": "ACTIVE",
    "agent2_evidenceStrategist": "ACTIVE",
    "agent3a_merchantSummary": "ACTIVE",
    "agent3b_rebuttalArchitect": "ACTIVE",
    "agent4_challengeAdvisor": "ACTIVE",
    "agent5_policyIntelligence": "ACTIVE",
    "agent6_analyticsInsight": "ACTIVE"
  }
}
```

### Frontend Implementation Notes
- Call this on app startup to verify agents are available.
- If `geminiAvailable` is `false`, disable AI features and show a notification: "AI features are temporarily unavailable."
- Can be used for a health indicator in the admin panel.

---

## Dispute Status Lifecycle

As agents run, the `status` field on the dispute entity changes:

```
NEW → (Agent 1 runs, no status change) → ENRICHED → REBUTTAL_READY → (submit to MC)
```

| Status | Meaning | Set By |
|--------|---------|--------|
| `NEW` | Freshly ingested from Mastercard queue | Ingestion |
| `ENRICHED` | Agent 2 completed evidence enrichment | Agent 2 |
| `REBUTTAL_READY` | Agent 3b generated a rebuttal document | Agent 3b |

---

## Error Handling

All agent endpoints return `500` with this structure on failure:
```json
{
  "error": "descriptive error message"
}
```

Common errors:
- `"Dispute not found: {id}"` — invalid disputeId
- `"Evidence map not found for claim: {claimId}. Run enrichment first."` — trying Agent 3a/3b/4 before Agent 2
- `"Enrichment not yet completed for claim: {claimId}"` — Agent 2 is still running or failed
- `"Gemini not available"` — API key not configured or Gemini service is down

### Recommended Error UX
- Show a user-friendly toast notification for errors
- For "Run enrichment first" errors, show a button linking to the enrichment action
- For Gemini unavailability, show a persistent banner and disable AI features

---

## Key Data Relationships

```
disputes table (disputeId = id, claimId)
    │
    └── evidence_maps table (linked by claimId and disputeId)
            ├── fetchPlan (JSON string — Agent 2)
            ├── annotatedMap (JSON string — Agent 2)
            ├── merchantSummary (text — Agent 3a)
            ├── challengeRecommendation (JSON string — Agent 4)
            ├── rebuttalDocument (text — Agent 3b)
            ├── enrichmentStatus ("IN_PROGRESS" | "COMPLETED" | "FAILED")
            └── recommendationStrength ("STRONG_CHALLENGE" | "MODERATE_CHALLENGE" | "WEAK_CHALLENGE" | "ACCEPT")
```

The dispute's `issuerSummary` field (Agent 1 output) lives directly on the `disputes` table.

---

## Suggested UI Layout for Dispute Detail Page

```
┌──────────────────────────────────────────────────────┐
│  Claim #200002038279  │  Status: ENRICHED  │  $616.34│
├──────────────────────────────────────────────────────┤
│                                                      │
│  ┌─── ISSUER SUMMARY (Agent 1) ───────────────────┐ │
│  │  Rendered Markdown summary                      │ │
│  │  [Re-generate Summary]                          │ │
│  └─────────────────────────────────────────────────┘ │
│                                                      │
│  ┌─── EVIDENCE MAP (Agent 2) ─────────────────────┐ │
│  │  [Enrich Evidence] button (if not yet done)     │ │
│  │  Completeness: ████████░░ 75%                   │ │
│  │  ┌────────────────────────────────────────────┐ │ │
│  │  │ ✅ authorization_record  STRONG            │ │ │
│  │  │ ✅ 3ds_authentication    STRONG            │ │ │
│  │  │ ✅ delivery_proof        STRONG            │ │ │
│  │  │ ❌ signature_confirm     MISSING           │ │ │
│  │  │ ❌ terms_of_service      MISSING           │ │ │
│  │  └────────────────────────────────────────────┘ │ │
│  │  Critical Gaps: signature, ToS                  │ │
│  │  Manual Upload: [Upload Evidence]               │ │
│  └─────────────────────────────────────────────────┘ │
│                                                      │
│  ┌─── MERCHANT SUMMARY (Agent 3a) ───────────────┐ │
│  │  Rendered Markdown merchant-side summary        │ │
│  └─────────────────────────────────────────────────┘ │
│                                                      │
│  ┌─── RECOMMENDATION (Agent 4) ──────────────────┐ │
│  │  🟡 MODERATE CHALLENGE  (75% confidence)       │ │
│  │  Win Probability: 70%                          │ │
│  │  Strengths: • 3DS auth  • AVS/CVV match       │ │
│  │  Weaknesses: • Missing reason code  • No ToS  │ │
│  │  [Challenge — Generate Rebuttal]  [Accept]      │ │
│  └─────────────────────────────────────────────────┘ │
│                                                      │
│  ┌─── REBUTTAL (Agent 3b) ───────────────────────┐ │
│  │  (Only shown after user clicks "Challenge")    │ │
│  │  Rendered Markdown rebuttal document            │ │
│  │  [Copy]  [Download PDF]  [Submit to Mastercard] │ │
│  └─────────────────────────────────────────────────┘ │
│                                                      │
└──────────────────────────────────────────────────────┘
```

---

## Summary of All Agent Endpoints

| Agent | Method | Endpoint | Trigger | Response Time |
|-------|--------|----------|---------|---------------|
| Status Check | GET | `/api/agents/status` | App startup | < 1s |
| Agent 1 | POST | `/api/agents/summarize/{disputeId}` | New claim arrives | 5-15s |
| Agent 2 | POST | `/api/agents/enrich/{disputeId}` | User clicks "Enrich" | 30-60s |
| Agent 3a | POST | `/api/agents/merchant-summary/{disputeId}` | After enrichment | 10-20s |
| Agent 4 | POST | `/api/agents/recommend/{disputeId}` | After enrichment | 10-20s |
| Agent 3b | POST | `/api/agents/rebuttal/{disputeId}` | User confirms challenge | 15-30s |
| Full Pipeline | POST | `/api/agents/full-pipeline/{disputeId}` | "Quick Analysis" button | 60-120s |
| Evidence Map | GET | `/api/agents/evidence-map/{disputeId}` | Page load (read saved) | < 1s |
| Policy Diff | POST | `/api/agents/policy/diff` | Admin action | 15-30s |
| Policy Analyze | POST | `/api/agents/policy/analyze` | Admin action | 15-30s |
| Analytics Insights | GET | `/api/agents/analytics/insights` | Analytics page | 10-20s |
| Analytics Stats | GET | `/api/agents/analytics/statistics` | Analytics page | < 1s |
