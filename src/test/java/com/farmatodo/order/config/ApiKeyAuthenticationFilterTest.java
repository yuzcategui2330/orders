package com.farmatodo.order.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyAuthenticationFilterTest {

	private final ApiKeyAuthenticationFilter filter =
			new ApiKeyAuthenticationFilter(new TokenizationProperties("secret-key", 0.0, "encryption-key"));

	@Test
	void rejectsMissingApiKey() throws Exception {
		MockHttpServletRequest request = tokenizeRequest(null);
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicBoolean continued = new AtomicBoolean(false);

		filter.doFilter(request, response, chain(continued));

		assertEquals(401, response.getStatus());
		assertTrue(response.getContentAsString().contains(ApiKeyAuthenticationFilter.INVALID_API_KEY));
		assertFalse(continued.get());
	}

	@Test
	void rejectsInvalidApiKey() throws Exception {
		MockHttpServletRequest request = tokenizeRequest("wrong-key");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicBoolean continued = new AtomicBoolean(false);

		filter.doFilter(request, response, chain(continued));

		assertEquals(401, response.getStatus());
		assertFalse(continued.get());
	}

	@Test
	void acceptsConfiguredApiKey() throws Exception {
		MockHttpServletRequest request = tokenizeRequest("secret-key");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicBoolean continued = new AtomicBoolean(false);

		filter.doFilter(request, response, chain(continued));

		assertTrue(continued.get());
		assertEquals(200, response.getStatus());
	}

	private MockHttpServletRequest tokenizeRequest(String apiKey) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/tokens");
		request.setServletPath("/api/v1/tokens");
		if (apiKey != null) {
			request.addHeader(ApiKeyAuthenticationFilter.HEADER, apiKey);
		}
		return request;
	}

	private FilterChain chain(AtomicBoolean continued) {
		return (request, response) -> continued.set(true);
	}
}
