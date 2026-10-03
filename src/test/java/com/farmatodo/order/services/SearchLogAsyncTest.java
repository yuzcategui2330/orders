package com.farmatodo.order.services;

import com.farmatodo.order.config.AsyncConfig;
import com.farmatodo.order.repositories.SearchLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = {AsyncConfig.class, SearchLogAsyncTest.Beans.class})
class SearchLogAsyncTest {

	@Autowired
	private SearchLogService searchLogService;

	@Autowired
	private SearchLogRepository searchLogRepository;

	@Test
	void recordsOnAnotherThreadAndHidesStorageFailures() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<String> threadName = new AtomicReference<>();
		when(searchLogRepository.save(any())).thenAnswer(invocation -> {
			threadName.set(Thread.currentThread().getName());
			latch.countDown();
			throw new RuntimeException("db down");
		});

		assertDoesNotThrow(() -> searchLogService.record("amox", 3));
		assertTrue(latch.await(3, TimeUnit.SECONDS));
		assertNotEquals(Thread.currentThread().getName(), threadName.get());
		assertTrue(threadName.get().startsWith("search-log-"));
	}

	@Configuration
	static class Beans {

		@Bean
		SearchLogRepository searchLogRepository() {
			return mock(SearchLogRepository.class);
		}

		@Bean
		SearchLogService searchLogService(SearchLogRepository searchLogRepository) {
			return new SearchLogService(searchLogRepository);
		}
	}
}
