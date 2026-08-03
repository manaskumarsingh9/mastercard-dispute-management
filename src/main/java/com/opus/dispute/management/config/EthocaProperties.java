package com.opus.dispute.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ethoca")
public class EthocaProperties {
    private String baseUrl = "https://sandbox.api.ethocaweb.com";
    private String apiKeyId;
    private String apiSecret;
}
