package com.farmatodo.order.services;

import com.farmatodo.order.config.AsyncConfig;
import com.farmatodo.order.domains.TransactionAction;
import com.farmatodo.order.domains.TransactionModule;
import com.farmatodo.order.domains.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig(classes = {AsyncConfig.class, TransactionLogAsyncTest.Beans.class})
class TransactionLogAsyncTest {

	@Autowired
	private TransactionLogService transactionLogService;

	@Autowired
	private TransactionLogWriter transactionLogWriter;

	@Test
	void recordsOnAnotherThreadAndHidesStorageFailures() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<String> threadName = new AtomicReference<>();
		doAnswer(invocation -> {
			threadName.set(Thread.currentThread().getName());
			latch.countDown();
			throw new RuntimeException("db down");
		}).when(transactionLogWriter).save(any());

		assertDoesNotThrow(() -> transactionLogService.success(
				TransactionType.ORDER_CREATE,
				TransactionAction.CREATE,
				TransactionModule.ORDERS,
				Map.of("client_id", 7),
				Map.of("id", 10)));

		assertTrue(latch.await(3, TimeUnit.SECONDS));
		assertNotEquals(Thread.currentThread().getName(), threadName.get());
		assertTrue(threadName.get().startsWith("transaction-log-"));
	}

	@Configuration
	static class Beans {

		@Bean
		TransactionLogWriter transactionLogWriter() {
			return mock(TransactionLogWriter.class);
		}

		@Bean
		TransactionLogRecorder transactionLogRecorder(TransactionLogWriter transactionLogWriter) {
			return new TransactionLogRecorder(transactionLogWriter);
		}

		@Bean
		TransactionLogService transactionLogService(TransactionLogRecorder transactionLogRecorder) {
			return new TransactionLogService(JsonMapper.builder().build(), transactionLogRecorder);
		}
	}
}
