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
