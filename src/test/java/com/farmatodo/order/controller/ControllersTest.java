package com.farmatodo.order.controller;

import com.farmatodo.order.domains.Client;
import com.farmatodo.order.domains.User;
import com.farmatodo.order.domains.request.AddItemRequest;
import com.farmatodo.order.domains.request.ChangePasswordRequest;
import com.farmatodo.order.domains.request.CreateOrderRequest;
import com.farmatodo.order.domains.request.LoginRequest;
import com.farmatodo.order.domains.request.MakeRegistrationRequest;
import com.farmatodo.order.domains.request.PayOrderRequest;
import com.farmatodo.order.domains.request.RefreshTokenRequest;
import com.farmatodo.order.domains.request.TokenizeCardRequest;
import com.farmatodo.order.domains.request.UpdateItemRequest;
import com.farmatodo.order.services.AuthService;
import com.farmatodo.order.services.ClientService;
import com.farmatodo.order.services.OrderService;
import com.farmatodo.order.services.ProductService;
import com.farmatodo.order.services.TokenizationService;
import com.farmatodo.order.services.UserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ControllersTest {

	@Test
	void userControllerReturnsTheServiceResult() {
		UserService userService = mock(UserService.class);
		UserController controller = new UserController(userService);
		User user = User.withCredentials("ana", "Secret1234");
		when(userService.create(user)).thenReturn(user);
		when(userService.update(1L, user)).thenReturn(user);

		assertEquals(201, controller.create(user).getStatusCode().value());
		assertEquals(200, controller.update(1L, user).getStatusCode().value());
	}

	@Test
	void authControllerDelegatesLoginRefreshAndPasswordChange() {
		AuthService authService = mock(AuthService.class);
		AuthController controller = new AuthController(authService);
		LoginRequest login = new LoginRequest("ana", "Secret1234");
		RefreshTokenRequest refresh = new RefreshTokenRequest("refresh");
		ChangePasswordRequest change = new ChangePasswordRequest("Oldpass123", "Newpass123");

		assertEquals(200, controller.login(login).getStatusCode().value());
		assertEquals(200, controller.refreshToken(refresh).getStatusCode().value());
		assertEquals(204, controller.changePassword(change).getStatusCode().value());
	}

	@Test
	void clientControllerDelegatesRegistrationAndQueries() {
		ClientService clientService = mock(ClientService.class);
		ClientController controller = new ClientController(clientService);
		MakeRegistrationRequest registration = new MakeRegistrationRequest("ana", "Secret1234", "Ana", "Perez", null, "a@b.c", null);
		Client client = Client.create("Ana", "Perez", null, "a@b.c", null, 1L);
		when(clientService.makeRegistration(registration)).thenReturn(client);
		when(clientService.update(1L, client)).thenReturn(client);
		when(clientService.findById(1L)).thenReturn(client);

		assertEquals(201, controller.makeRegistration(registration).getStatusCode().value());
		assertEquals(200, controller.update(1L, client).getStatusCode().value());
		assertEquals(200, controller.findById(1L).getStatusCode().value());
		assertEquals(200, controller.list().getStatusCode().value());
	}

	@Test
	void orderControllerDelegatesEachCommand() {
		OrderService orderService = mock(OrderService.class);
		OrderController controller = new OrderController(orderService);
		CreateOrderRequest create = new CreateOrderRequest(7L);
		AddItemRequest add = new AddItemRequest(5L, 1);
		UpdateItemRequest update = new UpdateItemRequest(5L, 2);
		PayOrderRequest pay = new PayOrderRequest("token");

		assertEquals(201, controller.create(create).getStatusCode().value());
		assertEquals(200, controller.findById(1L).getStatusCode().value());
		assertEquals(200, controller.addItem(1L, add).getStatusCode().value());
		assertEquals(200, controller.updateItem(1L, update).getStatusCode().value());
		assertEquals(200, controller.removeItem(1L, 5L).getStatusCode().value());
		assertEquals(200, controller.pay(1L, pay).getStatusCode().value());
		assertEquals(200, controller.cancel(1L).getStatusCode().value());
	}

	@Test
	void productAndTokenControllersDelegate() {
		ProductService productService = mock(ProductService.class);
		TokenizationService tokenizationService = mock(TokenizationService.class);
		ProductController products = new ProductController(productService);
		TokenController tokens = new TokenController(tokenizationService);
		TokenizeCardRequest request = new TokenizeCardRequest("4242424242424242", "123", 12, 2030, "Ana", 1L);

		assertEquals(200, products.search("amox", 0, 20).getStatusCode().value());
		assertEquals(201, tokens.tokenize(request).getStatusCode().value());
	}
}
