package com.opus.dispute.management.service;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class ReasonCodeRulesServiceTest {

    private final ReasonCodeRulesService service = new ReasonCodeRulesService(new DataSourceService());

    @Test
    void loadsAllNineInScopeReasonCodes() {
        Set<String> codes = service.getSupportedReasonCodes();

        assertThat(codes).containsExactlyInAnyOrder(
                "4837", "4853", "4863", "4834", "4831", "4855", "4841", "4808", "4859"
        );
    }

    @Test
    void excludes4871EvenThoughItIsDefinedInTheRulesFile() {
        Set<String> codes = service.getSupportedReasonCodes();

        assertThat(codes).doesNotContain("4871");
    }

    @Test
    void doesNotContainCodesAbsentFromRulesFile() {
        Set<String> codes = service.getSupportedReasonCodes();

        assertThat(codes).doesNotContain("4801", "4802", "4900");
    }

    @Test
    void repeatedCallsReturnSameCachedSet() {
        Set<String> first = service.getSupportedReasonCodes();
        Set<String> second = service.getSupportedReasonCodes();

        assertThat(first).isSameAs(second);
    }
}
