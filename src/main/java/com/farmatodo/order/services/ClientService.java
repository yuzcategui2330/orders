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

@Service
public class ClientService {

	public static final String CLIENT_NOT_FOUND = "Client Not Found";

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
		requireText(request.email(), "Email is required");
		User user = userService.create(User.withCredentials(request.username(), request.password()));
		Client client = Client.create(
				request.name().trim(),
				request.lastName().trim(),
				blankToNull(request.phone()),
				request.email().trim(),
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
			client.setPhone(changes.getPhone());
		}
		if (changes.getEmail() != null) {
			client.setEmail(changes.getEmail());
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
