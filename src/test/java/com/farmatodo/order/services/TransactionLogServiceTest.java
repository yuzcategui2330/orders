package com.farmatodo.order.services;

import com.farmatodo.order.domains.TransactionAction;
import com.farmatodo.order.domains.TransactionModule;
import com.farmatodo.order.domains.TransactionStatus;
import com.farmatodo.order.domains.TransactionType;
import com.farmatodo.order.domains.request.MakeRegistrationRequest;
import com.farmatodo.order.exceptions.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class TransactionLogServiceTest {

	private final TransactionLogRecorder recorder = mock(TransactionLogRecorder.class);
	private final List<TransactionLogEntry> entries = new ArrayList<>();
	private final TransactionLogService service = new TransactionLogService(JsonMapper.builder().build(), recorder);

	@BeforeEach
	void captureEntries() {
		doAnswer(invocation -> {
			entries.add(invocation.getArgument(0));
			return null;
		}).when(recorder).record(any());
	}

	@Test
	void stripsThePasswordFromTheRequestPayload() {
		service.success(
				TransactionType.CLIENT_CREATE,
				TransactionAction.CREATE,
				TransactionModule.CLIENTS,
				new MakeRegistrationRequest("ana", "Secret1234", "Ana", "Perez", null, "ana@mail.com", "Calle 1"),
				Map.of("id", 4));

		TransactionLogEntry entry = entries.getFirst();
		assertEquals(TransactionStatus.SUCCESS, entry.status());
		assertEquals(TransactionAction.CREATE, entry.action());
		assertFalse(entry.requestPayload().toString().contains("Secret1234"));
		assertFalse(entry.requestPayload().has("password"));
		assertEquals("ana", entry.requestPayload().get("username").asString());
	}

	@Test
	void failureKeepsTheBusinessError() {
		ApiException exception = new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "Payment rejected by the provider");

		ApiException thrown = assertThrows(ApiException.class, () -> TransactionAudit.run(
				service,
				TransactionType.PAYMENT_PROCESS,
				TransactionAction.PROCESS,
				TransactionModule.PAYMENTS,
				Map.of("order_id", 10),
				() -> {
					throw exception;
				}));

		assertEquals(exception, thrown);
		TransactionLogEntry entry = entries.getFirst();
		assertEquals(TransactionAction.FAIL, entry.action());
		assertEquals(TransactionStatus.ERROR, entry.status());
		assertEquals("Payment rejected by the provider", entry.errorMessage());
		assertEquals(422, entry.responsePayload().get("status").asInt());
	}

	@Test
	void schedulingFailureDoesNotEscapeToTheCaller() {
		doThrow(new RuntimeException("queue full")).when(recorder).record(any());

		assertDoesNotThrow(() -> service.failure(
				TransactionType.PAYMENT_PROCESS,
				TransactionModule.PAYMENTS,
				Map.of("order_id", 10),
				new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "Payment rejected by the provider")));
	}

	@Test
	void eventsFromTheSameRequestShareTheTransactionId() {
		TransactionContext.open();
		try {
			service.success(TransactionType.ORDER_CREATE, TransactionAction.CREATE, TransactionModule.ORDERS, Map.of("client_id", 7), Map.of("id", 10));
			service.success(TransactionType.ORDER_UPDATE, TransactionAction.UPDATE, TransactionModule.ORDERS, Map.of("order_id", 10), Map.of("id", 10));
		} finally {
			TransactionContext.close();
		}

		assertEquals(entries.get(0).transactionId(), entries.get(1).transactionId());
	}

	@Test
	void writerUsesAnIndependentTransaction() throws Exception {
		Transactional transactional = TransactionLogWriter.class
				.getMethod("save", TransactionLogEntry.class)
				.getAnnotation(Transactional.class);

		assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
	}
}
