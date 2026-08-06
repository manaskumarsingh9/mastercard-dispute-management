# Backend Integration Guide — Settings & Pipeline Automation

This document describes the frontend settings that require Spring Boot backend support to function end-to-end. The frontend currently stores these settings and displays the UI, but the backend must consume them to control actual execution.

---

## 1. Tab Visibility (Frontend-Only — No Backend Action Needed)

Tab visibility is purely a frontend concern. The sidebar shows/hides navigation items based on `hide-tabs.*` flags. No backend changes required.

---

## 2. Pipeline Automation (`pipeline.auto-enrich`)

### What the Frontend Does

The Settings page (Admin view) has a **Pipeline Automation** toggle with two modes:

- **Manual** (`pipeline.auto-enrich=false`): After Phase 1 (automatic ingestion) completes for a dispute, the user reviews the case details and manually clicks "Start Enrichment" to trigger the agent pipeline (Phase 2).
- **Auto** (`pipeline.auto-enrich=true`): After Phase 1 completes, Phase 2 should start automatically without waiting for user action.

The frontend stores this flag via `PUT /api/config` (currently handled by the Vite proxy plugin writing to a local file). In production, this should be stored in the backend.

### What the Backend Needs to Implement

#### 2.1 Store the Setting

Create a configuration table or use an existing settings mechanism to persist:

```
Key: pipeline.auto-enrich
Value: true | false
Default: false
```

**Option A — Database table:**
```sql
CREATE TABLE app_settings (
    key VARCHAR(100) PRIMARY KEY,
    value VARCHAR(500) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO app_settings (key, value) VALUES ('pipeline.auto-enrich', 'false');
```

**Option B — Application properties file** (simpler, but requires restart for changes):
```properties
# application.yml or application.properties
pipeline.auto-enrich=false
```

If using Option B, you'll need a REST endpoint to update the property at runtime (e.g., using Spring Cloud Config or a custom `@RefreshScope` bean).

#### 2.2 REST API Endpoints

The frontend expects these endpoints (currently proxied through Vite, but should be native Spring Boot endpoints in production):

**GET /api/config**
Returns all frontend-relevant configuration as a flat JSON object:
```json
{
  "hide-tabs.pre-chargeback-alerts": "true",
  "hide-tabs.policy-diff": "true",
  "hide-tabs.analytics-insights": "true",
  "hide-tabs.integrations": "true",
  "hide-tabs.business-profile": "true",
  "hide-tabs.data-points": "true",
  "pipeline.auto-enrich": "false"
}
```

**PUT /api/config**
Accepts a partial JSON object with keys to update. Merges with existing config:
```json
// Request body
{
  "pipeline.auto-enrich": "true"
}

// Response: full merged config
{
  "hide-tabs.pre-chargeback-alerts": "true",
  ...
  "pipeline.auto-enrich": "true"
}
```

**Spring Boot Controller:**
```java
@RestController
@RequestMapping("/api/config")
public class AppConfigController {

    @Autowired
    private AppConfigService configService;

    @GetMapping
    public ResponseEntity<Map<String, String>> getConfig() {
        return ResponseEntity.ok(configService.getAllSettings());
    }

    @PutMapping
    public ResponseEntity<Map<String, String>> updateConfig(
            @RequestBody Map<String, String> updates) {
        configService.updateSettings(updates);
        return ResponseEntity.ok(configService.getAllSettings());
    }
}
```

**Service (database-backed):**
```java
@Service
public class AppConfigService {

    @Autowired
    private AppSettingsRepository repo;

    private static final Map<String, String> DEFAULTS = Map.of(
        "hide-tabs.pre-chargeback-alerts", "true",
        "hide-tabs.policy-diff", "true",
        "hide-tabs.analytics-insights", "true",
        "hide-tabs.integrations", "true",
        "hide-tabs.business-profile", "true",
        "hide-tabs.data-points", "true",
        "pipeline.auto-enrich", "false"
    );

    // Whitelist of keys the frontend is allowed to write
    private static final Set<String> ALLOWED_KEYS = Set.of(
        "hide-tabs.pre-chargeback-alerts",
        "hide-tabs.policy-diff",
        "hide-tabs.analytics-insights",
        "hide-tabs.integrations",
        "hide-tabs.business-profile",
        "hide-tabs.data-points",
        "pipeline.auto-enrich"
    );

    public Map<String, String> getAllSettings() {
        Map<String, String> result = new HashMap<>(DEFAULTS);
        repo.findAll().forEach(s -> result.put(s.getKey(), s.getValue()));
        return result;
    }

    public void updateSettings(Map<String, String> updates) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            if (!ALLOWED_KEYS.contains(entry.getKey())) {
                throw new IllegalArgumentException("Unknown config key: " + entry.getKey());
            }
            repo.save(new AppSetting(entry.getKey(), entry.getValue()));
        }
    }
}
```

#### 2.3 Wire Auto-Enrich into the Ingestion Flow

This is the critical integration point. The ingestion flow currently works like this:

```
POST /api/ingestion/pull-claims
  → Calls Mastercom POST /v6/queues (paginated)
  → Deduplicates by claimId
  → Inserts new disputes with status NEW
  → Returns ingestion stats
```

After ingestion, the frontend currently relies on the user to manually open a dispute and click "Start Enrichment" to trigger the agent pipeline.

**With auto-enrich enabled**, the backend should automatically trigger Phase 2 after Phase 1 completes:

```java
@Service
public class IngestionService {

    @Autowired
    private AppConfigService configService;

    @Autowired
    private AgentPipelineService agentPipeline;

    @Async  // Run enrichment asynchronously so ingestion response isn't blocked
    public void postIngestionHook(List<Dispute> newlyIngestedDisputes) {
        String autoEnrich = configService.getAllSettings()
            .getOrDefault("pipeline.auto-enrich", "false");

        if (!"true".equals(autoEnrich)) {
            return; // Manual mode — do nothing, user will trigger enrichment
        }

        for (Dispute dispute : newlyIngestedDisputes) {
            try {
                // Skip disputes that match auto-accept rules (if any)
                if (autoAcceptService.shouldAutoAccept(dispute)) {
                    dispute.setStatus("NOT_REPRESENTED");
                    disputeRepository.save(dispute);
                    continue;
                }

                // Phase 2: Data Enrichment
                // 1. Agent 1 — Issuer Claim Summary
                agentPipeline.runAgent1(dispute.getId());

                // 2. Agent 2 Task 1 — Plan evidence gathering
                agentPipeline.runAgent2Task1(dispute.getId());

                // 3. Evidence Engine — Call relevant APIs
                evidenceEngine.fetchEvidence(dispute.getId());

                // 4. Agent 2 Task 2 — Verify + annotate evidence map
                agentPipeline.runAgent2Task2(dispute.getId());

                // Update dispute status
                dispute.setStatus("ENRICHED");
                disputeRepository.save(dispute);

            } catch (Exception e) {
                log.error("Auto-enrich failed for dispute {}: {}",
                    dispute.getId(), e.getMessage());
                dispute.setStatus("MISSING_DATA");
                disputeRepository.save(dispute);
            }
        }
    }
}
```

**Call this hook from the ingestion endpoint:**
```java
@PostMapping("/api/ingestion/pull-claims")
public ResponseEntity<?> pullClaims() {
    IngestionResult result = ingestionService.pullAndIngest();

    // Trigger auto-enrich asynchronously if enabled
    if (!result.getNewDisputes().isEmpty()) {
        ingestionService.postIngestionHook(result.getNewDisputes());
    }

    return ResponseEntity.ok(result.getStats());
}
```

#### 2.4 Frontend Status Updates During Auto-Enrich

When auto-enrich is running, the frontend needs to know the enrichment is in progress. The existing dispute status values handle this:

| Status | Meaning |
|--------|---------|
| `NEW` | Just ingested, not yet processed |
| `INITIATED` | Enrichment started |
| `ENRICHED` | All agents completed successfully |
| `MISSING_DATA` | Enrichment failed or incomplete |

The frontend polls `/api/disputes` periodically (30s cache). As disputes transition through statuses during auto-enrich, the dashboard updates automatically.

---

## 3. Auto-Accept Rules

### What the Frontend Does

The Settings page manages auto-accept rules stored in `config/auto-accept-rules.json`. Rules define conditions under which disputes should be automatically accepted without enrichment.

### What the Backend Needs to Implement

#### 3.1 Store Rules

```sql
CREATE TABLE auto_accept_rules (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    enabled BOOLEAN DEFAULT true,
    priority INTEGER NOT NULL,
    conditions JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auto_accept_config (
    id INTEGER PRIMARY KEY DEFAULT 1,
    enabled BOOLEAN DEFAULT false,
    CHECK (id = 1)  -- Singleton row
);
```

#### 3.2 REST API Endpoints

**GET /api/auto-accept-rules**
```json
{
  "enabled": true,
  "rules": [
    {
      "id": "rule-1",
      "name": "Low Value Disputes",
      "description": "Auto-accept disputes below $25",
      "enabled": true,
      "priority": 1,
      "conditions": {
        "amountBelow": 25
      },
      "createdAt": "2026-03-15T10:00:00Z",
      "updatedAt": "2026-03-15T10:00:00Z"
    }
  ]
}
```

**PUT /api/auto-accept-rules**
Accepts the full config object (enabled flag + rules array). Validates and replaces.

#### 3.3 Evaluate Rules During Ingestion

```java
@Service
public class AutoAcceptService {

    @Autowired
    private AutoAcceptRuleRepository ruleRepo;

    @Autowired
    private AutoAcceptConfigRepository configRepo;

    public boolean shouldAutoAccept(Dispute dispute) {
        AutoAcceptConfig config = configRepo.findById(1).orElse(null);
        if (config == null || !config.isEnabled()) {
            return false;
        }

        List<AutoAcceptRule> rules = ruleRepo.findByEnabledTrueOrderByPriorityAsc();

        for (AutoAcceptRule rule : rules) {
            if (matchesAllConditions(rule.getConditions(), dispute)) {
                return true; // First matching rule wins
            }
        }
        return false;
    }

    private boolean matchesAllConditions(RuleConditions cond, Dispute d) {
        // AND logic: ALL specified conditions must match

        if (cond.getAmountBelow() != null && d.getAmount() >= cond.getAmountBelow()) {
            return false;
        }
        if (cond.getAmountAbove() != null && d.getAmount() <= cond.getAmountAbove()) {
            return false;
        }
        if (cond.getReasonCodes() != null && !cond.getReasonCodes().isEmpty()
                && !cond.getReasonCodes().contains(d.getReasonCode())) {
            return false;
        }
        if (cond.getDisputeTypes() != null && !cond.getDisputeTypes().isEmpty()
                && !cond.getDisputeTypes().contains(d.getDisputeType())) {
            return false;
        }
        if (cond.getMerchantNames() != null && !cond.getMerchantNames().isEmpty()
                && cond.getMerchantNames().stream()
                    .noneMatch(m -> d.getMerchantName().toLowerCase().contains(m.toLowerCase()))) {
            return false;
        }
        if (cond.getAgeAboveDays() != null) {
            long daysOld = ChronoUnit.DAYS.between(
                d.getCreatedAt().toLocalDate(), LocalDate.now());
            if (daysOld <= cond.getAgeAboveDays()) {
                return false;
            }
        }
        return true;
    }
}
```

---

## 4. Integration Sequence (Complete Flow)

```
Ingestion triggered (POST /api/ingestion/pull-claims)
    │
    ├── 1. Pull claims from Mastercom queue
    ├── 2. Deduplicate + insert new disputes (status: NEW)
    │
    ├── 3. For each new dispute:
    │      │
    │      ├── Check auto-accept rules (if engine enabled)
    │      │     ├── Match found → set status NOT_REPRESENTED, skip enrichment
    │      │     └── No match → continue
    │      │
    │      ├── Check pipeline.auto-enrich setting
    │      │     ├── false (Manual) → leave status NEW, wait for user action
    │      │     └── true (Auto) → trigger agent pipeline:
    │      │           ├── Agent 1: Issuer Claim Summary
    │      │           ├── Agent 2 Task 1: Plan evidence
    │      │           ├── Evidence Engine: Fetch from APIs
    │      │           ├── Agent 2 Task 2: Verify evidence map
    │      │           └── Set status ENRICHED (or MISSING_DATA on failure)
    │      │
    │      └── (Phase 3 always requires user decision — "Challenge?" in the flow diagram)
    │
    └── 4. Return ingestion stats to frontend
```

---

## 5. Security Considerations

1. **Authorization**: The `PUT /api/config` and `PUT /api/auto-accept-rules` endpoints should require admin-level authentication. The frontend's Admin/User toggle is cosmetic — actual security must be enforced server-side.

2. **Input Validation**: Whitelist allowed config keys. Reject unknown keys in `PUT /api/config`.

3. **Audit Logging**: Log all config changes with the user who made them and timestamp.

4. **Rate Limiting**: Consider rate limiting on config write endpoints to prevent abuse.

---

## 6. Migration Checklist

- [ ] Create `app_settings` table (or equivalent storage)
- [ ] Create `auto_accept_rules` and `auto_accept_config` tables
- [ ] Implement `GET /api/config` and `PUT /api/config` endpoints
- [ ] Implement `GET /api/auto-accept-rules` and `PUT /api/auto-accept-rules` endpoints
- [ ] Implement `AutoAcceptService.shouldAutoAccept()` evaluation logic
- [ ] Add `pipeline.auto-enrich` check in the ingestion post-hook
- [ ] Implement async agent pipeline trigger for auto-enrich
- [ ] Add admin authorization to config/rules endpoints
- [ ] Add audit logging for config changes
- [ ] Test: manual mode — verify enrichment does NOT auto-start
- [ ] Test: auto mode — verify enrichment starts after ingestion
- [ ] Test: auto-accept rule matching with various condition combinations
- [ ] Test: priority ordering (lowest priority number wins)
