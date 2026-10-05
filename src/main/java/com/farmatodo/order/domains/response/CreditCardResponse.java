package com.farmatodo.order.domains.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreditCardResponse(
		@JsonProperty("id") Long id,
		@JsonProperty("token") String token,
		@JsonProperty("holder_name") String holderName,
		@JsonProperty("expiration_month") Integer expirationMonth,
		@JsonProperty("expiration_year") Integer expirationYear,
		@JsonProperty("client_id") Long clientId
) {
}
