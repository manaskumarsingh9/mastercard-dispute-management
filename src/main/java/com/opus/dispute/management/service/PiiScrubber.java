package com.opus.dispute.management.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class PiiScrubber {

    private static final Pattern PAN_PATTERN = Pattern.compile("\\b\\d{13,19}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(?:\\+?1[-.]?)?\\(?\\d{3}\\)?[-.]?\\d{3}[-.]?\\d{4}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern IP_PATTERN = Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");
    private static final Pattern CVV_PATTERN = Pattern.compile("(?i)\\b(cvv|cvc|cvv2|cvc2)[:\\s]*\\d{3,4}\\b");
    private static final Pattern EXPIRY_PATTERN = Pattern.compile("(?i)\\b(exp(?:iry)?|expiration)[:\\s]*\\d{2}/\\d{2,4}\\b");

    public String scrub(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String scrubbed = text;
        scrubbed = PAN_PATTERN.matcher(scrubbed).replaceAll(match -> maskPan(match.group()));
        scrubbed = EMAIL_PATTERN.matcher(scrubbed).replaceAll("[EMAIL_REDACTED]");
        scrubbed = PHONE_PATTERN.matcher(scrubbed).replaceAll("[PHONE_REDACTED]");
        scrubbed = SSN_PATTERN.matcher(scrubbed).replaceAll("[SSN_REDACTED]");
        scrubbed = IP_PATTERN.matcher(scrubbed).replaceAll("[IP_REDACTED]");
        scrubbed = CVV_PATTERN.matcher(scrubbed).replaceAll("[CVV_REDACTED]");
        scrubbed = EXPIRY_PATTERN.matcher(scrubbed).replaceAll("[EXPIRY_REDACTED]");

        return scrubbed;
    }

    private String maskPan(String pan) {
        if (pan.length() < 8) {
            return "[PAN_REDACTED]";
        }
        String first6 = pan.substring(0, 6);
        String last4 = pan.substring(pan.length() - 4);
        String masked = "*".repeat(pan.length() - 10);
        return first6 + masked + last4;
    }

    public String maskAccountNumber(String accountNum) {
        if (accountNum == null || accountNum.length() < 8) {
            return accountNum;
        }
        return accountNum.substring(0, 6) + "***" + accountNum.substring(accountNum.length() - 4);
    }
}
