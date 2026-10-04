package com.farmatodo.order.services;

import com.farmatodo.order.domains.TransactionLog;
import com.farmatodo.order.repositories.TransactionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

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
				json(entry.requestPayload()),
				json(entry.responsePayload()),
				entry.status(),
				entry.errorMessage()));
	}

	private String json(JsonNode node) {
		return node == null ? null : node.toString();
	}
}
