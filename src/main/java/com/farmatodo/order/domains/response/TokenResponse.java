package com.farmatodo.order.domains.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record TokenResponse(
		@JsonProperty("access_token") String accessToken,
		@JsonProperty("refresh_token") String refreshToken,
		@JsonProperty("expires_at") LocalDateTime expiresAt
) {
}
