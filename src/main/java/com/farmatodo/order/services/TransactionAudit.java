package com.farmatodo.order.services;

import com.farmatodo.order.domains.TransactionAction;
import com.farmatodo.order.domains.TransactionModule;
import com.farmatodo.order.domains.TransactionType;

import java.util.function.Supplier;

final class TransactionAudit {

	private TransactionAudit() {
	}

	static <T> T run(
			TransactionLogService transactionLogService,
			TransactionType type,
			TransactionAction action,
			TransactionModule module,
			Object request,
			Supplier<T> operation) {
		try {
			T result = operation.get();
			transactionLogService.success(type, action, module, request, result);
			return result;
		} catch (RuntimeException exception) {
			transactionLogService.failure(type, module, request, exception);
			throw exception;
		}
	}
}
