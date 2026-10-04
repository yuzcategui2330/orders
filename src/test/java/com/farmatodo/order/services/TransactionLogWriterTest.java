package com.farmatodo.order.services;

import com.farmatodo.order.domains.TransactionAction;
import com.farmatodo.order.domains.TransactionLog;
import com.farmatodo.order.domains.TransactionModule;
import com.farmatodo.order.domains.TransactionStatus;
import com.farmatodo.order.domains.TransactionType;
import com.farmatodo.order.repositories.TransactionLogRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TransactionLogWriterTest {

	@Test
	void savePersistsTheEntryInItsOwnTransaction() {
		TransactionLogRepository repository = mock(TransactionLogRepository.class);
		TransactionLogWriter writer = new TransactionLogWriter(repository);
		TransactionLogEntry entry = new TransactionLogEntry(
				UUID.randomUUID(),
				LocalDateTime.now(),
				TransactionType.PAYMENT_PROCESS,
				TransactionAction.PROCESS,
				TransactionModule.PAYMENTS,
				null,
				null,
				TransactionStatus.SUCCESS,
				null);

		writer.save(entry);

		verify(repository).save(any(TransactionLog.class));
	}
}
