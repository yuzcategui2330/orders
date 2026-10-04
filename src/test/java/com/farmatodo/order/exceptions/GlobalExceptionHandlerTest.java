package com.farmatodo.order.exceptions;

import com.farmatodo.order.controller.TokenController;
import com.farmatodo.order.domains.request.TokenizeCardRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void mapsAnApiExceptionToItsStatusAndMessage() {
		var response = handler.handleApiException(new ApiException(HttpStatus.NOT_FOUND, "Client Not Found"));

		assertEquals(404, response.getStatusCode().value());
		assertEquals("Client Not Found", response.getBody().message());
	}

	@Test
	void joinsValidationMessages() throws Exception {
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "card");
		bindingResult.addError(new FieldError("card", "number", "must be valid"));
		bindingResult.addError(new FieldError("card", "cvv", "is required"));

		var response = handler.handleValidation(validationException(bindingResult));

		assertEquals(400, response.getStatusCode().value());
		assertEquals("must be valid, is required", response.getBody().message());
	}

	@Test
	void usesAFallbackWhenValidationHasNoMessages() throws Exception {
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "card");

		var response = handler.handleValidation(validationException(bindingResult));

		assertEquals("Invalid request", response.getBody().message());
	}

	private MethodArgumentNotValidException validationException(BeanPropertyBindingResult bindingResult) throws Exception {
		MethodParameter parameter = new MethodParameter(
				TokenController.class.getDeclaredMethod("tokenize", TokenizeCardRequest.class),
				0);
		return new MethodArgumentNotValidException(parameter, bindingResult);
	}
}
