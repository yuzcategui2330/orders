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
@Table(name = "clients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Client {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonProperty("id")
	@Column(name = "id")
	private Long id;

	@Setter
	@JsonProperty("name")
	@Column(name = "name", nullable = false, length = 80)
	private String name;

	@Setter
	@JsonProperty("last_name")
	@Column(name = "last_name", nullable = false, length = 80)
	private String lastName;

	@Setter
	@JsonProperty("phone")
	@Column(name = "phone", length = 20)
	private String phone;

	@Setter
	@JsonProperty("email")
	@Column(name = "email", nullable = false, length = 120)
	private String email;

	@Setter
	@JsonProperty("user_id")
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Setter
	@JsonProperty("address")
	@Column(name = "address", length = 255)
	private String address;

	@JsonProperty("created_at")
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@JsonProperty("updated_at")
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	public static Client create(String name, String lastName, String phone, String email, String address, Long userId) {
		Client client = new Client();
		client.name = name;
		client.lastName = lastName;
		client.phone = phone;
		client.email = email;
		client.address = address;
		client.userId = userId;
		return client;
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
		if (!(other instanceof Client client)) {
			return false;
		}
		return id != null && id.equals(client.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
