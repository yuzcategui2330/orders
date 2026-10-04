package com.farmatodo.order.services;

import com.farmatodo.order.domains.RefreshToken;
import com.farmatodo.order.domains.User;
import com.farmatodo.order.domains.request.ChangePasswordRequest;
import com.farmatodo.order.domains.request.LoginRequest;
import com.farmatodo.order.domains.response.TokenResponse;
import com.farmatodo.order.exceptions.ApiException;
import com.farmatodo.order.repositories.RefreshTokenRepository;
import com.farmatodo.order.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final JwtService jwtService = mock(JwtService.class);
	private final AuthService authService = new AuthService(
			userRepository,
			refreshTokenRepository,
			passwordEncoder,
			new PasswordPolicy(),
			jwtService);

	@AfterEach
	void clearSecurity() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void loginIssuesTokensForAnActiveUser() {
		User user = activeUser();
		when(userRepository.findByUsername("ana")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Secret1234", "hash")).thenReturn(true);
		when(jwtService.generateAccessToken(user)).thenReturn("access");
		when(jwtService.accessTokenExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(15));
		when(jwtService.refreshTokenDays()).thenReturn(7L);

		TokenResponse tokens = authService.login(new LoginRequest("ana", "Secret1234"));

		assertEquals("access", tokens.accessToken());
		assertNotNull(tokens.refreshToken());
		verify(user).setLastLogin(any());
		verify(refreshTokenRepository).save(any(RefreshToken.class));
	}

	@Test
	void loginRejectsMissingOrBadCredentials() {
		assertEquals(401, assertThrows(ApiException.class, () -> authService.login(new LoginRequest(null, null))).getStatus().value());
		when(userRepository.findByUsername("ana")).thenReturn(Optional.empty());
		assertEquals(401, assertThrows(ApiException.class, () -> authService.login(new LoginRequest("ana", "Secret1234"))).getStatus().value());

		User user = activeUser();
		when(userRepository.findByUsername("ana")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Secret1234", "hash")).thenReturn(false);
		assertEquals(401, assertThrows(ApiException.class, () -> authService.login(new LoginRequest("ana", "Secret1234"))).getStatus().value());

		when(passwordEncoder.matches("Secret1234", "hash")).thenReturn(true);
		when(user.getIsActive()).thenReturn(false);
		assertEquals(401, assertThrows(ApiException.class, () -> authService.login(new LoginRequest("ana", "Secret1234"))).getStatus().value());
	}

	@Test
	void refreshRotatesAValidToken() {
		User user = activeUser();
		RefreshToken stored = new RefreshToken(user, "hash", LocalDateTime.now().plusDays(1));
		when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));
		when(jwtService.generateAccessToken(user)).thenReturn("access");
		when(jwtService.accessTokenExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(15));
		when(jwtService.refreshTokenDays()).thenReturn(7L);

		TokenResponse tokens = authService.refresh("refresh-token");

		assertEquals("access", tokens.accessToken());
		assertEquals(Boolean.TRUE, stored.getRevoked());
	}

	@Test
	void refreshRejectsBlankRevokedExpiredOrInactiveTokens() {
		assertEquals(401, assertThrows(ApiException.class, () -> authService.refresh(" ")).getStatus().value());
		when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
		assertEquals(401, assertThrows(ApiException.class, () -> authService.refresh("missing")).getStatus().value());

		User user = activeUser();
		RefreshToken revoked = new RefreshToken(user, "hash", LocalDateTime.now().plusDays(1));
		revoked.setRevoked(true);
		when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revoked));
		assertEquals(401, assertThrows(ApiException.class, () -> authService.refresh("revoked")).getStatus().value());

		RefreshToken expired = new RefreshToken(user, "hash", LocalDateTime.now().minusMinutes(1));
		when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));
		assertEquals(401, assertThrows(ApiException.class, () -> authService.refresh("expired")).getStatus().value());

		when(user.getIsActive()).thenReturn(false);
		RefreshToken inactive = new RefreshToken(user, "hash", LocalDateTime.now().plusDays(1));
		when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(inactive));
		assertEquals(401, assertThrows(ApiException.class, () -> authService.refresh("inactive")).getStatus().value());
	}

	@Test
	void changePasswordUpdatesTheHashAndRevokesRefreshTokens() {
		User user = activeUser();
		when(user.getId()).thenReturn(8L);
		when(userRepository.findById(8L)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Oldpass123", "hash")).thenReturn(true);
		when(passwordEncoder.encode("Newpass123")).thenReturn("new-hash");
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(new AccessToken(8L, "ana"), null, List.of()));

		authService.changePassword(new ChangePasswordRequest("Oldpass123", "Newpass123"));

		verify(user).setPassword("new-hash");
		verify(userRepository).saveAndFlush(user);
		verify(refreshTokenRepository).revokeByUser(user);
	}

	@Test
	void changePasswordRejectsABadSessionOrPassword() {
		assertThrows(ApiException.class, () -> authService.changePassword(new ChangePasswordRequest("Oldpass123", "short")));

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(new AccessToken(8L, "ana"), null, List.of()));
		when(userRepository.findById(8L)).thenReturn(Optional.empty());
		assertEquals(404, assertThrows(ApiException.class,
				() -> authService.changePassword(new ChangePasswordRequest("Oldpass123", "Newpass123"))).getStatus().value());

		User user = activeUser();
		when(userRepository.findById(8L)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Oldpass123", "hash")).thenReturn(false);
		assertEquals(401, assertThrows(ApiException.class,
				() -> authService.changePassword(new ChangePasswordRequest("Oldpass123", "Newpass123"))).getStatus().value());

		SecurityContextHolder.clearContext();
		assertEquals(401, assertThrows(ApiException.class,
				() -> authService.changePassword(new ChangePasswordRequest("Oldpass123", "Newpass123"))).getStatus().value());
	}

	private User activeUser() {
		User user = mock(User.class);
		when(user.getPassword()).thenReturn("hash");
		when(user.getIsActive()).thenReturn(true);
		return user;
	}
}
