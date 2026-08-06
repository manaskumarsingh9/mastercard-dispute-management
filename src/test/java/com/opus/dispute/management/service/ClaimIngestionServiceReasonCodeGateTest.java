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
import org.springframework.test.util.ReflectionTestUtils;

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
        // maxNewClaims is a Spring @Value field; plain Mockito @InjectMocks does not
        // resolve @Value placeholders, so it defaults to 0 and the ingestion loop
        // would skip every queue before the reason-code gate is ever reached.
        ReflectionTestUtils.setField(claimIngestionService, "maxNewClaims", 3);

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

        // ingestFromQueues polls all of QUEUES_TO_POLL ("Pending", "Rejects", "Unworked");
        // since the mocked MastercardApiClient returns the same unsupported-reason-code
        // claim regardless of queue, the gate deletes it once per queue polled.
        verify(disputeRepository, atLeastOnce()).deleteById(1L);
    }
}
