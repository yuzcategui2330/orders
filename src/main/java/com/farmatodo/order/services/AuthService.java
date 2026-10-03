package com.farmatodo.order.services;

import com.farmatodo.order.domains.request.ChangePasswordRequest;
import com.farmatodo.order.domains.request.LoginRequest;
import com.farmatodo.order.domains.RefreshToken;
import com.farmatodo.order.domains.response.TokenResponse;
import com.farmatodo.order.domains.User;
import com.farmatodo.order.config.JwtAuthenticationFilter;
import com.farmatodo.order.exceptions.ApiException;
import com.farmatodo.order.repositories.RefreshTokenRepository;
import com.farmatodo.order.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

	private static final String INVALID_CREDENTIALS = "Invalid credentials";
	private static final String USER_INACTIVE = "User is inactive";
	private static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";
	private static final String USER_NOT_FOUND = "User Not Found";

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final PasswordPolicy passwordPolicy;
	private final JwtService jwtService;
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthService(
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder,
			PasswordPolicy passwordPolicy,
			JwtService jwtService) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicy = passwordPolicy;
		this.jwtService = jwtService;
	}

	@Transactional
	public TokenResponse login(LoginRequest request) {
		if (request.username() == null || request.password() == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
		}
		User user = userRepository.findByUsername(request.username())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS));
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
		}
		if (!Boolean.TRUE.equals(user.getIsActive())) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, USER_INACTIVE);
		}
		user.setLastLogin(LocalDateTime.now());
		return issueTokens(user);
	}

	@Transactional
	public TokenResponse refresh(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_REFRESH_TOKEN);
		}
		RefreshToken stored = refreshTokenRepository.findByTokenHash(hashToken(refreshToken))
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, INVALID_REFRESH_TOKEN));
		if (Boolean.TRUE.equals(stored.getRevoked()) || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_REFRESH_TOKEN);
		}
		User user = stored.getUser();
		if (!Boolean.TRUE.equals(user.getIsActive())) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, USER_INACTIVE);
		}
		stored.setRevoked(true);
		return issueTokens(user);
	}

	@Transactional
	public void changePassword(ChangePasswordRequest request) {
		passwordPolicy.validate(request.newPassword());
		AccessToken accessToken = currentAccessToken();
		User user = userRepository.findById(accessToken.userId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
		if (request.currentPassword() == null || !passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
		}
		user.setPassword(passwordEncoder.encode(request.newPassword()));
		userRepository.saveAndFlush(user);
		refreshTokenRepository.revokeByUser(user);
	}

	private AccessToken currentAccessToken() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AccessToken accessToken)) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, JwtAuthenticationFilter.INVALID_ACCESS_TOKEN);
		}
		return accessToken;
	}

	private TokenResponse issueTokens(User user) {
		String refreshToken = newRefreshToken();
		LocalDateTime refreshExpiresAt = LocalDateTime.now().plusDays(jwtService.refreshTokenDays());
		refreshTokenRepository.save(new RefreshToken(user, hashToken(refreshToken), refreshExpiresAt));
		return new TokenResponse(jwtService.generateAccessToken(user), refreshToken, jwtService.accessTokenExpiresAt());
	}

	private String newRefreshToken() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hashToken(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}
}
