package com.farmatodo.order.domains;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonProperty("id")
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@JsonProperty("token_hash")
	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@JsonProperty("expires_at")
	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Setter
	@JsonProperty("revoked")
	@Column(name = "revoked", nullable = false)
	private Boolean revoked = Boolean.FALSE;

	@JsonProperty("created_at")
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public RefreshToken(User user, String tokenHash, LocalDateTime expiresAt) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
		this.revoked = Boolean.FALSE;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
	}
}
