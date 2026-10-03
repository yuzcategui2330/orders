package com.farmatodo.order.services;

import com.farmatodo.order.config.ProductSearchProperties;
import com.farmatodo.order.domains.Product;
import com.farmatodo.order.domains.ProductSearchPage;
import com.farmatodo.order.exceptions.ApiException;
import com.farmatodo.order.repositories.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProductService {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final ProductRepository productRepository;
	private final SearchLogService searchLogService;
	private final ProductSearchProperties productSearchProperties;

	public ProductService(
			ProductRepository productRepository,
			SearchLogService searchLogService,
			ProductSearchProperties productSearchProperties) {
		this.productRepository = productRepository;
		this.searchLogService = searchLogService;
		this.productSearchProperties = productSearchProperties;
	}

	public ProductSearchPage search(String query, Integer page, Integer size) {
		if (query == null || query.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Query is required");
		}
		int pageNumber = page == null ? DEFAULT_PAGE : page;
		int pageSize = size == null ? DEFAULT_SIZE : size;
		if (pageNumber < 0 || pageSize < 1) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid pagination");
		}
		pageSize = Math.min(pageSize, MAX_SIZE);
		Page<Product> result = productRepository.searchByNameOrShortName(
				escapeLike(query.trim()),
				productSearchProperties.minStock(),
				PageRequest.of(pageNumber, pageSize, Sort.by("name")));
		recordSearch(query.trim(), result.getTotalElements());
		return new ProductSearchPage(
				result.getContent(),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages());
	}

	private void recordSearch(String searchTerm, long resultCount) {
		try {
			searchLogService.record(searchTerm, resultCount);
		} catch (RuntimeException exception) {
			log.warn("Search log was not scheduled for term {}", searchTerm, exception);
		}
	}

	private String escapeLike(String query) {
		return query
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
	}
}
