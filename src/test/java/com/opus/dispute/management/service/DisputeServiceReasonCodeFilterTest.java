package com.opus.dispute.management.service;

import com.opus.dispute.management.entity.Dispute;
import com.opus.dispute.management.repository.DisputeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    void getAllDisputesFiltersByDefinedReasonCodes() {
        DisputeService disputeService = new DisputeService(disputeRepository, reasonCodeRulesService);

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
