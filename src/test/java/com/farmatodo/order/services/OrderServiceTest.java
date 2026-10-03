package com.farmatodo.order.services;

import com.farmatodo.order.domains.request.AddItemRequest;
import com.farmatodo.order.domains.Category;
import com.farmatodo.order.domains.CreditCard;
import com.farmatodo.order.domains.Order;
import com.farmatodo.order.domains.OrderDetail;
import com.farmatodo.order.domains.OrderStatus;
import com.farmatodo.order.domains.request.PayOrderRequest;
import com.farmatodo.order.domains.Payment;
import com.farmatodo.order.domains.PaymentStatus;
import com.farmatodo.order.domains.Product;
import com.farmatodo.order.domains.TransactionAction;
import com.farmatodo.order.domains.TransactionModule;
import com.farmatodo.order.domains.TransactionType;
import com.farmatodo.order.domains.request.UpdateItemRequest;
import com.farmatodo.order.exceptions.ApiException;
import com.farmatodo.order.repositories.ClientRepository;
import com.farmatodo.order.repositories.CreditCardRepository;
import com.farmatodo.order.repositories.OrderDetailRepository;
import com.farmatodo.order.repositories.OrderRepository;
import com.farmatodo.order.repositories.PaymentRepository;
import com.farmatodo.order.repositories.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderDetailRepository orderDetailRepository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ClientRepository clientRepository;

	@Mock
	private CreditCardRepository creditCardRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private RejectionGate rejectionGate;

	@Mock
	private OrderMailService orderMailService;

	@Mock
	private TransactionLogService transactionLogService;

	private OrderService orderService;
	private Order order;
	private final List<OrderDetail> details = new ArrayList<>();

	@BeforeEach
	void setUp() {
		orderService = new OrderService(
				orderRepository,
				orderDetailRepository,
				productRepository,
				clientRepository,
				creditCardRepository,
				paymentRepository,
				rejectionGate,
				orderMailService,
				transactionLogService);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(new AccessToken(1L, "jdoe"), null, List.of()));
		order = Order.draft(7L).withId(10L);
		when(clientRepository.findByIdAndUserId(7L, 1L)).thenReturn(Optional.of(mock(com.farmatodo.order.domains.Client.class)));
		when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
		when(orderDetailRepository.findByOrderId(10L)).thenAnswer(invocation -> details);
		when(orderDetailRepository.findByOrderIdAndProductId(any(), any())).thenAnswer(invocation -> details.stream()
				.filter(detail -> detail.getProductId().equals(invocation.getArgument(1)))
				.findFirst());
		when(orderDetailRepository.save(any(OrderDetail.class))).thenAnswer(invocation -> {
			OrderDetail detail = invocation.getArgument(0);
			if (details.stream().noneMatch(current -> current == detail)) {
				details.add(detail);
			}
			return detail;
		});
		doAnswer(invocation -> {
			details.removeIf(detail -> detail == invocation.getArgument(0));
			return null;
		}).when(orderDetailRepository).delete(any(OrderDetail.class));
	}

	@AfterEach
	void clearSecurity() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void addItemReservesStockAndSumsOrderAmount() {
		Product first = product(5L, 10, "5.00");
		Product second = product(8L, 4, "3.50");

		orderService.addItem(10L, new AddItemRequest(5L, 2));
		orderService.addItem(10L, new AddItemRequest(8L, 1));

		assertEquals(0, new BigDecimal("13.50").compareTo(order.getAmount()));
		assertEquals(2, details.size());
		assertEquals(2, first.getReservedQty());
		assertEquals(1, second.getReservedQty());
		assertEquals(10, first.getStockQuantity());
	}

	@Test
	void addExistingProductIncrementsQuantityAndReservation() {
		Product product = product(5L, 10, "5.00");

		orderService.addItem(10L, new AddItemRequest(5L, 1));
		orderService.addItem(10L, new AddItemRequest(5L, 2));

		assertEquals(1, details.size());
		assertEquals(3, details.getFirst().getQuantity());
		assertEquals(0, new BigDecimal("15.00").compareTo(details.getFirst().getAmount()));
		assertEquals(0, new BigDecimal("15.00").compareTo(order.getAmount()));
		assertEquals(3, product.getReservedQty());
	}

	@Test
	void reducingQuantityReleasesReservationAndRemovesDetailAtZero() {
		Product product = product(5L, 10, "5.00");
		orderService.addItem(10L, new AddItemRequest(5L, 3));

		orderService.updateItem(10L, new UpdateItemRequest(5L, 1));

		assertEquals(1, product.getReservedQty());
		assertEquals(0, new BigDecimal("5.00").compareTo(order.getAmount()));

		orderService.updateItem(10L, new UpdateItemRequest(5L, 0));

		assertEquals(0, product.getReservedQty());
		assertEquals(0, details.size());
		assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(order.getAmount()));
	}

	@Test
	void cancelReleasesReservedStockAndKeepsDetails() {
		Product product = product(5L, 10, "5.00");
		orderService.addItem(10L, new AddItemRequest(5L, 2));

		orderService.cancel(10L);

		assertEquals(OrderStatus.CANCELLED, order.getStatus());
		assertEquals(0, product.getReservedQty());
		assertEquals(1, details.size());
		assertEquals(10, product.getStockQuantity());
	}

	@Test
	void payWithTokenFromTokenizationFindsTheClientCard() {
		Product product = product(5L, 10, "5.00");
		orderService.addItem(10L, new AddItemRequest(5L, 2));
		String token = java.util.UUID.randomUUID().toString();
		CreditCard creditCard = CreditCard.create(token, "cipher", 12, Year.now().getValue() + 1, "Jane Doe", 7L).withId(3L);
		when(creditCardRepository.findByTokenAndClientId(token, 7L)).thenReturn(Optional.of(creditCard));
		when(rejectionGate.rejected()).thenReturn(false);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var payment = orderService.pay(10L, new PayOrderRequest(token));

		assertEquals(OrderStatus.PAID, order.getStatus());
		assertEquals(8, product.getStockQuantity());
		assertEquals(0, product.getReservedQty());
		assertEquals(PaymentStatus.APPROVED, payment.status());
		assertEquals(3L, payment.creditCardId());
		assertEquals(0, new BigDecimal("10.00").compareTo(payment.amount()));
		ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository).save(saved.capture());
		assertEquals(10L, saved.getValue().getOrderId());
		assertEquals(3L, saved.getValue().getCreditCardId());
		verify(creditCardRepository).findByTokenAndClientId(token, 7L);
		verify(orderMailService).notifyPaid(eq(order), any());
		verify(transactionLogService).success(
				eq(TransactionType.PAYMENT_PROCESS),
				eq(TransactionAction.PROCESS),
				eq(TransactionModule.PAYMENTS),
				any(),
				eq(payment));
	}

	@Test
	void payRejectsTokenThatDoesNotBelongToTheOrderClient() {
		product(5L, 10, "5.00");
		orderService.addItem(10L, new AddItemRequest(5L, 2));
		when(creditCardRepository.findByTokenAndClientId("foreign-token", 7L)).thenReturn(Optional.empty());

		ApiException exception = assertThrows(ApiException.class, () -> orderService.pay(10L, new PayOrderRequest("foreign-token")));

		assertEquals(404, exception.getStatus().value());
		assertEquals(OrderService.CREDIT_CARD_NOT_FOUND, exception.getMessage());
		assertEquals(OrderStatus.DRAFT, order.getStatus());
		verify(paymentRepository, never()).save(any());
	}

	@Test
	void payRejectsExpiredCardWithoutChangingStock() {
		Product product = product(5L, 10, "5.00");
		orderService.addItem(10L, new AddItemRequest(5L, 2));
		CreditCard creditCard = CreditCard.create("expired-token", "cipher", 1, 2000, "Jane Doe", 7L).withId(3L);
		when(creditCardRepository.findByTokenAndClientId("expired-token", 7L)).thenReturn(Optional.of(creditCard));

		ApiException exception = assertThrows(ApiException.class, () -> orderService.pay(10L, new PayOrderRequest("expired-token")));

		assertEquals(OrderService.CARD_EXPIRED, exception.getMessage());
		verify(transactionLogService).failure(
				eq(TransactionType.PAYMENT_PROCESS),
				eq(TransactionModule.PAYMENTS),
				any(),
				eq(exception));
		assertEquals(OrderStatus.DRAFT, order.getStatus());
		assertEquals(10, product.getStockQuantity());
		assertEquals(2, product.getReservedQty());
		verify(paymentRepository, never()).save(any());
	}

	@Test
	void addItemRejectsWhenAvailableStockIsNotEnough() {
		Product product = product(5L, 1, "5.00");

		ApiException exception = assertThrows(ApiException.class, () -> orderService.addItem(10L, new AddItemRequest(5L, 2)));

		assertEquals(OrderService.INSUFFICIENT_STOCK, exception.getMessage());
		assertEquals(0, product.getReservedQty());
		assertEquals(0, details.size());
	}

	@Test
	void findByIdDoesNotWriteATransactionLog() {
		when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

		orderService.findById(10L);

		verify(transactionLogService, never()).success(any(), any(), any(), any(), any());
		verify(transactionLogService, never()).failure(any(), any(), any(), any());
	}

	private Product product(Long id, int stock, String price) {
		Product product = Product.create("Amoxicillin", "Amox", Category.MEDICINES, stock)
				.withId(id)
				.priced(new BigDecimal(price));
		when(productRepository.findByIdForUpdate(id)).thenReturn(Optional.of(product));
		return product;
	}
}
