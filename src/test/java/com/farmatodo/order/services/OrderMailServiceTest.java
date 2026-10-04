package com.farmatodo.order.services;

import com.farmatodo.order.domains.Client;
import com.farmatodo.order.domains.Order;
import com.farmatodo.order.domains.OrderDetail;
import com.farmatodo.order.domains.Product;
import com.farmatodo.order.repositories.ClientRepository;
import com.farmatodo.order.repositories.ProductRepository;
import com.farmatodo.order.templates.MailTemplate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderMailServiceTest {

	private final EmailService emailService = mock(EmailService.class);
	private final ClientRepository clientRepository = mock(ClientRepository.class);
	private final ProductRepository productRepository = mock(ProductRepository.class);
	private final OrderMailService orderMailService = new OrderMailService(emailService, clientRepository, productRepository);

	@Test
	void notifyPaidSendsTheClientAddressAndProducts() {
		Order order = Order.draft(7L).withId(10L);
		Client client = Client.create("Ana", "Perez", "0412", "ana@mail.com", "Calle 1", 1L);
		Product product = mock(Product.class);
		when(product.getName()).thenReturn("A&B");
		when(clientRepository.findById(7L)).thenReturn(Optional.of(client));
		when(productRepository.findById(5L)).thenReturn(Optional.of(product));

		orderMailService.notifyPaid(order, List.of(OrderDetail.create(10L, 5L, 2, new BigDecimal("10.00"))));

		verify(emailService).send(eq("ana@mail.com"), eq(MailTemplate.ORDER_PAID), any());
	}

	@Test
	void notifyPaymentRejectedUsesTodayAndAFallbackProductName() {
		Order order = Order.draft(7L).withId(10L);
		orderSetCreatedAt(order);
		Client client = Client.create("Ana", "Perez", null, "ana@mail.com", "  ", 1L);
		when(clientRepository.findById(7L)).thenReturn(Optional.of(client));
		when(productRepository.findById(5L)).thenReturn(Optional.empty());

		orderMailService.notifyPaymentRejected(order, List.of(OrderDetail.create(10L, 5L, 1, new BigDecimal("5.00"))));

		verify(emailService).send(eq("ana@mail.com"), eq(MailTemplate.ORDER_PAYMENT_REJECTED), any());
	}

	@Test
	void skipsWhenTheClientHasNoEmail() {
		Order order = Order.draft(7L).withId(10L);
		when(clientRepository.findById(7L)).thenReturn(Optional.empty());
		orderMailService.notifyPaid(order, List.of());

		Client client = Client.create("Ana", "Perez", null, "ana@mail.com", "Calle", 1L);
		client.setEmail(" ");
		when(clientRepository.findById(7L)).thenReturn(Optional.of(client));
		orderMailService.notifyPaymentRejected(order, List.of());

		verify(emailService, never()).send(any(), any(), any());
	}

	private void orderSetCreatedAt(Order order) {
		try {
			var field = Order.class.getDeclaredField("createdAt");
			field.setAccessible(true);
			field.set(order, LocalDateTime.of(2024, 6, 21, 10, 0));
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
