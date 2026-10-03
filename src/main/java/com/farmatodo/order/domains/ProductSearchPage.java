package com.farmatodo.order.domains;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ProductSearchPage(
		@JsonProperty("products") List<Product> products,
		@JsonProperty("page") int page,
		@JsonProperty("size") int size,
		@JsonProperty("total_elements") long totalElements,
		@JsonProperty("total_pages") int totalPages
) {
}
