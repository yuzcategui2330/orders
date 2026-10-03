package com.farmatodo.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tokenization")
public record TokenizationProperties(String apiKey, double rejectionRate, String encryptionKey) {
}
