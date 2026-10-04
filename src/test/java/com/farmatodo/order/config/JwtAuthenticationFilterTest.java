package com.farmatodo.order.config;

import com.farmatodo.order.services.AccessToken;
import com.farmatodo.order.services.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

	private final JwtService jwtService = mock(JwtService.class);
	private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

	@AfterEach
	void clearSecurity() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void continuesWhenTheRequestHasNoBearerToken() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicBoolean continued = new AtomicBoolean(false);

		filter.doFilter(new MockHttpServletRequest(), response, chain(continued));

		assertTrue(continued.get());
		assertEquals(200, response.getStatus());
	}

	@Test
	void rejectsAnEmptyBearerToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer ");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicBoolean continued = new AtomicBoolean(false);

		filter.doFilter(request, response, chain(continued));

		assertFalse(continued.get());
		assertEquals(401, response.getStatus());
		assertTrue(response.getContentAsString().contains(JwtAuthenticationFilter.INVALID_ACCESS_TOKEN));
	}

	@Test
	void authenticatesAValidAccessToken() throws Exception {
		when(jwtService.parseAccessToken("token")).thenReturn(new AccessToken(4L, "ana"));
		MockHttpServletRequest request = bearer("token");
		AtomicBoolean continued = new AtomicBoolean(false);

		filter.doFilter(request, new MockHttpServletResponse(), chain(continued));

		assertTrue(continued.get());
		assertInstanceOf(AccessToken.class, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
	}

	@Test
	void rejectsAnExpiredToken() throws Exception {
		when(jwtService.parseAccessToken("expired")).thenThrow(new ExpiredJwtException(null, null, "expired"));
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(bearer("expired"), response, chain(new AtomicBoolean(false)));

		assertEquals(401, response.getStatus());
		assertTrue(response.getContentAsString().contains(JwtAuthenticationFilter.ACCESS_TOKEN_EXPIRED));
	}

	@Test
	void rejectsAnInvalidToken() throws Exception {
		when(jwtService.parseAccessToken("bad")).thenThrow(new JwtException("bad"));
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(bearer("bad"), response, chain(new AtomicBoolean(false)));

		assertEquals(401, response.getStatus());
		assertTrue(response.getContentAsString().contains(JwtAuthenticationFilter.INVALID_ACCESS_TOKEN));
	}

	private MockHttpServletRequest bearer(String token) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + token);
		return request;
	}

	private FilterChain chain(AtomicBoolean continued) {
		return (request, response) -> continued.set(true);
	}
}
