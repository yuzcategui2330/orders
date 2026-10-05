package com.farmatodo.order.services;

import com.farmatodo.order.domains.Client;
import com.farmatodo.order.domains.User;
import com.farmatodo.order.domains.request.MakeRegistrationRequest;
import com.farmatodo.order.exceptions.ApiException;
import com.farmatodo.order.repositories.ClientRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientServiceTest {

	private final ClientRepository clientRepository = mock(ClientRepository.class);
	private final UserService userService = mock(UserService.class);
	private final TransactionLogService transactionLogService = mock(TransactionLogService.class);
	private final ClientService clientService = new ClientService(clientRepository, userService, transactionLogService);

	@AfterEach
	void clearSecurity() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void registrationCreatesTheUserAndTheClient() {
		User user = mock(User.class);
		when(user.getId()).thenReturn(3L);
		when(userService.create(any(User.class))).thenReturn(user);
		when(clientRepository.saveAndFlush(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Client client = clientService.makeRegistration(new MakeRegistrationRequest(
				"ana", "Secret1234", " Ana ", " Perez ", "584141234567", " ana@mail.com ", null));

		assertEquals("Ana", client.getName());
		assertEquals("Perez", client.getLastName());
		assertEquals("584141234567", client.getPhone());
		assertEquals("ana@mail.com", client.getEmail());
		assertNull(client.getAddress());
		assertEquals(3L, client.getUserId());
		verify(transactionLogService).success(any(), any(), any(), any(), any());
	}

	@Test
	void registrationRejectsBlankRequiredFields() {
		assertEquals("Username is required", assertThrows(ApiException.class, () -> clientService.makeRegistration(
				new MakeRegistrationRequest(" ", "Secret1234", "Ana", "Perez", null, "ana@mail.com", null))).getMessage());
		assertEquals("Last name is required", assertThrows(ApiException.class, () -> clientService.makeRegistration(
				new MakeRegistrationRequest("ana", "Secret1234", "Ana", " ", null, "ana@mail.com", null))).getMessage());
		assertEquals("Email is required", assertThrows(ApiException.class, () -> clientService.makeRegistration(
				new MakeRegistrationRequest("ana", "Secret1234", "Ana", "Perez", null, " ", null))).getMessage());
		verify(transactionLogService, org.mockito.Mockito.atLeastOnce()).failure(any(), any(), any(), any());
	}

	@Test
	void registrationRejectsAnInvalidEmailBeforeCreatingTheUser() {
		ApiException exception = assertThrows(ApiException.class, () -> clientService.makeRegistration(
				new MakeRegistrationRequest("ana", "Secret1234", "Ana", "Perez", "584141234567", "yuzcategui@egmail", null)));

		assertEquals(ClientService.INVALID_EMAIL, exception.getMessage());
		assertEquals(400, exception.getStatus().value());
		verify(userService, never()).create(any());
	}

	@Test
	void registrationAcceptsAPhoneWithoutTheCountryPrefix() {
		User user = mock(User.class);
		when(user.getId()).thenReturn(3L);
		when(userService.create(any(User.class))).thenReturn(user);
		when(clientRepository.saveAndFlush(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Client client = clientService.makeRegistration(new MakeRegistrationRequest(
				"ana", "Secret1234", "Ana", "Perez", "4141234567", "ana@mail.com", null));

		assertEquals("4141234567", client.getPhone());

		Client withZero = clientService.makeRegistration(new MakeRegistrationRequest(
				"ana", "Secret1234", "Ana", "Perez", "04141234567", "ana@mail.com", null));

		assertEquals("04141234567", withZero.getPhone());
	}

	@Test
	void registrationRejectsAPhoneOutsideTheAllowedPrefixes() {
		ApiException exception = assertThrows(ApiException.class, () -> clientService.makeRegistration(
				new MakeRegistrationRequest("ana", "Secret1234", "Ana", "Perez", "02121234567", "ana@mail.com", null)));

		assertEquals(ClientService.INVALID_PHONE, exception.getMessage());
		verify(userService, never()).create(any());
	}

	@Test
	void registrationRejectsADuplicateEmailOrPhone() {
		when(clientRepository.existsByEmailIgnoreCase("ana@mail.com")).thenReturn(true);

		ApiException email = assertThrows(ApiException.class, () -> clientService.makeRegistration(
				new MakeRegistrationRequest("ana", "Secret1234", "Ana", "Perez", "584141234567", "ana@mail.com", null)));

		assertEquals(409, email.getStatus().value());
		assertEquals(ClientService.EMAIL_ALREADY_EXISTS, email.getMessage());

		when(clientRepository.existsByEmailIgnoreCase("otra@mail.com")).thenReturn(false);
		when(clientRepository.existsByPhone("584221234567")).thenReturn(true);

		ApiException phone = assertThrows(ApiException.class, () -> clientService.makeRegistration(
				new MakeRegistrationRequest("ana", "Secret1234", "Ana", "Perez", "584221234567", "otra@mail.com", null)));

		assertEquals(ClientService.PHONE_ALREADY_EXISTS, phone.getMessage());
		verify(userService, never()).create(any());
	}

	@Test
	void updateRejectsAnInvalidEmail() {
		authenticate();
		Client stored = Client.create("Ana", "Perez", "584141234567", "ana@mail.com", "Calle", 1L);
		when(clientRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(stored));
		Client changes = Client.create("Ana", "Perez", "584141234567", "sin-arroba", "Calle", 1L);

		ApiException exception = assertThrows(ApiException.class, () -> clientService.update(9L, changes));

		assertEquals(ClientService.INVALID_EMAIL, exception.getMessage());
		assertEquals("ana@mail.com", stored.getEmail());
	}

	@Test
	void updateChangesOnlyTheProvidedFields() {
		authenticate();
		Client stored = Client.create("Ana", "Perez", "584141234567", "ana@mail.com", "Calle", 1L);
		when(clientRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.of(stored));
		when(clientRepository.saveAndFlush(stored)).thenReturn(stored);
		Client changes = Client.create("Ana Maria", "Lopez", "584241234567", "nueva@mail.com", "Otra calle", 1L);

		Client updated = clientService.update(9L, changes);

		assertEquals("Ana Maria", updated.getName());
		assertEquals("Lopez", updated.getLastName());
		assertEquals("584241234567", updated.getPhone());
		assertEquals("nueva@mail.com", updated.getEmail());
		assertEquals("Otra calle", updated.getAddress());

		clientService.update(9L, Client.create("Ana Maria", "Lopez", "584241234567", "nueva@mail.com", "Otra calle", 1L));
		Client empty = Client.create("x", "y", "z", "e", "a", 1L);
		empty.setName(null);
		empty.setLastName(null);
		empty.setPhone(null);
		empty.setEmail(null);
		empty.setAddress(null);
		clientService.update(9L, empty);
		assertEquals("Ana Maria", stored.getName());
	}

	@Test
	void findByIdRejectsAClientOwnedBySomeoneElse() {
		authenticate();
		when(clientRepository.findByIdAndUserId(9L, 1L)).thenReturn(Optional.empty());

		ApiException exception = assertThrows(ApiException.class, () -> clientService.findById(9L));

		assertEquals(ClientService.CLIENT_NOT_FOUND, exception.getMessage());
	}

	@Test
	void listReturnsTheAuthenticatedUsersClients() {
		authenticate();
		when(clientRepository.findByUserId(1L)).thenReturn(List.of());

		assertEquals(0, clientService.list().size());
	}

	@Test
	void protectedCallsRequireAnAccessToken() {
		assertEquals(401, assertThrows(ApiException.class, () -> clientService.list()).getStatus().value());
	}

	private void authenticate() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(new AccessToken(1L, "ana"), null, List.of()));
	}
}
