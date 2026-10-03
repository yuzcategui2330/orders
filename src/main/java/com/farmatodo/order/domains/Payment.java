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
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonProperty("id")
	@Column(name = "id")
	private Long id;

	@JsonProperty("amount")
	@Column(name = "amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@JsonProperty("reference")
	@Column(name = "reference", nullable = false, length = 64)
	private String reference;

	@JsonProperty("credit_card_id")
	@Column(name = "credit_card_id", nullable = false)
	private Long creditCardId;

	@Enumerated(EnumType.STRING)
	@JsonProperty("status")
	@Column(name = "status", nullable = false, length = 20)
	private PaymentStatus status;

	@JsonProperty("order_id")
	@Column(name = "order_id", nullable = false)
	private Long orderId;

	@JsonProperty("created_at")
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@JsonProperty("updated_at")
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public static Payment approved(BigDecimal amount, String reference, Long creditCardId, Long orderId) {
		Payment payment = new Payment();
		payment.amount = amount;
		payment.reference = reference;
		payment.creditCardId = creditCardId;
		payment.status = PaymentStatus.APPROVED;
		payment.orderId = orderId;
		return payment;
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
		if (!(other instanceof Payment payment)) {
			return false;
		}
		return id != null && id.equals(payment.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
