package com.farmatodo.order.services;

import com.farmatodo.order.config.TokenizationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Random;

@Component
public class RejectionGate {

	private final TokenizationProperties properties;
	private final Random random;

	@Autowired
	public RejectionGate(TokenizationProperties properties) {
		this(properties, new SecureRandom());
	}

	RejectionGate(TokenizationProperties properties, Random random) {
		this.properties = properties;
		this.random = random;
	}

	public boolean rejected() {
		double rate = properties.rejectionRate();
		if (Double.isNaN(rate) || rate <= 0) {
			return false;
		}
		if (rate >= 1) {
			return true;
		}
		return random.nextDouble() < rate;
	}
}
