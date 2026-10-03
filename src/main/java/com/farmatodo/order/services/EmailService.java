package com.farmatodo.order.services;

import com.farmatodo.order.templates.MailTemplate;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@Service
public class EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailService.class);
	private static final Set<String> RAW_VARIABLES = Set.of("products");

	private final JavaMailSender mailSender;
	private final String from;

	public EmailService(JavaMailSender mailSender, @Value("${spring.mail.username}") String from) {
		this.mailSender = mailSender;
		this.from = from;
	}

	public void send(String to, MailTemplate template, Map<String, String> variables) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
			helper.setFrom(new InternetAddress(from, "Farmatodo"));
			helper.setTo(to);
			helper.setSubject(template.subject());
			helper.setText(render(template, variables), true);
			mailSender.send(message);
		} catch (Exception exception) {
			log.warn("Email {} was not sent to {}", template.fileName(), to, exception);
		}
	}

	private String render(MailTemplate template, Map<String, String> variables) throws Exception {
		String html = new String(new ClassPathResource(template.classpathLocation()).getContentAsByteArray(), StandardCharsets.UTF_8);
		for (Map.Entry<String, String> variable : variables.entrySet()) {
			String value = variable.getValue() == null ? "" : variable.getValue();
			if (!RAW_VARIABLES.contains(variable.getKey())) {
				value = escape(value);
			}
			html = html.replace("{{" + variable.getKey() + "}}", value);
		}
		return html;
	}

	private String escape(String value) {
		return value
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;");
	}
}
