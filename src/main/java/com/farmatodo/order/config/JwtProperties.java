package com.farmatodo.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, long accessTokenMinutes, long refreshTokenDays) {
}
