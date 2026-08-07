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
