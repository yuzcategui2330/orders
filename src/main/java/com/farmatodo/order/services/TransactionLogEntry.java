package com.farmatodo.order.services;

import com.farmatodo.order.domains.TransactionAction;
import com.farmatodo.order.domains.TransactionModule;
import com.farmatodo.order.domains.TransactionStatus;
import com.farmatodo.order.domains.TransactionType;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionLogEntry(
		UUID transactionId,
		LocalDateTime createdAt,
		TransactionType type,
		TransactionAction action,
		TransactionModule module,
		JsonNode requestPayload,
		JsonNode responsePayload,
		TransactionStatus status,
		String errorMessage
) {
}
