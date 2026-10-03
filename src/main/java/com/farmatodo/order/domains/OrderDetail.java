package com.farmatodo.order.domains;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "order_details")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderDetail {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonProperty("id")
	@Column(name = "id")
	private Long id;

	@JsonProperty("order_id")
	@Column(name = "order_id", nullable = false)
	private Long orderId;

	@JsonProperty("product_id")
	@Column(name = "product_id", nullable = false)
	private Long productId;

	@JsonProperty("quantity")
	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@JsonProperty("amount")
	@Column(name = "amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@JsonProperty("created_at")
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@JsonProperty("updated_at")
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public static OrderDetail create(Long orderId, Long productId, int quantity, BigDecimal amount) {
		OrderDetail detail = new OrderDetail();
		detail.orderId = orderId;
		detail.productId = productId;
		detail.quantity = quantity;
		detail.amount = amount;
		return detail;
	}

	public void changeQuantity(int quantity, BigDecimal amount) {
		this.quantity = quantity;
		this.amount = amount;
	}

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof OrderDetail detail)) {
			return false;
		}
		return id != null && id.equals(detail.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
