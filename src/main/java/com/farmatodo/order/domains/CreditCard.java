package com.farmatodo.order.domains;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "credit_cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditCard {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonProperty("id")
	@Column(name = "id")
	private Long id;

	@JsonProperty("token")
	@Column(name = "token", nullable = false, length = 36, unique = true)
	private String token;

	@JsonProperty("card_number")
	@Column(name = "card_number", nullable = false, length = 512)
	private String cardNumber;

	@JsonProperty("expiration_month")
	@Column(name = "expiration_month", nullable = false)
	private Integer expirationMonth;

	@JsonProperty("expiration_year")
	@Column(name = "expiration_year", nullable = false)
	private Integer expirationYear;

	@JsonProperty("holder_name")
	@Column(name = "holder_name", nullable = false, length = 120)
	private String holderName;

	@JsonProperty("client_id")
	@Column(name = "client_id", nullable = false)
	private Long clientId;

	@JsonProperty("created_at")
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public static CreditCard create(
			String token,
			String encryptedCardNumber,
			int expirationMonth,
			int expirationYear,
			String holderName,
			Long clientId) {
		CreditCard creditCard = new CreditCard();
		creditCard.token = token;
		creditCard.cardNumber = encryptedCardNumber;
		creditCard.expirationMonth = expirationMonth;
		creditCard.expirationYear = expirationYear;
		creditCard.holderName = holderName;
		creditCard.clientId = clientId;
		return creditCard;
	}

	public CreditCard withId(Long id) {
		this.id = id;
		return this;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof CreditCard creditCard)) {
			return false;
		}
		return id != null && id.equals(creditCard.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
