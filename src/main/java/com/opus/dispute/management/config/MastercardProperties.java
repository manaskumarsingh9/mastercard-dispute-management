package com.opus.dispute.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Mastercard API integration
 */
@Data
@Component
@ConfigurationProperties(prefix = "mastercard")
public class MastercardProperties {
    private String baseUrl;
    private String consumerKey;
    private String keystorePassword;
    private String keystorePath;
}
