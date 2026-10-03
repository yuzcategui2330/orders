package com.farmatodo.order.services;

import com.farmatodo.order.config.TokenizationProperties;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RejectionGateTest {

	@Test
	void rejectsWhenRandomValueFallsInsideConfiguredRate() {
		RejectionGate gate = new RejectionGate(properties(0.2), fixed(0.1));

		assertTrue(gate.rejected());
	}

	@Test
	void acceptsWhenRandomValueFallsOutsideConfiguredRate() {
		RejectionGate gate = new RejectionGate(properties(0.2), fixed(0.9));

		assertFalse(gate.rejected());
	}

	@Test
	void neverRejectsWhenRateIsZero() {
		RejectionGate gate = new RejectionGate(properties(0.0), fixed(0.0));

		assertFalse(gate.rejected());
	}

	private TokenizationProperties properties(double rate) {
		return new TokenizationProperties("test-key", rate, "test-encryption-key");
	}

	private Random fixed(double value) {
		return new Random() {
			@Override
			public double nextDouble() {
				return value;
			}
		};
	}
}
