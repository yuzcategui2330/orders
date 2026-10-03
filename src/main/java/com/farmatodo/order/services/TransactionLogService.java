package com.farmatodo.order.services;

import com.farmatodo.order.domains.TransactionAction;
import com.farmatodo.order.domains.TransactionModule;
import com.farmatodo.order.domains.TransactionStatus;
import com.farmatodo.order.domains.TransactionType;
import com.farmatodo.order.exceptions.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class TransactionLogService {

	private static final int MAX_ERROR_LENGTH = 4000;
	private static final String PASSWORD = "password";

	private final JsonMapper jsonMapper;
	private final TransactionLogRecorder transactionLogRecorder;

	public TransactionLogService(JsonMapper jsonMapper, TransactionLogRecorder transactionLogRecorder) {
		this.jsonMapper = jsonMapper;
		this.transactionLogRecorder = transactionLogRecorder;
	}

	public void success(
			TransactionType type,
			TransactionAction action,
			TransactionModule module,
			Object request,
			Object response) {
		TransactionLogEntry entry = entry(type, action, module, request, response, TransactionStatus.SUCCESS, null);
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					submit(entry);
				}
			});
			return;
		}
		submit(entry);
	}

	public void failure(TransactionType type, TransactionModule module, Object request, RuntimeException exception) {
		String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
		Map<String, Object> error = new LinkedHashMap<>();
		error.put("message", message);
		if (exception instanceof ApiException apiException) {
			error.put("status", apiException.getStatus().value());
		}
		submit(entry(
				type,
				TransactionAction.FAIL,
				module,
				request,
				error,
				TransactionStatus.ERROR,
				trim(message)));
	}

	private TransactionLogEntry entry(
			TransactionType type,
			TransactionAction action,
			TransactionModule module,
			Object request,
			Object response,
			TransactionStatus status,
			String errorMessage) {
		return new TransactionLogEntry(
				TransactionContext.current(),
				LocalDateTime.now(),
				type,
				action,
				module,
				toJson(request),
				toJson(response),
				status,
				errorMessage);
	}

	private void submit(TransactionLogEntry entry) {
		try {
			transactionLogRecorder.record(entry);
		} catch (RuntimeException exception) {
			log.warn("Transaction log {} was not scheduled", entry.transactionId(), exception);
		}
	}

	private JsonNode toJson(Object value) {
		if (value == null) {
			return null;
		}
		try {
			JsonNode node = jsonMapper.valueToTree(value);
			removePassword(node);
			return node;
		} catch (RuntimeException exception) {
			log.warn("Transaction payload was not serialized", exception);
			return null;
		}
	}

	private void removePassword(JsonNode node) {
		if (node instanceof ObjectNode objectNode) {
			objectNode.remove(PASSWORD);
			objectNode.properties().forEach(field -> removePassword(field.getValue()));
		} else if (node instanceof ArrayNode arrayNode) {
			arrayNode.forEach(this::removePassword);
		}
	}

	private String trim(String message) {
		if (message.length() <= MAX_ERROR_LENGTH) {
			return message;
		}
		return message.substring(0, MAX_ERROR_LENGTH);
	}
}
