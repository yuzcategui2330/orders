package com.farmatodo.order.domains;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonProperty("id")
	@Column(name = "id")
	private Long id;

	@JsonProperty("transaction_id")
	@Column(name = "transaction_id", nullable = false)
	private UUID transactionId;

	@JsonProperty("created_at")
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Enumerated(EnumType.STRING)
	@JsonProperty("type")
	@Column(name = "type", nullable = false, length = 40)
	private TransactionType type;

	@Enumerated(EnumType.STRING)
	@JsonProperty("action")
	@Column(name = "action", nullable = false, length = 20)
	private TransactionAction action;

	@Enumerated(EnumType.STRING)
	@JsonProperty("module")
	@Column(name = "module", nullable = false, length = 40)
	private TransactionModule module;

	@JdbcTypeCode(SqlTypes.JSON)
	@JsonProperty("request_payload")
	@Column(name = "request_payload", columnDefinition = "jsonb")
	private String requestPayload;

	@JdbcTypeCode(SqlTypes.JSON)
	@JsonProperty("response_payload")
	@Column(name = "response_payload", columnDefinition = "jsonb")
	private String responsePayload;

	@Enumerated(EnumType.STRING)
	@JsonProperty("status")
	@Column(name = "status", nullable = false, length = 20)
	private TransactionStatus status;

	@JsonProperty("error_message")
	@Column(name = "error_message")
	private String errorMessage;

	public static TransactionLog create(
			UUID transactionId,
			LocalDateTime createdAt,
			TransactionType type,
			TransactionAction action,
			TransactionModule module,
			String requestPayload,
			String responsePayload,
			TransactionStatus status,
			String errorMessage) {
		TransactionLog transactionLog = new TransactionLog();
		transactionLog.transactionId = transactionId;
		transactionLog.createdAt = createdAt;
		transactionLog.type = type;
		transactionLog.action = action;
		transactionLog.module = module;
		transactionLog.requestPayload = requestPayload;
		transactionLog.responsePayload = responsePayload;
		transactionLog.status = status;
		transactionLog.errorMessage = errorMessage;
		return transactionLog;
	}

	@PrePersist
	void onCreate() {
		if (this.createdAt == null) {
			this.createdAt = LocalDateTime.now();
		}
	}
}
