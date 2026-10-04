package com.farmatodo.order.services;

import com.farmatodo.order.domains.User;
import com.farmatodo.order.exceptions.ApiException;
import com.farmatodo.order.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final UserService userService = new UserService(userRepository, passwordEncoder, new PasswordPolicy());

	@Test
	void createHashesThePasswordAndDefaultsActive() {
		when(passwordEncoder.encode("Secret1234")).thenReturn("hash");
		when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		User request = User.withCredentials("ana", "Secret1234");
		request.setIsActive(null);

		User created = userService.create(request);

		assertEquals("hash", created.getPassword());
		assertTrue(created.getIsActive());
	}

	@Test
	void createKeepsAnExplicitInactiveFlag() {
		when(passwordEncoder.encode("Secret1234")).thenReturn("hash");
		when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		User user = User.withCredentials("ana", "Secret1234");
		user.setIsActive(Boolean.FALSE);

		User created = userService.create(user);

		assertFalse(created.getIsActive());
	}

	@Test
	void createRejectsADuplicateUsername() {
		when(userRepository.existsByUsername("ana")).thenReturn(true);

		ApiException exception = assertThrows(ApiException.class, () -> userService.create(User.withCredentials("ana", "Secret1234")));

		assertEquals(UserService.USERNAME_ALREADY_EXIST, exception.getMessage());
	}

	@Test
	void saveTranslatesAUsernameConstraintIntoTheApiError() {
		when(passwordEncoder.encode("Secret1234")).thenReturn("hash");
		when(userRepository.saveAndFlush(any(User.class))).thenThrow(
				new DataIntegrityViolationException("dup", new RuntimeException("uk_users_username")));

		ApiException exception = assertThrows(ApiException.class, () -> userService.create(User.withCredentials("ana", "Secret1234")));

		assertEquals(UserService.USERNAME_ALREADY_EXIST, exception.getMessage());
	}

	@Test
	void saveRethrowsUnrelatedIntegrityErrors() {
		when(passwordEncoder.encode("Secret1234")).thenReturn("hash");
		when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("other"));

		assertThrows(DataIntegrityViolationException.class, () -> userService.create(User.withCredentials("ana", "Secret1234")));
	}

	@Test
	void updateChangesUsernameAndActiveFlag() {
		User stored = User.withCredentials("ana", "hash");
		when(userRepository.findById(4L)).thenReturn(Optional.of(stored));
		when(userRepository.saveAndFlush(stored)).thenReturn(stored);
		User changes = User.withCredentials("ana2", null);
		changes.setIsActive(Boolean.FALSE);

		User updated = userService.update(4L, changes);

		assertSame(stored, updated);
		assertEquals("ana2", stored.getUsername());
		assertFalse(stored.getIsActive());
		verify(userRepository).existsByUsername("ana2");
	}

	@Test
	void updateKeepsTheUsernameWhenItDoesNotChange() {
		User stored = User.withCredentials("ana", "hash");
		when(userRepository.findById(4L)).thenReturn(Optional.of(stored));
		when(userRepository.saveAndFlush(stored)).thenReturn(stored);

		userService.update(4L, User.withCredentials("ana", null));

		assertEquals("ana", stored.getUsername());
	}

	@Test
	void updateRejectsATakenUsername() {
		User stored = User.withCredentials("ana", "hash");
		when(userRepository.findById(4L)).thenReturn(Optional.of(stored));
		when(userRepository.existsByUsername("ana2")).thenReturn(true);

		ApiException exception = assertThrows(
				ApiException.class,
				() -> userService.update(4L, User.withCredentials("ana2", null)));

		assertEquals(UserService.USERNAME_ALREADY_EXIST, exception.getMessage());
	}

	@Test
	void updateRejectsAMissingUser() {
		when(userRepository.findById(4L)).thenReturn(Optional.empty());

		ApiException exception = assertThrows(ApiException.class, () -> userService.update(4L, User.withCredentials("ana", null)));

		assertEquals(UserService.USER_NOT_FOUND, exception.getMessage());
	}
}
