package com.opus.dispute.management.service;

import org.springframework.stereotype.Component;

@Component
public class AcceptanceStatusResolver {

    public static final String NOT_REPRESENTED = "NOT_REPRESENTED";
    public static final String ACCEPTED_PRE_ARBITRATION = "ACCEPTED_PRE_ARBITRATION";
    public static final String ACCEPTED_ARBITRATION = "ACCEPTED_ARBITRATION";

    public String resolve(String progressState) {
        if (progressState == null || progressState.isBlank()) {
            return NOT_REPRESENTED;
        }

        String upper = progressState.toUpperCase();

        if (upper.startsWith("ARB") || upper.contains("-ARB-")) {
            return ACCEPTED_ARBITRATION;
        }

        if (upper.startsWith("PRE_ARB") || upper.startsWith("PREARB")
                || upper.contains("-PRE_ARB-") || upper.contains("-PREARB-")) {
            return ACCEPTED_PRE_ARBITRATION;
        }

        if (upper.startsWith("CB2") || upper.startsWith("SC2")
                || upper.contains("SECOND_PRESENTMENT") || upper.contains("REPRESENTMENT")) {
            return ACCEPTED_PRE_ARBITRATION;
        }

        return NOT_REPRESENTED;
    }

    public String describe(String status) {
        return switch (status) {
            case ACCEPTED_PRE_ARBITRATION ->
                    "Chargeback accepted at pre-arbitration stage. Acquirer will not escalate further.";
            case ACCEPTED_ARBITRATION ->
                    "Chargeback accepted at arbitration stage. Acquirer will not contest the arbitration ruling.";
            default ->
                    "Chargeback accepted. Acquirer will not represent (no second presentment will be filed).";
        };
    }
}
