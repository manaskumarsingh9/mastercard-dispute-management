# Mastercard Dispute Management System

## Overview
A Spring Boot REST API application for managing payment disputes. It provides:
- Local CRUD operations for dispute entities stored in PostgreSQL
- Full Mastercard Mastercom v6 API integration (26 endpoints) via OAuth 1.0a authentication
- Covers the entire issuer dispute lifecycle: transaction search, claims, retrieval requests, chargebacks, case filings, fraud reporting, and fee collection
- Automated claim ingestion from Mastercard queues with scheduled polling and on-demand triggers
- 6 AI-powered agents using Google Gemini 2.5 Flash for dispute analysis, evidence strategy, and rebuttal generation

## Tech Stack
- **Language:** Java 19 (GraalVM 22.3)
- **Framework:** Spring Boot 4.0.3
- **Database:** PostgreSQL (Replit built-in)
- **Build:** Apache Maven (via `mvnw` wrapper)
- **HTTP Client:** OkHttp 3 (for Mastercard, Ethoca & Gemini API calls)
- **Auth:** Mastercard OAuth 1.0a Signer Library, Ethoca HMAC-SHA1
- **AI:** Google Gemini 2.5 Flash (via REST API)
- **JSON:** Gson
- **Utilities:** Lombok

## Project Layout
```
src/main/java/com/opus/dispute/management/
├── MastercardDisputeManagementApplication.java  # Entry point (@EnableScheduling, @EnableAsync)
├── config/
│   ├── EthocaProperties.java                   # Ethoca API config binding
│   ├── MastercardProperties.java               # Mastercard API config binding
│   └── WebConfig.java                          # CORS configuration
├── controller/
│   ├── AgentController.java                    # AI agent endpoints (/api/agents)
│   ├── DisputeController.java                  # Local CRUD endpoints (/api/disputes)
│   ├── EthocaController.java                   # Ethoca Consumer Clarity & Merchant Actions (/api/ethoca)
│   ├── IngestionController.java                # Claim ingestion endpoints (/api/ingestion)
│   └── MastercardApiController.java            # All Mastercom v6 API endpoints
├── entity/
│   ├── CaseFilingDetail.java                   # Case filing detail sub-entity
│   ├── ChargebackDetail.java                   # Chargeback detail sub-entity
│   ├── Dispute.java                            # JPA entity for ingested claims
│   ├── AgentConversation.java                  # Per-case per-agent conversation history
│   ├── EvidenceMap.java                        # Annotated evidence map entity
│   ├── FeeDetail.java                          # Fee detail sub-entity
│   ├── IngestionState.java                     # Tracks polling state (rolling window)
│   ├── PolicyDocument.java                     # Versioned policy document entity
│   └── RetrievalDetail.java                    # Retrieval detail sub-entity
├── repository/
│   ├── CaseFilingDetailRepository.java         # Case filing detail repository
│   ├── ChargebackDetailRepository.java         # Chargeback detail repository
│   ├── DisputeRepository.java                  # Spring Data JPA repository
│   ├── EvidenceMapRepository.java              # Evidence map persistence
│   ├── FeeDetailRepository.java                # Fee detail repository
│   ├── IngestionStateRepository.java           # Ingestion state persistence
│   ├── PolicyDocumentRepository.java           # Policy document versioning
│   └── RetrievalDetailRepository.java          # Retrieval detail repository
└── service/
    ├── ClaimDetailService.java                 # Fetches & stores claim details from Mastercard API
    ├── ClaimIngestionService.java              # Core ingestion logic (queue polling, dedup)
    ├── PolicyDocumentService.java               # Policy upload, text extraction (PDF/DOCX/TXT), versioning, auto-diff
    ├── DisputeService.java                     # Business logic
    ├── EthocaApiClient.java                    # Ethoca HTTP client (HMAC-SHA1)
    ├── DataSourceService.java                  # Reads evidence data files from src/data/sources/issuer/ and src/data/sources/acquirer/
    ├── EvidenceEngineService.java              # Evidence fetching framework (legacy mock adapters)
    ├── EvidenceUploadService.java              # Manual evidence upload + auto-reassess (Agents 2→4)
    ├── GeminiService.java                      # Gemini API client (REST, text + multimodal)
    ├── MediaFile.java                          # Record for binary evidence (PDF/image with mime type + base64)
    ├── MastercardApiClient.java                # Mastercard HTTP client (OAuth 1.0a)
    ├── StripeEvidenceService.java              # Stripe API evidence fetcher (Charges, PaymentIntents, Balance Txns)
    ├── PiiScrubber.java                        # PII masking before LLM calls
    ├── ScheduledIngestionTask.java             # Scheduled task for periodic polling
    ├── SecondPresentmentService.java           # ZIP + base64 encode acquirer evidence, submit to Mastercard
    ├── SummarizationQueueService.java          # Single-threaded queue for Agent 1 (prevents OOM)
    └── agent/
        ├── AnalyticsInsightAgent.java          # Agent 6: Dispute analytics & trends
        ├── CaseSummarizerAgent.java            # Agent 1: Issuer case summary
        ├── ChallengeAdvisorAgent.java          # Agent 4: Challenge recommendation
        ├── EvidenceStrategistAgent.java         # Agent 2: Evidence planning & annotation
        ├── MerchantSummaryAgent.java           # Agent 3a: Merchant-side summary
        ├── PolicyIntelligenceAgent.java         # Agent 5: Policy diff & analysis
        └── RebuttalArchitectAgent.java         # Agent 3b: Rebuttal document generation
src/data/
├── reason-code-rules.json                      # Mastercard reason code → required evidence mapping
└── sources/
    ├── issuer/                                 # Issuer-side evidence data (used by Agent 1)
    │   ├── customer-comms/  device/  fraud-tools/  identity/  merchant/  psp/  shipping/
    └── acquirer/                               # Acquirer/merchant-side evidence data (used by Agent 2)
        ├── customer-comms/  device/  fraud-tools/  identity/  merchant/  psp/  shipping/
src/main/resources/
└── application.properties                      # App config (DB, Mastercard, Gemini, ingestion)
```

## AI Agents (Gemini-Powered)

All agents use PII scrubbing before sending data to Gemini. Agents 1 and 2 support **multimodal evidence** — they can analyze PDF documents and images (PNG, JPEG, GIF, WebP) alongside JSON text files. Media files are sent as inline base64 data to Gemini's multimodal API. Agents 2, 3b, and 4 maintain **per-case conversation history** (stored in `agent_conversations` table) — each subsequent run for the same claim sends all prior turns to Gemini, ensuring consistent scoring and awareness of previous assessments. Agent 3b also accepts optional `userInstructions` for tone/angle/content refinement. Endpoints are under `/api/agents/`.

| # | Agent | Trigger | Endpoint |
|---|-------|---------|----------|
| 1 | **Case Summarizer** | At ingestion or on-demand (queued) | `POST /api/agents/summarize/{disputeId}`, `POST /api/agents/summarize/batch` |
| 2 | **Evidence Strategist** | Merchant clicks "Enrich" | `POST /api/agents/enrich/{disputeId}` |
| 3a | **Merchant Summary** | After enrichment | `POST /api/agents/merchant-summary/{disputeId}` |
| 3b | **Rebuttal Architect** | After merchant confirms challenge | `POST /api/agents/rebuttal/{disputeId}` |
| 4 | **Challenge Advisor** | After enrichment (configurable) | `POST /api/agents/recommend/{disputeId}` |
| 5 | **Policy Intelligence** | Admin uploads policy doc | File upload + versioning + auto-diff (see Policy Management section) |
| 6 | **Analytics Insight** | On-demand | `GET /api/agents/analytics/insights`, `GET /api/agents/analytics/statistics` |

Additional endpoints:
- `GET /api/agents/status` — check agent availability
- `POST /api/agents/full-pipeline/{disputeId}` — run agents 1→2→3a→4 in sequence
- `GET /api/agents/evidence-map/{disputeId}` — get stored evidence map
- `GET /api/agents/summarize/status/{disputeId}` — poll summarization job status
- `GET /api/agents/summarize/queue` — get overall queue depth and counts
- `GET /api/agents/reason-codes` — get all reason code rules (from reason-code-rules.json)
- `GET /api/agents/reason-codes/{code}` — get a specific reason code rule by code (e.g. 4834)
- `GET /api/agents/evidence-gaps/{disputeId}` — get missing/critical evidence items with upload suggestions
- `POST /api/agents/upload-evidence/{disputeId}` — upload evidence file (multipart form: category, evidenceName, file, reRunAgents)
- `POST /api/agents/upload-evidence-batch/{disputeId}` — upload multiple evidence files at once
- `POST /api/agents/reassess/{disputeId}` — re-run Agents 2→4 to update confidence/win probability after evidence changes

### Summarization Queue (OOM Prevention)
Agent 1 (Case Summarizer) uses a single-threaded queue to prevent out-of-memory errors when the frontend triggers summarization for many disputes at once. Requests are enqueued immediately (HTTP 202) and processed one at a time. The frontend polls for results. Duplicate requests for already-queued disputes are ignored. Job statuses: QUEUED → PROCESSING → COMPLETED/FAILED.

### Agent Flow (Dispute Lifecycle)
1. **Ingestion** → Agent 1 generates issuer summary (stored with dispute)
2. **Merchant clicks case** → sees issuer data + summary (pure read, no API calls)
3. **Merchant clicks "Enrich"** → Agent 2 plans evidence, fetches via evidence engine, annotates results
4. **Evidence map displayed** → merchant sees missing evidence highlighted with priority (critical/high/medium)
5. **Merchant uploads missing evidence** → `POST /api/agents/upload-evidence/{id}` saves file to acquirer sources
6. **Auto-reassess** → Agents 2 and 4 re-run to update completeness score, confidence, and win probability
7. **Agent 3a** → summarizes merchant-side evidence
8. **Agent 4** → recommends challenge/accept based on full picture
9. **Merchant decides** → if challenging, Agent 3b generates rebuttal document
10. **Submit to Mastercard** → via existing Mastercom API endpoints

## Settings & Configuration

### App Config API
- `GET /api/config` — returns all frontend-relevant settings (tab visibility, pipeline mode)
- `PUT /api/config` — partial update of settings (whitelisted keys only)

Settings are stored in the `app_settings` database table with key-value pairs. Changes take effect immediately without restart.

### Urgency & Highlighting Configuration
Configurable settings that control how dispute deadlines are highlighted in the dashboard:
- `urgency.enabled` — master toggle for urgency indicators (default `true`)
- `urgency.critical-days` — days remaining before deadline to show critical/red (default `2`)
- `urgency.warning-days` — days remaining before deadline to show warning/yellow (default `5`)
- `urgency.buffer-days` — treat deadlines as N days earlier than network deadline (default `0`)
  - Example: if set to `1`, a dispute due Jan 10 is treated as due Jan 9 for highlighting
- Validation: warning-days must be >= critical-days; all values 0-90
- These settings are returned by `GET /api/config` and updated via `PUT /api/config`

### Policy Management (Agent 5 — Full System)
Policy documents are versioned and stored in the `policy_documents` table. When a new version is uploaded, it's automatically diffed against the previous version. Supports `.txt`, `.pdf`, and `.docx` file uploads with automatic text extraction (Apache PDFBox for PDF, Apache POI for DOCX).

**Merchant Policy Endpoints:**
- `POST /api/agents/policy/merchant/upload` — multipart file upload. Auto-diffs against previous merchant policy version, generates config recommendations. Returns diff summary + structured JSON recommendations.
- `GET /api/agents/policy/merchant/history` — list all stored merchant policy versions (id, version, filename, timestamps, flags)
- `GET /api/agents/policy/merchant/latest` — get full details of the latest merchant policy (content, diff, recommendations)

**Network Policy Endpoints:**
- `POST /api/agents/policy/network/upload` — multipart file upload + `networkName` param (default: "Mastercard"). Auto-diffs against previous version of the same network. First upload gets an initial analysis instead of diff.
- `GET /api/agents/policy/network/history?networkName=Mastercard` — list all versions for a network
- `GET /api/agents/policy/network/latest?networkName=Mastercard` — get full details of the latest network policy

**Shared Endpoints:**
- `GET /api/agents/policy/{id}` — get any policy document by database ID (full content + diff + recommendations)

**Legacy Endpoints (backward compatible):**
- `POST /api/agents/policy/diff` — manual diff with `previousPolicy` + `newPolicy` text in JSON body
- `POST /api/agents/policy/analyze` — single policy analysis with `policyDocument` + `networkName` in JSON body
- `POST /api/agents/policy/merchant-analyze` — merchant policy analysis with `policyDocument` text in JSON body

**Merchant vs Network Policy Flow:**
- Merchant policy upload → auto-diff against previous merchant policy → config recommendations from Gemini
- Network policy upload → auto-diff against previous same-network policy → no config recommendations (diff only)
- Policies are keyed by type: `MERCHANT` or `NETWORK:<NAME>` (e.g., `NETWORK:MASTERCARD`)

### Agent Response Length
- `agents.response-length` — controls verbosity of all AI agent responses (default `long`)
  - `"long"`: Full detailed responses (current behavior — good for beginners)
  - `"short"`: Brief, precise responses — no points missed but no verbose explanations
- Affects all 7 agents (1, 2, 3a, 3b, 4, 5, 6) — each has tailored short-mode instructions
- Validated to only accept `"short"` or `"long"`
- Takes effect immediately on next agent invocation (no restart needed)

### Pipeline Automation
- `pipeline.auto-enrich` setting controls whether the agent pipeline (Agents 1→2→3a→4) runs automatically after ingestion
- When `false` (default): user manually clicks "Start Enrichment" on each dispute
- When `true`: after ingestion, the pipeline runs asynchronously for each new dispute
- Auto-enrich runs via `PostIngestionPipelineService` (async, non-blocking to the ingestion response)

### Auto-Sync (Scheduled Ingestion)
- `ingestion.auto-sync.enabled` — enables/disables automated Mastercard queue polling (default `false`)
- `ingestion.auto-sync.frequency` — `once_daily` (default) or `twice_daily`
  - `once_daily`: cron at `00:00` (midnight)
  - `twice_daily`: cron at `00:00` + `12:00` (noon)
- Each run uses `lastPolledTo` from IngestionState as the start time → no gaps in coverage
- Settings persist in DB; schedule updates immediately when config changes (no restart needed)
- `GET /api/config/auto-sync-status` — returns current schedule info (enabled, frequency, activeJobs, nextRuns, description)
- Managed by `ScheduledIngestionTask` using dynamic `TaskScheduler` + `CronTrigger`

### Auto-Accept Rules
- `GET /api/auto-accept-rules` — get rules config (enabled flag + rules list)
- `PUT /api/auto-accept-rules` — replace rules config
- Rules evaluate during post-ingestion hook. If a dispute matches, it's accepted (skips enrichment)
- Acceptance status is **stage-aware** based on `progressState`:
  - `CB1-*` (first chargeback) → status `NOT_REPRESENTED` — acquirer chose not to represent
  - `CB2-*` / `SC2-*` / second presentment → status `ACCEPTED_PRE_ARBITRATION` — acquirer won't escalate further
  - `PRE_ARB-*` / `PREARB-*` → status `ACCEPTED_PRE_ARBITRATION` — acquirer accepts at pre-arbitration
  - `ARB-*` → status `ACCEPTED_ARBITRATION` — acquirer accepts arbitration ruling
- Stage resolution is centralized in `AcceptanceStatusResolver` (used by both auto-accept and manual accept)
- Supports conditions: `amountBelow`, `amountAbove`, `reasonCodes`, `disputeTypes`, `merchantNames`, `ageAboveDays`

### Manual Accept
- `POST /api/disputes/{id}/accept` — manually accept a dispute at the correct lifecycle stage
- Returns the resolved status, action, progressState, and a human-readable description

### Second Presentment Submission
- `POST /api/disputes/{id}/second-presentment` — raise a second presentment to Mastercard with all acquirer evidence
  - Collects all acquirer evidence files (text + media, including user-uploaded evidence)
  - Builds a ZIP file, base64-encodes it per Mastercard's `DocumentStructure` spec
  - Submits via Mastercard's `createChargeback` API with `chargebackType: SECOND_PRESENTMENT`
  - References the original `CHARGEBACK` chargebackId as `disputeChargebackID`
  - Optional `messageText` in request body (max 100 chars)
  - Updates dispute status to `SECOND_PRESENTMENT_SUBMITTED`
  - Returns full submission details including Mastercard's response (new chargebackId)

### New Entities
- `AppSetting` — key-value config store (table: `app_settings`)
- `AutoAcceptRule` — rule with conditions JSON (table: `auto_accept_rules`)

### New Services
- `AppConfigService` — settings CRUD with defaults and whitelist
- `AutoAcceptService` — rule evaluation engine
- `PostIngestionPipelineService` — async post-ingestion hook (auto-accept + auto-enrich)

## API Endpoints

### Local Dispute CRUD
- `GET /api/disputes` — list all disputes
- `POST /api/disputes` — create a dispute
- `GET /api/disputes/{id}` — get a dispute by ID
- `PATCH /api/disputes/{id}` — partial update (supplement fields: merchantCategory, customerEmail, cardNumber, evidenceFileId, etc.)
- `DELETE /api/disputes/{id}` — delete a dispute
- `GET /api/disputes/{id}/details` — get dispute with all detail sub-entities (chargebacks, retrievals, fees, case filings)
- `POST /api/disputes/{id}/fetch-details` — trigger on-demand claim detail fetch from Mastercard API
- `GET /api/disputes/{id}/sources` — get all evidence source files (both issuer and acquirer, prefixed)
- `GET /api/disputes/{id}/sources/{category}` — get evidence files by category from both sides
- `GET /api/disputes/{id}/sources/{side}/{category}` — get evidence files by side (issuer/acquirer) and category
- `GET /api/disputes/{id}/download/issuer` — download all issuer evidence as `{claimId}_{reasonCode}_issuer_evidences.zip`
- `GET /api/disputes/{id}/download/acquirer` — download all acquirer evidence as `{claimId}_{reasonCode}_acquirer_evidences.zip`

### Claim Ingestion (`/api/ingestion`)
- `POST /api/ingestion/ingest` — trigger on-demand ingestion from Mastercard queues
- `GET /api/ingestion/status` — get ingestion state (last poll times, total claims, etc.)
- `GET /api/ingestion/disputes?page=0&size=50` — get paginated ingested disputes
- `GET /api/ingestion/queues` — list available Mastercard queue names

### Mastercard Mastercom v6 APIs (Issuer Perspective)

All endpoints are under `/api/mastercard/` and proxy to the Mastercard Mastercom v6 sandbox.

| # | API | Method | Endpoint |
|---|-----|--------|----------|
| 0 | Health Check | GET | `/api/mastercard/test` |
| 1 | Transaction Search | POST | `/api/mastercard/transactions/search` |
| 2a | Clearing Detail | GET | `/api/mastercard/claims/{claimId}/transactions/clearing/{transactionId}` |
| 2b | Authorization Detail | GET | `/api/mastercard/claims/{claimId}/transactions/authorization/{transactionId}` |
| 3a | Create Claim | POST | `/api/mastercard/claims` |
| 3b | Get Claim Detail | GET | `/api/mastercard/claims/{claimId}` |
| 4a | Load Data for Retrieval | GET | `/api/mastercard/claims/{claimId}/retrievalrequests/loaddataforretrievalrequests` |
| 4b | Create Retrieval Request | POST | `/api/mastercard/claims/{claimId}/retrievalrequests` |
| 5a | Issuer Response to Fulfillment | POST | `/api/mastercard/claims/{claimId}/retrievalrequests/{requestId}/fulfillments/response` |
| 5b | Get Retrieval Documents | GET | `/api/mastercard/claims/{claimId}/retrievalrequests/{requestId}/documents?format=ORIGINAL` |
| 5c | Retrieval Fulfillment Status | PUT | `/api/mastercard/retrievalrequests/status` |
| 5d | Get Chargeback Documents | GET | `/api/mastercard/claims/{claimId}/chargebacks/{chargebackId}/documents?format=ORIGINAL` |
| 6a | Load Data for Chargeback | POST | `/api/mastercard/claims/{claimId}/chargebacks/loaddataforchargebacks` |
| 6b | Create Chargeback | POST | `/api/mastercard/claims/{claimId}/chargebacks` |
| 7 | Chargeback Reversal | POST | `/api/mastercard/claims/{claimId}/chargebacks/{chargebackId}/reversal` |
| 8 | Update Chargeback | PUT | `/api/mastercard/claims/{claimId}/chargebacks/{chargebackId}` |
| 9a | Chargeback Status | PUT | `/api/mastercard/chargebacks/status` |
| 9b | Acknowledge Chargebacks | PUT | `/api/mastercard/chargebacks/acknowledge` |
| 10 | Create Case Filing | POST | `/api/mastercard/cases` |
| 11 | Update Case Filing | PUT | `/api/mastercard/cases/{caseId}` |
| 12 | Get Case Documents | GET | `/api/mastercard/cases/{caseId}/documents?format=ORIGINAL` |
| 13a | Load Data for Fraud | GET | `/api/mastercard/claims/{claimId}/fraud/loaddataforfraud` |
| 13b | Create Fraud Event | POST | `/api/mastercard/claims/{claimId}/fraud/mastercard` |
| 14a | Load Data for Fee | POST | `/api/mastercard/claims/{claimId}/fees/loaddataforfees` |
| 14b | Create Fee | POST | `/api/mastercard/claims/{claimId}/fee` |
| 15a | Get Queue Names | GET | `/api/mastercard/queues/names` |
| 15b | Get Claims from Queue | GET | `/api/mastercard/queues?queue-name=Pending` |
| 15c | Get Claims from Queue (Date Range) | POST | `/api/mastercard/queues` |

### Ethoca Consumer Clarity & Merchant Actions APIs

All endpoints are under `/api/ethoca/` and proxy to the Ethoca sandbox (`sandbox.api.ethocaweb.com`).
Uses HMAC-SHA1 authentication (`ETHOCA-SHA1 KeyRef=<keyId>,Signature=<sig>`).

| # | API | Method | Endpoint |
|---|-----|--------|----------|
| 0 | Status Check | GET | `/api/ethoca/status` |
| 1 | Health Check | GET | `/api/ethoca/healthcheck` |
| 2 | Search Orders | POST | `/api/ethoca/orders/search` |
| 3 | Merchant Reports (First Party Trust) | POST | `/api/ethoca/merchants/reports` |
| 4 | Descriptor Notifications | POST | `/api/ethoca/merchants/notifications` |
| 5 | Subscription Action (Cancel/Pause/Resume/etc.) | PUT | `/api/ethoca/subscriptions/actions/{actionId}` |

**Search Orders Request Example:**
```json
{
  "requestReference": {
    "originatorChannel": "DIGITAL",
    "correlationId": "track123",
    "locale": "en-US"
  },
  "searchCriteria": {
    "paymentType": "MC",
    "cardLastFour": "1234",
    "transactionAmount": "250.35",
    "transactionCurrencyCode": "USD",
    "transactionDateTime": "2020-06-18T17:11:05-05:00"
  }
}
```

**Subscription Action Request Example:**
```json
{
  "actionId": "52318413-bd05-4960-a745-f79e5c3d6de9",
  "actionType": "CANCEL",
  "offerAllowed": true,
  "cardholder": {
    "card": { "cardBrand": "MC", "firstSix": "510346", "lastFour": "3605" }
  }
}
```

## Claim Ingestion System

### How It Works
The ingestion system periodically polls Mastercard's queue endpoints (`getQueueSummaryPost`) to fetch new and updated claims, storing them in the local PostgreSQL database.

### Scheduled Polling
- Runs every hour (configurable via `ingestion.scheduled.interval-ms`)
- Uses a rolling 5-day window (Mastercard's max allowed range)
- Polls queues: Pending, Rejects, Unworked
- Deduplicates by `claimId` — existing claims get updated, new ones get inserted
- Tracks state in `ingestion_state` table (last poll times, counts)

### On-Demand Trigger
Frontend can POST to `/api/ingestion/ingest` with:
```json
{
  "queueName": "Pending",
  "lastModifiedDateFrom": "2026-03-28T00:00",
  "lastModifiedDateTo": "2026-04-01T12:00"
}
```

### Configuration
In `application.properties`:
- `ingestion.max-new-claims=3` — max new disputes accepted per ingestion run (prevents DB overpopulation and LLM overload)
- `ingestion.scheduled.enabled=true` — enable/disable scheduled polling
- `ingestion.scheduled.interval-ms=3600000` — polling interval (default: 1 hour)
- `ingestion.scheduled.initial-delay-ms=60000` — delay before first poll (default: 1 min)

## Configuration
`application.properties` uses environment variables for DB connection:
- `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` — set by Replit database

Mastercard API config is in `application.properties`:
- `mastercard.base-url` — Mastercard sandbox URL
- `mastercard.consumer-key` — OAuth consumer key
- `mastercard.keystore-path` — Path to `.p12` signing key (not included in repo)
- `mastercard.keystore-password` — Keystore password

Stripe API config:
- `stripe.secret-key` — Stripe Secret Key (via `STRIPE_SECRET_KEY` env var)
- Stripe evidence is automatically merged during Agent 2 (Evidence Strategist) enrichment when `stripeDisputeId` or `stripeChargeId` is set on a dispute
- Stripe endpoints: `GET /api/agents/stripe/status`, `POST /api/agents/stripe/evidence/{disputeId}`

Gemini AI config:
- `gemini.api-key` — Google Gemini API key (via `GEMINI_API_KEY` env var)

Ethoca API config:
- `ethoca.base-url` — Ethoca base URL (sandbox by default)
- `ethoca.api-key-id` — Ethoca API Key ID (via `ETHOCA_API_KEY_ID` env var)
- `ethoca.api-secret` — Ethoca API secret for HMAC-SHA1 signing (via `ETHOCA_API_SECRET` env var)

**Note:** The Mastercard `.p12` keystore file is not committed to the repo. Mastercard API features will log a warning but the app will still start and local dispute CRUD will work. Similarly, Ethoca features require `ETHOCA_API_KEY_ID` and `ETHOCA_API_SECRET` to be set.

## Running
- **Start (background):** `./mvnw spring-boot:run > /tmp/app.log 2>&1 &`
- **Stop:** `fuser -k 5000/tcp`
- **Check logs:** `tail -50 /tmp/app.log`
- Port: **5000**

## Express Server Migration
The Spring Boot backend now covers all Express API server functionality. The frontend team can remove the Express middleware server entirely. See `docs/EXPRESS_TO_SPRING_BOOT_MIGRATION.md` for the full endpoint mapping and migration steps.

## Deployment
- Build: `./mvnw package -DskipTests -q`
- Run: `java -jar target/mastercard-dispute-management-0.0.1-SNAPSHOT.jar`
- Target: autoscale
- **Local Deployment Guide**: See `docs/LOCAL_DEPLOYMENT_GUIDE.md` for setting up on your laptop
- **AWS Deployment Guide**: See `docs/AWS_DEPLOYMENT_GUIDE.md` for full step-by-step instructions
- **Docker**: `Dockerfile` (multi-stage build) and `docker-compose.yml` (local dev with PostgreSQL) at project root
- **EC2 Bootstrap**: `ec2-startup.sh` — installs Java, fetches secrets from AWS Secrets Manager, configures systemd service
- **CI/CD**: `.github/workflows/deploy.yml` — GitHub Actions pipeline (build JAR → upload to S3 → deploy to EC2 with health check)
