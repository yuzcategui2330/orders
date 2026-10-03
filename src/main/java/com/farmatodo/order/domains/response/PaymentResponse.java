package com.farmatodo.order.domains.response;

import com.farmatodo.order.domains.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
		@JsonProperty("id") Long id,
		@JsonProperty("amount") BigDecimal amount,
		@JsonProperty("reference") String reference,
		@JsonProperty("credit_card_id") Long creditCardId,
		@JsonProperty("status") PaymentStatus status,
		@JsonProperty("order_id") Long orderId,
		@JsonProperty("created_at") LocalDateTime createdAt,
		@JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
