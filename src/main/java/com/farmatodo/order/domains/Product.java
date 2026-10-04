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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonProperty("id")
	@Column(name = "id")
	private Long id;

	@JsonProperty("name")
	@Column(name = "name", nullable = false, length = 120)
	private String name;

	@JsonProperty("short_name")
	@Column(name = "short_name", nullable = false, length = 80)
	private String shortName;

	@JsonProperty("description")
	@Column(name = "description", length = 70)
	private String description;

	@Enumerated(EnumType.STRING)
	@JsonProperty("category")
	@Column(name = "category", nullable = false, length = 32)
	private Category category;

	@JsonProperty("stock_quantity")
	@Column(name = "stock_quantity", nullable = false)
	private Integer stockQuantity;

	@JsonProperty("price")
	@Column(name = "price", nullable = false, precision = 12, scale = 2)
	private BigDecimal price = BigDecimal.ZERO;

	@JsonProperty("reserved_qty")
	@Column(name = "reserved_qty", nullable = false)
	private Integer reservedQty = 0;

	@JsonProperty("created_at")
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public static Product create(String name, String shortName, Category category, int stockQuantity) {
		Product product = new Product();
		product.name = name;
		product.shortName = shortName;
		product.category = category;
		product.stockQuantity = stockQuantity;
		product.price = BigDecimal.ZERO;
		product.reservedQty = 0;
		return product;
	}

	public Product withId(Long id) {
		this.id = id;
		return this;
	}

	public Product priced(BigDecimal price) {
		this.price = price;
		return this;
	}

	public Product described(String description) {
		this.description = description;
		return this;
	}

	public int availableStock() {
		return stockQuantity - reservedQty;
	}

	public void reserve(int quantity) {
		this.reservedQty += quantity;
	}

	public void release(int quantity) {
		this.reservedQty -= quantity;
	}

	public void commitSale(int quantity) {
		this.stockQuantity -= quantity;
		this.reservedQty -= quantity;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof Product product)) {
			return false;
		}
		return id != null && id.equals(product.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
