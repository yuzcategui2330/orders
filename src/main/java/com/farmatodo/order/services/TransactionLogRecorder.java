package com.farmatodo.order.services;

import com.farmatodo.order.config.AsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransactionLogRecorder {

	private final TransactionLogWriter transactionLogWriter;

	public TransactionLogRecorder(TransactionLogWriter transactionLogWriter) {
		this.transactionLogWriter = transactionLogWriter;
	}

	@Async(AsyncConfig.TRANSACTION_LOG_EXECUTOR)
	public void record(TransactionLogEntry entry) {
		try {
			transactionLogWriter.save(entry);
		} catch (RuntimeException exception) {
			log.warn("Transaction log {} was not stored", entry.transactionId(), exception);
		}
	}
}
