package com.farmatodo.order.domains.response;

import com.farmatodo.order.domains.Category;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record PurchasedProductResponse(
		@JsonProperty("product_id") Long productId,
		@JsonProperty("name") String name,
		@JsonProperty("short_name") String shortName,
		@JsonProperty("description") String description,
		@JsonProperty("category") Category category,
		@JsonProperty("quantity") Integer quantity,
		@JsonProperty("amount") BigDecimal amount
) {
}
