package com.farmatodo.order.domains.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TokenizeCardRequest(
		@NotBlank(message = "Card number is required")
		@JsonProperty("card_number")
		String cardNumber,
		@NotBlank(message = "CVV is required")
		@Pattern(regexp = "\\d{3,4}", message = "Invalid CVV")
		@JsonProperty("cvv")
		String cvv,
		@NotNull(message = "Expiration month is required")
		@Min(value = 1, message = "Invalid expiration month")
		@Max(value = 12, message = "Invalid expiration month")
		@JsonProperty("expiration_month")
		Integer expirationMonth,
		@NotNull(message = "Expiration year is required")
		@Min(value = 2000, message = "Invalid expiration year")
		@Max(value = 2100, message = "Invalid expiration year")
		@JsonProperty("expiration_year")
		Integer expirationYear,
		@NotBlank(message = "Holder name is required")
		@Size(max = 120, message = "Holder name is too long")
		@JsonProperty("holder_name")
		String holderName,
		@NotNull(message = "Client is required")
		@JsonProperty("client_id")
		Long clientId
) {
}
