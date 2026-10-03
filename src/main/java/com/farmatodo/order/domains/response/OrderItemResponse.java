package com.farmatodo.order.domains.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record OrderItemResponse(
		@JsonProperty("id") Long id,
		@JsonProperty("product_id") Long productId,
		@JsonProperty("quantity") Integer quantity,
		@JsonProperty("amount") BigDecimal amount
) {
}
