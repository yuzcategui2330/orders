package com.farmatodo.order.domains;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonProperty("id")
	@Column(name = "id")
	private Long id;

	@JsonProperty("client_id")
	@Column(name = "client_id", nullable = false)
	private Long clientId;

	@JsonProperty("amount")
	@Column(name = "amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal amount = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@JsonProperty("status")
	@Column(name = "status", nullable = false, length = 20)
	private OrderStatus status;

	@JsonProperty("delivered")
	@Column(name = "delivered", nullable = false)
	private Boolean delivered = Boolean.FALSE;

	@JsonProperty("created_at")
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@JsonProperty("updated_at")
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public static Order draft(Long clientId) {
		Order order = new Order();
		order.clientId = clientId;
		order.amount = BigDecimal.ZERO.setScale(2);
		order.status = OrderStatus.DRAFT;
		order.delivered = Boolean.FALSE;
		return order;
	}

	public Order withId(Long id) {
		this.id = id;
		return this;
	}

	public void updateAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public void markPaid() {
		this.status = OrderStatus.PAID;
	}

	public void markCancelled() {
		this.status = OrderStatus.CANCELLED;
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
		if (!(other instanceof Order order)) {
			return false;
		}
		return id != null && id.equals(order.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
