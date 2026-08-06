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
