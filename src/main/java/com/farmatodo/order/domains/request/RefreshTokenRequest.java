package com.farmatodo.order.domains.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshTokenRequest(@JsonProperty("refresh_token") String refreshToken) {
}
