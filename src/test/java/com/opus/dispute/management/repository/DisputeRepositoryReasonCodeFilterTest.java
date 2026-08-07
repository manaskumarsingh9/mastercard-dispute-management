package com.opus.dispute.management.repository;

import com.opus.dispute.management.entity.Dispute;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:disputeRepositoryReasonCodeFilterTest;NON_KEYWORDS=KEY;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
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
