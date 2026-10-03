package com.farmatodo.order.services;

import com.farmatodo.order.exceptions.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

	public static final String INVALID_PASSWORD =
			"Password must contain letters and numbers and be longer than 8 characters";

	public void validate(String password) {
		if (!isValid(password)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_PASSWORD);
		}
	}

	private boolean isValid(String password) {
		if (password == null || password.length() <= 8) {
			return false;
		}
		boolean hasLetter = false;
		boolean hasDigit = false;
		for (int index = 0; index < password.length(); index++) {
			char character = password.charAt(index);
			if (Character.isLetter(character)) {
				hasLetter = true;
			} else if (Character.isDigit(character)) {
				hasDigit = true;
			}
			if (hasLetter && hasDigit) {
				return true;
			}
		}
		return false;
	}
}
