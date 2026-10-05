package com.farmatodo.order.domains.response;

import com.farmatodo.order.domains.Client;
import com.farmatodo.order.domains.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ClientOrderResponse(
		@JsonProperty("id") Long id,
		@JsonProperty("amount") BigDecimal amount,
		@JsonProperty("status") OrderStatus status,
		@JsonProperty("delivered") Boolean delivered,
		@JsonProperty("created_at") LocalDateTime createdAt,
		@JsonProperty("updated_at") LocalDateTime updatedAt,
		@JsonProperty("client") Client client,
		@JsonProperty("credit_card") CreditCardResponse creditCard,
		@JsonProperty("products") List<PurchasedProductResponse> products
) {
}
