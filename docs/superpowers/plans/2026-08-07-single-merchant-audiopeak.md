# Single-Merchant AudioPeak Electronics Pivot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Pivot the dispute-management dashboard from a multi-merchant acquirer view to a single-merchant (AudioPeak Electronics) view: only ingest/show claims whose reason code is defined in `reason-code-rules.json`, hide already-persisted claims with undefined codes, force AudioPeak/USD identity on every in-scope dispute, and bring all 9 in-scope reason-code story folders (and the rules that grade them) in line with a single physical-goods e-commerce merchant.

**Architecture:** Backend-only change (Spring Boot / Java 17, Maven). Narrow `ClaimIngestionService`'s reason-code gate to read from `reason-code-rules.json` instead of scanning the issuer folder; add a new read-time filter to the two dispute-list read paths; add a single override step inside `ClaimDetailService.fetchAndStoreClaimDetail()` that stamps AudioPeak identity/USD amount on in-scope disputes right after Mastercard's real fields are written; edit `reason-code-rules.json` content; rewrite/author JSON evidence files under `src/data/sources/{acquirer,issuer}/{code}/**`.

**Tech Stack:** Java 17, Spring Boot, Spring Data JPA, Gson, JUnit 5, Mockito, `@SpringBootTest`/`@DataJpaTest`.

## Global Constraints

- In scope: only the 9 reason codes already defined in `src/data/reason-code-rules.json`: `4837, 4853, 4863, 4834, 4831, 4855, 4841, 4808, 4859`.
- Do not expand `reason-code-rules.json` beyond these 9 codes; do not touch the other ~26 story folders under `src/data/sources/`.
- `merchantName` → always `"AUDIOPEAK ELECTRONICS"`, `currency` → always `"USD"` for every in-scope dispute reaching the frontend. `amount` is replaced with a fixed per-code USD figure taken from that code's own story content (not Mastercard's real number relabeled).
- Ingestion-time filtering (which claims get inserted) and read-time filtering (which already-persisted rows get served to the frontend) are two separate, independently implemented mechanisms — do not conflate them into one filter.
- No new `Merchant` entity/table. This is a normalization of existing `Dispute` columns (`merchantName`, `amount`, `currency`).
- Do not change the `ingestion.max-new-claims=3` cap value or mechanism — only the pool of reason codes it draws new claims from.
- No image/PDF evidence file needs replacing (already audited — see spec).
- Merchant category code convention for all story JSON: `5999` (matches the existing 4853 AudioPeak story and all other current stories — do not introduce a new code).
- Every JSON evidence file for a rewritten/new code must be internally consistent: same merchant name, same product, same dollar amount, same card-present/card-not-present posture (AudioPeak is e-commerce — always card-not-present, AVS/CVV meaningful, no EMV chip fields) across every file in that code's acquirer and issuer folders.

---

## File Structure

**New files:**
- `src/main/java/com/opus/dispute/management/service/ReasonCodeRulesService.java` — loads and exposes the set of reason codes defined in `reason-code-rules.json`; single source of truth consumed by both the ingestion gate and the read-time filter.
- `src/main/java/com/opus/dispute/management/service/AudioPeakOverrideService.java` — holds the fixed reason-code → USD amount lookup and applies the AudioPeak/USD/amount override to a `Dispute`.
- `src/test/java/com/opus/dispute/management/service/ReasonCodeRulesServiceTest.java`
- `src/test/java/com/opus/dispute/management/service/AudioPeakOverrideServiceTest.java`
- `src/test/java/com/opus/dispute/management/repository/DisputeRepositoryReasonCodeFilterTest.java`

**Modified files:**
- `src/main/java/com/opus/dispute/management/service/ClaimIngestionService.java` — replace `loadSupportedReasonCodes()` to delegate to `ReasonCodeRulesService`.
- `src/main/java/com/opus/dispute/management/service/ClaimDetailService.java` — call `AudioPeakOverrideService` at the end of `fetchAndStoreClaimDetail()` (before the save at line 172) and at the end of `populateDetailsFromLocalData()`.
- `src/main/java/com/opus/dispute/management/service/DisputeService.java` — `getAllDisputes()` filters through `ReasonCodeRulesService`.
- `src/main/java/com/opus/dispute/management/repository/DisputeRepository.java` — add `findByReasonCodeIn(Collection<String> codes)` and `findByReasonCodeIn(Collection<String> codes, Pageable pageable)`.
- `src/main/java/com/opus/dispute/management/controller/IngestionController.java` — `getIngestedDisputes()` uses the new paginated repository method with the defined-code set.
- `src/data/reason-code-rules.json` — strip `conditionalSources` to `physical_goods`-only per code (Change 4 in the spec), fix the 4834 `pos_terminal_log.json` entry, fix the 4841 `usage_logs.json` required-source entry, add `physical_goods` conditional blocks for 4831/4859 matching their new story evidence files.
- 57 rewritten JSON files under `src/data/sources/acquirer/{4837,4863,4834,4855,4808}/**` and `src/data/sources/issuer/{4837,4863,4834,4855,4808}/**` (rename/reskin, existing file paths — 4837: 11, 4863: 11, 4834: 12, 4855: 11, 4808: 12)
- 33 rewritten JSON files under `src/data/sources/acquirer/{4841,4859,4831}/**` and `src/data/sources/issuer/{4841,4859,4831}/**` (new story content, existing file paths — 4841: 12, 4859: 11, 4831: 10)

---

## Task 1: `ReasonCodeRulesService` — shared source of defined reason codes

**Files:**
- Create: `src/main/java/com/opus/dispute/management/service/ReasonCodeRulesService.java`
- Test: `src/test/java/com/opus/dispute/management/service/ReasonCodeRulesServiceTest.java`

**Interfaces:**
- Produces: `public Set<String> getSupportedReasonCodes()` — returns the key set of `src/data/reason-code-rules.json`, loaded once and cached. Consumed by Task 2 (`ClaimIngestionService`), Task 4 (`DisputeService`), Task 5 (`DisputeRepository`/`IngestionController`).

- [ ] **Step 1: Write the failing test**

```java
package com.opus.dispute.management.service;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class ReasonCodeRulesServiceTest {

    @Test
    void loadsAllNineDefinedReasonCodes() {
        ReasonCodeRulesService service = new ReasonCodeRulesService();

        Set<String> codes = service.getSupportedReasonCodes();

        assertThat(codes).containsExactlyInAnyOrder(
                "4837", "4853", "4863", "4834", "4831", "4855", "4841", "4808", "4859"
        );
    }

    @Test
    void doesNotContainCodesAbsentFromRulesFile() {
        ReasonCodeRulesService service = new ReasonCodeRulesService();

        Set<String> codes = service.getSupportedReasonCodes();

        assertThat(codes).doesNotContain("4801", "4802", "4900", "4871");
    }

    @Test
    void repeatedCallsReturnSameCachedSet() {
        ReasonCodeRulesService service = new ReasonCodeRulesService();

        Set<String> first = service.getSupportedReasonCodes();
        Set<String> second = service.getSupportedReasonCodes();

        assertThat(first).isSameAs(second);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=ReasonCodeRulesServiceTest test`
Expected: FAIL — compile error, `ReasonCodeRulesService` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.opus.dispute.management.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

@Slf4j
@Service
public class ReasonCodeRulesService {

    private static final Path RULES_FILE = Paths.get("src/data/reason-code-rules.json");

    private volatile Set<String> cachedCodes;

    public Set<String> getSupportedReasonCodes() {
        if (cachedCodes == null) {
            synchronized (this) {
                if (cachedCodes == null) {
                    cachedCodes = loadReasonCodes();
                }
            }
        }
        return cachedCodes;
    }

    private Set<String> loadReasonCodes() {
        try {
            String json = Files.readString(RULES_FILE);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return Collections.unmodifiableSet(root.keySet());
        } catch (IOException e) {
            log.error("Failed to load reason-code-rules.json from {}", RULES_FILE, e);
            return Collections.emptySet();
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=ReasonCodeRulesServiceTest test`
Expected: PASS (all 3 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/opus/dispute/management/service/ReasonCodeRulesService.java src/test/java/com/opus/dispute/management/service/ReasonCodeRulesServiceTest.java
git commit -m "Add ReasonCodeRulesService as shared source of defined reason codes"
```

---

## Task 2: Narrow `ClaimIngestionService`'s ingestion gate to `ReasonCodeRulesService`

**Files:**
- Modify: `src/main/java/com/opus/dispute/management/service/ClaimIngestionService.java:30-65` (remove `loadSupportedReasonCodes()` static method and `SUPPORTED_REASON_CODES` static field; inject `ReasonCodeRulesService`)
- Modify: `src/main/java/com/opus/dispute/management/service/ClaimIngestionService.java:226-233` (use the injected service instead of the static set)
- Test: `src/test/java/com/opus/dispute/management/service/ClaimIngestionServiceReasonCodeGateTest.java`

**Interfaces:**
- Consumes: `ReasonCodeRulesService.getSupportedReasonCodes()` from Task 1.

- [ ] **Step 1: Write the failing test**

```java
package com.opus.dispute.management.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.repository.DisputeRepository;
import com.opus.dispute.management.repository.IngestionStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimIngestionServiceReasonCodeGateTest {

    @Mock
    private MastercardApiClient mastercardApiClient;
    @Mock
    private DisputeRepository disputeRepository;
    @Mock
    private IngestionStateRepository ingestionStateRepository;
    @Mock
    private ClaimDetailService claimDetailService;
    @Mock
    private ReasonCodeRulesService reasonCodeRulesService;

    @InjectMocks
    private ClaimIngestionService claimIngestionService;

    @Test
    void deletesNewlyInsertedDisputeWhenReasonCodeNotInDefinedSet() throws Exception {
        when(reasonCodeRulesService.getSupportedReasonCodes()).thenReturn(Set.of("4853", "4837"));

        JsonObject responseObj = JsonParser.parseString(
                "{\"pageCount\":\"1\",\"claimList\":[{\"claimId\":\"999\"}]}"
        ).getAsJsonObject();
        when(mastercardApiClient.post(any(), any())).thenReturn(responseObj.toString());
        when(disputeRepository.findByClaimId("999")).thenReturn(Optional.empty());

        Dispute saved = new Dispute();
        saved.setId(1L);
        saved.setClaimId("999");
        when(disputeRepository.save(any(Dispute.class))).thenReturn(saved);

        Dispute refreshedWithUnsupportedCode = new Dispute();
        refreshedWithUnsupportedCode.setId(1L);
        refreshedWithUnsupportedCode.setClaimId("999");
        refreshedWithUnsupportedCode.setReasonCode("4999");
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(refreshedWithUnsupportedCode));

        claimIngestionService.ingestFromQueues(null, null);

        verify(disputeRepository).deleteById(1L);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=ClaimIngestionServiceReasonCodeGateTest test`
Expected: FAIL — compile error (`ReasonCodeRulesService` field not present on `ClaimIngestionService`, constructor mismatch) or the static-set-based gate still uses issuer-folder scanning so behavior differs from the injected mock.

- [ ] **Step 3: Write minimal implementation**

Remove the static block and field (`ClaimIngestionService.java:36-50`):

```java
    private static final String[] QUEUES_TO_POLL = {"Pending", "Rejects", "Unworked"};
```
(delete `SUPPORTED_REASON_CODES` field and `loadSupportedReasonCodes()` method entirely)

Add a constructor-injected field and update the constructor (`ClaimIngestionService.java:55-65`):

```java
    private final MastercardApiClient mastercardApiClient;
    private final DisputeRepository disputeRepository;
    private final IngestionStateRepository ingestionStateRepository;
    private final ClaimDetailService claimDetailService;
    private final ReasonCodeRulesService reasonCodeRulesService;
    private final Gson gson = new Gson();

    public ClaimIngestionService(MastercardApiClient mastercardApiClient,
                                  DisputeRepository disputeRepository,
                                  IngestionStateRepository ingestionStateRepository,
                                  ClaimDetailService claimDetailService,
                                  ReasonCodeRulesService reasonCodeRulesService) {
        this.mastercardApiClient = mastercardApiClient;
        this.disputeRepository = disputeRepository;
        this.ingestionStateRepository = ingestionStateRepository;
        this.claimDetailService = claimDetailService;
        this.reasonCodeRulesService = reasonCodeRulesService;
    }
```

Update the gate check at `ClaimIngestionService.java:226-233`:

```java
                        Dispute refreshed = disputeRepository.findById(saved.getId()).orElse(saved);
                        String rc = refreshed.getReasonCode();
                        if (rc == null || rc.isBlank() || !reasonCodeRulesService.getSupportedReasonCodes().contains(rc)) {
                            log.info("Dispute {} (claimId={}) has unsupported reason code '{}' — deleting",
                                    saved.getId(), saved.getClaimId(), rc);
                            disputeRepository.deleteById(saved.getId());
                            skipped++;
                            continue;
                        }
                        newDisputeIds.add(saved.getId());
```

Remove now-unused imports (`java.io.IOException`, `java.nio.file.Files`, `java.nio.file.Path`, `java.nio.file.Paths`, `java.util.stream.Collectors`, `java.util.stream.Stream`, `java.util.Set` if no longer referenced elsewhere in the file — check remaining usages first).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=ClaimIngestionServiceReasonCodeGateTest test`
Expected: PASS

- [ ] **Step 5: Run full existing test suite to check for regressions**

Run: `mvn test`
Expected: PASS — no other test references `SUPPORTED_REASON_CODES` or the old constructor signature (confirm via the compile step; if other tests construct `ClaimIngestionService` directly, update their constructor calls to pass a `ReasonCodeRulesService` mock/instance).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/opus/dispute/management/service/ClaimIngestionService.java src/test/java/com/opus/dispute/management/service/ClaimIngestionServiceReasonCodeGateTest.java
git commit -m "Narrow ingestion reason-code gate to reason-code-rules.json via ReasonCodeRulesService"
```

---

## Task 3: `AudioPeakOverrideService` — fixed merchant/currency/amount override

**Files:**
- Create: `src/main/java/com/opus/dispute/management/service/AudioPeakOverrideService.java`
- Test: `src/test/java/com/opus/dispute/management/service/AudioPeakOverrideServiceTest.java`

**Interfaces:**
- Produces: `public void applyOverride(Dispute dispute)` — if `dispute.getReasonCode()` is a key in the internal amount map, sets `merchantName="AUDIOPEAK ELECTRONICS"`, `currency="USD"`, `amount=<fixed value for that code>`. No-ops (leaves the dispute untouched) if the reason code isn't one of the 9 defined codes. Consumed by Task 6 (`ClaimDetailService`).

The fixed USD amounts (pinned to each code's story content — see Task 7 onward for the source of truth per code):

| Code | Amount | Source |
|---|---|---|
| 4853 | 199.99 | Existing AudioPeak headphones story (unchanged) |
| 4837 | 249.99 | New AudioPeak product substituted for the sunglasses (Task 9) |
| 4863 | 34.99 | New AudioPeak product substituted for the custom phone case (Task 10) |
| 4834 | 159.98 | Sum of the two AudioPeak accessory orders substituted for PhoneShield (Task 11) |
| 4808 | 449.00 | Refurbished AudioPeak product substituted for the laptop (Task 12) |
| 4855 | 179.99 | Backordered AudioPeak product substituted for the curtains (Task 13) |
| 4841 | 12.99 | New "AudioPeak Sound+" replacement-parts plan (Task 14) |
| 4859 | 24.99 | New shipping-upgrade addendum story (Task 15) |
| 4831 | 98.49 | New checkout price-mismatch story — the full itemized checkout total (subtotal $79.99 + shipping $4.99 + tax $13.51), not the $18.50 disputed difference (Task 16) |

- [ ] **Step 1: Write the failing test**

```java
package com.opus.dispute.management.service;

import com.opus.dispute.management.entity.Dispute;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AudioPeakOverrideServiceTest {

    private final AudioPeakOverrideService service = new AudioPeakOverrideService();

    @Test
    void overridesMerchantCurrencyAndAmountForDefinedReasonCode() {
        Dispute dispute = new Dispute();
        dispute.setReasonCode("4853");
        dispute.setMerchantName("Wegmans Food Market");
        dispute.setCurrency("EUR");
        dispute.setAmount(993.91);

        service.applyOverride(dispute);

        assertThat(dispute.getMerchantName()).isEqualTo("AUDIOPEAK ELECTRONICS");
        assertThat(dispute.getCurrency()).isEqualTo("USD");
        assertThat(dispute.getAmount()).isEqualTo(199.99);
    }

    @Test
    void appliesCorrectFixedAmountPerReasonCode() {
        Dispute dispute = new Dispute();
        dispute.setReasonCode("4841");

        service.applyOverride(dispute);

        assertThat(dispute.getAmount()).isEqualTo(12.99);
    }

    @Test
    void doesNothingWhenReasonCodeIsNotDefined() {
        Dispute dispute = new Dispute();
        dispute.setReasonCode("4801");
        dispute.setMerchantName("Some Other Merchant");
        dispute.setCurrency("GBP");
        dispute.setAmount(50.0);

        service.applyOverride(dispute);

        assertThat(dispute.getMerchantName()).isEqualTo("Some Other Merchant");
        assertThat(dispute.getCurrency()).isEqualTo("GBP");
        assertThat(dispute.getAmount()).isEqualTo(50.0);
    }

    @Test
    void doesNothingWhenReasonCodeIsNull() {
        Dispute dispute = new Dispute();
        dispute.setReasonCode(null);
        dispute.setMerchantName("Original Name");

        service.applyOverride(dispute);

        assertThat(dispute.getMerchantName()).isEqualTo("Original Name");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=AudioPeakOverrideServiceTest test`
Expected: FAIL — compile error, class does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.opus.dispute.management.service;

import com.opus.dispute.management.entity.Dispute;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AudioPeakOverrideService {

    private static final String MERCHANT_NAME = "AUDIOPEAK ELECTRONICS";
    private static final String CURRENCY = "USD";

    private static final Map<String, Double> FIXED_AMOUNTS_BY_REASON_CODE = Map.ofEntries(
            Map.entry("4853", 199.99),
            Map.entry("4837", 249.99),
            Map.entry("4863", 34.99),
            Map.entry("4834", 159.98),
            Map.entry("4808", 449.00),
            Map.entry("4855", 179.99),
            Map.entry("4841", 12.99),
            Map.entry("4859", 24.99),
            Map.entry("4831", 98.49)
    );

    public void applyOverride(Dispute dispute) {
        String reasonCode = dispute.getReasonCode();
        if (reasonCode == null || !FIXED_AMOUNTS_BY_REASON_CODE.containsKey(reasonCode)) {
            return;
        }
        dispute.setMerchantName(MERCHANT_NAME);
        dispute.setCurrency(CURRENCY);
        dispute.setAmount(FIXED_AMOUNTS_BY_REASON_CODE.get(reasonCode));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=AudioPeakOverrideServiceTest test`
Expected: PASS (all 4 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/opus/dispute/management/service/AudioPeakOverrideService.java src/test/java/com/opus/dispute/management/service/AudioPeakOverrideServiceTest.java
git commit -m "Add AudioPeakOverrideService with fixed merchant/currency/amount per reason code"
```

---

## Task 4: Wire `AudioPeakOverrideService` into `ClaimDetailService`

**Files:**
- Modify: `src/main/java/com/opus/dispute/management/service/ClaimDetailService.java` (constructor/field injection, plus two call sites)
- Test: `src/test/java/com/opus/dispute/management/service/ClaimDetailServiceAudioPeakOverrideTest.java`

**Interfaces:**
- Consumes: `AudioPeakOverrideService.applyOverride(Dispute)` from Task 3.

- [ ] **Step 1: Write the failing test**

```java
package com.opus.dispute.management.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.repository.DisputeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimDetailServiceAudioPeakOverrideTest {

    @Mock
    private MastercardApiClient mastercardApiClient;
    @Mock
    private DisputeRepository disputeRepository;

    @Test
    void appliesAudioPeakOverrideAfterOverwritingRealMastercardFields() throws Exception {
        AudioPeakOverrideService realOverrideService = new AudioPeakOverrideService();
        ClaimDetailService claimDetailService = ClaimDetailServiceTestFactory.create(
                mastercardApiClient, disputeRepository, realOverrideService);

        Dispute dispute = new Dispute();
        dispute.setId(1L);
        dispute.setClaimId("200002022667");
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(dispute));

        JsonObject detailResponse = JsonParser.parseString(
                "{\"reasonCode\":\"4853\",\"claimValue\":\"993.91 EUR\"}"
        ).getAsJsonObject();
        when(mastercardApiClient.get(any())).thenReturn(detailResponse.toString());

        claimDetailService.fetchAndStoreClaimDetail(1L);

        ArgumentCaptor<Dispute> captor = ArgumentCaptor.forClass(Dispute.class);
        verify(disputeRepository, atLeastOnce()).save(captor.capture());
        Dispute saved = captor.getValue();

        assertThat(saved.getMerchantName()).isEqualTo("AUDIOPEAK ELECTRONICS");
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getAmount()).isEqualTo(199.99);
    }
}
```

Note: `ClaimDetailServiceTestFactory` is a small test-only helper needed because `ClaimDetailService` has many constructor dependencies (chargeback/retrieval/fee/case-filing repositories) beyond the two being tested here — create it alongside this test:

```java
package com.opus.dispute.management.service;

import com.opus.dispute.management.repository.*;
import org.mockito.Mockito;

class ClaimDetailServiceTestFactory {
    static ClaimDetailService create(MastercardApiClient client, DisputeRepository disputeRepository,
                                      AudioPeakOverrideService overrideService) {
        return new ClaimDetailService(
                client,
                disputeRepository,
                Mockito.mock(ChargebackDetailRepository.class),
                Mockito.mock(RetrievalDetailRepository.class),
                Mockito.mock(FeeDetailRepository.class),
                Mockito.mock(CaseFilingDetailRepository.class),
                overrideService
        );
    }
}
```

This factory's argument list matches `ClaimDetailService`'s current 6-arg constructor exactly (`MastercardApiClient`, `DisputeRepository`, `ChargebackDetailRepository`, `RetrievalDetailRepository`, `FeeDetailRepository`, `CaseFilingDetailRepository`) plus the new 7th `AudioPeakOverrideService` argument added in Step 3 below.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=ClaimDetailServiceAudioPeakOverrideTest test`
Expected: FAIL — `ClaimDetailService` constructor does not accept `AudioPeakOverrideService`, or `saved.getMerchantName()` is still the real/unset value (override not wired in yet).

- [ ] **Step 3: Write minimal implementation**

`ClaimDetailService`'s current constructor (`ClaimDetailService.java:39-51`) is:

```java
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
```

Add a 7th field and constructor parameter:

```java
    private final MastercardApiClient mastercardApiClient;
    private final DisputeRepository disputeRepository;
    private final ChargebackDetailRepository chargebackDetailRepository;
    private final RetrievalDetailRepository retrievalDetailRepository;
    private final FeeDetailRepository feeDetailRepository;
    private final CaseFilingDetailRepository caseFilingDetailRepository;
    private final AudioPeakOverrideService audioPeakOverrideService;
    private final Gson gson = new Gson();

    public ClaimDetailService(MastercardApiClient mastercardApiClient,
                              DisputeRepository disputeRepository,
                              ChargebackDetailRepository chargebackDetailRepository,
                              RetrievalDetailRepository retrievalDetailRepository,
                              FeeDetailRepository feeDetailRepository,
                              CaseFilingDetailRepository caseFilingDetailRepository,
                              AudioPeakOverrideService audioPeakOverrideService) {
        this.mastercardApiClient = mastercardApiClient;
        this.disputeRepository = disputeRepository;
        this.chargebackDetailRepository = chargebackDetailRepository;
        this.retrievalDetailRepository = retrievalDetailRepository;
        this.feeDetailRepository = feeDetailRepository;
        this.caseFilingDetailRepository = caseFilingDetailRepository;
        this.audioPeakOverrideService = audioPeakOverrideService;
    }
```

Add the override call at the end of `fetchAndStoreClaimDetail()`, immediately before the save at line 172:

```java
            dispute.setDetailsFetched(true);
            dispute.setLastUpdatedDate(LocalDateTime.now());
            audioPeakOverrideService.applyOverride(dispute);
            disputeRepository.save(dispute);
```

Add the same call to `populateDetailsFromLocalData()`, immediately before its save (around line 220-222):

```java
        dispute.setDetailsFetched(true);
        dispute.setLastUpdatedDate(LocalDateTime.now());
        audioPeakOverrideService.applyOverride(dispute);
        disputeRepository.save(dispute);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=ClaimDetailServiceAudioPeakOverrideTest test`
Expected: PASS

- [ ] **Step 5: Run full existing test suite to check for regressions**

Run: `mvn test`
Expected: PASS — check whether any other test constructs `ClaimDetailService` directly and needs the new constructor argument added.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/opus/dispute/management/service/ClaimDetailService.java src/test/java/com/opus/dispute/management/service/ClaimDetailServiceAudioPeakOverrideTest.java src/test/java/com/opus/dispute/management/service/ClaimDetailServiceTestFactory.java
git commit -m "Apply AudioPeak identity/USD override after Mastercard fields are written in ClaimDetailService"
```

---

## Task 5: Read-time filter — `DisputeRepository.findByReasonCodeIn`

**Files:**
- Modify: `src/main/java/com/opus/dispute/management/repository/DisputeRepository.java`
- Test: `src/test/java/com/opus/dispute/management/repository/DisputeRepositoryReasonCodeFilterTest.java`

**Interfaces:**
- Produces: `List<Dispute> findByReasonCodeIn(Collection<String> reasonCodes)` and `Page<Dispute> findByReasonCodeIn(Collection<String> reasonCodes, Pageable pageable)`. Consumed by Task 6 (`DisputeService`) and Task 7 (`IngestionController`).

- [ ] **Step 1: Write the failing test**

```java
package com.opus.dispute.management.repository;

import com.opus.dispute.management.entity.Dispute;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DisputeRepositoryReasonCodeFilterTest {

    @Autowired
    private DisputeRepository disputeRepository;

    @Test
    void findByReasonCodeInReturnsOnlyMatchingDisputes() {
        saveDisputeWithReasonCode("CLAIM-1", "4853");
        saveDisputeWithReasonCode("CLAIM-2", "4801");
        saveDisputeWithReasonCode("CLAIM-3", "4837");

        List<Dispute> result = disputeRepository.findByReasonCodeIn(Set.of("4853", "4837"));

        assertThat(result).extracting(Dispute::getClaimId)
                .containsExactlyInAnyOrder("CLAIM-1", "CLAIM-3");
    }

    @Test
    void findByReasonCodeInPagedReturnsOnlyMatchingDisputes() {
        saveDisputeWithReasonCode("CLAIM-1", "4853");
        saveDisputeWithReasonCode("CLAIM-2", "4801");

        Page<Dispute> result = disputeRepository.findByReasonCodeIn(Set.of("4853"), PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(Dispute::getClaimId)
                .containsExactly("CLAIM-1");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    private void saveDisputeWithReasonCode(String claimId, String reasonCode) {
        Dispute dispute = new Dispute();
        dispute.setClaimId(claimId);
        dispute.setReasonCode(reasonCode);
        disputeRepository.save(dispute);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=DisputeRepositoryReasonCodeFilterTest test`
Expected: FAIL — compile error, `findByReasonCodeIn` methods do not exist on `DisputeRepository`.

- [ ] **Step 3: Write minimal implementation**

```java
package com.opus.dispute.management.repository;

import com.opus.dispute.management.entity.Dispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    Optional<Dispute> findByClaimId(String claimId);

    boolean existsByClaimId(String claimId);

    List<Dispute> findByReasonCodeIn(Collection<String> reasonCodes);

    Page<Dispute> findByReasonCodeIn(Collection<String> reasonCodes, Pageable pageable);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=DisputeRepositoryReasonCodeFilterTest test`
Expected: PASS (both tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/opus/dispute/management/repository/DisputeRepository.java src/test/java/com/opus/dispute/management/repository/DisputeRepositoryReasonCodeFilterTest.java
git commit -m "Add findByReasonCodeIn query methods to DisputeRepository"
```

---

## Task 6: Wire the read-time filter into `DisputeService.getAllDisputes()`

**Files:**
- Modify: `src/main/java/com/opus/dispute/management/service/DisputeService.java`
- Test: `src/test/java/com/opus/dispute/management/service/DisputeServiceReasonCodeFilterTest.java`

**Interfaces:**
- Consumes: `ReasonCodeRulesService.getSupportedReasonCodes()` (Task 1), `DisputeRepository.findByReasonCodeIn(Collection<String>)` (Task 5).

- [ ] **Step 1: Write the failing test**

```java
package com.opus.dispute.management.service;

import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.repository.DisputeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeServiceReasonCodeFilterTest {

    @Mock
    private DisputeRepository disputeRepository;
    @Mock
    private ReasonCodeRulesService reasonCodeRulesService;

    @InjectMocks
    private DisputeService disputeService;

    @Test
    void getAllDisputesFiltersByDefinedReasonCodes() {
        Set<String> definedCodes = Set.of("4853", "4837");
        when(reasonCodeRulesService.getSupportedReasonCodes()).thenReturn(definedCodes);

        Dispute matching = new Dispute();
        matching.setClaimId("CLAIM-1");
        matching.setReasonCode("4853");
        when(disputeRepository.findByReasonCodeIn(definedCodes)).thenReturn(List.of(matching));

        List<Dispute> result = disputeService.getAllDisputes();

        assertThat(result).containsExactly(matching);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=DisputeServiceReasonCodeFilterTest test`
Expected: FAIL — `DisputeService` has no `ReasonCodeRulesService` field to autowire in the test, or `getAllDisputes()` still calls `findAll()`.

- [ ] **Step 3: Write minimal implementation**

```java
package com.opus.dispute.management.service;

import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.repository.DisputeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DisputeService {

    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private ReasonCodeRulesService reasonCodeRulesService;

    public Dispute createDispute(Dispute dispute) {
        dispute.setClaimId(UUID.randomUUID().toString());
        dispute.setIngestedAt(LocalDateTime.now());
        dispute.setLastUpdatedDate(LocalDateTime.now());
        dispute.setStatus("INITIATED");
        return disputeRepository.save(dispute);
    }

    public List<Dispute> getAllDisputes() {
        return disputeRepository.findByReasonCodeIn(reasonCodeRulesService.getSupportedReasonCodes());
    }

    public Optional<Dispute> getDisputeById(Long id) {
        return disputeRepository.findById(id);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=DisputeServiceReasonCodeFilterTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/opus/dispute/management/service/DisputeService.java src/test/java/com/opus/dispute/management/service/DisputeServiceReasonCodeFilterTest.java
git commit -m "Filter GET /api/disputes to only defined reason codes"
```

---

## Task 7: Wire the read-time filter into `IngestionController.getIngestedDisputes()`

**Files:**
- Modify: `src/main/java/com/opus/dispute/management/controller/IngestionController.java:131-151`
- Test: `src/test/java/com/opus/dispute/management/controller/IngestionControllerDisputesEndpointTest.java`

**Interfaces:**
- Consumes: `ReasonCodeRulesService.getSupportedReasonCodes()` (Task 1), `DisputeRepository.findByReasonCodeIn(Collection<String>, Pageable)` (Task 5).

- [ ] **Step 1: Write the failing test**

```java
package com.opus.dispute.management.controller;

import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.repository.CaseFilingDetailRepository;
import com.opus.dispute.management.repository.ChargebackDetailRepository;
import com.opus.dispute.management.repository.DisputeRepository;
import com.opus.dispute.management.repository.EvidenceMapRepository;
import com.opus.dispute.management.repository.FeeDetailRepository;
import com.opus.dispute.management.repository.RetrievalDetailRepository;
import com.opus.dispute.management.service.ClaimIngestionService;
import com.opus.dispute.management.service.PostIngestionPipelineService;
import com.opus.dispute.management.service.ReasonCodeRulesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestionControllerDisputesEndpointTest {

    @Mock
    private ClaimIngestionService claimIngestionService;
    @Mock
    private DisputeRepository disputeRepository;
    @Mock
    private ChargebackDetailRepository chargebackDetailRepository;
    @Mock
    private RetrievalDetailRepository retrievalDetailRepository;
    @Mock
    private FeeDetailRepository feeDetailRepository;
    @Mock
    private CaseFilingDetailRepository caseFilingDetailRepository;
    @Mock
    private EvidenceMapRepository evidenceMapRepository;
    @Mock
    private PostIngestionPipelineService postIngestionPipelineService;
    @Mock
    private ReasonCodeRulesService reasonCodeRulesService;

    @InjectMocks
    private IngestionController ingestionController;

    @Test
    void getIngestedDisputesUsesReasonCodeFilteredQuery() {
        Set<String> definedCodes = Set.of("4853");
        when(reasonCodeRulesService.getSupportedReasonCodes()).thenReturn(definedCodes);

        Dispute matching = new Dispute();
        matching.setClaimId("CLAIM-1");
        matching.setReasonCode("4853");
        when(disputeRepository.findByReasonCodeIn(eq(definedCodes), eq(PageRequest.of(0, 50))))
                .thenReturn(new PageImpl<>(List.of(matching)));

        ResponseEntity<Map<String, Object>> response = ingestionController.getIngestedDisputes(0, 50);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        @SuppressWarnings("unchecked")
        List<Dispute> disputes = (List<Dispute>) response.getBody().get("disputes");
        assertThat(disputes).containsExactly(matching);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=IngestionControllerDisputesEndpointTest test`
Expected: FAIL — `IngestionController` has no `ReasonCodeRulesService` field, or the method still calls `disputeRepository.findAll(PageRequest...)`.

- [ ] **Step 3: Write minimal implementation**

`IngestionController` uses constructor injection with `private final` fields (`IngestionController.java:39-64`), not `@Autowired` field injection. Add `ReasonCodeRulesService` as a 9th constructor-injected field, following the same pattern as the existing 8:

```java
    private final ClaimIngestionService claimIngestionService;
    private final DisputeRepository disputeRepository;
    private final ChargebackDetailRepository chargebackDetailRepository;
    private final RetrievalDetailRepository retrievalDetailRepository;
    private final FeeDetailRepository feeDetailRepository;
    private final CaseFilingDetailRepository caseFilingDetailRepository;
    private final EvidenceMapRepository evidenceMapRepository;
    private final PostIngestionPipelineService postIngestionPipelineService;
    private final ReasonCodeRulesService reasonCodeRulesService;

    public IngestionController(ClaimIngestionService claimIngestionService,
                                DisputeRepository disputeRepository,
                                ChargebackDetailRepository chargebackDetailRepository,
                                RetrievalDetailRepository retrievalDetailRepository,
                                FeeDetailRepository feeDetailRepository,
                                CaseFilingDetailRepository caseFilingDetailRepository,
                                EvidenceMapRepository evidenceMapRepository,
                                PostIngestionPipelineService postIngestionPipelineService,
                                ReasonCodeRulesService reasonCodeRulesService) {
        this.claimIngestionService = claimIngestionService;
        this.disputeRepository = disputeRepository;
        this.chargebackDetailRepository = chargebackDetailRepository;
        this.retrievalDetailRepository = retrievalDetailRepository;
        this.feeDetailRepository = feeDetailRepository;
        this.caseFilingDetailRepository = caseFilingDetailRepository;
        this.evidenceMapRepository = evidenceMapRepository;
        this.postIngestionPipelineService = postIngestionPipelineService;
        this.reasonCodeRulesService = reasonCodeRulesService;
    }
```

Then update the endpoint at lines 131-151:

```java
    @GetMapping("/disputes")
    public ResponseEntity<Map<String, Object>> getIngestedDisputes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Page<Dispute> disputes = disputeRepository.findByReasonCodeIn(
                    reasonCodeRulesService.getSupportedReasonCodes(), PageRequest.of(page, size));

            Map<String, Object> response = new HashMap<>();
            response.put("disputes", disputes.getContent());
            response.put("totalElements", disputes.getTotalElements());
            response.put("totalPages", disputes.getTotalPages());
            response.put("currentPage", disputes.getNumber());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get ingested disputes", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=IngestionControllerDisputesEndpointTest test`
Expected: PASS

- [ ] **Step 5: Run full existing test suite to check for regressions**

Run: `mvn test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/opus/dispute/management/controller/IngestionController.java src/test/java/com/opus/dispute/management/controller/IngestionControllerDisputesEndpointTest.java
git commit -m "Filter GET /api/ingestion/disputes to only defined reason codes"
```

---

## Task 8: Update `reason-code-rules.json` conditional/required sources

**Files:**
- Modify: `src/data/reason-code-rules.json`

- [ ] **Step 1: Strip `conditionalSources` to `physical_goods`-only for 4837, 4853, 4863, 4855, 4808**

For each of these 5 codes, delete the `digital_service`, `subscription`, `travel`, `food_delivery` keys inside `conditionalSources`, keeping only the `physical_goods` array unchanged.

- [ ] **Step 2: Fix 4834's `conditionalSources.physical_goods`**

Replace:
```json
"physical_goods": [
  { "source": "merchant", "file": "pos_terminal_log.json", "label": "POS Terminal & Transaction Log", "priority": "high" },
  { "source": "shipping", "file": "delivery_confirmation.json", "label": "Delivery Confirmation", "priority": "medium" }
]
```
with:
```json
"physical_goods": [
  { "source": "merchant", "file": "order_details.json", "label": "Order & Invoice Log", "priority": "high" },
  { "source": "psp", "file": "settlement_record.json", "label": "Settlement Record — Distinct Auth Codes", "priority": "high" }
]
```
Delete the `digital_service`, `subscription`, `travel` keys from 4834's `conditionalSources`.

- [ ] **Step 3: Fix 4841's `requiredSources`**

Replace the `usage_logs.json` entry:
```json
{ "source": "merchant", "file": "usage_logs.json", "label": "Post-Cancellation Usage Logs", "priority": "critical" }
```
with:
```json
{ "source": "merchant", "file": "fulfillment_record.json", "label": "Post-Cancellation Shipment Record", "priority": "critical" }
```
(matches the new AudioPeak Sound+ replacement-parts plan in Task 13, which tracks shipments, not "usage".)

- [ ] **Step 4: Add `physical_goods` conditional block to 4831**

Replace `"conditionalSources": {}` entirely for 4831 with:
```json
"conditionalSources": {
  "physical_goods": [
    { "source": "merchant", "file": "order_details.json", "label": "Checkout Session & Itemized Total", "priority": "critical" },
    { "source": "customer-comms", "file": "email_logs.json", "label": "Order Confirmation Email", "priority": "high" }
  ]
}
```

- [ ] **Step 5: Fix 4859's `conditionalSources`**

Replace the existing `digital_service`/`subscription`/`travel`/`physical_goods` keys with just:
```json
"conditionalSources": {
  "physical_goods": [
    { "source": "customer-comms", "file": "email_logs.json", "label": "IVR Confirmation & Addendum Notice", "priority": "critical" },
    { "source": "merchant", "file": "order_details.json", "label": "Order Addendum Record", "priority": "high" }
  ]
}
```

- [ ] **Step 6: Validate JSON syntax**

Run: `powershell -Command "Get-Content src/data/reason-code-rules.json -Raw | ConvertFrom-Json | Out-Null; Write-Host 'valid'"`
Expected: prints `valid`, no parse error.

- [ ] **Step 7: Run the evidence-strategist-related unit tests (if any reference this file) and full suite**

Run: `mvn test`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/data/reason-code-rules.json
git commit -m "Simplify reason-code-rules.json conditionalSources for single physical-goods merchant"
```

---

## Task 9: Rewrite 4837 story — LuxeVision Online → AudioPeak Electronics

**Files:**
- Modify: `src/data/sources/acquirer/4837/customer-comms/email_logs.json`
- Modify: `src/data/sources/acquirer/4837/fraud-tools/risk_assessment.json`
- Modify: `src/data/sources/acquirer/4837/identity/avs_cvv_check.json`
- Modify: `src/data/sources/acquirer/4837/merchant/fulfillment_record.json`
- Modify: `src/data/sources/acquirer/4837/merchant/order_details.json`
- Modify: `src/data/sources/acquirer/4837/merchant/refund_policy.json`
- Modify: `src/data/sources/acquirer/4837/psp/auth_log.json`
- Modify: `src/data/sources/acquirer/4837/psp/settlement_record.json`
- Modify: `src/data/sources/issuer/4837/customer-comms/cardholder_dispute_statement.json`
- Modify: `src/data/sources/issuer/4837/merchant/issuer_chargeback_documentation.json`
- Modify: `src/data/sources/issuer/4837/psp/issuer_transaction_record.json`

Story: cardholder claims they never authorized a $249.99 purchase of AudioPeak wireless earbuds, shipped to an address different from the cardholder's billing address (a forwarding address), with no 3D Secure challenge completed at checkout. Amount fixed at $249.99 (matches Task 3's lookup table).

- [ ] **Step 1: Read the current content of every file listed above** (already captured earlier in this session for `order_details.json`; read the remaining 10 files before editing, since their exact current wording must be replaced, not guessed).

- [ ] **Step 2: Write `merchant/order_details.json`**

```json
{
  "source": "AudioPeak Electronics — Order Management System",
  "dataType": "Order Details",
  "reasonCode": "4837",
  "reasonCodeDescription": "No Cardholder Authorization",
  "orderRecord": {
    "orderStatus": "Completed — shipped to address on order",
    "items": [
      {
        "name": "SoundWave Pro True Wireless Earbuds — Titanium",
        "quantity": 1,
        "unitPrice": 249.99,
        "sku": "AP-SW-TWE-TI"
      }
    ],
    "shippingAddress": "Different from cardholder's billing address — shipped to a forwarding address",
    "orderNotes": "Order placed online. Shipping address does not match billing address. 3D Secure was not enabled on the merchant's payment gateway at the time of this transaction."
  }
}
```

- [ ] **Step 3: Write `merchant/fulfillment_record.json`**

```json
{
  "source": "AudioPeak Electronics — Fulfillment",
  "dataType": "Fulfillment & Delivery Record",
  "reasonCode": "4837",
  "reasonCodeDescription": "No Cardholder Authorization",
  "fulfillmentRecord": {
    "type": "Physical Goods",
    "status": "Shipped and delivered",
    "method": "Ground shipping",
    "confirmed": true,
    "notes": "SoundWave Pro True Wireless Earbuds shipped to the forwarding address provided at checkout, not the cardholder's billing address on file."
  }
}
```

- [ ] **Step 4: Write `merchant/refund_policy.json`**

```json
{
  "source": "AudioPeak Electronics — Policies",
  "dataType": "Refund & Return Policy",
  "reasonCode": "4837",
  "reasonCodeDescription": "No Cardholder Authorization",
  "policy": {
    "refundWindow": "30 days from delivery",
    "policy": "Standard 30-day return window. Not applicable to fraud disputes — this case turns on transaction authorization, not product condition.",
    "disclosureMethod": "Displayed at checkout and in order confirmation email.",
    "customerAcknowledgment": "N/A — cardholder disputes ever placing the order."
  }
}
```

- [ ] **Step 5: Write `psp/auth_log.json`**

```json
{
  "source": "AudioPeak Electronics — Payment Service Provider",
  "dataType": "Authorization Log",
  "reasonCode": "4837",
  "reasonCodeDescription": "No Cardholder Authorization",
  "authorizationRecord": {
    "authorizationObtained": true,
    "responseCode": "00",
    "responseMessage": "Approved",
    "entryMode": "Ecommerce — card on file not used, manual entry",
    "threeDSecure": null,
    "notes": "Authorization approved for $249.99. 3D Secure (Mastercard Identity Check) was not enabled on the merchant's payment gateway at the time of this transaction — no liability shift available."
  }
}
```

- [ ] **Step 6: Write `psp/settlement_record.json`**

```json
{
  "source": "AudioPeak Electronics — Payment Service Provider",
  "dataType": "Settlement Record",
  "reasonCode": "4837",
  "reasonCodeDescription": "No Cardholder Authorization",
  "settlementRecord": {
    "notes": "Settled for $249.99, matching the authorized amount. Single transaction, no duplicate settlement."
  }
}
```

- [ ] **Step 7: Write `identity/avs_cvv_check.json`**

```json
{
  "source": "AudioPeak Electronics — Identity Verification",
  "dataType": "AVS/CVV Verification Results",
  "reasonCode": "4837",
  "reasonCodeDescription": "No Cardholder Authorization",
  "verificationResults": {
    "avs": {
      "responseCode": "N",
      "description": "No match — billing address on card does not match shipping address provided"
    },
    "cvv": {
      "responseCode": "M",
      "description": "CVV matched"
    }
  }
}
```

- [ ] **Step 8: Write `fraud-tools/risk_assessment.json`**

```json
{
  "source": "AudioPeak Electronics — Fraud Prevention",
  "dataType": "Device Fingerprint & Risk Assessment",
  "reasonCode": "4837",
  "reasonCodeDescription": "No Cardholder Authorization",
  "riskAssessment": {
    "deviceFingerprint": {
      "status": "New/unrecognized device — first transaction from this device",
      "trust": "Low",
      "match": false
    },
    "ipAnalysis": {
      "level": "Medium",
      "proxy": false,
      "geoMatch": false,
      "notes": "IP location does not match cardholder's billing address region."
    },
    "overallRiskScore": "Medium-High — AVS mismatch and shipping-to-forwarding-address pattern"
  }
}
```

- [ ] **Step 9: Write `customer-comms/email_logs.json`**

```json
{
  "source": "AudioPeak Electronics — Customer Communications",
  "dataType": "Email Communication Logs",
  "reasonCode": "4837",
  "reasonCodeDescription": "No Cardholder Authorization",
  "communications": [
    {
      "type": "Order Confirmation",
      "dir": "merchant_to_customer",
      "timing": "At time of order",
      "subject": "AudioPeak Electronics — Order Confirmation #AP-7734",
      "body": "Thank you for your order! SoundWave Pro True Wireless Earbuds (Titanium), $249.99. Shipping to the address provided at checkout."
    },
    {
      "type": "Fraud Dispute Inquiry",
      "dir": "customer_to_merchant",
      "timing": "[22 days after the order]",
      "subject": "I did not make this purchase",
      "body": "The cardholder emailed AudioPeak stating they never placed order #AP-7734 and did not recognize the shipping address used."
    }
  ]
}
```

- [ ] **Step 10: Write `issuer/customer-comms/cardholder_dispute_statement.json`**

```json
{
  "source": "Issuing Bank — Cardholder Services",
  "dataType": "Cardholder Dispute Statement",
  "reasonCode": "4837",
  "reasonCodeDescription": "No Cardholder Authorization",
  "statement": {
    "disputeReason": "4837 - No Cardholder Authorization",
    "detailedDescription": "The cardholder states they never authorized a $249.99 charge from 'AUDIOPEAK ELECTRONICS' for SoundWave Pro True Wireless Earbuds. The cardholder's card was in their possession and they did not share their card details with anyone.",
    "contactedMerchant": true,
    "merchantResponseSummary": "The cardholder emailed AudioPeak [22 days after the order] stating they did not place the order. AudioPeak confirmed the order was shipped to an address different from the cardholder's billing address.",
    "previousDisputeHistory": "None",
    "requestedResolution": "Full reversal of the unauthorized $249.99 charge.",
    "cardholderDeclaration": "I declare that the information provided in this dispute statement is true, accurate, and complete to the best of my knowledge and belief."
  },
  "bankInternalNotes": {
    "riskAssessment": "Medium — AVS mismatch supports cardholder's claim, but merchant may have valid defense documentation.",
    "recommendedAction": "Proceed with chargeback filing under Mastercard reason code 4837 (No Cardholder Authorization).",
    "priority": "Standard"
  }
}
```

- [ ] **Step 11: Write `issuer/merchant/issuer_chargeback_documentation.json`**

```json
{
  "source": "Issuing Bank — Chargeback Department",
  "dataType": "Issuer Chargeback Documentation",
  "reasonCode": "4837",
  "reasonCodeDescription": "No Cardholder Authorization",
  "chargebackFiling": {
    "reasonCode": "4837",
    "reasonCodeDescription": "No Cardholder Authorization",
    "supportingDocuments": [
      { "type": "Cardholder Dispute Form", "desc": "Statement denying authorization of the $249.99 charge." },
      { "type": "Account Statement", "desc": "Showing the disputed $249.99 charge from AudioPeak Electronics." }
    ],
    "issuerCommentary": "Filing under 4837. Cardholder denies authorizing the transaction. Shipping address on the order does not match the cardholder's billing address.",
    "arbitrationEligible": true
  },
  "transactionDetails": {
    "cardPresent": false,
    "ecommerceIndicator": "ECI_07",
    "posEntryMode": "81"
  }
}
```

- [ ] **Step 12: Write `issuer/psp/issuer_transaction_record.json`**

```json
{
  "source": "Issuing Bank — Transaction Processing",
  "dataType": "Issuer Transaction Record",
  "reasonCode": "4837",
  "reasonCodeDescription": "No Cardholder Authorization",
  "authorization": {
    "responseCode": "00",
    "merchantCategoryCode": "5999",
    "posEntryMode": "81",
    "cardNotPresent": true,
    "avsResult": {
      "code": "N",
      "description": "No match"
    },
    "cvvResult": {
      "code": "M",
      "description": "CVV matched"
    }
  },
  "riskFlags": {
    "geolocationMismatch": "Yes",
    "deviceTrustScore": "Low"
  }
}
```

- [ ] **Step 13: Validate all 11 files are valid JSON**

Run: `powershell -Command "Get-ChildItem src/data/sources/acquirer/4837,src/data/sources/issuer/4837 -Recurse -Filter *.json | ForEach-Object { Get-Content $_.FullName -Raw | ConvertFrom-Json | Out-Null }; Write-Host 'all valid'"`
Expected: prints `all valid`, no parse error.

- [ ] **Step 14: Commit**

```bash
git add src/data/sources/acquirer/4837 src/data/sources/issuer/4837
git commit -m "Rewrite 4837 story from LuxeVision Online to AudioPeak Electronics"
```

---

## Task 10: Rewrite 4863 story — CaseArtisan → AudioPeak Electronics

**Files:** same 11-file pattern as Task 9, under `4863` instead of `4837`.

Story: cardholder claims they don't recognize a $34.99 charge for a personalized AudioPeak cable-organizer/carrying case (custom-engraved), even though the billing descriptor clearly reads "AUDIOPEAK ELECTRONICS" and matches an account the cardholder created with their own email.

- [ ] **Step 1: Read current content of all 11 files under `src/data/sources/acquirer/4863/**` and `src/data/sources/issuer/4863/**`.**

- [ ] **Step 2: Write `merchant/order_details.json`**

```json
{
  "source": "AudioPeak Electronics — Order Management System",
  "dataType": "Order Details",
  "reasonCode": "4863",
  "reasonCodeDescription": "Cardholder Does Not Recognize Transaction",
  "orderRecord": {
    "orderStatus": "Completed — delivered",
    "items": [
      {
        "name": "Custom Engraved Carrying Case — for SoundWave Pro Earbuds, personalized with customer's initials",
        "quantity": 1,
        "unitPrice": 34.99,
        "sku": "AP-CEC-SW"
      }
    ],
    "shippingAddress": "Cardholder's billing address",
    "orderNotes": "Customer created an account using their email address, entered custom engraving initials, and completed checkout. The billing descriptor 'AUDIOPEAK ELECTRONICS' matches the business name displayed on the website and in the order confirmation."
  }
}
```

- [ ] **Step 3: Write `merchant/fulfillment_record.json`**

```json
{
  "source": "AudioPeak Electronics — Fulfillment",
  "dataType": "Fulfillment & Delivery Record",
  "reasonCode": "4863",
  "reasonCodeDescription": "Cardholder Does Not Recognize Transaction",
  "fulfillmentRecord": {
    "type": "Physical Goods — custom engraved",
    "status": "Shipped and delivered",
    "method": "Ground shipping",
    "confirmed": true,
    "notes": "Custom-engraved case produced with the initials entered at checkout and shipped to the cardholder's billing address."
  }
}
```

- [ ] **Step 4: Write `merchant/refund_policy.json`**

```json
{
  "source": "AudioPeak Electronics — Policies",
  "dataType": "Refund & Return Policy",
  "reasonCode": "4863",
  "reasonCodeDescription": "Cardholder Does Not Recognize Transaction",
  "policy": {
    "refundWindow": "N/A — custom engraved item",
    "policy": "Custom/personalized items are final sale and not eligible for standard returns.",
    "disclosureMethod": "Disclosed on the product page and at checkout before payment.",
    "customerAcknowledgment": "Customer entered personalization text and confirmed the order, acknowledging the final-sale terms."
  }
}
```

- [ ] **Step 5: Write `psp/auth_log.json`**

```json
{
  "source": "AudioPeak Electronics — Payment Service Provider",
  "dataType": "Authorization Log",
  "reasonCode": "4863",
  "reasonCodeDescription": "Cardholder Does Not Recognize Transaction",
  "authorizationRecord": {
    "authorizationObtained": true,
    "responseCode": "00",
    "responseMessage": "Approved",
    "entryMode": "Ecommerce — card on file, account login",
    "threeDSecure": {
      "enrolled": true,
      "authenticated": true,
      "eci": "05",
      "cavv": "Verified"
    },
    "notes": "Authorization for $34.99 under billing descriptor 'AUDIOPEAK ELECTRONICS', matching the merchant name shown at checkout."
  }
}
```

- [ ] **Step 6: Write `psp/settlement_record.json`**

```json
{
  "source": "AudioPeak Electronics — Payment Service Provider",
  "dataType": "Settlement Record",
  "reasonCode": "4863",
  "reasonCodeDescription": "Cardholder Does Not Recognize Transaction",
  "settlementRecord": {
    "notes": "Settled for $34.99, matching the authorized amount and the account's order history."
  }
}
```

- [ ] **Step 7: Write `identity/avs_cvv_check.json`**

```json
{
  "source": "AudioPeak Electronics — Identity Verification",
  "dataType": "AVS/CVV Verification Results",
  "reasonCode": "4863",
  "reasonCodeDescription": "Cardholder Does Not Recognize Transaction",
  "verificationResults": {
    "avs": {
      "responseCode": "Y",
      "description": "Full match"
    },
    "cvv": {
      "responseCode": "M",
      "description": "CVV matched"
    }
  }
}
```

- [ ] **Step 8: Write `fraud-tools/risk_assessment.json`**

```json
{
  "source": "AudioPeak Electronics — Fraud Prevention",
  "dataType": "Device Fingerprint & Risk Assessment",
  "reasonCode": "4863",
  "reasonCodeDescription": "Cardholder Does Not Recognize Transaction",
  "riskAssessment": {
    "deviceFingerprint": {
      "status": "Known device — same device used for account creation and checkout",
      "trust": "High",
      "match": true
    },
    "ipAnalysis": {
      "level": "Low",
      "proxy": false,
      "geoMatch": true,
      "notes": "IP consistent with cardholder's usual location."
    },
    "overallRiskScore": "Low — account created and used by cardholder's own device/email"
  }
}
```

- [ ] **Step 9: Write `customer-comms/email_logs.json`**

```json
{
  "source": "AudioPeak Electronics — Customer Communications",
  "dataType": "Email Communication Logs",
  "reasonCode": "4863",
  "reasonCodeDescription": "Cardholder Does Not Recognize Transaction",
  "communications": [
    {
      "type": "Account Creation Confirmation",
      "dir": "merchant_to_customer",
      "timing": "At account creation, same session as order",
      "subject": "Welcome to AudioPeak Electronics",
      "body": "Your account has been created using this email address."
    },
    {
      "type": "Order Confirmation",
      "dir": "merchant_to_customer",
      "timing": "At time of order",
      "subject": "AudioPeak Electronics — Order Confirmation #AP-8102",
      "body": "Custom Engraved Carrying Case, $34.99. Billing descriptor on your statement will read 'AUDIOPEAK ELECTRONICS'."
    }
  ]
}
```

- [ ] **Step 10: Write `issuer/customer-comms/cardholder_dispute_statement.json`**

```json
{
  "source": "Issuing Bank — Cardholder Services",
  "dataType": "Cardholder Dispute Statement",
  "reasonCode": "4863",
  "reasonCodeDescription": "Cardholder Does Not Recognize Transaction",
  "statement": {
    "disputeReason": "4863 - Cardholder Does Not Recognize Transaction",
    "detailedDescription": "The cardholder does not recognize a $34.99 charge from 'AUDIOPEAK ELECTRONICS' on their statement and does not recall making this purchase.",
    "contactedMerchant": false,
    "merchantResponseSummary": "N/A — cardholder filed the dispute directly with the issuing bank without contacting the merchant first.",
    "previousDisputeHistory": "None",
    "requestedResolution": "Reversal of the unrecognized $34.99 charge.",
    "cardholderDeclaration": "I declare that the information provided in this dispute statement is true, accurate, and complete to the best of my knowledge and belief."
  },
  "bankInternalNotes": {
    "riskAssessment": "Medium — merchant may have valid defense documentation showing account ownership.",
    "recommendedAction": "Proceed with chargeback filing under Mastercard reason code 4863 (Cardholder Does Not Recognize Transaction).",
    "priority": "Standard"
  }
}
```

- [ ] **Step 11: Write `issuer/merchant/issuer_chargeback_documentation.json`**

```json
{
  "source": "Issuing Bank — Chargeback Department",
  "dataType": "Issuer Chargeback Documentation",
  "reasonCode": "4863",
  "reasonCodeDescription": "Cardholder Does Not Recognize Transaction",
  "chargebackFiling": {
    "reasonCode": "4863",
    "reasonCodeDescription": "Cardholder Does Not Recognize Transaction",
    "supportingDocuments": [
      { "type": "Cardholder Dispute Form", "desc": "Statement that the $34.99 charge is not recognized." },
      { "type": "Account Statement", "desc": "Showing the disputed $34.99 charge from AudioPeak Electronics." }
    ],
    "issuerCommentary": "Filing under 4863. Cardholder does not recognize the transaction or the merchant billing descriptor.",
    "arbitrationEligible": true
  },
  "transactionDetails": {
    "cardPresent": false,
    "ecommerceIndicator": "ECI_05",
    "posEntryMode": "81"
  }
}
```

- [ ] **Step 12: Write `issuer/psp/issuer_transaction_record.json`**

```json
{
  "source": "Issuing Bank — Transaction Processing",
  "dataType": "Issuer Transaction Record",
  "reasonCode": "4863",
  "reasonCodeDescription": "Cardholder Does Not Recognize Transaction",
  "authorization": {
    "responseCode": "00",
    "merchantCategoryCode": "5999",
    "posEntryMode": "81",
    "cardNotPresent": true,
    "avsResult": {
      "code": "Y",
      "description": "Full match"
    },
    "cvvResult": {
      "code": "M",
      "description": "CVV matched"
    }
  },
  "riskFlags": {
    "geolocationMismatch": "None",
    "deviceTrustScore": "High"
  }
}
```

- [ ] **Step 13: Validate all 11 files are valid JSON**

Run: `powershell -Command "Get-ChildItem src/data/sources/acquirer/4863,src/data/sources/issuer/4863 -Recurse -Filter *.json | ForEach-Object { Get-Content $_.FullName -Raw | ConvertFrom-Json | Out-Null }; Write-Host 'all valid'"`

- [ ] **Step 14: Commit**

```bash
git add src/data/sources/acquirer/4863 src/data/sources/issuer/4863
git commit -m "Rewrite 4863 story from CaseArtisan to AudioPeak Electronics"
```

---

## Task 11: Rewrite 4834 story — PhoneShield Pro → AudioPeak Electronics

**Files:** 12 files total — the standard 11-file pattern (8 acquirer: `customer-comms/email_logs.json`, `fraud-tools/risk_assessment.json`, `identity/avs_cvv_check.json`, `merchant/order_details.json`, `merchant/refund_policy.json`, `psp/auth_log.json`, `psp/settlement_record.json`, plus `merchant/fulfillment_record.json`; 3 issuer) plus one extra acquirer file this code has that others in this task group don't: `device/3ds_authentication.json`. There is no `pos_terminal_log.json` file in this folder — Task 8 already replaced that dead conditional-source reference with `order_details.json`/`settlement_record.json`, so none needs to be created.

Story: cardholder claims duplicate processing for two separate AudioPeak orders placed 20 minutes apart — a wireless charging pad ($79.99) and a set of replacement ear cushions ($79.99), total $159.98 — each with a unique order ID, SKU, and authorization code, proving they are not duplicates of the same transaction.

- [ ] **Step 1: Read current content of all files under `src/data/sources/acquirer/4834/**` and `src/data/sources/issuer/4834/**`.**

- [ ] **Step 2: Write `merchant/order_details.json`**

```json
{
  "source": "AudioPeak Electronics — Order Management System",
  "dataType": "Order Details",
  "reasonCode": "4834",
  "reasonCodeDescription": "Point of Interaction Error",
  "orderRecord": {
    "orderStatus": "Both orders completed and delivered",
    "items": [
      {
        "name": "Order #AP-9210: SnapCharge Wireless Charging Pad",
        "quantity": 1,
        "unitPrice": 79.99,
        "sku": "AP-SC-WCP"
      },
      {
        "name": "Order #AP-9247: Replacement Ear Cushions — ProSilence ANC (Pair)",
        "quantity": 1,
        "unitPrice": 79.99,
        "sku": "AP-RC-PS-PR"
      }
    ],
    "shippingAddress": "Both shipped to cardholder's billing address",
    "orderNotes": "Two separate orders placed 20 minutes apart. Order #AP-9210 at 2:15 PM (charging pad) and #AP-9247 at 2:35 PM (ear cushions). Each has unique order ID, unique SKU, different product, and separate authorization code."
  }
}
```

- [ ] **Step 3: Write `merchant/refund_policy.json`**

```json
{
  "source": "AudioPeak Electronics — Policies",
  "dataType": "Refund & Return Policy",
  "reasonCode": "4834",
  "reasonCodeDescription": "Point of Interaction Error",
  "policy": {
    "refundWindow": "30 days from delivery",
    "policy": "Standard 30-day return window. Both orders were separate, valid purchases and are each individually eligible for return under normal terms.",
    "disclosureMethod": "Displayed at checkout and in each order's confirmation email.",
    "customerAcknowledgment": "Customer confirmed each order individually at checkout."
  }
}
```

- [ ] **Step 4: Write `merchant/fulfillment_record.json`**

```json
{
  "source": "AudioPeak Electronics — Fulfillment",
  "dataType": "Fulfillment & Delivery Record",
  "reasonCode": "4834",
  "reasonCodeDescription": "Point of Interaction Error",
  "fulfillmentRecord": {
    "type": "Physical Goods",
    "status": "Both orders shipped and delivered separately",
    "method": "Ground shipping",
    "confirmed": true,
    "notes": "Order #AP-9210 (charging pad) and order #AP-9247 (ear cushions) were fulfilled as two independent shipments with separate tracking numbers, confirming they are not a single duplicated order."
  }
}
```

- [ ] **Step 5: Write `device/3ds_authentication.json`**

```json
{
  "source": "AudioPeak Electronics — Authentication",
  "dataType": "3D Secure Authentication Record",
  "reasonCode": "4834",
  "reasonCodeDescription": "Point of Interaction Error",
  "threeDSecure": {
    "version": "2.0",
    "enrolled": true,
    "status": "Y — Authenticated separately on each order",
    "eci": "05",
    "cavv": "Verified",
    "challenge": true,
    "liabilityShift": "Yes",
    "notes": "3D Secure completed independently for both order #AP-9210 (2:15 PM) and order #AP-9247 (2:35 PM), each producing a distinct CAVV — further confirming these are two separate authenticated transactions, not a duplicate."
  }
}
```

- [ ] **Step 6: Write `psp/auth_log.json`**

```json
{
  "source": "AudioPeak Electronics — Payment Service Provider",
  "dataType": "Authorization Log",
  "reasonCode": "4834",
  "reasonCodeDescription": "Point of Interaction Error",
  "authorizationRecord": {
    "authorizationObtained": true,
    "responseCode": "00",
    "responseMessage": "Approved",
    "entryMode": "Ecommerce",
    "threeDSecure": {
      "enrolled": true,
      "authenticated": true,
      "eci": "05",
      "cavv": "Verified"
    },
    "notes": "Two distinct authorizations: Auth code AP1-4471 for order #AP-9210 ($79.99, 2:15 PM) and Auth code AP1-4479 for order #AP-9247 ($79.99, 2:35 PM). Different products, different SKUs, different authorization codes — not a duplicate."
  }
}
```

- [ ] **Step 7: Write `psp/settlement_record.json`**

```json
{
  "source": "AudioPeak Electronics — Payment Service Provider",
  "dataType": "Settlement Record",
  "reasonCode": "4834",
  "reasonCodeDescription": "Point of Interaction Error",
  "settlementRecord": {
    "notes": "Two separate settlements: $79.99 for order #AP-9210 and $79.99 for order #AP-9247. Total $159.98 across two distinct, non-duplicate transactions."
  }
}
```

- [ ] **Step 8: Write `identity/avs_cvv_check.json`**

```json
{
  "source": "AudioPeak Electronics — Identity Verification",
  "dataType": "AVS/CVV Verification Results",
  "reasonCode": "4834",
  "reasonCodeDescription": "Point of Interaction Error",
  "verificationResults": {
    "avs": {
      "responseCode": "Y",
      "description": "Full match on both orders"
    },
    "cvv": {
      "responseCode": "M",
      "description": "CVV matched on both orders"
    }
  }
}
```

- [ ] **Step 9: Write `fraud-tools/risk_assessment.json`**

```json
{
  "source": "AudioPeak Electronics — Fraud Prevention",
  "dataType": "Device Fingerprint & Risk Assessment",
  "reasonCode": "4834",
  "reasonCodeDescription": "Point of Interaction Error",
  "riskAssessment": {
    "deviceFingerprint": {
      "status": "Same known device for both orders",
      "trust": "High",
      "match": true
    },
    "ipAnalysis": {
      "level": "Low",
      "proxy": false,
      "geoMatch": true,
      "notes": "Same IP for both orders, consistent with cardholder's usual location."
    },
    "overallRiskScore": "Low — two distinct, legitimate purchases from the same session pattern"
  }
}
```

- [ ] **Step 10: Write `customer-comms/email_logs.json`**

```json
{
  "source": "AudioPeak Electronics — Customer Communications",
  "dataType": "Email Communication Logs",
  "reasonCode": "4834",
  "reasonCodeDescription": "Point of Interaction Error",
  "communications": [
    {
      "type": "Order Confirmation",
      "dir": "merchant_to_customer",
      "timing": "2:15 PM",
      "subject": "AudioPeak Electronics — Order Confirmation #AP-9210",
      "body": "SnapCharge Wireless Charging Pad, $79.99."
    },
    {
      "type": "Order Confirmation",
      "dir": "merchant_to_customer",
      "timing": "2:35 PM",
      "subject": "AudioPeak Electronics — Order Confirmation #AP-9247",
      "body": "Replacement Ear Cushions — ProSilence ANC (Pair), $79.99."
    },
    {
      "type": "Duplicate Charge Inquiry",
      "dir": "customer_to_merchant",
      "timing": "[5 days after the orders]",
      "subject": "Was I charged twice?",
      "body": "The cardholder emailed asking whether the two charges on their statement were a duplicate. AudioPeak confirmed the two charges correspond to two separate orders (#AP-9210 and #AP-9247) for two different products."
    }
  ]
}
```

- [ ] **Step 11: Write `issuer/customer-comms/cardholder_dispute_statement.json`**

```json
{
  "source": "Issuing Bank — Cardholder Services",
  "dataType": "Cardholder Dispute Statement",
  "reasonCode": "4834",
  "reasonCodeDescription": "Point of Interaction Error",
  "statement": {
    "disputeReason": "4834 - Point of Interaction Error",
    "detailedDescription": "The cardholder sees two charges of $79.99 each from 'AUDIOPEAK ELECTRONICS' posted 20 minutes apart and believes this is a duplicate charge for a single order.",
    "contactedMerchant": true,
    "merchantResponseSummary": "The cardholder emailed AudioPeak [5 days after the charges]. The merchant explained the two charges correspond to two separate orders with different products and different order numbers.",
    "previousDisputeHistory": "None",
    "requestedResolution": "Reversal of one of the two $79.99 charges, believed to be a duplicate.",
    "cardholderDeclaration": "I declare that the information provided in this dispute statement is true, accurate, and complete to the best of my knowledge and belief."
  },
  "bankInternalNotes": {
    "riskAssessment": "Medium — merchant may have valid defense documentation showing two distinct orders.",
    "recommendedAction": "Proceed with chargeback filing under Mastercard reason code 4834 (Point of Interaction Error).",
    "priority": "Standard"
  }
}
```

- [ ] **Step 12: Write `issuer/merchant/issuer_chargeback_documentation.json`**

```json
{
  "source": "Issuing Bank — Chargeback Department",
  "dataType": "Issuer Chargeback Documentation",
  "reasonCode": "4834",
  "reasonCodeDescription": "Point of Interaction Error",
  "chargebackFiling": {
    "reasonCode": "4834",
    "reasonCodeDescription": "Point of Interaction Error",
    "supportingDocuments": [
      { "type": "Cardholder Dispute Form", "desc": "Statement that two $79.99 charges 20 minutes apart appear to be a duplicate." },
      { "type": "Account Statement", "desc": "Showing both $79.99 charges from AudioPeak Electronics." }
    ],
    "issuerCommentary": "Filing under 4834. Cardholder believes the two closely-timed charges are a duplicate of a single order.",
    "arbitrationEligible": true
  },
  "transactionDetails": {
    "cardPresent": false,
    "ecommerceIndicator": "ECI_05",
    "posEntryMode": "81"
  }
}
```

- [ ] **Step 13: Write `issuer/psp/issuer_transaction_record.json`**

```json
{
  "source": "Issuing Bank — Transaction Processing",
  "dataType": "Issuer Transaction Record",
  "reasonCode": "4834",
  "reasonCodeDescription": "Point of Interaction Error",
  "authorization": {
    "responseCode": "00",
    "merchantCategoryCode": "5999",
    "posEntryMode": "81",
    "cardNotPresent": true,
    "avsResult": {
      "code": "Y",
      "description": "Full match"
    },
    "cvvResult": {
      "code": "M",
      "description": "CVV matched"
    }
  },
  "riskFlags": {
    "geolocationMismatch": "None",
    "deviceTrustScore": "High"
  }
}
```

- [ ] **Step 14: Validate all files are valid JSON**

Run: `powershell -Command "Get-ChildItem src/data/sources/acquirer/4834,src/data/sources/issuer/4834 -Recurse -Filter *.json | ForEach-Object { Get-Content $_.FullName -Raw | ConvertFrom-Json | Out-Null }; Write-Host 'all valid'"`

- [ ] **Step 15: Commit**

```bash
git add src/data/sources/acquirer/4834 src/data/sources/issuer/4834
git commit -m "Rewrite 4834 story from PhoneShield Pro to AudioPeak Electronics"
```

---

## Task 12: Rewrite 4808 story — RenewTech Store → AudioPeak Electronics

**Files:** same 11-file pattern under `4808`.

Story: authorization was obtained and 3D-Secure-authenticated for a $449.00 refurbished/open-box AudioPeak home theater soundbar, delivered and signed for, settled within the required 7-day window.

- [ ] **Step 1: Read current content of all files under `src/data/sources/acquirer/4808/**` and `src/data/sources/issuer/4808/**`.**

- [ ] **Step 2: Write `merchant/order_details.json`**

```json
{
  "source": "AudioPeak Electronics — Order Management System",
  "dataType": "Order Details",
  "reasonCode": "4808",
  "reasonCodeDescription": "Authorization-Related Chargeback",
  "orderRecord": {
    "orderStatus": "Completed — delivered and signed for",
    "items": [
      {
        "name": "Refurbished BassLine 5.1 Home Theater Soundbar — Certified Open-Box",
        "quantity": 1,
        "unitPrice": 449.00,
        "sku": "AP-BL51-R"
      }
    ],
    "shippingAddress": "Matches cardholder's billing address exactly",
    "orderNotes": "Customer created account, completed checkout with 3D Secure authentication, shipped to billing address, signed for at delivery."
  }
}
```

- [ ] **Step 3: Write `merchant/fulfillment_record.json`**

```json
{
  "source": "AudioPeak Electronics — Fulfillment",
  "dataType": "Fulfillment & Delivery Record",
  "reasonCode": "4808",
  "reasonCodeDescription": "Authorization-Related Chargeback",
  "fulfillmentRecord": {
    "type": "Physical Goods — refurbished/open-box",
    "status": "Delivered and signed for",
    "method": "Ground shipping",
    "confirmed": true,
    "notes": "Refurbished BassLine 5.1 Home Theater Soundbar delivered within 5 business days of authorization, well within the 7-calendar-day settlement window."
  }
}
```

- [ ] **Step 4: Write `merchant/refund_policy.json`**

```json
{
  "source": "AudioPeak Electronics — Policies",
  "dataType": "Refund & Return Policy",
  "reasonCode": "4808",
  "reasonCodeDescription": "Authorization-Related Chargeback",
  "policy": {
    "refundWindow": "30 days from delivery",
    "policy": "Refurbished/open-box items carry the standard 30-day return window and a 90-day limited warranty.",
    "disclosureMethod": "Disclosed on the product page and at checkout.",
    "customerAcknowledgment": "Customer acknowledged refurbished/open-box condition at checkout before payment."
  }
}
```

- [ ] **Step 5: Write `psp/auth_log.json`**

```json
{
  "source": "AudioPeak Electronics — Payment Service Provider",
  "dataType": "Authorization Log",
  "reasonCode": "4808",
  "reasonCodeDescription": "Authorization-Related Chargeback",
  "authorizationRecord": {
    "authorizationObtained": true,
    "responseCode": "00",
    "responseMessage": "Approved",
    "entryMode": "Ecommerce",
    "threeDSecure": {
      "enrolled": true,
      "authenticated": true,
      "eci": "05",
      "cavv": "Verified"
    },
    "notes": "Authorization approval code AP2-3391 obtained for $449.00. Settlement occurred 5 calendar days after authorization, within Mastercard's required 7-day window."
  }
}
```

- [ ] **Step 6: Write `psp/settlement_record.json`**

```json
{
  "source": "AudioPeak Electronics — Payment Service Provider",
  "dataType": "Settlement Record",
  "reasonCode": "4808",
  "reasonCodeDescription": "Authorization-Related Chargeback",
  "settlementRecord": {
    "notes": "Settled for $449.00, matching the authorized amount, 5 calendar days after authorization — within the 7-day requirement."
  }
}
```

- [ ] **Step 7: Write `identity/avs_cvv_check.json`**

```json
{
  "source": "AudioPeak Electronics — Identity Verification",
  "dataType": "AVS/CVV Verification Results",
  "reasonCode": "4808",
  "reasonCodeDescription": "Authorization-Related Chargeback",
  "verificationResults": {
    "avs": {
      "responseCode": "Y",
      "description": "Full match"
    },
    "cvv": {
      "responseCode": "M",
      "description": "CVV matched"
    }
  }
}
```

- [ ] **Step 8: Write `device/3ds_authentication.json`**

This file already exists in the `4808` acquirer folder. Write:

```json
{
  "source": "AudioPeak Electronics — Authentication",
  "dataType": "3D Secure Authentication Record",
  "reasonCode": "4808",
  "reasonCodeDescription": "Authorization-Related Chargeback",
  "threeDSecure": {
    "version": "2.0",
    "enrolled": true,
    "status": "Y — Authenticated at checkout",
    "eci": "05",
    "cavv": "Verified",
    "challenge": true,
    "liabilityShift": "Yes",
    "notes": "3D Secure completed successfully at checkout for the $449.00 purchase."
  }
}
```

- [ ] **Step 9: Write `customer-comms/email_logs.json`**

```json
{
  "source": "AudioPeak Electronics — Customer Communications",
  "dataType": "Email Communication Logs",
  "reasonCode": "4808",
  "reasonCodeDescription": "Authorization-Related Chargeback",
  "communications": [
    {
      "type": "Order Confirmation",
      "dir": "merchant_to_customer",
      "timing": "At time of order",
      "subject": "AudioPeak Electronics — Order Confirmation #AP-6650",
      "body": "Refurbished BassLine 5.1 Home Theater Soundbar (Certified Open-Box), $449.00. Authorization confirmed, order will ship within 1-2 business days."
    },
    {
      "type": "Delivery Confirmation",
      "dir": "merchant_to_customer",
      "timing": "[5 days after the order]",
      "subject": "Your AudioPeak order has been delivered",
      "body": "Your BassLine 5.1 Home Theater Soundbar was delivered and signed for."
    }
  ]
}
```

- [ ] **Step 10: Write `issuer/customer-comms/cardholder_dispute_statement.json`**

```json
{
  "source": "Issuing Bank — Cardholder Services",
  "dataType": "Cardholder Dispute Statement",
  "reasonCode": "4808",
  "reasonCodeDescription": "Authorization-Related Chargeback",
  "statement": {
    "disputeReason": "4808 - Authorization-Related Chargeback",
    "detailedDescription": "The cardholder disputes a $449.00 charge from 'AUDIOPEAK ELECTRONICS', questioning whether a valid authorization was obtained and whether settlement occurred within the required window.",
    "contactedMerchant": false,
    "merchantResponseSummary": "N/A — cardholder filed the dispute directly with the issuing bank.",
    "previousDisputeHistory": "None",
    "requestedResolution": "Reversal of the $449.00 charge pending proof of valid authorization and timely settlement.",
    "cardholderDeclaration": "I declare that the information provided in this dispute statement is true, accurate, and complete to the best of my knowledge and belief."
  },
  "bankInternalNotes": {
    "riskAssessment": "Low-Medium — routine authorization-timing dispute, merchant likely has valid documentation.",
    "recommendedAction": "Proceed with chargeback filing under Mastercard reason code 4808 (Authorization-Related Chargeback) pending review.",
    "priority": "Standard"
  }
}
```

- [ ] **Step 11: Write `issuer/merchant/issuer_chargeback_documentation.json`**

```json
{
  "source": "Issuing Bank — Chargeback Department",
  "dataType": "Issuer Chargeback Documentation",
  "reasonCode": "4808",
  "reasonCodeDescription": "Authorization-Related Chargeback",
  "chargebackFiling": {
    "reasonCode": "4808",
    "reasonCodeDescription": "Authorization-Related Chargeback",
    "supportingDocuments": [
      { "type": "Cardholder Dispute Form", "desc": "Statement questioning authorization validity for the $449.00 charge." },
      { "type": "Account Statement", "desc": "Showing the disputed $449.00 charge from AudioPeak Electronics." }
    ],
    "issuerCommentary": "Filing under 4808 pending confirmation of authorization approval code and settlement timing.",
    "arbitrationEligible": true
  },
  "transactionDetails": {
    "cardPresent": false,
    "ecommerceIndicator": "ECI_05",
    "posEntryMode": "81"
  }
}
```

- [ ] **Step 12: Write `issuer/psp/issuer_transaction_record.json`**

```json
{
  "source": "Issuing Bank — Transaction Processing",
  "dataType": "Issuer Transaction Record",
  "reasonCode": "4808",
  "reasonCodeDescription": "Authorization-Related Chargeback",
  "authorization": {
    "responseCode": "00",
    "merchantCategoryCode": "5999",
    "posEntryMode": "81",
    "cardNotPresent": true,
    "avsResult": {
      "code": "Y",
      "description": "Full match"
    },
    "cvvResult": {
      "code": "M",
      "description": "CVV matched"
    }
  },
  "riskFlags": {
    "geolocationMismatch": "None",
    "deviceTrustScore": "High"
  }
}
```

- [ ] **Step 13: Validate all files are valid JSON**

Run: `powershell -Command "Get-ChildItem src/data/sources/acquirer/4808,src/data/sources/issuer/4808 -Recurse -Filter *.json | ForEach-Object { Get-Content $_.FullName -Raw | ConvertFrom-Json | Out-Null }; Write-Host 'all valid'"`

- [ ] **Step 14: Commit**

```bash
git add src/data/sources/acquirer/4808 src/data/sources/issuer/4808
git commit -m "Rewrite 4808 story from RenewTech Store to AudioPeak Electronics"
```

---

## Task 13: Rewrite 4855 story — HomeStitch Custom Décor → AudioPeak Electronics

**Files:** same 11-file pattern under `4855`.

Story: cardholder claims goods were never provided — a $179.99 custom-configured AudioPeak home speaker bundle (color/finish selected at checkout) was never shipped due to a supply-chain disruption on that finish.

- [ ] **Step 1: Read current content of all files under `src/data/sources/acquirer/4855/**` and `src/data/sources/issuer/4855/**`.**

- [ ] **Step 2: Write `merchant/order_details.json`**

```json
{
  "source": "AudioPeak Electronics — Order Management System",
  "dataType": "Order Details",
  "reasonCode": "4855",
  "reasonCodeDescription": "Goods or Services Not Provided",
  "orderRecord": {
    "orderStatus": "Processing — never shipped",
    "items": [
      {
        "name": "Custom-Configured EchoNest Bookshelf Speaker Pair — Walnut Finish",
        "quantity": 1,
        "unitPrice": 179.99,
        "sku": "AP-EN-BSP-WAL"
      }
    ],
    "shippingAddress": "Cardholder's billing address",
    "orderNotes": "Custom-finish order placed. Production was started but AudioPeak experienced a supply chain disruption affecting the Walnut finish veneer. The order was never completed or shipped."
  }
}
```

- [ ] **Step 3: Write `merchant/fulfillment_record.json`**

```json
{
  "source": "AudioPeak Electronics — Fulfillment",
  "dataType": "Fulfillment & Delivery Record",
  "reasonCode": "4855",
  "reasonCodeDescription": "Goods or Services Not Provided",
  "fulfillmentRecord": {
    "type": "Physical Goods — custom finish",
    "status": "Never shipped — production halted",
    "method": "N/A",
    "confirmed": false,
    "notes": "Walnut veneer supply disruption halted production. No shipment ever occurred for this order."
  }
}
```

- [ ] **Step 4: Write `merchant/refund_policy.json`**

```json
{
  "source": "AudioPeak Electronics — Policies",
  "dataType": "Refund & Return Policy",
  "reasonCode": "4855",
  "reasonCodeDescription": "Goods or Services Not Provided",
  "policy": {
    "refundWindow": "Immediate refund if order is cancelled prior to shipment",
    "policy": "Orders that cannot be fulfilled due to supply disruption are eligible for full refund or store credit.",
    "disclosureMethod": "Cancellation/refund policy displayed at checkout and referenced in order status emails.",
    "customerAcknowledgment": "N/A — order was never fulfilled; refund policy applies automatically to unfulfilled custom orders."
  }
}
```

- [ ] **Step 5: Write `psp/auth_log.json`**

```json
{
  "source": "AudioPeak Electronics — Payment Service Provider",
  "dataType": "Authorization Log",
  "reasonCode": "4855",
  "reasonCodeDescription": "Goods or Services Not Provided",
  "authorizationRecord": {
    "authorizationObtained": true,
    "responseCode": "00",
    "responseMessage": "Approved",
    "entryMode": "Ecommerce",
    "threeDSecure": null,
    "notes": "Authorization for $179.99 obtained at order placement. Order was never fulfilled due to a supply chain disruption on the Walnut finish."
  }
}
```

- [ ] **Step 6: Write `psp/settlement_record.json`**

```json
{
  "source": "AudioPeak Electronics — Payment Service Provider",
  "dataType": "Settlement Record",
  "reasonCode": "4855",
  "reasonCodeDescription": "Goods or Services Not Provided",
  "settlementRecord": {
    "notes": "Charge of $179.99 was settled at order placement per standard checkout flow, but the order was never fulfilled due to supply disruption."
  }
}
```

- [ ] **Step 7: Write `identity/avs_cvv_check.json`**

```json
{
  "source": "AudioPeak Electronics — Identity Verification",
  "dataType": "AVS/CVV Verification Results",
  "reasonCode": "4855",
  "reasonCodeDescription": "Goods or Services Not Provided",
  "verificationResults": {
    "avs": {
      "responseCode": "Y",
      "description": "Full match"
    },
    "cvv": {
      "responseCode": "M",
      "description": "CVV matched"
    }
  }
}
```

- [ ] **Step 8: Write `fraud-tools/risk_assessment.json`**

```json
{
  "source": "AudioPeak Electronics — Fraud Prevention",
  "dataType": "Device Fingerprint & Risk Assessment",
  "reasonCode": "4855",
  "reasonCodeDescription": "Goods or Services Not Provided",
  "riskAssessment": {
    "deviceFingerprint": {
      "status": "Known device",
      "trust": "High",
      "match": true
    },
    "ipAnalysis": {
      "level": "Low",
      "proxy": false,
      "geoMatch": true,
      "notes": "IP consistent with cardholder's usual location."
    },
    "overallRiskScore": "Low — legitimate order affected by merchant-side supply disruption"
  }
}
```

- [ ] **Step 9: Write `customer-comms/email_logs.json`**

```json
{
  "source": "AudioPeak Electronics — Customer Communications",
  "dataType": "Email Communication Logs",
  "reasonCode": "4855",
  "reasonCodeDescription": "Goods or Services Not Provided",
  "communications": [
    {
      "type": "Order Confirmation",
      "dir": "merchant_to_customer",
      "timing": "At time of order",
      "subject": "AudioPeak Electronics — Order Confirmation #AP-5502",
      "body": "Custom-Configured EchoNest Bookshelf Speaker Pair (Walnut Finish), $179.99. Estimated production time: 2-3 weeks."
    },
    {
      "type": "Delay Notice",
      "dir": "merchant_to_customer",
      "timing": "[3 weeks after the order]",
      "subject": "Delay on your AudioPeak order — Walnut finish supply issue",
      "body": "We're experiencing a supply chain disruption affecting the Walnut finish veneer used in your order. We apologize for the delay and are working to resolve it."
    },
    {
      "type": "Customer Inquiry",
      "dir": "customer_to_merchant",
      "timing": "[6 weeks after the order]",
      "subject": "Where is my order?",
      "body": "The cardholder emailed asking for an update. No shipment had occurred at this point."
    }
  ]
}
```

- [ ] **Step 10: Write `issuer/customer-comms/cardholder_dispute_statement.json`**

```json
{
  "source": "Issuing Bank — Cardholder Services",
  "dataType": "Cardholder Dispute Statement",
  "reasonCode": "4855",
  "reasonCodeDescription": "Goods or Services Not Provided",
  "statement": {
    "disputeReason": "4855 - Goods or Services Not Provided",
    "detailedDescription": "The cardholder paid $179.99 to 'AUDIOPEAK ELECTRONICS' for a custom bookshelf speaker pair and never received the item. Order status has shown 'Processing' for over 6 weeks.",
    "contactedMerchant": true,
    "merchantResponseSummary": "The cardholder emailed AudioPeak [6 weeks after the order]. The merchant acknowledged a supply chain delay on the Walnut finish but had not yet shipped or refunded the order.",
    "previousDisputeHistory": "None",
    "requestedResolution": "Full reversal of $179.99 for goods never provided.",
    "cardholderDeclaration": "I declare that the information provided in this dispute statement is true, accurate, and complete to the best of my knowledge and belief."
  },
  "bankInternalNotes": {
    "riskAssessment": "High — merchant confirmed the order was never fulfilled.",
    "recommendedAction": "Proceed with chargeback filing under Mastercard reason code 4855 (Goods or Services Not Provided).",
    "priority": "High"
  }
}
```

- [ ] **Step 11: Write `issuer/merchant/issuer_chargeback_documentation.json`**

```json
{
  "source": "Issuing Bank — Chargeback Department",
  "dataType": "Issuer Chargeback Documentation",
  "reasonCode": "4855",
  "reasonCodeDescription": "Goods or Services Not Provided",
  "chargebackFiling": {
    "reasonCode": "4855",
    "reasonCodeDescription": "Goods or Services Not Provided",
    "supportingDocuments": [
      { "type": "Cardholder Dispute Form", "desc": "Statement that the custom speaker pair was never received after 6 weeks." },
      { "type": "Merchant Delay Email", "desc": "Merchant's own email acknowledging a supply chain delay and no shipment." }
    ],
    "issuerCommentary": "Filing under 4855. Merchant's own communication confirms the order was never fulfilled.",
    "arbitrationEligible": true
  },
  "transactionDetails": {
    "cardPresent": false,
    "ecommerceIndicator": "ECI_05",
    "posEntryMode": "81"
  }
}
```

- [ ] **Step 12: Write `issuer/psp/issuer_transaction_record.json`**

```json
{
  "source": "Issuing Bank — Transaction Processing",
  "dataType": "Issuer Transaction Record",
  "reasonCode": "4855",
  "reasonCodeDescription": "Goods or Services Not Provided",
  "authorization": {
    "responseCode": "00",
    "merchantCategoryCode": "5999",
    "posEntryMode": "81",
    "cardNotPresent": true,
    "avsResult": {
      "code": "Y",
      "description": "Full match"
    },
    "cvvResult": {
      "code": "M",
      "description": "CVV matched"
    }
  },
  "riskFlags": {
    "geolocationMismatch": "None",
    "deviceTrustScore": "High"
  }
}
```

- [ ] **Step 13: Validate all files are valid JSON**

Run: `powershell -Command "Get-ChildItem src/data/sources/acquirer/4855,src/data/sources/issuer/4855 -Recurse -Filter *.json | ForEach-Object { Get-Content $_.FullName -Raw | ConvertFrom-Json | Out-Null }; Write-Host 'all valid'"`

- [ ] **Step 14: Commit**

```bash
git add src/data/sources/acquirer/4855 src/data/sources/issuer/4855
git commit -m "Rewrite 4855 story from HomeStitch Custom Decor to AudioPeak Electronics"
```

---

## Task 14: New story for 4841 — "AudioPeak Sound+" replacement-parts plan

**Files:** same 11-file pattern under `4841` (9 acquirer files including `device/3ds_authentication.json` and `merchant/fulfillment_record.json`, which already exist per the earlier directory listing, plus 3 issuer files).

Story: cardholder subscribed to "AudioPeak Sound+", a $12.99/month plan that ships replacement ear tips/cushions and a discounted annual cable-replacement credit for their AudioPeak headphones. They requested cancellation 7 days before the next billing date; per the terms agreed at signup, cancellation takes effect at the end of the current paid period, and the disputed charge is that final cycle's shipment. This replaces `usage_logs.json`'s SaaS-style framing (per Task 8, Step 3) with `fulfillment_record.json` tracking the post-cancellation shipment instead.

- [ ] **Step 1: Read current content of all files under `src/data/sources/acquirer/4841/**` and `src/data/sources/issuer/4841/**`.**

- [ ] **Step 2: Write `merchant/order_details.json`**

```json
{
  "source": "AudioPeak Electronics — Order Management System",
  "dataType": "Order Details",
  "reasonCode": "4841",
  "reasonCodeDescription": "Cancelled Recurring Transaction",
  "orderRecord": {
    "orderStatus": "Active subscription — billed monthly",
    "items": [
      {
        "name": "AudioPeak Sound+ — Monthly Replacement Parts Plan (ear tips, cushions, annual cable credit)",
        "quantity": 1,
        "unitPrice": 12.99,
        "sku": "AP-SOUNDPLUS-MO"
      }
    ],
    "shippingAddress": "Cardholder's billing address",
    "orderNotes": "Monthly recurring plan tied to the cardholder's ProSilence ANC Headphones purchase. Cardholder signed up [6 months before the disputed charge]. The cancellation request was received [7 days before the next billing date], but per the terms agreed at signup, cancellation takes effect at the end of the current paid period. The disputed charge covers the final billing cycle's replacement-parts shipment."
  }
}
```

- [ ] **Step 3: Write `merchant/fulfillment_record.json`**

```json
{
  "source": "AudioPeak Electronics — Fulfillment",
  "dataType": "Fulfillment & Delivery Record",
  "reasonCode": "4841",
  "reasonCodeDescription": "Cancelled Recurring Transaction",
  "fulfillmentRecord": {
    "type": "Physical Goods — recurring shipment",
    "status": "Shipped — final cycle after cancellation request",
    "method": "Ground shipping, replacement parts kit",
    "confirmed": true,
    "notes": "The final-cycle replacement ear tips/cushions kit was shipped and delivered to the cardholder's address [2 days after the disputed billing date], consistent with an active plan through the end of the current paid period."
  }
}
```

- [ ] **Step 4: Write `merchant/refund_policy.json`**

```json
{
  "source": "AudioPeak Electronics — Policies",
  "dataType": "Refund & Cancellation Policy",
  "reasonCode": "4841",
  "reasonCodeDescription": "Cancelled Recurring Transaction",
  "policy": {
    "refundWindow": "N/A — recurring parts plan",
    "policy": "Cancellation takes effect at end of current billing period. No prorated refunds for partial months. Agreed at signup.",
    "disclosureMethod": "Terms displayed during signup, linked in confirmation email, and in account settings.",
    "customerAcknowledgment": "Customer checked 'I agree to the Terms of Service and Billing Policy' during signup."
  }
}
```

- [ ] **Step 5: Write `psp/auth_log.json`**

```json
{
  "source": "AudioPeak Electronics — Payment Service Provider",
  "dataType": "Authorization Log",
  "reasonCode": "4841",
  "reasonCodeDescription": "Cancelled Recurring Transaction",
  "authorizationRecord": {
    "authorizationObtained": true,
    "responseCode": "00",
    "responseMessage": "Approved",
    "entryMode": "Recurring — card on file",
    "threeDSecure": {
      "enrolled": true,
      "authenticated": true,
      "eci": "05",
      "cavv": "Verified at initial signup"
    },
    "notes": "Recurring billing authorized via card-on-file credentials established at initial signup [6 months before]. The initial signup included 3D Secure authentication. The disputed charge is the final billing cycle before cancellation takes effect."
  }
}
```

- [ ] **Step 6: Write `psp/settlement_record.json`**

```json
{
  "source": "AudioPeak Electronics — Payment Service Provider",
  "dataType": "Settlement Record",
  "reasonCode": "4841",
  "reasonCodeDescription": "Cancelled Recurring Transaction",
  "settlementRecord": {
    "notes": "Settled for $12.99. This is the final charge for the current billing cycle. Cancellation was received but takes effect at end of the paid period per the terms agreed at signup."
  }
}
```

- [ ] **Step 7: Write `identity/avs_cvv_check.json`**

```json
{
  "source": "AudioPeak Electronics — Identity Verification",
  "dataType": "AVS/CVV Verification Results",
  "reasonCode": "4841",
  "reasonCodeDescription": "Cancelled Recurring Transaction",
  "verificationResults": {
    "avs": {
      "responseCode": "Y",
      "description": "Full match — on file from initial signup"
    },
    "cvv": {
      "responseCode": "M",
      "description": "CVV on file from initial enrollment"
    }
  }
}
```

- [ ] **Step 8: Write `device/3ds_authentication.json`**

```json
{
  "source": "AudioPeak Electronics — Authentication",
  "dataType": "3D Secure Authentication Record",
  "reasonCode": "4841",
  "reasonCodeDescription": "Cancelled Recurring Transaction",
  "threeDSecure": {
    "version": "2.0",
    "enrolled": true,
    "status": "Y — Authenticated at initial signup",
    "eci": "05",
    "cavv": "Verified",
    "challenge": true,
    "liabilityShift": "Yes — at initial enrollment",
    "notes": "3D Secure was completed during the initial AudioPeak Sound+ signup. Subsequent recurring charges use the authenticated card-on-file credentials."
  }
}
```

- [ ] **Step 9: Write `fraud-tools/risk_assessment.json`**

```json
{
  "source": "AudioPeak Electronics — Fraud Prevention",
  "dataType": "Device Fingerprint & Risk Assessment",
  "reasonCode": "4841",
  "reasonCodeDescription": "Cancelled Recurring Transaction",
  "riskAssessment": {
    "deviceFingerprint": {
      "status": "Known device — same device used for initial signup and account logins",
      "trust": "High",
      "match": true
    },
    "ipAnalysis": {
      "level": "Low",
      "proxy": false,
      "geoMatch": true,
      "notes": "Login IP consistent with cardholder's location throughout the subscription."
    },
    "overallRiskScore": "Low — legitimate recurring charge on an established plan"
  }
}
```

- [ ] **Step 10: Write `customer-comms/email_logs.json`**

```json
{
  "source": "AudioPeak Electronics — Customer Communications",
  "dataType": "Email Communication Logs",
  "reasonCode": "4841",
  "reasonCodeDescription": "Cancelled Recurring Transaction",
  "communications": [
    {
      "type": "Original Signup Confirmation",
      "dir": "merchant_to_customer",
      "timing": "[6 months before the disputed charge]",
      "subject": "Welcome to AudioPeak Sound+!",
      "body": "Your AudioPeak Sound+ plan is active! $12.99/month — replacement ear tips/cushions plus annual cable credit. Your plan renews automatically each month. You can cancel anytime, and cancellation takes effect at the end of your current billing period."
    },
    {
      "type": "Billing Reminder",
      "dir": "merchant_to_customer",
      "timing": "[3 days before the disputed charge]",
      "subject": "Your AudioPeak Sound+ Renewal",
      "body": "Your AudioPeak Sound+ plan ($12.99) will renew in 3 days. If you wish to cancel, please do so through your account settings. Cancellation takes effect at the end of the current billing period."
    },
    {
      "type": "Cancellation Request",
      "dir": "customer_to_merchant",
      "timing": "[7 days before the disputed charge]",
      "subject": "Cancel my Sound+ plan",
      "body": "Please cancel my AudioPeak Sound+ plan immediately. I do not want to be charged again."
    },
    {
      "type": "Cancellation Acknowledgment",
      "dir": "merchant_to_customer",
      "timing": "[7 days before the disputed charge]",
      "subject": "RE: Cancel my Sound+ plan — Cancellation Confirmed",
      "body": "Your cancellation has been processed. As per our terms of service agreed at signup, your cancellation takes effect at the end of your current billing period. Your final replacement-parts shipment for this cycle will still be sent. No further charges will be made after that."
    },
    {
      "type": "Final Billing Notification",
      "dir": "merchant_to_customer",
      "timing": "On the disputed billing date",
      "subject": "AudioPeak Sound+ — Final Billing",
      "body": "Your final AudioPeak Sound+ charge of $12.99 has been processed. This covers your remaining billing period's replacement-parts shipment. Your plan will not renew after this cycle. Thank you for being a subscriber."
    }
  ]
}
```

- [ ] **Step 11: Write `issuer/customer-comms/cardholder_dispute_statement.json`**

```json
{
  "source": "Issuing Bank — Cardholder Services",
  "dataType": "Cardholder Dispute Statement",
  "reasonCode": "4841",
  "reasonCodeDescription": "Cancelled Recurring Transaction",
  "statement": {
    "disputeReason": "4841 - Cancelled Recurring Transaction",
    "detailedDescription": "The cardholder states they cancelled their AudioPeak Sound+ plan [7 days before the disputed billing date] via the merchant's website. Despite this, a $12.99 monthly charge was processed. The cardholder believes all charges after the cancellation should stop immediately.",
    "contactedMerchant": true,
    "merchantResponseSummary": "The cardholder emailed AudioPeak [7 days before the billing date] to cancel. The merchant responded acknowledging the cancellation but stated it would take effect at the end of the current billing cycle, not immediately.",
    "previousDisputeHistory": "None",
    "requestedResolution": "Reversal of the $12.99 post-cancellation charge.",
    "cardholderDeclaration": "I declare that the information provided in this dispute statement is true, accurate, and complete to the best of my knowledge and belief."
  },
  "bankInternalNotes": {
    "riskAssessment": "Medium — merchant may have valid defense documentation.",
    "recommendedAction": "Proceed with chargeback filing under Mastercard reason code 4841 (Cancelled Recurring Transaction).",
    "priority": "Standard"
  }
}
```

- [ ] **Step 12: Write `issuer/merchant/issuer_chargeback_documentation.json`**

```json
{
  "source": "Issuing Bank — Chargeback Department",
  "dataType": "Issuer Chargeback Documentation",
  "reasonCode": "4841",
  "reasonCodeDescription": "Cancelled Recurring Transaction",
  "chargebackFiling": {
    "reasonCode": "4841",
    "reasonCodeDescription": "Cancelled Recurring Transaction",
    "supportingDocuments": [
      { "type": "Cardholder Dispute Form", "desc": "Statement that the plan was cancelled before the charge date." },
      { "type": "Cardholder's Email Record", "desc": "Email from cardholder to merchant requesting cancellation." }
    ],
    "issuerCommentary": "Filing under 4841. The cardholder cancelled their recurring plan before the disputed charge. The cardholder expected the cancellation to be immediate.",
    "arbitrationEligible": true
  },
  "transactionDetails": {
    "cardPresent": false,
    "ecommerceIndicator": "ECI_07",
    "posEntryMode": "81"
  }
}
```

- [ ] **Step 13: Write `issuer/psp/issuer_transaction_record.json`**

```json
{
  "source": "Issuing Bank — Transaction Processing",
  "dataType": "Issuer Transaction Record",
  "reasonCode": "4841",
  "reasonCodeDescription": "Cancelled Recurring Transaction",
  "authorization": {
    "responseCode": "00",
    "merchantCategoryCode": "5999",
    "posEntryMode": "81",
    "cardNotPresent": true,
    "avsResult": {
      "code": "Y",
      "description": "Full match"
    },
    "cvvResult": {
      "code": "M",
      "description": "CVV on file from initial signup"
    }
  },
  "riskFlags": {
    "geolocationMismatch": "None",
    "deviceTrustScore": "High"
  }
}
```

- [ ] **Step 14: Validate all files are valid JSON**

Run: `powershell -Command "Get-ChildItem src/data/sources/acquirer/4841,src/data/sources/issuer/4841 -Recurse -Filter *.json | ForEach-Object { Get-Content $_.FullName -Raw | ConvertFrom-Json | Out-Null }; Write-Host 'all valid'"`

- [ ] **Step 15: Commit**

```bash
git add src/data/sources/acquirer/4841 src/data/sources/issuer/4841
git commit -m "Replace CloudVault SaaS story with AudioPeak Sound+ replacement-parts plan for 4841"
```

---

## Task 15: New story for 4859 — unauthorized shipping-upgrade addendum via IVR

**Files:** same 11-file pattern under `4859` (8 acquirer files including `device/3ds_authentication.json`, plus 3 issuer files).

Story: cardholder was charged a $24.99 "Expedited Shipping Upgrade" addendum on an existing AudioPeak order. AudioPeak's system logged a recorded IVR (phone) confirmation where the cardholder pressed "1" to accept the upgrade after receiving an automated call; the cardholder claims they never authorized this and don't recall the call.

- [ ] **Step 1: Read current content of all files under `src/data/sources/acquirer/4859/**` and `src/data/sources/issuer/4859/**`.**

- [ ] **Step 2: Write `merchant/order_details.json`**

```json
{
  "source": "AudioPeak Electronics — Order Management System",
  "dataType": "Order Details",
  "reasonCode": "4859",
  "reasonCodeDescription": "Addendum, No-show, or ATM Dispute",
  "orderRecord": {
    "orderStatus": "Addendum charge — expedited shipping upgrade added to existing order",
    "items": [
      {
        "name": "Order Addendum — Expedited Shipping Upgrade for Order #AP-7401",
        "quantity": 1,
        "unitPrice": 24.99,
        "sku": "AP-ADDENDUM-SHIP"
      }
    ],
    "shippingAddress": "Cardholder's billing address (same as original order #AP-7401)",
    "orderNotes": "Original order #AP-7401 was placed online [3 days before the addendum charge]. An automated IVR call was placed to the phone number on file offering an expedited shipping upgrade; the recorded call log shows the recipient pressed '1' to accept, triggering the $24.99 addendum charge."
  }
}
```

- [ ] **Step 3: Write `merchant/refund_policy.json`**

```json
{
  "source": "AudioPeak Electronics — Policies",
  "dataType": "Refund & Addendum Policy",
  "reasonCode": "4859",
  "reasonCodeDescription": "Addendum, No-show, or ATM Dispute",
  "policy": {
    "refundWindow": "N/A — addendum service already rendered",
    "policy": "Shipping-upgrade addendums confirmed via recorded IVR acceptance are non-refundable once the expedited shipment has occurred.",
    "disclosureMethod": "IVR script discloses the $24.99 charge amount before requesting confirmation; a recorded confirmation email is also sent immediately after acceptance.",
    "customerAcknowledgment": "Recorded IVR acceptance (keypress '1') logged with timestamp and phone number on file."
  }
}
```

- [ ] **Step 4: Write `psp/auth_log.json`**

```json
{
  "source": "AudioPeak Electronics — Payment Service Provider",
  "dataType": "Authorization Log",
  "reasonCode": "4859",
  "reasonCodeDescription": "Addendum, No-show, or ATM Dispute",
  "authorizationRecord": {
    "authorizationObtained": true,
    "responseCode": "00",
    "responseMessage": "Approved",
    "entryMode": "Ecommerce — addendum to card on file",
    "threeDSecure": {
      "enrolled": true,
      "authenticated": true,
      "eci": "05",
      "cavv": "Verified at original order"
    },
    "notes": "Addendum charge of $24.99 authorized against the card on file from original order #AP-7401, following recorded IVR acceptance."
  }
}
```

- [ ] **Step 5: Write `psp/settlement_record.json`**

```json
{
  "source": "AudioPeak Electronics — Payment Service Provider",
  "dataType": "Settlement Record",
  "reasonCode": "4859",
  "reasonCodeDescription": "Addendum, No-show, or ATM Dispute",
  "settlementRecord": {
    "notes": "Settled for $24.99 as an addendum to order #AP-7401. IVR acceptance log and expedited shipment record are on file."
  }
}
```

- [ ] **Step 6: Write `identity/avs_cvv_check.json`**

```json
{
  "source": "AudioPeak Electronics — Identity Verification",
  "dataType": "AVS/CVV Verification Results",
  "reasonCode": "4859",
  "reasonCodeDescription": "Addendum, No-show, or ATM Dispute",
  "verificationResults": {
    "avs": {
      "responseCode": "Y",
      "description": "Full match — card on file from original order"
    },
    "cvv": {
      "responseCode": "M",
      "description": "CVV on file from original order, not re-collected for addendum"
    }
  }
}
```

- [ ] **Step 7: Write `device/3ds_authentication.json`**

```json
{
  "source": "AudioPeak Electronics — Authentication",
  "dataType": "3D Secure Authentication Record",
  "reasonCode": "4859",
  "reasonCodeDescription": "Addendum, No-show, or ATM Dispute",
  "threeDSecure": {
    "version": "2.0",
    "enrolled": true,
    "status": "Y — Authenticated at original order",
    "eci": "05",
    "cavv": "Verified",
    "challenge": true,
    "liabilityShift": "Yes — at original order",
    "notes": "3D Secure was completed at the original order #AP-7401. The addendum charge reuses the authenticated card-on-file credentials, confirmed separately via recorded IVR acceptance."
  }
}
```

- [ ] **Step 8: Write `fraud-tools/risk_assessment.json`**

```json
{
  "source": "AudioPeak Electronics — Fraud Prevention",
  "dataType": "Device Fingerprint & Risk Assessment",
  "reasonCode": "4859",
  "reasonCodeDescription": "Addendum, No-show, or ATM Dispute",
  "riskAssessment": {
    "deviceFingerprint": {
      "status": "N/A — IVR phone confirmation, not a device session",
      "trust": "N/A",
      "match": null
    },
    "ipAnalysis": {
      "level": "Low",
      "proxy": false,
      "geoMatch": true,
      "notes": "Original order placed from cardholder's usual location. Addendum confirmed via phone call to the number on file, not a web session."
    },
    "overallRiskScore": "Low — recorded IVR acceptance to the verified phone number on file"
  }
}
```

- [ ] **Step 9: Write `customer-comms/email_logs.json`**

```json
{
  "source": "AudioPeak Electronics — Customer Communications",
  "dataType": "Email Communication Logs",
  "reasonCode": "4859",
  "reasonCodeDescription": "Addendum, No-show, or ATM Dispute",
  "communications": [
    {
      "type": "Original Order Confirmation",
      "dir": "merchant_to_customer",
      "timing": "[3 days before the addendum charge]",
      "subject": "AudioPeak Electronics — Order Confirmation #AP-7401",
      "body": "Your order is confirmed and will ship via standard ground shipping."
    },
    {
      "type": "IVR Call Log",
      "dir": "system_note",
      "timing": "[1 day before the addendum charge]",
      "subject": "Automated Call — Expedited Shipping Offer",
      "body": "Automated IVR call placed to the phone number on file for order #AP-7401. Script: 'Press 1 to upgrade to expedited shipping for $24.99, or stay on the line for other options.' Call log shows keypress '1' received at 11:42 AM, call duration 47 seconds."
    },
    {
      "type": "Addendum Confirmation Email",
      "dir": "merchant_to_customer",
      "timing": "Immediately after the IVR call",
      "subject": "AudioPeak Electronics — Shipping Upgrade Confirmed for Order #AP-7401",
      "body": "You've upgraded to expedited shipping for $24.99, confirmed by phone. Your order will now arrive sooner."
    },
    {
      "type": "Customer Dispute Inquiry",
      "dir": "customer_to_merchant",
      "timing": "[10 days after the addendum charge]",
      "subject": "I didn't authorize this extra charge",
      "body": "The cardholder emailed stating they don't recall the IVR call or agreeing to any shipping upgrade. AudioPeak responded with the recorded call log showing the keypress acceptance."
    }
  ]
}
```

- [ ] **Step 10: Write `issuer/customer-comms/cardholder_dispute_statement.json`**

```json
{
  "source": "Issuing Bank — Cardholder Services",
  "dataType": "Cardholder Dispute Statement",
  "reasonCode": "4859",
  "reasonCodeDescription": "Addendum, No-show, or ATM Dispute",
  "statement": {
    "disputeReason": "4859 - Addendum, No-show, or ATM Dispute",
    "detailedDescription": "The cardholder was charged an additional $24.99 'Expedited Shipping Upgrade' addendum by 'AUDIOPEAK ELECTRONICS' on top of their original order. The cardholder states they never agreed to this extra charge and does not recall any phone call about it.",
    "contactedMerchant": true,
    "merchantResponseSummary": "The cardholder emailed AudioPeak [10 days after the addendum charge] to dispute it. AudioPeak responded citing a recorded IVR call log showing a keypress acceptance of the upgrade offer.",
    "previousDisputeHistory": "None",
    "requestedResolution": "Reversal of the $24.99 addendum charge.",
    "cardholderDeclaration": "I declare that the information provided in this dispute statement is true, accurate, and complete to the best of my knowledge and belief."
  },
  "bankInternalNotes": {
    "riskAssessment": "Medium — merchant may have valid defense documentation via recorded IVR log.",
    "recommendedAction": "Proceed with chargeback filing under Mastercard reason code 4859 (Addendum, No-show, or ATM Dispute).",
    "priority": "Standard"
  }
}
```

- [ ] **Step 11: Write `issuer/merchant/issuer_chargeback_documentation.json`**

```json
{
  "source": "Issuing Bank — Chargeback Department",
  "dataType": "Issuer Chargeback Documentation",
  "reasonCode": "4859",
  "reasonCodeDescription": "Addendum, No-show, or ATM Dispute",
  "chargebackFiling": {
    "reasonCode": "4859",
    "reasonCodeDescription": "Addendum, No-show, or ATM Dispute",
    "supportingDocuments": [
      { "type": "Cardholder Dispute Form", "desc": "Statement that the $24.99 shipping-upgrade addendum was never authorized." },
      { "type": "Account Statement", "desc": "Showing the $24.99 addendum charge from AudioPeak Electronics." }
    ],
    "issuerCommentary": "Filing under 4859. The cardholder denies authorizing the addendum charge and does not recall the referenced phone call.",
    "arbitrationEligible": true
  },
  "transactionDetails": {
    "cardPresent": false,
    "ecommerceIndicator": "ECI_07",
    "posEntryMode": "81"
  }
}
```

- [ ] **Step 12: Write `issuer/psp/issuer_transaction_record.json`**

```json
{
  "source": "Issuing Bank — Transaction Processing",
  "dataType": "Issuer Transaction Record",
  "reasonCode": "4859",
  "reasonCodeDescription": "Addendum, No-show, or ATM Dispute",
  "authorization": {
    "responseCode": "00",
    "merchantCategoryCode": "5999",
    "posEntryMode": "81",
    "cardNotPresent": true,
    "avsResult": {
      "code": "Y",
      "description": "Full match"
    },
    "cvvResult": {
      "code": "M",
      "description": "CVV on file from original order"
    }
  },
  "riskFlags": {
    "geolocationMismatch": "None",
    "deviceTrustScore": "High"
  }
}
```

- [ ] **Step 13: Validate all files are valid JSON**

Run: `powershell -Command "Get-ChildItem src/data/sources/acquirer/4859,src/data/sources/issuer/4859 -Recurse -Filter *.json | ForEach-Object { Get-Content $_.FullName -Raw | ConvertFrom-Json | Out-Null }; Write-Host 'all valid'"`

- [ ] **Step 14: Commit**

```bash
git add src/data/sources/acquirer/4859 src/data/sources/issuer/4859
git commit -m "Replace Seaside Grand Hotel no-show story with AudioPeak shipping-addendum IVR story for 4859"
```

---

## Task 16: New story for 4831 — checkout price-mismatch (expired promo code)

**Files:** same 11-file pattern under `4831` (7 acquirer files, 3 issuer files — no `device/3ds_authentication.json` in this code's current tree).

Story: cardholder expected $16.51 off via a "SAVE20" promo code they recall entering at checkout; the code had expired 2 hours before checkout completed and was silently not applied, so the cardholder was charged $18.50 more than expected ($92.50 shown at cart vs $111.00 posted — no wait, keep numbers small and matched to the $18.50 fixed amount). Cardholder saw an $85.00 subtotal in an abandoned-cart email screenshot but the checkout session log shows the itemized total (subtotal + shipping + tax) they actually confirmed before paying was $18.50 higher due to standard shipping + tax, not a hidden fee.

- [ ] **Step 1: Read current content of all files under `src/data/sources/acquirer/4831/**` and `src/data/sources/issuer/4831/**`.**

- [ ] **Step 2: Write `merchant/order_details.json`**

```json
{
  "source": "AudioPeak Electronics — Order Management System",
  "dataType": "Order Details",
  "reasonCode": "4831",
  "reasonCodeDescription": "Transaction Amount Differs",
  "orderRecord": {
    "orderStatus": "Completed",
    "items": [
      {
        "name": "AudioPeak Portable Bluetooth Speaker — Compact",
        "quantity": 1,
        "unitPrice": 79.99,
        "sku": "AP-PBS-C"
      }
    ],
    "shippingAddress": "Cardholder's billing address",
    "orderNotes": "Checkout session itemized total: Subtotal $79.99, Standard Shipping $4.99, Tax $13.51 — Total $98.49. The cardholder's cart-preview screenshot shows only the $79.99 item subtotal before shipping and tax were calculated at the final checkout step. The $18.50 difference is shipping plus tax, both itemized and disclosed on the checkout confirmation page the cardholder confirmed before payment."
  }
}
```

- [ ] **Step 3: Write `merchant/refund_policy.json`**

```json
{
  "source": "AudioPeak Electronics — Policies",
  "dataType": "Refund & Pricing Policy",
  "reasonCode": "4831",
  "reasonCodeDescription": "Transaction Amount Differs",
  "policy": {
    "refundWindow": "N/A — pricing dispute, not a return",
    "policy": "Checkout totals include item subtotal, shipping, and applicable tax, itemized on the checkout confirmation page prior to payment.",
    "disclosureMethod": "Itemized total shown on checkout confirmation page and in the order confirmation email before and after payment.",
    "customerAcknowledgment": "Customer clicked 'Confirm & Pay' on the checkout confirmation page showing the itemized $98.49 total."
  }
}
```

- [ ] **Step 4: Write `psp/auth_log.json`**

```json
{
  "source": "AudioPeak Electronics — Payment Service Provider",
  "dataType": "Authorization Log",
  "reasonCode": "4831",
  "reasonCodeDescription": "Transaction Amount Differs",
  "authorizationRecord": {
    "authorizationObtained": true,
    "responseCode": "00",
    "responseMessage": "Approved",
    "entryMode": "Ecommerce",
    "threeDSecure": {
      "enrolled": true,
      "authenticated": true,
      "eci": "05",
      "cavv": "Verified"
    },
    "notes": "Authorization for the full itemized checkout total of $98.49 (subtotal $79.99 + shipping $4.99 + tax $13.51), matching the amount shown on the checkout confirmation page."
  }
}
```

- [ ] **Step 5: Write `psp/settlement_record.json`**

```json
{
  "source": "AudioPeak Electronics — Payment Service Provider",
  "dataType": "Settlement Record",
  "reasonCode": "4831",
  "reasonCodeDescription": "Transaction Amount Differs",
  "settlementRecord": {
    "notes": "Settled for $98.49, matching the authorized amount and the itemized checkout total (subtotal + shipping + tax)."
  }
}
```

- [ ] **Step 6: Write `identity/avs_cvv_check.json`**

```json
{
  "source": "AudioPeak Electronics — Identity Verification",
  "dataType": "AVS/CVV Verification Results",
  "reasonCode": "4831",
  "reasonCodeDescription": "Transaction Amount Differs",
  "verificationResults": {
    "avs": {
      "responseCode": "Y",
      "description": "Full match"
    },
    "cvv": {
      "responseCode": "M",
      "description": "CVV matched"
    }
  }
}
```

- [ ] **Step 7: Write `fraud-tools/risk_assessment.json`**

```json
{
  "source": "AudioPeak Electronics — Fraud Prevention",
  "dataType": "Device Fingerprint & Risk Assessment",
  "reasonCode": "4831",
  "reasonCodeDescription": "Transaction Amount Differs",
  "riskAssessment": {
    "deviceFingerprint": {
      "status": "Known device",
      "trust": "High",
      "match": true
    },
    "ipAnalysis": {
      "level": "Low",
      "proxy": false,
      "geoMatch": true,
      "notes": "IP consistent with cardholder's usual location."
    },
    "overallRiskScore": "Low — standard checkout pricing dispute, no fraud indicators"
  }
}
```

- [ ] **Step 8: Write `customer-comms/email_logs.json`**

```json
{
  "source": "AudioPeak Electronics — Customer Communications",
  "dataType": "Email Communication Logs",
  "reasonCode": "4831",
  "reasonCodeDescription": "Transaction Amount Differs",
  "communications": [
    {
      "type": "Order Confirmation",
      "dir": "merchant_to_customer",
      "timing": "At time of order",
      "subject": "AudioPeak Electronics — Order Confirmation #AP-3390",
      "body": "AudioPeak Portable Bluetooth Speaker — Compact. Subtotal: $79.99. Shipping: $4.99. Tax: $13.51. Total charged: $98.49."
    },
    {
      "type": "Pricing Dispute Inquiry",
      "dir": "customer_to_merchant",
      "timing": "[6 days after the order]",
      "subject": "Why was I charged more than I expected?",
      "body": "The cardholder emailed stating they expected to pay $79.99 based on the cart preview and were surprised by the $98.49 charge. AudioPeak responded with the itemized checkout confirmation showing shipping and tax were added at the final step, both disclosed before payment."
    }
  ]
}
```

- [ ] **Step 9: Write `issuer/customer-comms/cardholder_dispute_statement.json`**

```json
{
  "source": "Issuing Bank — Cardholder Services",
  "dataType": "Cardholder Dispute Statement",
  "reasonCode": "4831",
  "reasonCodeDescription": "Transaction Amount Differs",
  "statement": {
    "disputeReason": "4831 - Transaction Amount Differs",
    "detailedDescription": "The cardholder ordered from 'AUDIOPEAK ELECTRONICS' expecting to pay $79.99 based on the cart preview screen, but the posted amount is $98.49 — $18.50 more. The cardholder believes they were overcharged.",
    "contactedMerchant": true,
    "merchantResponseSummary": "The cardholder emailed AudioPeak [6 days after the order]. The merchant explained the $18.50 difference is standard shipping ($4.99) and tax ($13.51), both itemized on the checkout confirmation page before payment.",
    "previousDisputeHistory": "None",
    "requestedResolution": "Reversal of the $18.50 difference between the cart-preview amount and posted amount.",
    "cardholderDeclaration": "I declare that the information provided in this dispute statement is true, accurate, and complete to the best of my knowledge and belief."
  },
  "bankInternalNotes": {
    "riskAssessment": "Medium — merchant may have valid defense documentation showing itemized checkout total.",
    "recommendedAction": "Proceed with chargeback filing under Mastercard reason code 4831 (Transaction Amount Differs).",
    "priority": "Standard"
  }
}
```

- [ ] **Step 10: Write `issuer/merchant/issuer_chargeback_documentation.json`**

```json
{
  "source": "Issuing Bank — Chargeback Department",
  "dataType": "Issuer Chargeback Documentation",
  "reasonCode": "4831",
  "reasonCodeDescription": "Transaction Amount Differs",
  "chargebackFiling": {
    "reasonCode": "4831",
    "reasonCodeDescription": "Transaction Amount Differs",
    "supportingDocuments": [
      { "type": "Cardholder Dispute Form", "desc": "Statement indicating expected amount was $79.99." },
      { "type": "Account Statement", "desc": "Showing posted amount of $98.49." }
    ],
    "issuerCommentary": "Filing under 4831. Posted amount of $98.49 differs from the cardholder's expected amount of $79.99.",
    "arbitrationEligible": true
  },
  "transactionDetails": {
    "cardPresent": false,
    "ecommerceIndicator": "ECI_05",
    "posEntryMode": "81"
  }
}
```

- [ ] **Step 11: Write `issuer/psp/issuer_transaction_record.json`**

```json
{
  "source": "Issuing Bank — Transaction Processing",
  "dataType": "Issuer Transaction Record",
  "reasonCode": "4831",
  "reasonCodeDescription": "Transaction Amount Differs",
  "authorization": {
    "responseCode": "00",
    "merchantCategoryCode": "5999",
    "posEntryMode": "81",
    "cardNotPresent": true,
    "avsResult": {
      "code": "Y",
      "description": "Full match"
    },
    "cvvResult": {
      "code": "M",
      "description": "CVV matched"
    }
  },
  "riskFlags": {
    "geolocationMismatch": "None",
    "deviceTrustScore": "High"
  }
}
```

- [ ] **Step 12: Validate all files are valid JSON**

Run: `powershell -Command "Get-ChildItem src/data/sources/acquirer/4831,src/data/sources/issuer/4831 -Recurse -Filter *.json | ForEach-Object { Get-Content $_.FullName -Raw | ConvertFrom-Json | Out-Null }; Write-Host 'all valid'"`

Note: this story's transaction settles at $98.49 (the full itemized checkout total), matching the `"4831"` entry already set to `98.49` in `AudioPeakOverrideService.FIXED_AMOUNTS_BY_REASON_CODE` (Task 3) — no further reconciliation needed here.

- [ ] **Step 13: Commit**

```bash
git add src/data/sources/acquirer/4831 src/data/sources/issuer/4831
git commit -m "Replace Olive Garden Terrace tip story with AudioPeak checkout price-mismatch story for 4831"
```

---

## Task 17: Full regression pass and manual smoke check

**Files:** none (verification only).

- [ ] **Step 1: Run the full Maven test suite**

Run: `mvn test`
Expected: PASS, no failures or errors across all modules.

- [ ] **Step 2: Validate every JSON file across all 9 in-scope reason codes in one pass**

Run: `powershell -Command "$codes = '4837','4853','4863','4834','4831','4855','4841','4808','4859'; foreach ($c in $codes) { Get-ChildItem \"src/data/sources/acquirer/$c\",\"src/data/sources/issuer/$c\" -Recurse -Filter *.json -ErrorAction SilentlyContinue | ForEach-Object { Get-Content $_.FullName -Raw | ConvertFrom-Json | Out-Null } }; Write-Host 'all 9 codes valid'"`
Expected: prints `all 9 codes valid`.

- [ ] **Step 3: Confirm `reason-code-rules.json` is still valid and unchanged in key set**

Run: `powershell -Command "(Get-Content src/data/reason-code-rules.json -Raw | ConvertFrom-Json).PSObject.Properties.Name | Sort-Object"`
Expected: exactly `4808, 4831, 4834, 4837, 4841, 4853, 4855, 4859, 4863` (9 keys, alphabetically/numerically as printed).

- [ ] **Step 4: Grep for any leftover old merchant names across all 9 in-scope story folders**

Run: `powershell -Command "$codes = '4837','4853','4863','4834','4831','4855','4841','4808','4859'; $old = 'LuxeVision','CaseArtisan','PhoneShield','RenewTech','HomeStitch','CloudVault','Seaside Grand Hotel','Olive Garden Terrace'; foreach ($c in $codes) { Select-String -Path \"src/data/sources/acquirer/$c/**/*.json\",\"src/data/sources/issuer/$c/**/*.json\" -Pattern ($old -join '|') -ErrorAction SilentlyContinue } "`
Expected: no output (no matches) — confirms no leftover references to any of the old, pre-pivot merchant names remain in any in-scope story file.

- [ ] **Step 5: Grep for any leftover old merchant names in the two stray out-of-scope files, confirming they were correctly left untouched**

Run: `powershell -Command "Select-String -Path 'src/data/sources/acquirer/fraud-tools/manual_suggestion_0_case_3998.jpg','src/data/sources/acquirer/merchant/pre-renewal_notification_case_3987.jpg' -Pattern 'Macy' -ErrorAction SilentlyContinue"`
Expected: binary-file match warnings are fine/expected (these are image files, not text) — this step is a sanity check that the files still exist unmodified, not a strict content assertion.

- [ ] **Step 6: Manual smoke test — start the app and hit the two read endpoints**

Run: `mvn spring-boot:run` (in one terminal), then in another:
```bash
curl http://localhost:8080/api/disputes
curl "http://localhost:8080/api/ingestion/disputes?page=0&size=50"
```
Expected: both return only disputes (if any exist in the local dev DB) whose `reasonCode` is one of the 9 defined codes, each with `merchantName: "AUDIOPEAK ELECTRONICS"` and `currency: "USD"` if any disputes are present. If the local DB is empty, this step instead confirms both endpoints return an empty but well-formed response (`{"disputes":[], "totalElements":0, ...}` for the paginated one; `[]` for `/api/disputes`) without error.

Stop the app afterward (`Ctrl+C` in the `spring-boot:run` terminal).

- [ ] **Step 7: Final commit if any smoke-test fixes were needed, otherwise confirm clean state**

Run: `git status`
Expected: working tree clean (all changes already committed in Tasks 1-16). If smoke testing surfaced a bug requiring a fix, fix it, re-run Steps 1-6, then commit the fix separately with a descriptive message.

---

## Summary of what this plan does NOT do (explicitly out of scope, per the design spec)

- Does not expand `reason-code-rules.json` beyond the 9 codes.
- Does not touch any of the ~26 other reason-code story folders under `src/data/sources/` (they simply stop being ingested once Task 2 lands).
- Does not introduce a `Merchant` entity/table.
- Does not change the `ingestion.max-new-claims=3` cap value.
- Does not replace any image/PDF evidence file (audited in the design spec — all existing image/PDF evidence has concrete supporting content).
- Does not touch `merchantId`/`merchantCategory` fields (only `merchantName`, `currency`, `amount` are overridden) — flag during Task 17 smoke testing if either of these leaks the real Mastercard merchant identity to the frontend in a way that matters, and raise it as a follow-up rather than expanding this plan's scope.
