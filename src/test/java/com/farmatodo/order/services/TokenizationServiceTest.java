package com.farmatodo.order.services;

import com.farmatodo.order.config.TokenizationProperties;
import com.farmatodo.order.domains.response.CardTokenResponse;
import com.farmatodo.order.domains.CreditCard;
import com.farmatodo.order.domains.request.TokenizeCardRequest;
import com.farmatodo.order.exceptions.ApiException;
import com.farmatodo.order.repositories.ClientRepository;
import com.farmatodo.order.repositories.CreditCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenizationServiceTest {

	private static final String PAN = "4242424242424242";

	@Mock
	private CreditCardRepository creditCardRepository;

	@Mock
	private ClientRepository clientRepository;

	@Mock
	private RejectionGate rejectionGate;

	private TokenizationService tokenizationService;

	@BeforeEach
	void setUp() {
		CardCipher cardCipher = new CardCipher(new TokenizationProperties("test-key", 0.0, "test-encryption-key"));
		tokenizationService = new TokenizationService(
				creditCardRepository,
				clientRepository,
				cardCipher,
				rejectionGate);
	}

	@Test
	void tokenizeStoresEncryptedCardAndReturnsToken() {
		when(clientRepository.existsById(7L)).thenReturn(true);
		when(rejectionGate.rejected()).thenReturn(false);
		when(creditCardRepository.save(any(CreditCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CardTokenResponse response = tokenizationService.tokenize(validRequest());

		ArgumentCaptor<CreditCard> saved = ArgumentCaptor.forClass(CreditCard.class);
		verify(creditCardRepository).save(saved.capture());
		assertEquals(saved.getValue().getToken(), response.token());
		assertNotEquals(PAN, saved.getValue().getCardNumber());
		assertFalse(saved.getValue().getCardNumber().contains(PAN));
		assertEquals(7L, saved.getValue().getClientId());
	}

	@Test
	void tokenizeRejectsWhenProviderProbabilityMatches() {
		when(clientRepository.existsById(7L)).thenReturn(true);
		when(rejectionGate.rejected()).thenReturn(true);

		ApiException exception = assertThrows(ApiException.class, () -> tokenizationService.tokenize(validRequest()));

		assertEquals(422, exception.getStatus().value());
		assertEquals(TokenizationService.REJECTED, exception.getMessage());
		verify(creditCardRepository, never()).save(any());
	}

	@Test
	void tokenizeRejectsInvalidCardNumber() {
		TokenizeCardRequest request = new TokenizeCardRequest(
				"4242424242424243", "123", 12, Year.now().getValue() + 1, "Jane Doe", 7L);

		ApiException exception = assertThrows(ApiException.class, () -> tokenizationService.tokenize(request));

		assertEquals(400, exception.getStatus().value());
		assertEquals(TokenizationService.INVALID_CARD_NUMBER, exception.getMessage());
		verify(creditCardRepository, never()).save(any());
	}

	@Test
	void tokenizeRejectsExpiredCard() {
		TokenizeCardRequest request = new TokenizeCardRequest(PAN, "123", 1, 2000, "Jane Doe", 7L);

		ApiException exception = assertThrows(ApiException.class, () -> tokenizationService.tokenize(request));

		assertEquals(TokenizationService.CARD_EXPIRED, exception.getMessage());
		verify(creditCardRepository, never()).save(any());
	}

	@Test
	void tokenizeRejectsInvalidCvv() {
		TokenizeCardRequest request = new TokenizeCardRequest(
				PAN, "12", 12, Year.now().getValue() + 1, "Jane Doe", 7L);

		ApiException exception = assertThrows(ApiException.class, () -> tokenizationService.tokenize(request));

		assertEquals(TokenizationService.INVALID_CVV, exception.getMessage());
	}

	private TokenizeCardRequest validRequest() {
		return new TokenizeCardRequest(PAN, "123", 12, Year.now().getValue() + 1, "Jane Doe", 7L);
	}
}
