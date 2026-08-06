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
