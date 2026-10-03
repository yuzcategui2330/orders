package com.farmatodo.order.domains.response;

import com.farmatodo.order.domains.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
		@JsonProperty("id") Long id,
		@JsonProperty("client_id") Long clientId,
		@JsonProperty("amount") BigDecimal amount,
		@JsonProperty("status") OrderStatus status,
		@JsonProperty("delivered") Boolean delivered,
		@JsonProperty("created_at") LocalDateTime createdAt,
		@JsonProperty("updated_at") LocalDateTime updatedAt,
		@JsonProperty("items") List<OrderItemResponse> items
) {
}
