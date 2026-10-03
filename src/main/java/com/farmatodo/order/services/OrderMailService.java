package com.farmatodo.order.services;

import com.farmatodo.order.domains.Client;
import com.farmatodo.order.domains.Order;
import com.farmatodo.order.domains.OrderDetail;
import com.farmatodo.order.domains.Product;
import com.farmatodo.order.repositories.ClientRepository;
import com.farmatodo.order.repositories.ProductRepository;
import com.farmatodo.order.templates.MailTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderMailService {

	private static final Logger log = LoggerFactory.getLogger(OrderMailService.class);
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final EmailService emailService;
	private final ClientRepository clientRepository;
	private final ProductRepository productRepository;

	public OrderMailService(EmailService emailService, ClientRepository clientRepository, ProductRepository productRepository) {
		this.emailService = emailService;
		this.clientRepository = clientRepository;
		this.productRepository = productRepository;
	}

	public void notifyPaid(Order order, List<OrderDetail> details) {
		notify(MailTemplate.ORDER_PAID, order, details);
	}

	public void notifyPaymentRejected(Order order, List<OrderDetail> details) {
		notify(MailTemplate.ORDER_PAYMENT_REJECTED, order, details);
	}

	private void notify(MailTemplate template, Order order, List<OrderDetail> details) {
		Client client = clientRepository.findById(order.getClientId()).orElse(null);
		if (client == null || client.getEmail() == null || client.getEmail().isBlank()) {
			log.warn("Order {} has no client email", order.getId());
			return;
		}
		LocalDate date = order.getCreatedAt() == null ? LocalDate.now() : order.getCreatedAt().toLocalDate();
		String address = client.getAddress() == null || client.getAddress().isBlank() ? "Sin direccion" : client.getAddress();
		Map<String, String> variables = new LinkedHashMap<>();
		variables.put("customer_name", client.getName());
		variables.put("order_number", String.valueOf(order.getId()));
		variables.put("order_date", DATE_FORMAT.format(date));
		variables.put("total_amount", order.getAmount().toPlainString());
		variables.put("shipping_address", address);
		variables.put("products", productRows(details));
		emailService.send(client.getEmail(), template, variables);
	}

	private String productRows(List<OrderDetail> details) {
		StringBuilder rows = new StringBuilder();
		for (OrderDetail detail : details) {
			String name = productRepository.findById(detail.getProductId())
					.map(Product::getName)
					.orElse("Producto " + detail.getProductId());
			rows.append("<tr><td style=\"padding:4px 0;\">")
					.append(escape(name))
					.append("</td><td style=\"padding:4px 8px;\">x")
					.append(detail.getQuantity())
					.append("</td><td style=\"padding:4px 0;text-align:right;\">")
					.append(detail.getAmount().toPlainString())
					.append("</td></tr>");
		}
		return rows.toString();
	}

	private String escape(String value) {
		return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;");
	}
}
