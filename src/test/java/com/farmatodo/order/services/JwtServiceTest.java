package com.farmatodo.order.services;

import com.farmatodo.order.config.JwtProperties;
import com.farmatodo.order.domains.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

	private final JwtService jwtService = new JwtService(new JwtProperties(
			"orders-local-jwt-signing-key-2026-10-01",
			15,
			7));

	@Test
	void issuesATokenThatRoundTripsTheUser() {
		User user = mock(User.class);
		when(user.getId()).thenReturn(8L);
		when(user.getUsername()).thenReturn("ana");

		AccessToken accessToken = jwtService.parseAccessToken(jwtService.generateAccessToken(user));

		assertEquals(8L, accessToken.userId());
		assertEquals("ana", accessToken.username());
		assertTrue(jwtService.refreshTokenDays() == 7L);
		assertTrue(jwtService.accessTokenExpiresAt().isAfter(java.time.LocalDateTime.now()));
	}

	@Test
	void rejectsATokenSignedWithAnotherKey() {
		JwtService other = new JwtService(new JwtProperties("another-local-jwt-signing-key-2026-10-01", 15, 7));
		User user = mock(User.class);
		when(user.getId()).thenReturn(8L);
		when(user.getUsername()).thenReturn("ana");

		assertThrows(JwtException.class, () -> jwtService.parseAccessToken(other.generateAccessToken(user) + "x"));
	}

	@Test
	void rejectsATokenWithoutTheUserClaim() {
		String token = Jwts.builder()
				.subject("ana")
				.signWith(Keys.hmacShaKeyFor("orders-local-jwt-signing-key-2026-10-01".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
				.compact();

		assertThrows(JwtException.class, () -> jwtService.parseAccessToken(token));
	}
}
