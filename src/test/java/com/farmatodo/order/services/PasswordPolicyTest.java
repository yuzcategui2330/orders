package com.farmatodo.order.services;

import com.farmatodo.order.exceptions.ApiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

	private final PasswordPolicy passwordPolicy = new PasswordPolicy();

	@Test
	void acceptsAPasswordWithLettersAndDigits() {
		assertDoesNotThrow(() -> passwordPolicy.validate("abc123456"));
		assertDoesNotThrow(() -> passwordPolicy.validate("1234abcdE"));
	}

	@Test
	void rejectsPasswordsWithoutTheRequiredMix() {
		assertInvalid(null);
		assertInvalid("abc12345");
		assertInvalid("abcdefghi");
		assertInvalid("123456789");
	}

	private void assertInvalid(String password) {
		ApiException exception = assertThrows(ApiException.class, () -> passwordPolicy.validate(password));
		assertEquals(PasswordPolicy.INVALID_PASSWORD, exception.getMessage());
	}
}
