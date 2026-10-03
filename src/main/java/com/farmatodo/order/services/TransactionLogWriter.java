package com.farmatodo.order.services;

import com.farmatodo.order.domains.TransactionLog;
import com.farmatodo.order.repositories.TransactionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionLogWriter {

	private final TransactionLogRepository transactionLogRepository;

	public TransactionLogWriter(TransactionLogRepository transactionLogRepository) {
		this.transactionLogRepository = transactionLogRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void save(TransactionLogEntry entry) {
		transactionLogRepository.save(TransactionLog.create(
				entry.transactionId(),
				entry.createdAt(),
				entry.type(),
				entry.action(),
				entry.module(),
				entry.requestPayload(),
				entry.responsePayload(),
				entry.status(),
				entry.errorMessage()));
	}
}
