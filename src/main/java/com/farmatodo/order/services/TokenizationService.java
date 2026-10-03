package com.farmatodo.order.services;

import com.farmatodo.order.domains.response.CardTokenResponse;
import com.farmatodo.order.domains.CreditCard;
import com.farmatodo.order.domains.request.TokenizeCardRequest;
import com.farmatodo.order.exceptions.ApiException;
import com.farmatodo.order.repositories.ClientRepository;
import com.farmatodo.order.repositories.CreditCardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.UUID;

@Service
public class TokenizationService {

	public static final String INVALID_CARD_NUMBER = "Invalid card number";
	public static final String INVALID_CVV = "Invalid CVV";
	public static final String CARD_EXPIRED = "Card is expired";
	public static final String CLIENT_NOT_FOUND = "Client Not Found";
	public static final String REJECTED = "Tokenization rejected by the provider";

	private final CreditCardRepository creditCardRepository;
	private final ClientRepository clientRepository;
	private final CardCipher cardCipher;
	private final RejectionGate rejectionGate;

	public TokenizationService(
			CreditCardRepository creditCardRepository,
			ClientRepository clientRepository,
			CardCipher cardCipher,
			RejectionGate rejectionGate) {
		this.creditCardRepository = creditCardRepository;
		this.clientRepository = clientRepository;
		this.cardCipher = cardCipher;
		this.rejectionGate = rejectionGate;
	}

	@Transactional
	public CardTokenResponse tokenize(TokenizeCardRequest request) {
		String cardNumber = normalize(request.cardNumber());
		if (!isValidLuhn(cardNumber)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_CARD_NUMBER);
		}
		if (request.cvv() == null || !request.cvv().matches("\\d{3,4}")) {
			throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_CVV);
		}
		if (isExpired(request.expirationMonth(), request.expirationYear())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, CARD_EXPIRED);
		}
		if (request.holderName() == null || request.holderName().isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Holder name is required");
		}
		if (request.clientId() == null || !clientRepository.existsById(request.clientId())) {
			throw new ApiException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND);
		}
		if (rejectionGate.rejected()) {
			throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, REJECTED);
		}
		String token = UUID.randomUUID().toString();
		CreditCard creditCard = CreditCard.create(
				token,
				cardCipher.encrypt(cardNumber),
				request.expirationMonth(),
				request.expirationYear(),
				request.holderName().trim(),
				request.clientId());
		creditCardRepository.save(creditCard);
		return new CardTokenResponse(token);
	}

	private String normalize(String cardNumber) {
		if (cardNumber == null) {
			return "";
		}
		return cardNumber.replaceAll("[\\s-]", "");
	}

	private boolean isExpired(Integer month, Integer year) {
		if (month == null || year == null) {
			return true;
		}
		return YearMonth.of(year, month).isBefore(YearMonth.now());
	}

	private boolean isValidLuhn(String digits) {
		if (!digits.matches("\\d{13,19}")) {
			return false;
		}
		int sum = 0;
		boolean alternate = false;
		for (int index = digits.length() - 1; index >= 0; index--) {
			int value = digits.charAt(index) - '0';
			if (alternate) {
				value *= 2;
				if (value > 9) {
					value -= 9;
				}
			}
			sum += value;
			alternate = !alternate;
		}
		return sum % 10 == 0;
	}
}
