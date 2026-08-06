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
