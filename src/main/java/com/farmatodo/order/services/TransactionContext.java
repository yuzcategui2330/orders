package com.farmatodo.order.services;

import java.util.UUID;

public final class TransactionContext {

	private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

	private TransactionContext() {
	}

	public static void open() {
		CURRENT.set(UUID.randomUUID());
	}

	public static void close() {
		CURRENT.remove();
	}

	public static UUID current() {
		UUID transactionId = CURRENT.get();
		if (transactionId == null) {
			transactionId = UUID.randomUUID();
			CURRENT.set(transactionId);
		}
		return transactionId;
	}
}
