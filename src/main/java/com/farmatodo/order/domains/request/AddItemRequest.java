package com.farmatodo.order.domains.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AddItemRequest(
		@JsonProperty("product_id") Long productId,
		@JsonProperty("quantity") Integer quantity
) {
}
