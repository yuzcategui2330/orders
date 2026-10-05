package com.farmatodo.order.services;

import com.farmatodo.order.config.JwtAuthenticationFilter;
import com.farmatodo.order.domains.Client;
import com.farmatodo.order.domains.TransactionAction;
import com.farmatodo.order.domains.TransactionModule;
import com.farmatodo.order.domains.TransactionType;
import com.farmatodo.order.domains.request.MakeRegistrationRequest;
import com.farmatodo.order.domains.User;
import com.farmatodo.order.exceptions.ApiException;
import com.farmatodo.order.repositories.ClientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ClientService {

	public static final String CLIENT_NOT_FOUND = "Client Not Found";
	public static final String INVALID_EMAIL = "Invalid email";
	public static final String INVALID_PHONE = "Invalid phone";
	public static final String EMAIL_ALREADY_EXISTS = "Email already exists";
	public static final String PHONE_ALREADY_EXISTS = "Phone already exists";

	private static final Pattern EMAIL = Pattern.compile(
			"^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$");
	private static final Pattern PHONE = Pattern.compile("^0?(?:58)?(?:412|414|416|422|424|426)\\d{7}$");

	private final ClientRepository clientRepository;
	private final UserService userService;
	private final TransactionLogService transactionLogService;

	public ClientService(ClientRepository clientRepository, UserService userService, TransactionLogService transactionLogService) {
		this.clientRepository = clientRepository;
		this.userService = userService;
		this.transactionLogService = transactionLogService;
	}

	@Transactional
	public Client makeRegistration(MakeRegistrationRequest request) {
		return TransactionAudit.run(
				transactionLogService,
				TransactionType.CLIENT_CREATE,
				TransactionAction.CREATE,
				TransactionModule.CLIENTS,
				request,
				() -> register(request));
	}

	@Transactional
	public Client update(Long id, Client changes) {
		return TransactionAudit.run(
				transactionLogService,
				TransactionType.CLIENT_UPDATE,
				TransactionAction.UPDATE,
				TransactionModule.CLIENTS,
				clientPayload(id, changes),
				() -> applyUpdate(id, changes));
	}

	@Transactional(readOnly = true)
	public Client findById(Long id) {
		return findOwned(id);
	}

	@Transactional(readOnly = true)
	public List<Client> list() {
		return clientRepository.findByUserId(currentUserId());
	}

	private Client register(MakeRegistrationRequest request) {
		requireText(request.username(), "Username is required");
		requireText(request.name(), "Name is required");
		requireText(request.lastName(), "Last name is required");
		String email = requireEmail(request.email());
		String phone = requirePhone(request.phone());
		requireUniqueEmail(email, null);
		requireUniquePhone(phone, null);
		User user = userService.create(User.withCredentials(request.username(), request.password()));
		Client client = Client.create(
				request.name().trim(),
				request.lastName().trim(),
				phone,
				email,
				blankToNull(request.address()),
				user.getId());
		return clientRepository.saveAndFlush(client);
	}

	private Client applyUpdate(Long id, Client changes) {
		Client client = findOwned(id);
		if (changes.getName() != null) {
			client.setName(changes.getName());
		}
		if (changes.getLastName() != null) {
			client.setLastName(changes.getLastName());
		}
		if (changes.getPhone() != null) {
			String phone = requirePhone(changes.getPhone());
			requireUniquePhone(phone, client.getId());
			client.setPhone(phone);
		}
		if (changes.getEmail() != null) {
			String email = requireEmail(changes.getEmail());
			requireUniqueEmail(email, client.getId());
			client.setEmail(email);
		}
		if (changes.getAddress() != null) {
			client.setAddress(changes.getAddress());
		}
		return clientRepository.saveAndFlush(client);
	}

	private Map<String, Object> clientPayload(Long clientId, Client changes) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("client_id", clientId);
		payload.put("body", changes);
		return payload;
	}

	private Client findOwned(Long id) {
		return clientRepository.findByIdAndUserId(id, currentUserId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND));
	}

	private Long currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AccessToken accessToken)) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, JwtAuthenticationFilter.INVALID_ACCESS_TOKEN);
		}
		return accessToken.userId();
	}

	private String requireEmail(String value) {
		if (value == null || value.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Email is required");
		}
		String email = value.trim().toLowerCase();
		if (email.length() > 120 || !EMAIL.matcher(email).matches()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_EMAIL);
		}
		return email;
	}

	private String requirePhone(String value) {
		if (value == null || value.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Phone is required");
		}
		String phone = value.trim();
		if (!PHONE.matcher(phone).matches()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_PHONE);
		}
		return phone;
	}

	private void requireUniqueEmail(String email, Long currentId) {
		boolean taken = currentId == null
				? clientRepository.existsByEmailIgnoreCase(email)
				: clientRepository.existsByEmailIgnoreCaseAndIdNot(email, currentId);
		if (taken) {
			throw new ApiException(HttpStatus.CONFLICT, EMAIL_ALREADY_EXISTS);
		}
	}

	private void requireUniquePhone(String phone, Long currentId) {
		boolean taken = currentId == null
				? clientRepository.existsByPhone(phone)
				: clientRepository.existsByPhoneAndIdNot(phone, currentId);
		if (taken) {
			throw new ApiException(HttpStatus.CONFLICT, PHONE_ALREADY_EXISTS);
		}
	}

	private void requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, message);
		}
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
