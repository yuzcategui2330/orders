package com.farmatodo.order.domains;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonProperty("id")
	@Column(name = "id")
	private Long id;

	@Setter
	@JsonProperty("username")
	@Column(name = "username", nullable = false, length = 25, unique = true)
	private String username;

	@Setter
	@JsonProperty(value = "password", access = JsonProperty.Access.WRITE_ONLY)
	@Column(name = "password", nullable = false, length = 255)
	private String password;

	@JsonProperty("created_at")
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@JsonProperty("updated_at")
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Setter
	@JsonProperty("last_login")
	@Column(name = "last_login")
	private LocalDateTime lastLogin;

	@Setter
	@JsonProperty("is_active")
	@Column(name = "is_active", nullable = false)
	private Boolean isActive = Boolean.TRUE;

	public static User withCredentials(String username, String password) {
		User user = new User();
		user.setUsername(username);
		user.setPassword(password);
		return user;
	}

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof User user)) {
			return false;
		}
		return id != null && id.equals(user.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
