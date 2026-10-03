package com.farmatodo.order.domains.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginRequest(
		@JsonProperty("username") String username,
		@JsonProperty("password") String password
) {
}
