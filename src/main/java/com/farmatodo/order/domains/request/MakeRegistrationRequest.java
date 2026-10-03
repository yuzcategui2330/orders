package com.farmatodo.order.domains.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MakeRegistrationRequest(
		@JsonProperty("username") String username,
		@JsonProperty("password") String password,
		@JsonProperty("name") String name,
		@JsonProperty("last_name") String lastName,
		@JsonProperty("phone") String phone,
		@JsonProperty("email") String email,
		@JsonProperty("address") String address
) {
}
