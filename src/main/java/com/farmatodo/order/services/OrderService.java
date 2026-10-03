package com.farmatodo.order.services;

import com.farmatodo.order.config.JwtAuthenticationFilter;
import com.farmatodo.order.domains.request.AddItemRequest;
import com.farmatodo.order.domains.request.CreateOrderRequest;
import com.farmatodo.order.domains.CreditCard;
import com.farmatodo.order.domains.Order;
import com.farmatodo.order.domains.OrderDetail;
import com.farmatodo.order.domains.response.OrderItemResponse;
import com.farmatodo.order.domains.response.OrderResponse;
import com.farmatodo.order.domains.OrderStatus;
import com.farmatodo.order.domains.request.PayOrderRequest;
import com.farmatodo.order.domains.Payment;
import com.farmatodo.order.domains.response.PaymentResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

	public static final String ORDER_NOT_FOUND = "Order Not Found";
	public static final String PRODUCT_NOT_FOUND = "Product Not Found";
	public static final String CLIENT_NOT_FOUND = "Client Not Found";
	public static final String CREDIT_CARD_NOT_FOUND = "Credit card Not Found";
	public static final String INSUFFICIENT_STOCK = "Insufficient stock";
	public static final String INVALID_QUANTITY = "Invalid quantity";
	public static final String ORDER_CANNOT_BE_MODIFIED = "Order cannot be modified";
	public static final String ORDER_CANNOT_BE_PAID = "Order cannot be paid";
	public static final String ORDER_CANNOT_BE_CANCELLED = "Order cannot be cancelled";
	public static final String ORDER_HAS_NO_ITEMS = "Order has no items";
	public static final String CARD_EXPIRED = "Card is expired";
	public static final String PAYMENT_REJECTED = "Payment rejected by the provider";

	private final OrderRepository orderRepository;
	private final OrderDetailRepository orderDetailRepository;
	private final ProductRepository productRepository;
	private final ClientRepository clientRepository;
	private final CreditCardRepository creditCardRepository;
	private final PaymentRepository paymentRepository;
	private final RejectionGate rejectionGate;
	private final OrderMailService orderMailService;
	private final TransactionLogService transactionLogService;

	public OrderService(
			OrderRepository orderRepository,
			OrderDetailRepository orderDetailRepository,
			ProductRepository productRepository,
			ClientRepository clientRepository,
			CreditCardRepository creditCardRepository,
			PaymentRepository paymentRepository,
			RejectionGate rejectionGate,
			OrderMailService orderMailService,
			TransactionLogService transactionLogService) {
		this.orderRepository = orderRepository;
		this.orderDetailRepository = orderDetailRepository;
		this.productRepository = productRepository;
		this.clientRepository = clientRepository;
		this.creditCardRepository = creditCardRepository;
		this.paymentRepository = paymentRepository;
		this.rejectionGate = rejectionGate;
		this.orderMailService = orderMailService;
		this.transactionLogService = transactionLogService;
	}

	@Transactional
	public OrderResponse create(CreateOrderRequest request) {
		return TransactionAudit.run(
				transactionLogService,
				TransactionType.ORDER_CREATE,
				TransactionAction.CREATE,
				TransactionModule.ORDERS,
				request,
				() -> createDraft(request));
	}

	@Transactional(readOnly = true)
	public OrderResponse findById(Long orderId) {
		Order order = ownedOrder(orderId);
		return toResponse(order, orderDetailRepository.findByOrderId(order.getId()));
	}

	@Transactional
	public OrderResponse addItem(Long orderId, AddItemRequest request) {
		return TransactionAudit.run(
				transactionLogService,
				TransactionType.ORDER_UPDATE,
				TransactionAction.UPDATE,
				TransactionModule.ORDERS,
				orderPayload(orderId, request),
				() -> addDraftItem(orderId, request));
	}

	private OrderResponse addDraftItem(Long orderId, AddItemRequest request) {
		if (request.quantity() == null || request.quantity() < 1) {
			throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_QUANTITY);
		}
		Order order = lockDraft(orderId);
		Product product = lockProduct(request.productId());
		OrderDetail detail = orderDetailRepository.findByOrderIdAndProductId(order.getId(), product.getId()).orElse(null);
		reserve(product, request.quantity());
		if (detail == null) {
			detail = OrderDetail.create(order.getId(), product.getId(), request.quantity(), lineAmount(product, request.quantity()));
		} else {
			int quantity = detail.getQuantity() + request.quantity();
			detail.changeQuantity(quantity, lineAmount(product, quantity));
		}
		orderDetailRepository.save(detail);
		recalculate(order);
		return toResponse(order, orderDetailRepository.findByOrderId(order.getId()));
	}

	@Transactional
	public OrderResponse updateItem(Long orderId, UpdateItemRequest request) {
		return TransactionAudit.run(
				transactionLogService,
				TransactionType.ORDER_UPDATE,
				TransactionAction.UPDATE,
				TransactionModule.ORDERS,
				orderPayload(orderId, request),
				() -> changeDraftItem(orderId, request));
	}

	private OrderResponse changeDraftItem(Long orderId, UpdateItemRequest request) {
		if (request.quantity() == null || request.quantity() < 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_QUANTITY);
		}
		Order order = lockDraft(orderId);
		Product product = lockProduct(request.productId());
		OrderDetail detail = orderDetailRepository.findByOrderIdAndProductId(order.getId(), product.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, PRODUCT_NOT_FOUND));
		int delta = request.quantity() - detail.getQuantity();
		if (delta > 0) {
			reserve(product, delta);
		} else if (delta < 0) {
			product.release(-delta);
		}
		if (request.quantity() == 0) {
			orderDetailRepository.delete(detail);
		} else {
			detail.changeQuantity(request.quantity(), lineAmount(product, request.quantity()));
			orderDetailRepository.save(detail);
		}
		recalculate(order);
		return toResponse(order, orderDetailRepository.findByOrderId(order.getId()));
	}

	@Transactional
	public OrderResponse removeItem(Long orderId, Long productId) {
		return TransactionAudit.run(
				transactionLogService,
				TransactionType.ORDER_UPDATE,
				TransactionAction.UPDATE,
				TransactionModule.ORDERS,
				itemPayload(orderId, productId),
				() -> deleteDraftItem(orderId, productId));
	}

	private OrderResponse deleteDraftItem(Long orderId, Long productId) {
		Order order = lockDraft(orderId);
		Product product = lockProduct(productId);
		OrderDetail detail = orderDetailRepository.findByOrderIdAndProductId(order.getId(), product.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, PRODUCT_NOT_FOUND));
		product.release(detail.getQuantity());
		orderDetailRepository.delete(detail);
		recalculate(order);
		return toResponse(order, orderDetailRepository.findByOrderId(order.getId()));
	}

	@Transactional
	public PaymentResponse pay(Long orderId, PayOrderRequest request) {
		return TransactionAudit.run(
				transactionLogService,
				TransactionType.PAYMENT_PROCESS,
				TransactionAction.PROCESS,
				TransactionModule.PAYMENTS,
				orderPayload(orderId, request),
				() -> completePayment(orderId, request));
	}

	private PaymentResponse completePayment(Long orderId, PayOrderRequest request) {
		Order order = lockOwned(orderId);
		if (order.getStatus() != OrderStatus.DRAFT) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ORDER_CANNOT_BE_PAID);
		}
		List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId()).stream()
				.sorted(Comparator.comparing(OrderDetail::getProductId))
				.toList();
		if (details.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ORDER_HAS_NO_ITEMS);
		}
		if (request.token() == null || request.token().isBlank()) {
			throw new ApiException(HttpStatus.NOT_FOUND, CREDIT_CARD_NOT_FOUND);
		}
		CreditCard creditCard = creditCardRepository.findByTokenAndClientId(request.token(), order.getClientId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, CREDIT_CARD_NOT_FOUND));
		if (isExpired(creditCard)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, CARD_EXPIRED);
		}
		if (rejectionGate.rejected()) {
			orderMailService.notifyPaymentRejected(order, details);
			throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, PAYMENT_REJECTED);
		}
		for (OrderDetail detail : details) {
			Product product = lockProduct(detail.getProductId());
			if (product.getReservedQty() < detail.getQuantity() || product.getStockQuantity() < detail.getQuantity()) {
				throw new ApiException(HttpStatus.BAD_REQUEST, INSUFFICIENT_STOCK);
			}
			product.commitSale(detail.getQuantity());
		}
		order.markPaid();
		Payment payment = paymentRepository.save(Payment.approved(
				order.getAmount(),
				UUID.randomUUID().toString(),
				creditCard.getId(),
				order.getId()));
		orderMailService.notifyPaid(order, details);
		return toPayment(payment);
	}

	@Transactional
	public OrderResponse cancel(Long orderId) {
		return TransactionAudit.run(
				transactionLogService,
				TransactionType.ORDER_UPDATE,
				TransactionAction.UPDATE,
				TransactionModule.ORDERS,
				orderPayload(orderId, null),
				() -> cancelDraft(orderId));
	}

	private OrderResponse cancelDraft(Long orderId) {
		Order order = lockOwned(orderId);
		if (order.getStatus() != OrderStatus.DRAFT) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ORDER_CANNOT_BE_CANCELLED);
		}
		List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId()).stream()
				.sorted(Comparator.comparing(OrderDetail::getProductId))
				.toList();
		for (OrderDetail detail : details) {
			Product product = lockProduct(detail.getProductId());
			product.release(detail.getQuantity());
		}
		order.markCancelled();
		return toResponse(order, details);
	}

	private OrderResponse createDraft(CreateOrderRequest request) {
		if (request.clientId() == null || clientRepository.findByIdAndUserId(request.clientId(), currentUserId()).isEmpty()) {
			throw new ApiException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND);
		}
		Order order = orderRepository.save(Order.draft(request.clientId()));
		return toResponse(order, List.of());
	}

	private Map<String, Object> orderPayload(Long orderId, Object body) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", orderId);
		if (body != null) {
			payload.put("body", body);
		}
		return payload;
	}

	private Map<String, Object> itemPayload(Long orderId, Long productId) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("product_id", productId);
		return orderPayload(orderId, body);
	}

	private void reserve(Product product, int quantity) {
		if (product.availableStock() < quantity) {
			throw new ApiException(HttpStatus.BAD_REQUEST, INSUFFICIENT_STOCK);
		}
		product.reserve(quantity);
	}

	private void recalculate(Order order) {
		BigDecimal amount = orderDetailRepository.findByOrderId(order.getId()).stream()
				.map(OrderDetail::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
		order.updateAmount(amount);
	}

	private BigDecimal lineAmount(Product product, int quantity) {
		return product.getPrice().multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
	}

	private Order lockDraft(Long orderId) {
		Order order = lockOwned(orderId);
		if (order.getStatus() != OrderStatus.DRAFT) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ORDER_CANNOT_BE_MODIFIED);
		}
		return order;
	}

	private Order lockOwned(Long orderId) {
		Order order = orderRepository.findByIdForUpdate(orderId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND));
		if (clientRepository.findByIdAndUserId(order.getClientId(), currentUserId()).isEmpty()) {
			throw new ApiException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND);
		}
		return order;
	}

	private Order ownedOrder(Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND));
		if (clientRepository.findByIdAndUserId(order.getClientId(), currentUserId()).isEmpty()) {
			throw new ApiException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND);
		}
		return order;
	}

	private Product lockProduct(Long productId) {
		if (productId == null) {
			throw new ApiException(HttpStatus.NOT_FOUND, PRODUCT_NOT_FOUND);
		}
		return productRepository.findByIdForUpdate(productId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, PRODUCT_NOT_FOUND));
	}

	private boolean isExpired(CreditCard creditCard) {
		return YearMonth.of(creditCard.getExpirationYear(), creditCard.getExpirationMonth()).isBefore(YearMonth.now());
	}

	private OrderResponse toResponse(Order order, List<OrderDetail> details) {
		List<OrderItemResponse> items = details.stream()
				.map(detail -> new OrderItemResponse(detail.getId(), detail.getProductId(), detail.getQuantity(), detail.getAmount()))
				.toList();
		return new OrderResponse(
				order.getId(),
				order.getClientId(),
				order.getAmount(),
				order.getStatus(),
				order.getDelivered(),
				order.getCreatedAt(),
				order.getUpdatedAt(),
				items);
	}

	private PaymentResponse toPayment(Payment payment) {
		return new PaymentResponse(
				payment.getId(),
				payment.getAmount(),
				payment.getReference(),
				payment.getCreditCardId(),
				payment.getStatus(),
				payment.getOrderId(),
				payment.getCreatedAt(),
				payment.getUpdatedAt());
	}

	private Long currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AccessToken accessToken)) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, JwtAuthenticationFilter.INVALID_ACCESS_TOKEN);
		}
		return accessToken.userId();
	}
}
