# Single-Merchant (AudioPeak Electronics) Pivot — Design

## Background

This application was originally built from an acquirer bank's perspective, ingesting disputes for many different merchants from Mastercard's sandbox. We are pivoting the product to represent a single merchant — **AudioPeak Electronics**, an electronics e-commerce retailer — so every dispute shown in the dashboard should look like it belongs to that one merchant.

Reference claim `200002066482` (AudioPeak Electronics, reason code 4853) is already correct and needs no changes — it's the bar the other 8 codes should be brought up to.

## Current-state findings (from codebase research)

These correct assumptions in the original ask and matter for implementation:

1. **The ingestion gate does not currently use `reason-code-rules.json`.** `ClaimIngestionService.loadSupportedReasonCodes()` builds `SUPPORTED_REASON_CODES` by scanning subfolder names under `src/data/sources/issuer/` at class-load time — this yields 35 codes today, not the 9 defined in `reason-code-rules.json`. `reason-code-rules.json`'s 9 codes are currently only used by `EvidenceStrategistAgent` for AI prompt guidance (required sources, win rate, tips), not for gating which claims are persisted.
2. **Merchant name, amount, and currency already come from Mastercard's real API response**, never from the story/evidence files — set in `ClaimIngestionService.populateDisputeFields()`, `ClaimDetailService.overwriteDisputeFieldsFromDetail()`, and `ClaimDetailService.processChargebackDetails()`, all guarded by `if (dispute.getAmount() == null)` so a real value once set isn't clobbered. The story files are only ever read into an in-memory map and passed as free-text AI context — the system prompt explicitly tells the model the claim record (not the story files) is authoritative for these facts. There is no existing merge path to redirect; we are adding a new override step.
3. **No `Merchant` entity exists.** `merchantId`/`merchantName`/`merchantCategory` are flat string columns on `Dispute`. This pivot does not require a schema change — we're normalizing values on the existing columns, not modeling a new entity.
4. The "1000 claims per request, we take 3" behavior in point 2 of the original ask is Mastercard's sandbox page size plus an existing `ingestion.max-new-claims=3` cap (`ClaimIngestionService`, `application.properties`) — already implemented, not something this project builds.

## Scope

**In scope:** the 9 reason codes already defined in `src/data/reason-code-rules.json`: `4837, 4853, 4863, 4834, 4831, 4855, 4841, 4808, 4859`.

**Out of scope:** the other ~26 reason-code story folders under `src/data/sources/` are left untouched and will simply stop being ingested (see Change 1). `reason-code-rules.json` itself is not expanded.

## Branch

New feature branch off `develop` (current branch), e.g. `feature/single-merchant-audiopeak`.

## Changes

### 1. Narrow the ingestion gate to `reason-code-rules.json`

`ClaimIngestionService.loadSupportedReasonCodes()` currently scans `src/data/sources/issuer/*` directory names. Change it to load the key set from `src/data/reason-code-rules.json` instead. No other change to the ingestion loop is needed — the existing "new claim + reason code not supported → delete, count as skipped" logic (`ClaimIngestionService.java:226-233`) already does the right thing once the supported set is correct. This satisfies: only claims with a defined dispute code are ever inserted going forward, and the existing cap-of-3 selection pool is now implicitly restricted to defined codes.

### 2. Hide already-persisted undefined-code claims from the frontend (separate, read-time logic)

This is a distinct mechanism from Change 1 — it addresses rows that may already exist in the DB (from before this change, or from any future drift) with reason codes outside the 9 defined ones.

Add a reason-code filter to the two read paths that serve the frontend:
- `DisputeService.getAllDisputes()` (backs `GET /api/disputes`)
- `IngestionController`'s paginated `GET /api/ingestion/disputes`

Add a repository query (e.g. `findByReasonCodeIn` / a paginated equivalent) restricted to the `reason-code-rules.json` key set. This is a pure read-side filter — it does not delete or modify any existing row, it only stops undefined-code rows from being returned to the frontend.

### 3. Force AudioPeak Electronics identity on every dispute that reaches the frontend

At the point a new dispute passes the reason-code gate during ingestion (`ClaimIngestionService`, right before it's added to `newDisputeIds`), and in the other two places real Mastercard amount/currency/merchant fields get written (`ClaimDetailService.overwriteDisputeFieldsFromDetail()`, `ClaimDetailService.processChargebackDetails()`), override:

- `merchantName` → `"AUDIOPEAK ELECTRONICS"`
- `currency` → `"USD"`
- `amount` → a fixed USD figure per reason code, taken from that code's own (rewritten) AudioPeak story — **not** Mastercard's real amount relabeled. Each of the 9 codes gets one canonical dollar amount baked into the override logic (a small `Map<String, Double>` keyed by reason code), matching the amount stated in that code's story files, so the dispute record and the evidence narrative agree.

This intentionally replaces Mastercard's real merchant/amount/currency for the 9 in-scope disputes — the claim ID, dates, reason code, and all other real fields are untouched.

`merchantId`/`merchantCategory`: leave as Mastercard's real values (not user-facing in the same way merchantName is) unless review surfaces a place they leak to the frontend directly — flag during implementation if found.

### 4. Clean up `reason-code-rules.json` conditional/required sources for a single physical-goods e-commerce merchant

**Finding:** `EvidenceStrategistAgent.extractAllConditionalSources()` does not select one business-type scenario per claim — it merges every `conditionalSources` key (`physical_goods`, `digital_service`, `subscription`, `travel`, `food_delivery`) together, deduplicated by source/file, then flags anything from that merged list not found on disk as **missing evidence** in the AI's evidence-gap analysis (`EvidenceStrategistAgent.java:403-421`, consumed at `EvidenceStrategistAgent.java:219-222` and surfaced to the model at `EvidenceStrategistAgent.java:335-337`). Since AudioPeak Electronics only ever ships physical goods, entries like `usage_logs.json` (digital_service/subscription), `service_delivery_proof.json` (digital_service/travel), `ip_risk_report.json` (digital_service/travel/food_delivery), and `subscription_record.json` would always be reported as missing/weak evidence even on a fully documented case — this is a real, user-visible accuracy problem once every claim is guaranteed to be the same single physical-goods merchant.

**Fix:** for each of the 9 codes, strip `conditionalSources` down to just the scenario(s) that actually apply to AudioPeak:

- **4837, 4853, 4863, 4834, 4855, 4808** (all physical-goods scenarios already): delete the `digital_service`, `subscription`, `travel`, `food_delivery` keys entirely, keep only `physical_goods`.
  - **4834 exception:** its `physical_goods` bucket includes `pos_terminal_log.json` ("POS Terminal & Transaction Log") — a card-present/in-store concept. AudioPeak is e-commerce (card-not-present); the actual 4834 story (duplicate online orders) has no POS terminal file. Replace this entry with something that fits online duplicate-order defense (e.g. an order/invoice log entry) rather than carrying it over as-is.
- **4841**: `requiredSources` includes `usage_logs.json` labeled "Post-Cancellation Usage Logs" — assumes SaaS-style usage tracking that doesn't apply to a physical accessory-refill/replacement plan. Swap this for a field appropriate to the new 4841 story once its scenario is finalized (Change 5). The other required sources (`subscription_record.json`, `cancellation_policy.json`, `email_logs.json`, `terms_acceptance.json`, `auth_log.json`) already fit a recurring physical-goods plan and can stay.
- **4831, 4859**: currently have no `physical_goods` key at all (4831's only conditional key is `subscription`; 4859's are `digital_service`/`subscription`/`travel`/`physical_goods` — the last one already fits and stays, the other three get removed). Once their new AudioPeak scenarios are written (Change 5), add/confirm a `physical_goods`-appropriate conditional block matching the new story's actual evidence files.

This is a content edit to `src/data/reason-code-rules.json` only — no code change needed in `EvidenceStrategistAgent`, since simplifying the data itself is sufficient (the merge-all-scenarios behavior becomes harmless once there's only one scenario per code to merge).

### 6. Story rewrites — all files, both sides, per reason code

Every file under `src/data/sources/acquirer/{code}/**` and `src/data/sources/issuer/{code}/**` must be internally consistent with the AudioPeak Electronics identity and the fixed USD amount from Change 3 — not just the `order_details.json`/`cardholder_dispute_statement.json` "headline" files. That includes auth logs, settlement records, AVS/CVV checks, risk assessments, refund policies, email logs, fulfillment records, 3DS authentication, and the issuer-side chargeback documentation / transaction record — wherever the old merchant name, product, or amount appears.

**Image/PDF evidence audit:** checked every image/PDF under `src/data/sources` for pattern-only placeholders with no concrete supporting content. Findings: 4853's two image files (`product_listing_screenshot(2).jpg`, `battery_drain_evidence.png`) are genuine, detailed evidence mockups with concrete text/data already — no replacement needed. None of the other 8 in-scope codes have any image/PDF evidence today (JSON only). Two stray `.jpg` files exist directly under `acquirer/fraud-tools/` and `acquirer/merchant/` (not inside any reason-code folder) — real but unrelated Macy's documents, structurally invisible to the app since `DataSourceService` only reads `{acquirer|issuer}/{reasonCode}/{category}/*`; out of scope, left untouched.

| Code | Current story | Action |
|---|---|---|
| 4853 | AudioPeak Electronics — headphones | **No change** — already correct |
| 4837 | LuxeVision Online — designer sunglasses shipped to forwarding address, no 3DS | Rename to AudioPeak; swap sunglasses for an AudioPeak electronics product; keep the "shipped to non-billing address, no 3DS" fraud mechanic |
| 4863 | CaseArtisan — custom personalized phone case | Rename to AudioPeak; swap for an AudioPeak product with a matching billing-descriptor mechanic |
| 4834 | PhoneShield Pro — two separate orders 20 min apart (duplicate-processing defense) | Rename to AudioPeak; swap the two accessory orders for two AudioPeak products, keep the distinct-order-ID/SKU/auth-code defense |
| 4808 | RenewTech Store — refurbished laptop, 3DS, signed delivery | Rename to AudioPeak; swap laptop for refurbished/open-box AudioPeak audio gear |
| 4855 | HomeStitch Custom Décor — custom curtains, never shipped due to supply chain issue | Rename to AudioPeak; swap curtains for a custom/backordered AudioPeak product; keep "never shipped" mechanic |
| 4841 | CloudVault Premium — monthly cloud storage subscription (SaaS) | **New story** (SaaS doesn't fit an electronics retailer) — proposed: **"AudioPeak Sound+"** — a monthly accessory-refill/extended-replacement plan attached to a purchased device (e.g. replacement ear tips/cables shipped monthly, or extended battery-replacement coverage). Cancellation-takes-effect-at-end-of-cycle mechanic carries over. |
| 4859 | Seaside Grand Hotel — no-show room charge | **New story** — proposed: reinterpret 4859 under its non-hotel "addendum" meaning: a **post-purchase price-protection/addendum charge** — e.g. cardholder was charged an "order addendum" fee for an expedited-shipping upgrade added after checkout via a confirmation link, which the cardholder claims they never approved by phone but AudioPeak's system logged a recorded IVR confirmation. |
| 4831 | The Olive Garden Terrace — tip discrepancy on dine-in receipt | **New story** — proposed: **checkout price-mismatch** — cardholder expected a lower total shown at cart (e.g. before a promo code expired or a shipping surcharge was applied at payment step) and disputes the ~$X difference between the price they recall seeing and the amount posted; AudioPeak's defense is the order confirmation email and checkout session log showing the itemized total (subtotal + shipping + tax) the cardholder actually confirmed. |

The three "new story" scenarios above are proposals for your review before writing the actual JSON files — happy to adjust the mechanics if you have a different scenario in mind.

### 7. Fixed USD amounts per code (Change 3 lookup table)

To be finalized once the new/rewritten story content is written (each amount must match its story's own numbers):
- 4853: $199.99 (unchanged, already correct)
- 4837, 4863, 4834, 4808, 4855: carried over from each story's existing `unitPrice`/order total once the product is swapped (numbers can stay the same since only the product name changes, unless a more realistic AudioPeak price is preferred)
- 4841, 4859, 4831: set when the new story content is drafted

## Out of scope / explicitly not doing

- Not expanding `reason-code-rules.json` beyond the current 9 codes.
- Not touching the other ~26 reason-code folders under `src/data/sources/` — they simply stop being ingested once Change 1 lands.
- Not introducing a `Merchant` entity/table — this is a display-layer/persist-time normalization on existing `Dispute` columns.
- Not changing the existing `ingestion.max-new-claims=3` cap behavior itself, only the pool of codes it draws from.

## Open questions for implementation

- Exact new-story mechanics for 4841/4859/4831 (proposed above, pending your sign-off).
- Once 4841/4859/4831 story mechanics are approved, pin their `physical_goods`-equivalent `conditionalSources` block in `reason-code-rules.json` (Change 4) to match the actual evidence files being written.
- Whether `merchantId`/`merchantCategory` need overriding too, or `merchantName` alone is sufficient (to confirm no other field leaks the real merchant to the frontend).
- Final fixed USD amount for each of the 9 codes once story content is finalized.
