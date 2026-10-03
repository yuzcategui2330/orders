package com.farmatodo.order.templates;

public enum MailTemplate {

	ORDER_PAID("order-paid.html", "Tu pedido ha sido confirmado"),
	ORDER_PAYMENT_REJECTED("order-payment-rejected.html", "Problema con tu pago");

	private final String fileName;
	private final String subject;

	MailTemplate(String fileName, String subject) {
		this.fileName = fileName;
		this.subject = subject;
	}

	public String fileName() {
		return fileName;
	}

	public String subject() {
		return subject;
	}

	public String classpathLocation() {
		return "com/farmatodo/order/templates/" + fileName;
	}
}
