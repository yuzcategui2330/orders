package com.farmatodo.order.domains.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChangePasswordRequest(
		@JsonProperty("current_password") String currentPassword,
		@JsonProperty("new_password") String newPassword
) {
}
