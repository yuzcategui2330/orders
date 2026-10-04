package com.farmatodo.order.services;

import com.farmatodo.order.templates.MailTemplate;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

	private final JavaMailSender mailSender = mock(JavaMailSender.class);
	private final EmailService emailService = new EmailService(mailSender, "from@mail.com");

	@Test
	void sendRendersTheTemplateAndEscapesText() throws Exception {
		MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
		when(mailSender.createMimeMessage()).thenReturn(message);
		Map<String, String> variables = new LinkedHashMap<>();
		variables.put("customer_name", "Ana <b>&\"x\"");
		variables.put("order_number", null);
		variables.put("products", "<tr></tr>");

		emailService.send("to@mail.com", MailTemplate.ORDER_PAID, variables);

		verify(mailSender).send(message);
		String body = message.getContent().toString();
		assertTrue(body.contains("Ana &lt;b&gt;&amp;&quot;x&quot;"));
		assertTrue(body.contains("<tr></tr>"));
		assertTrue(MailTemplate.ORDER_PAID.classpathLocation().endsWith("order-paid.html"));
	}

	@Test
	void sendSwallowsMailFailures() {
		when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("smtp down"));

		assertDoesNotThrow(() -> emailService.send("to@mail.com", MailTemplate.ORDER_PAYMENT_REJECTED, Map.of()));
	}

	@Test
	void sendSwallowsAFailureFromTheTransport() throws Exception {
		MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
		when(mailSender.createMimeMessage()).thenReturn(message);
		doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(MimeMessage.class));

		assertDoesNotThrow(() -> emailService.send("to@mail.com", MailTemplate.ORDER_PAID, Map.of("customer_name", "Ana")));
	}
}
