package com.farmatodo.order.services;

import com.farmatodo.order.domains.User;
import com.farmatodo.order.exceptions.ApiException;
import com.farmatodo.order.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	public static final String USERNAME_ALREADY_EXIST = "Username Already Exist";
	public static final String USER_NOT_FOUND = "User Not Found";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final PasswordPolicy passwordPolicy;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicy = passwordPolicy;
	}

	@Transactional
	public User create(User user) {
		passwordPolicy.validate(user.getPassword());
		if (user.getIsActive() == null) {
			user.setIsActive(Boolean.TRUE);
		}
		if (userRepository.existsByUsername(user.getUsername())) {
			throw usernameAlreadyExist();
		}
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		return save(user);
	}

	@Transactional
	public User update(Long id, User changes) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
		if (changes.getUsername() != null && !changes.getUsername().equals(user.getUsername())) {
			if (userRepository.existsByUsername(changes.getUsername())) {
				throw usernameAlreadyExist();
			}
			user.setUsername(changes.getUsername());
		}
		if (changes.getIsActive() != null) {
			user.setIsActive(changes.getIsActive());
		}
		return save(user);
	}

	private User save(User user) {
		try {
			return userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException exception) {
			if (isUsernameConflict(exception)) {
				throw usernameAlreadyExist();
			}
			throw exception;
		}
	}

	private boolean isUsernameConflict(DataIntegrityViolationException exception) {
		String detail = exception.getMostSpecificCause().getMessage();
		return detail != null && detail.contains("uk_users_username");
	}

	private ApiException usernameAlreadyExist() {
		return new ApiException(HttpStatus.NOT_FOUND, USERNAME_ALREADY_EXIST);
	}
}
