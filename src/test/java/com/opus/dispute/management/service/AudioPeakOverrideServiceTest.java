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
