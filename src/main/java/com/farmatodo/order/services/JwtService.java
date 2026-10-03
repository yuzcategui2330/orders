package com.farmatodo.order.services;

import com.farmatodo.order.config.JwtProperties;
import com.farmatodo.order.domains.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

@Service
public class JwtService {

	private final JwtProperties jwtProperties;
	private final SecretKey secretKey;

	public JwtService(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
		this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String generateAccessToken(User user) {
		Instant expiresAt = Instant.now().plusSeconds(jwtProperties.accessTokenMinutes() * 60);
		return Jwts.builder()
				.subject(user.getUsername())
				.claim("user_id", user.getId())
				.issuedAt(new Date())
				.expiration(Date.from(expiresAt))
				.signWith(secretKey)
				.compact();
	}

	public AccessToken parseAccessToken(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		Object userId = claims.get("user_id");
		if (!(userId instanceof Number id) || claims.getSubject() == null || claims.getSubject().isBlank()) {
			throw new MalformedJwtException("Access token is missing required claims");
		}
		return new AccessToken(id.longValue(), claims.getSubject());
	}

	public LocalDateTime accessTokenExpiresAt() {
		return LocalDateTime.now().plusMinutes(jwtProperties.accessTokenMinutes());
	}

	public long refreshTokenDays() {
		return jwtProperties.refreshTokenDays();
	}
}
